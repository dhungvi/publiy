package org.msrg.publiy.broker.controller;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;

import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerControllable;
import org.msrg.publiy.broker.IBrokerOpStateListener;

import org.msrg.publiy.node.NodeFactory;
import org.msrg.publiy.node.NodeInstantiationData;
import org.msrg.publiy.pubsub.core.messagequeue.Timestamp;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastAnnotatorFactory;

import org.msrg.publiy.utils.annotations.IAnnotator;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.CasualLoggerEngine;



import org.msrg.publiy.client.publisher.SimpleFilePublisher;
import org.msrg.publiy.client.subscriber.DefaultSubscriptionListener;
import org.msrg.publiy.communication.core.packet.types.TBFTMessageManipulator_FilterPublication;
import org.msrg.publiy.communication.core.packet.types.TCommand;
import org.msrg.publiy.communication.socketbinding.IMessageSender;
import org.msrg.publiy.communication.socketbinding.Message;
import org.msrg.publiy.communication.socketbinding.MessageListener;
import org.msrg.publiy.communication.socketbinding.MessageReceiver;
import org.msrg.publiy.communication.socketbinding.MessageTypes;
import org.msrg.publiy.communication.socketbinding.MessagingFactory;
import org.msrg.publiy.component.ComponentStatus;
import org.msrg.publiy.component.IComponent;
import org.msrg.publiy.component.IComponentListener;

import org.msrg.publiy.broker.controller.message.ControlMessage;
import org.msrg.publiy.broker.controller.message.ControlMessageTypes;
import org.msrg.publiy.broker.controller.message.ErrorMessage;
import org.msrg.publiy.broker.controller.message.ErrorMessageTypes;
import org.msrg.publiy.broker.controller.message.InfoMessage;
import org.msrg.publiy.broker.controller.message.QueryMessage;
import org.msrg.publiy.broker.controller.message.QueryMessageTypes;
import org.msrg.publiy.broker.controller.sequence.IConnectionSequenceUpdateListener;
import org.msrg.publiy.broker.controller.sequence.LocalControllerSequencer;
import org.msrg.publiy.broker.controller.sequence.SequenceUpdateable;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.BrokerInfoComponentStatus;
import org.msrg.publiy.broker.info.BrokerInfoOpState;
import org.msrg.publiy.broker.info.BrokerInfoTypes;
import org.msrg.publiy.broker.info.ExceptionInfo;
import org.msrg.publiy.broker.info.ISessionInfo;
import org.msrg.publiy.broker.info.JoinInfo;
import org.msrg.publiy.broker.info.PSSessionInfo;
import org.msrg.publiy.broker.info.StatisticalInfo;
import org.msrg.publiy.broker.info.SubscriptionInfo;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class BrokerController extends MessageListener implements IConnectionSequenceUpdateListener, IBrokerController, IBrokerOpStateListener, IComponentListener {

	static {
		BrokerInternalTimer.start(false);
	}
	
	//This should be set for each 
	private static int DEFAULT_LOCAL_IP_INDEX = 0;
	static {
		String localIPindexStr = System.getProperty("BrokerController.DEFAULT_LOCAL_IP_INDEX");
		if(localIPindexStr != null && !localIPindexStr.equalsIgnoreCase("") ) {
			DEFAULT_LOCAL_IP_INDEX = new Integer(localIPindexStr).intValue();
		}
	}
	
	private Timestamp _timestamp = new Timestamp(null);
	private static int BROKER_ADDRESSES_SIZE = 1000;
	private final static int DEFAULT_INITAL_PORT;
	static {
		String default_broker_controller_port = System.getProperty("BrokerController.PORT", "11000");
		DEFAULT_INITAL_PORT = new Integer(default_broker_controller_port);
	}
	
	private static Inet4Address[] DEFAULT_INETSCOKET_ADDRESS = org.msrg.publiy.utils.LocalAddressGrabber.grabAll4Addresses(true).toArray(new Inet4Address[0]);
	public static int DEFAULT_CONTROLLER_PORT_OFFSET = 2000;
	public static InetSocketAddress[][] BROKER_BROKER_CONTROLLER_ADDRESSES = getBrokerBrokerControllerAddresses(DEFAULT_LOCAL_IP_INDEX);
	
	private static InetSocketAddress[][] getBrokerBrokerControllerAddresses(int index) {
		InetSocketAddress [][] addresses = new InetSocketAddress[BROKER_ADDRESSES_SIZE][2];
		for ( int i=0 ; i<BROKER_ADDRESSES_SIZE ; i++ )
		{
			int port = DEFAULT_INITAL_PORT + i;
			InetAddress address = DEFAULT_INETSCOKET_ADDRESS[index];
			InetSocketAddress brokerAddress = new InetSocketAddress(address, port);
			InetSocketAddress controllerAddress = getDefaultBrokerControllerAddressForBrokerAddress(brokerAddress);
			addresses[i][0] = brokerAddress;
			addresses[i][1] = controllerAddress;
		}
		
		return addresses;
	}
	
	public static void main(String[] argv) {
		InetSocketAddress[][] addresses = getBrokerBrokerControllerAddresses(DEFAULT_LOCAL_IP_INDEX);
		for ( int i=0 ; i<addresses.length ; i++ )
			System.out.println(addresses[i][0] + " " + addresses[i][1]); //OK
	}
	
	private InetSocketAddress _brokerControllerLocalAddress;
	private InetSocketAddress _brokerLocalAddress;
	private IBrokerControllable _broker = null;
	private ControlMessage<?> _lastExecutedControlMessage = null;
	
	private IMessageSender _messageSender;
	private SequenceUpdateable<IConnectionSequenceUpdateListener> _connectionSequenceUpdate;
	
	private MessageReceiver _messageReceiver;
	private Timer _messageSenderTimer = new Timer("MSender-Timer");

	public static InetSocketAddress getDefaultBrokerControllerAddressForBrokerAddress(InetSocketAddress brokerAddress) {
		InetAddress hostAddress = brokerAddress.getAddress();
		int brokerPort = brokerAddress.getPort();
		int controllerPort = brokerPort + DEFAULT_CONTROLLER_PORT_OFFSET;
		return new InetSocketAddress(hostAddress, controllerPort);
	}

	public BrokerController(InetSocketAddress brokerControllerLocalAddress, InetSocketAddress brokerLocalAddress) {
		super(brokerControllerLocalAddress, "BRKR_CTRL (" + brokerControllerLocalAddress + ", " + brokerLocalAddress + ")");
		_brokerControllerLocalAddress = brokerControllerLocalAddress;
		_brokerLocalAddress = brokerLocalAddress;
		LocalControllerSequencer.init(_brokerControllerLocalAddress);

		_messageReceiver = MessagingFactory.getNewMessageReceiver(this, getListeningPort());
	}

	private void initMessageSenderToBrokerControllerCentreAndSendMessage(Message message) {
		initMessageSenderToBrokerControllerCentreAndSendMessage(message.getTo(), message);
	}
	
	private void initMessageSenderToBrokerControllerCentreAndSendMessage(InetSocketAddress brokerControllerCentreAddress, Message message) {
		if(_messageSender == null ) {
			_messageSender = MessagingFactory.getNewMessageSender(_brokerControllerLocalAddress.toString(), _messageSenderTimer, brokerControllerCentreAddress);
			_messageSender.prepareAndStart();
			_connectionSequenceUpdate = _messageSender.connect();
			_connectionSequenceUpdate.registerUpdateListener(this);
		}
		else if(!_messageSender.canSendTo(brokerControllerCentreAddress) )
			throw new IllegalStateException("MessageSender '" + _messageSender + "' cannot send message to: " + brokerControllerCentreAddress);
		
		_messageSender.sendMessage(message);
	}
	
	@Override
	public void prepareAndStart() {
		this.start();
		_messageReceiver.prepareAndStart();
//		_messageSender.prepareAndStart();
	}

	protected void handleMessage(Message message) {
		if(_timestamp.isDuplicate(message.getSourceInstanceID()) ) {
			LoggerFactory.getLogger().info(this, "Handling DUPLICATE msg3: " + message);
			ComponentStatus componentStatus = _broker.getBrokerComponentState();
			InfoMessage infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_COMPONENT_STATUS, null, _lastExecutedControlMessage.getSourceInstanceID(), _lastExecutedControlMessage.getReplyAddress());
			BrokerInfoComponentStatus brokerInfoComponentStatus = new BrokerInfoComponentStatus(componentStatus);
			infoMessage.loadInfo(brokerInfoComponentStatus);
			infoMessage.minimizeRetries();
			
			initMessageSenderToBrokerControllerCentreAndSendMessage(infoMessage);
			return;
		}

		LoggerFactory.getLogger().info(this, "Handling msg2: " + message);
		MessageTypes mType = message.getMessageType();
		switch(mType) {
		case MESSAGE_TYPE_ACK:
			break;
			
		case MESSAGE_TYPE_CONTROL:
			ControlMessage<?> controlMessage = (ControlMessage<?>)message;
			handleControlMessage(controlMessage);
			break;
			
		case MESSAGE_TYPE_INFO:
			InfoMessage infoMessage = (InfoMessage)message;
			handleInfoMessage(infoMessage);
			break;
			
		case MESSAGE_TYPE_QUERY:
			QueryMessage queryMessage = (QueryMessage) message;
			handleQueryMessage(queryMessage);
			break;
			
		default:
			throw new UnsupportedOperationException("Don't know how to handle [" + mType + "]: " + message);
		}
	}
	
	@Override
	public String getListenerName() {
		return toString();
	}
	
	@Override
	public String toString() {
		return "BRKR_CTRL (" + _brokerControllerLocalAddress + ", " + _brokerLocalAddress + ")";
	}
	
	private void handleInfoMessage(InfoMessage infoMessage) {
		BrokerInfoTypes mInfoType = infoMessage.getInfoType();
		switch(mInfoType) {
		case BROKER_INFO_ERROR:
			ErrorMessage errorMessage = (ErrorMessage) infoMessage;
			handleErrorMessage(errorMessage);
			break;

		default:
			throw new UnsupportedOperationException("This is a BrokerController: it must not receive info messages! :: " + infoMessage);
		}
	}
	
	private void handleErrorMessage(ErrorMessage errorMessage) {
		throw new UnsupportedOperationException("This is a BrokerController: it must not receive error messages! :: " + errorMessage);
	}

	private void handleControlMessage(ControlMessage<?> controlMessage) {
		ControlMessageTypes commandType = controlMessage.getControlType();
		InetSocketAddress brokerControllerCentreReplyAddress = controlMessage.getReplyAddress();//getFrom();
		NodeInstantiationData<?> nodeInstantiationData = controlMessage.getNodeInstantiationData();
		Sequence commandSequence = controlMessage.getSourceInstanceID();
		
		switch( commandType ) {
		case CTRL_MESSAGE_JOIN:
			handleJoinControlMessage(controlMessage, brokerControllerCentreReplyAddress, commandSequence, nodeInstantiationData);
			break;
		
		case CTRL_MESSAGE_PLAY:
			handlePlayControlMessage(controlMessage, brokerControllerCentreReplyAddress, commandSequence, nodeInstantiationData);
			break;
		
		case CTRL_MESSAGE_PAUSE:
			handlePauseControlMessage(controlMessage, brokerControllerCentreReplyAddress, commandSequence, nodeInstantiationData);
			break;

		case CTRL_MESSAGE_SPEED:
			handleSpeedControlMessage(controlMessage, brokerControllerCentreReplyAddress, commandSequence, nodeInstantiationData);
			break;

		case CTRL_MESSAGE_TCOMMAND:
			handleTCommandControlMessage(controlMessage, brokerControllerCentreReplyAddress, commandSequence, nodeInstantiationData);
			break;
			
		case CTRL_MESSAGE_KILL:
			handleKillControlMessage(controlMessage, brokerControllerCentreReplyAddress, commandSequence);
			break;
		
		case CTRL_MESSAGE_RECOVER:
			handleRecoverControlMessage(controlMessage, brokerControllerCentreReplyAddress, commandSequence, nodeInstantiationData);
			break;
		
		case CTRL_MESSAGE_BYE: 
			handleByeControlMessage(controlMessage, brokerControllerCentreReplyAddress, commandSequence);
			break;
		
		case CTRL_MESSAGE_LOAD_PREPARED_SUBS:
			handleLoadPreparedSubsControlMessage(controlMessage, brokerControllerCentreReplyAddress, commandSequence);
			break;
			
		case CTRL_MESSAGE_BFT_MSG_MANIPULATE:
			handleBFTMessageManipulatorControlMessage(controlMessage, brokerControllerCentreReplyAddress, commandSequence);
			break;
			
		default:
			throw new UnsupportedOperationException("Don't know how to handle:: " + controlMessage);
		}
	}
	
	private void handleBFTMessageManipulatorControlMessage(
			ControlMessage<?> bftManipulatorMessage,
			InetSocketAddress brokerControllerCentreReplyAddress,
			Sequence commandSequence) {

		if(_broker == null ) {
			replyWithErrorMessage(brokerControllerCentreReplyAddress, ErrorMessageTypes.ERROR_MESSAGE_NO_BEROKER_CANNOT_KILL, commandSequence, null);
			return;
		}

		TBFTMessageManipulator_FilterPublication bftMessageManipulator = (TBFTMessageManipulator_FilterPublication) bftManipulatorMessage.getAttachment();
		
		boolean result = _broker.installBFTMessageManipulator(bftMessageManipulator);
		if(!result)
			replyWithErrorMessage(brokerControllerCentreReplyAddress, ErrorMessageTypes.ERROR_MESSAGE_OPERATION_FAILED, commandSequence, "Failed to load prepared subs.");			
	}

	private void handleLoadPreparedSubsControlMessage(
			ControlMessage<?> controlMessage,
			InetSocketAddress brokerControllerCentreReplyAddress,
			Sequence commandSequence) {

		if(_broker == null ) {
			replyWithErrorMessage(brokerControllerCentreReplyAddress, ErrorMessageTypes.ERROR_MESSAGE_NO_BEROKER_CANNOT_KILL, commandSequence, null);
			return;
		}

		boolean result = _broker.loadPreparedSubscriptions();
		if(!result)
			replyWithErrorMessage(brokerControllerCentreReplyAddress, ErrorMessageTypes.ERROR_MESSAGE_OPERATION_FAILED, commandSequence, "Failed to load prepared subs.");			
	}

	private void handleSpeedControlMessage(ControlMessage<?> controlMessage, InetSocketAddress brokerControllerCentreAddress,
			Sequence commandSequence, NodeInstantiationData<?> nodeInstantiationData) {
		
		if(_broker == null ) {
			replyWithErrorMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_NO_BEROKER_CANNOT_KILL, commandSequence, null);
			return;
		}
		
		String intervalStr = null;
		try{
			intervalStr = nodeInstantiationData.getProperty(SimpleFilePublisher.PROPERTY_DELAY);
			int interval = new Integer(intervalStr).intValue();
			boolean result = _broker.setSpeedLevel(interval);
			if(result == false )
				replyWithErrorMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_OPERATION_FAILED, commandSequence, "Set speed failed.");
		}catch(Exception x) {
			x.printStackTrace();
			replyWithExceptionMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_UNSUPPORTED_OPERATION, commandSequence, x, "Invalid speed:" + intervalStr);
			return;
		}
		
	}

	private void handlePauseControlMessage(ControlMessage<?> controlMessage, InetSocketAddress brokerControllerCentreAddress,
			Sequence commandSequence, NodeInstantiationData<?> nodeInstantiationData) {
		
		if(_broker == null ) {
			replyWithErrorMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_NO_BEROKER_CANNOT_KILL, commandSequence, null);
			return;
		}
		
		try{
			boolean result = _broker.pause();
			if(result == false )
				replyWithErrorMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_OPERATION_FAILED, commandSequence, "Cannot pause.");
		}catch(Exception x) {
			replyWithExceptionMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_UNSUPPORTED_OPERATION, commandSequence, x, "Cannot pause.");
			return;
		}
	}

	private void handlePlayControlMessage(ControlMessage<?> controlMessage, InetSocketAddress brokerControllerCentreAddress,
			Sequence commandSequence, NodeInstantiationData<?> nodeInstantiationData) {
		
		if(_broker == null ) {
			replyWithErrorMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_NO_BEROKER_CANNOT_KILL, commandSequence, null);
			return;
		}
		
		try{
			boolean result = _broker.play();
			if(result == false )
				replyWithErrorMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_OPERATION_FAILED, commandSequence, "Cannot play.");
		}catch(Exception x) {
			replyWithExceptionMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_UNSUPPORTED_OPERATION, commandSequence, x, "Cannot play.");
			return;
		}		
	}

	private void replyWithErrorMessage(InetSocketAddress brokerControllerCentreAddress, ErrorMessageTypes errorType, Sequence commandSequence, String comment) {
		ErrorMessage errorMessage = new ErrorMessage(brokerControllerCentreAddress, errorType, commandSequence, comment);
		initMessageSenderToBrokerControllerCentreAndSendMessage(errorMessage);
	}
	
	private void replyWithExceptionMessage(InetSocketAddress brokerControllerCentreAddress, ErrorMessageTypes errorType, Sequence commandSequence, Exception exception, String comment) {
		ErrorMessage exceptionMessage = new ErrorMessage(brokerControllerCentreAddress, errorType, commandSequence, comment);
		exceptionMessage.loadException(exception);
		initMessageSenderToBrokerControllerCentreAndSendMessage(exceptionMessage);
	}

	private void handleRecoverControlMessage(ControlMessage<?> controlMessage,
			InetSocketAddress brokerControllerCentreAddress,
			Sequence commandSequence,
			NodeInstantiationData<?> nodeInstantiationData) {
		if(_broker != null ) {
			replyWithErrorMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_BROKER_EXITS_CANNOT_RECOVER,
					commandSequence, null);
			return;
		}
		
		if(nodeInstantiationData == null || !nodeInstantiationData.validate(_brokerLocalAddress, BrokerOpState.BRKR_RECOVERY) ) {
			replyWithErrorMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_WRONG_ARG_FORMAT, commandSequence, null);
			return;
		}
		
		try{
			_lastExecutedControlMessage = controlMessage;
			_broker = NodeFactory.createNewNodeInstance(nodeInstantiationData, this, this);
			if(_broker != null ) {
				BrokerOpState opState = _broker.getBrokerOpStatus();
				InfoMessage infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_OP_STATUS, null, controlMessage.getSourceInstanceID(), brokerControllerCentreAddress);
				BrokerInfoOpState brokerInfoOpState = new BrokerInfoOpState(opState);
				infoMessage.loadInfo(brokerInfoOpState);
				initMessageSenderToBrokerControllerCentreAndSendMessage(infoMessage);
				
				_broker.prepareToStart();
			}
			else
				throw new IllegalStateException("NodeFactory created a null broker while handling the controlMessage: " + controlMessage);
		}catch(Exception creationEx) {
			_lastExecutedControlMessage = null;
			_broker = null;
			replyWithExceptionMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_EXCEPTION_WHILE_EXECUTING_CONTROL_MESSAGE, commandSequence, creationEx, null);
			return;
		}
	}

	private void handleByeControlMessage(ControlMessage<?> controlMessage,
			InetSocketAddress brokerControllerCentreAddress,
			Sequence commandSequence) {
		
		LoggerFactory.getLogger().info(this, "Closing application at: " + new SimpleDateFormat().format(new Date()));
		
//		TrafficLogger trafficLogger = TrafficLogger.getExistingTrafficLoggerInstance();
//		if(trafficLogger != null )
//			trafficLogger.forceFlush();
		
		if(_broker!=null) {
			CasualLoggerEngine casualLoggerEngine = _broker.getCasualLoggerEngine();
			if(casualLoggerEngine != null )
				casualLoggerEngine.stopComponent();
			
			IAnnotator annotator = TMulticastAnnotatorFactory.getTMulticast_Annotator();
			if(annotator!= null )
				annotator.forceFlush();
		
			_broker.stopComponent();
		
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.exit(1);
	}
	
	private void handleKillControlMessage(ControlMessage<?> controlMessage,
			InetSocketAddress brokerControllerCentreAddress,
			Sequence commandSequence) {
		if(_broker == null ) {
			replyWithErrorMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_NO_BEROKER_CANNOT_KILL, commandSequence, null);
			return;
		}
		
		try{
			_lastExecutedControlMessage = controlMessage;
			_broker.stopComponent();
			DefaultSubscriptionListener.discardInstance();
		}catch(Exception creationEx) {
			_lastExecutedControlMessage = null;
			replyWithExceptionMessage(brokerControllerCentreAddress, ErrorMessageTypes.ERROR_MESSAGE_EXCEPTION_WHILE_EXECUTING_CONTROL_MESSAGE,
					commandSequence, creationEx, null);
			return;
		}
	}

	private void handleTCommandControlMessage(ControlMessage<?> controlMessage, 
			InetSocketAddress brokerControllerCentreAddress, 
			Sequence commandSequence, 
			NodeInstantiationData<?> nodeInstantiationData) {
		
		TCommand tCommand = (TCommand) controlMessage.getAttachment();
		_broker.handleTCommandMessage(tCommand);
	}
	
	private void handleJoinControlMessage(ControlMessage<?> controlMessage, 
			InetSocketAddress brokerControllerCentreReplyAddress, 
			Sequence commandSequence, 
			NodeInstantiationData<?> nodeInstantiationData) {
		if(_broker != null ) {
			replyWithErrorMessage(brokerControllerCentreReplyAddress, 
					ErrorMessageTypes.ERROR_MESSAGE_BROKER_EXITS_CANNOT_JOIN,
					commandSequence, null);
			return;
		}
		
		if(nodeInstantiationData == null || !nodeInstantiationData.validate(_brokerLocalAddress, BrokerOpState.BRKR_PUBSUB_JOIN)) {
			replyWithErrorMessage(brokerControllerCentreReplyAddress,
					ErrorMessageTypes.ERROR_MESSAGE_WRONG_ARG_FORMAT,
					commandSequence, null);
			return;
		}
		
		try{
			_lastExecutedControlMessage = controlMessage;
			_broker = NodeFactory.createNewNodeInstance(nodeInstantiationData, this, this);
			if(_broker != null ) {
				BrokerOpState opState = _broker.getBrokerOpStatus();
				InfoMessage infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_OP_STATUS, null, controlMessage.getSourceInstanceID(), brokerControllerCentreReplyAddress);
				BrokerInfoOpState brokerInfoOpState = new BrokerInfoOpState(opState);
				infoMessage.loadInfo(brokerInfoOpState);
				initMessageSenderToBrokerControllerCentreAndSendMessage(infoMessage);
				
				_broker.prepareToStart();
			}
			else
				throw new IllegalStateException("NodeFactory created a null broker while handing controlmessage: " + controlMessage);
		}catch(Exception creationEx) {
			_lastExecutedControlMessage = null;
			_broker = null;
			replyWithExceptionMessage(brokerControllerCentreReplyAddress, ErrorMessageTypes.ERROR_MESSAGE_EXCEPTION_WHILE_EXECUTING_CONTROL_MESSAGE,
					commandSequence, creationEx, null);
			return;
		}
	}
	
	private void handleQueryMessage(QueryMessage qMessage) {
		QueryMessageTypes queryType = qMessage.getQueryType();
		InetSocketAddress brokerControllerCentreReplyAddress = qMessage.getReplyAddress();//qMessage.getFrom();
		Sequence reqSeq = qMessage.getSourceInstanceID();
		InfoMessage infoMessage = null;

		if(_broker == null ) {
			replyWithErrorMessage(brokerControllerCentreReplyAddress, ErrorMessageTypes.ERROR_MESSAGE_NO_BEROKER_CANNOT_QUERY,
					reqSeq, null);
			return;
		}
		
		switch(queryType) {
		case QUERY_MESSAGE_EXCEPTIONS:
		{
			ExceptionInfo[] exceptionInfos = _broker.getExceptionInfos();
			infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_EXCEPTIONS, null, reqSeq, brokerControllerCentreReplyAddress);
			infoMessage.loadInfos(exceptionInfos);
			break;
		}
		
		case QUERY_MESSAGE_COMPONENTSTATE:
		{
			ComponentStatus componentState = _broker.getBrokerComponentState();
			infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_COMPONENT_STATUS, null, reqSeq, brokerControllerCentreReplyAddress);
			BrokerInfoComponentStatus brokerInfoComponentStatus = new BrokerInfoComponentStatus(componentState);
			infoMessage.loadInfo(brokerInfoComponentStatus);
			break;
		}
		
		case QUERY_MESSAGE_ISESSIONS:
		{
			ISessionInfo[] sessionInfos = _broker.getSessions();
			infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_SESSIONS, null, reqSeq, brokerControllerCentreReplyAddress);
			infoMessage.loadInfos(sessionInfos);
			break;
		}
		
		case QUERY_MESSAGE_OPSTATE:
		{
			BrokerOpState opState = _broker.getBrokerOpStatus();
			infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_OP_STATUS, null, reqSeq, brokerControllerCentreReplyAddress);
			BrokerInfoOpState brokerInfoOpState = new BrokerInfoOpState(opState);
			infoMessage.loadInfo(brokerInfoOpState);
			break;
		}
		
		case QUERY_MESSAGE_PSSESSIONS:
		{
			PSSessionInfo[] psSessionInfos = _broker.getPSSessions();
			infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_PS_SESSIONS, null, reqSeq, brokerControllerCentreReplyAddress);
			infoMessage.loadInfos(psSessionInfos);
			break;
		}
		
		case QUERY_MESSAGE_STATISTICS:
		{
			StatisticalInfo[] statInfos = _broker.getBrokerStatisticalInfos();
			infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_STATISTICS, null, reqSeq, brokerControllerCentreReplyAddress);
			infoMessage.loadInfos(statInfos);
			break;
		}
		
		case QUERY_MESSAGE_SUBSCRIPTIONS:
		{
			SubscriptionInfo[] subscriptionInfos = _broker.getSubscriptionInfos();
			infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_SUBSCRIPTIONS, null, reqSeq, brokerControllerCentreReplyAddress);
			infoMessage.loadInfos(subscriptionInfos);
			break;
		}
		
		case QUERY_MESSAGE_TOPOLOGY_LINKS:
		{
			JoinInfo[] joinInfos = _broker.getTopologyLinks();
			infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_JOINS, null, reqSeq, brokerControllerCentreReplyAddress);
			infoMessage.loadInfos(joinInfos);
			break;
		}
		
		default:
			throw new UnsupportedOperationException("Don't know how to hanld: " + qMessage);
		}
		
		if(infoMessage != null )
			initMessageSenderToBrokerControllerCentreAndSendMessage(infoMessage);
		
	}

	@Override
	public void brokerOpStateChanged(IBroker broker) {
		ControlMessage<?> lastExecutedControlMessage = _lastExecutedControlMessage;
		if(broker != _broker )
			return;
		
		if(lastExecutedControlMessage == null )
			return;
		BrokerOpState opState = _broker.getBrokerOpStatus();
		InfoMessage infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_OP_STATUS, null, lastExecutedControlMessage.getSourceInstanceID(), lastExecutedControlMessage.getReplyAddress());
		BrokerInfoOpState brokerInfoOpState = new BrokerInfoOpState(opState);
		infoMessage.loadInfo(brokerInfoOpState);
		initMessageSenderToBrokerControllerCentreAndSendMessage(infoMessage);
		
		if(_lastExecutedControlMessage.getControlType() == ControlMessageTypes.CTRL_MESSAGE_KILL &&
				_broker.getBrokerOpStatus() == BrokerOpState.BRKR_END )
		{
			_broker = null;
		}
	}

	@Override
	public void componentStateChanged(IComponent component) {
		ControlMessage<?> lastExecutedControlMessage = _lastExecutedControlMessage;
		if(component != _broker )
			return;
		
		if(lastExecutedControlMessage == null )
			return;
		
		ComponentStatus componentStatus = _broker.getBrokerComponentState();
		InfoMessage infoMessage = new InfoMessage(BrokerInfoTypes.BROKER_INFO_COMPONENT_STATUS, null, lastExecutedControlMessage.getSourceInstanceID(), lastExecutedControlMessage.getReplyAddress());
		BrokerInfoComponentStatus brokerInfoComponentStatus = new BrokerInfoComponentStatus(componentStatus);
		infoMessage.loadInfo(brokerInfoComponentStatus);
		initMessageSenderToBrokerControllerCentreAndSendMessage(infoMessage);
		
		if(_lastExecutedControlMessage.getControlType() == ControlMessageTypes.CTRL_MESSAGE_KILL &&
				_broker.getBrokerOpStatus() == BrokerOpState.BRKR_END )
		{
			_broker = null;
			_lastExecutedControlMessage = null;
		}
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_BROKER_CONTROLLER;
	}

	@Override
	public void sequenceUpdated(SequenceUpdateable<?> sequence) {
		LoggerFactory.getLogger().debug(this, "Status of connection to brokerControllerCentre " + sequence.getSequenceUpdateStatus() + "."); 
	}

}
