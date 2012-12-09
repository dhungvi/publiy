package org.msrg.publiy.broker.controllercentre;


import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.PubForwardingStrategy;


import org.msrg.publiy.node.NodeInstantiationData;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.messagequeue.Timestamp;

import org.msrg.publiy.utils.LocalAddressGrabber;
import org.msrg.publiy.utils.PropertyGrabber;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;


import org.msrg.publiy.client.publisher.SimpleFilePublisher;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.TCommand;
import org.msrg.publiy.communication.socketbinding.IMessageSender;
import org.msrg.publiy.communication.socketbinding.Message;
import org.msrg.publiy.communication.socketbinding.MessageListener;
import org.msrg.publiy.communication.socketbinding.MessageReceiver;
import org.msrg.publiy.communication.socketbinding.MessagingFactory;
import org.msrg.publiy.gui.failuretimeline.ExtendedFailureTimelineEvents;

import org.msrg.publiy.broker.controller.BrokerController;
//import org.msrg.publiy.broker.controller.message.AckMessage;
import org.msrg.publiy.broker.controller.message.ControlMessage;
import org.msrg.publiy.broker.controller.message.ControlMessageTypes;
import org.msrg.publiy.broker.controller.message.ErrorMessage;
import org.msrg.publiy.broker.controller.message.InfoMessage;
import org.msrg.publiy.broker.controller.message.QueryMessage;
import org.msrg.publiy.broker.controller.message.QueryMessageTypes;
import org.msrg.publiy.broker.controller.sequence.IConnectionSequenceUpdateListener;
import org.msrg.publiy.broker.controller.sequence.IMessageSequenceUpdateListener;
import org.msrg.publiy.broker.controller.sequence.LocalControllerSequencer;
import org.msrg.publiy.broker.controller.sequence.SequenceUpdateable;
import org.msrg.publiy.broker.controllercentre.cache.BrokerRemoteCache;
import org.msrg.publiy.broker.controllercentre.cache.CachedBrokerInfo;
import org.msrg.publiy.broker.controllercentre.cache.IBrokerRemoteCache;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.BrokerInfoComponentStatus;
import org.msrg.publiy.broker.info.BrokerInfoOpState;
import org.msrg.publiy.broker.info.BrokerInfoTypes;
import org.msrg.publiy.broker.info.ExceptionInfo;
import org.msrg.publiy.broker.info.IBrokerInfo;
import org.msrg.publiy.broker.info.ISessionInfo;
import org.msrg.publiy.broker.info.JoinInfo;
import org.msrg.publiy.broker.info.PSSessionInfo;
import org.msrg.publiy.broker.info.SubscriptionInfo;

public class BrokerControllerCentreImp extends MessageListener implements IBrokerControllerCentre, ActionListener {
	
	public static final int INIT_PORT;
	static {
		String portInit = System.getProperty("BrokerControllerCenter.INITPORT", "10000");
		INIT_PORT = new Integer(portInit);
	}

	public Map<InetSocketAddress, InetSocketAddress> _broker_brokerController_address_mapping = 
		new HashMap<InetSocketAddress, InetSocketAddress>();
	public Map<InetSocketAddress, InetSocketAddress> _brokerController_broker_address_mapping = 
		new HashMap<InetSocketAddress, InetSocketAddress>();
	
	private LocalControllerSequencer _localControllerSequencer;
	private Timer _messageSenderTimer = new Timer("MSender-Timer");
	private Map<InetSocketAddress, IMessageSender> _messageSenders = new HashMap<InetSocketAddress, IMessageSender>();
	private Map<InetSocketAddress, MessageReceiver> _messageReceivers = new HashMap<InetSocketAddress, MessageReceiver>();
	
//	private MessageReceiver _messageReceiver;
	private Timestamp _timestamp = new Timestamp(null);

	// All ``broker controller related'' synchronization takes place on this object.
	private Map<InetSocketAddress, IBrokerRemoteCache> _controllableBrokers = new HashMap<InetSocketAddress, IBrokerRemoteCache>();
//	private Map<InetSocketAddress, IBrokerRemoteCache> _controllableBrokerControllers = new HashMap<InetSocketAddress, IBrokerRemoteCache>();

	// caution: the below can cause memory leak; for that we use the next two lines.
	private Map<Sequence, IBrokerRemoteQuerier> _brokerQuerierRequestSequences = new HashMap<Sequence, IBrokerRemoteQuerier>();
	private Set<Sequence> _outRequestSequenceSetOld = new HashSet<Sequence>();
	private Set<Sequence> _outRequestSequenceSetNew = new HashSet<Sequence>();
	
	private Set<InetSocketAddress> _usedAddresses = new HashSet<InetSocketAddress>();
	
	void discardOldRequestSequenceSet() {
		synchronized (_brokerQuerierRequestSequences) {
			Iterator<Sequence> reqSeqIt = _outRequestSequenceSetOld.iterator();
			while ( reqSeqIt.hasNext())
			{
				Sequence reqSeq = reqSeqIt.next();
				reqSeqIt.remove();
				_brokerQuerierRequestSequences.remove(reqSeq);
			}
			
			_outRequestSequenceSetOld = _outRequestSequenceSetNew;
			_outRequestSequenceSetNew = new HashSet<Sequence>();
		}
	}
	
	public BrokerControllerCentreImp(InetSocketAddress localAddress) {
		super(localAddress, "BRKR_CTRL_CNTR (" + localAddress + ")");
		LocalControllerSequencer.init(localAddress);
		_localControllerSequencer = LocalControllerSequencer.getLocalControllerSequencer();
	}
	
	public void prepareAndStart() {
//		_messageReceiver.prepareAndStart();
//		_messageSender.prepareAndStart();
		this.start();
	}

	private int _nextAllocatePort = 10000;
	@Override
	public boolean addBroker(InetSocketAddress brokerAddress, InetSocketAddress brokerControllerAddress) {
		synchronized (_controllableBrokers) {
			if(_usedAddresses.contains(brokerAddress) || _usedAddresses.contains(brokerControllerAddress))
				return false;
			
			_usedAddresses.add(brokerAddress);
			_usedAddresses.add(brokerControllerAddress);

			_brokerController_broker_address_mapping.put(brokerControllerAddress, brokerAddress);
			_broker_brokerController_address_mapping.put(brokerAddress, brokerControllerAddress);
			
			IBrokerRemoteCache brokerRemoteCache = new BrokerRemoteCache(brokerAddress, brokerControllerAddress);
			_controllableBrokers.put(brokerAddress, brokerRemoteCache);
			
			_nextAllocatePort ++;
			InetSocketAddress redirectionAddress = new InetSocketAddress(_localControllerSequencer.getLocalAddress().getAddress(), _nextAllocatePort);
			
			IMessageSender prevMessageSender = _messageSenders.get(brokerControllerAddress);
			if(prevMessageSender == null) {
				IMessageSender messageSender = MessagingFactory.getNewMessageSender("MSenderTo-" + brokerControllerAddress, _messageSenderTimer, brokerControllerAddress);
				messageSender.enableRedirectedReplies(redirectionAddress);
				messageSender.prepareAndStart();
				_messageSenders.put(brokerControllerAddress, messageSender);
			}
			
			MessageReceiver prevMessageReceiver = _messageReceivers.get(brokerControllerAddress);
			if(prevMessageReceiver == null) {
				MessageReceiver messageReceiver = MessagingFactory.getNewMessageReceiver(this, redirectionAddress.getPort());
				messageReceiver.prepareAndStart();
				_messageReceivers.put(brokerControllerAddress, messageReceiver);
			}
		}
		return true;
	}
	
	@Override
	public SequenceUpdateable<IConnectionSequenceUpdateListener> connect(InetSocketAddress brokerControllerAddress) {
		IMessageSender mSender = _messageSenders.get(brokerControllerAddress);
		SequenceUpdateable<IConnectionSequenceUpdateListener> connectionUpdateSequence = mSender.connect();
		return connectionUpdateSequence;
	}

	@Override
	public boolean addBroker(InetSocketAddress brokerAddress) {
		InetSocketAddress brokerControllerAddress = createBrokerControllerAddress(brokerAddress);
		return addBroker(brokerAddress, brokerControllerAddress);
	}
	
	@Override
	public InetSocketAddress createBrokerControllerAddress(
			InetSocketAddress brokerAddress) {
		InetSocketAddress existingBrokerControllerAddress = getBrokerControllerAddress(brokerAddress);
		if(existingBrokerControllerAddress != null)
			return existingBrokerControllerAddress;
		
		InetSocketAddress newBrokerControllerAddress = BrokerController.getDefaultBrokerControllerAddressForBrokerAddress(brokerAddress);
		_broker_brokerController_address_mapping.put(brokerAddress, newBrokerControllerAddress);
		_brokerController_broker_address_mapping.put(newBrokerControllerAddress, brokerAddress);
		return newBrokerControllerAddress;
	}
	
	private InetSocketAddress getBrokerAddress(InetSocketAddress brokerControllerAddress) {
		return _brokerController_broker_address_mapping.get(brokerControllerAddress);
	}
	
	@Override
	public InetSocketAddress getBrokerControllerAddress(
			InetSocketAddress brokerAddress) {
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return null;
		
		else
			return brokerRemoteCache.getBrokerControllerAddress();
	}

	@Override
	public CachedBrokerInfo<PSSessionInfo> getCachedPSSessions(
			InetSocketAddress brokerAddress) {
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return null;
		
		else
			return brokerRemoteCache.getCachedPSSessions();
	}

	@Override
	public CachedBrokerInfo<ISessionInfo> getCachedSessions(
			InetSocketAddress brokerAddress) {
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return null;
		
		else
			return brokerRemoteCache.getCachedSessions();
	}

	@Override
	public CachedBrokerInfo<BrokerInfoComponentStatus> getCachedComponentState(
			InetSocketAddress brokerAddress) {
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return null;
		
		else
			return brokerRemoteCache.getCachedComponentState();
	}
	
	@Override
	public CachedBrokerInfo<BrokerInfoOpState> getCachedOpState(
			InetSocketAddress brokerAddress) {
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return null;
		
		else
			return brokerRemoteCache.getCachedOpState();
	}

	@Override
	public CachedBrokerInfo<SubscriptionInfo> getCachedSubscriptions(
			InetSocketAddress brokerAddress) {
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return null;
		
		else
			return brokerRemoteCache.getCachedSubscriptions();
	}

	@Override
	public CachedBrokerInfo<JoinInfo> getCachedTopologyLinks(
			InetSocketAddress brokerAddress) {
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return null;
		
		else
			return brokerRemoteCache.getCachedTopologyLinks();
	}
	
	@Override
	public CachedBrokerInfo<ExceptionInfo>[] getCachedExceptionInfos(
			InetSocketAddress brokerAddress) {
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return null;
		
		else
			return brokerRemoteCache.getCachedExceptions();
	}
	
	@Override
	public Sequence getPSSessions(InetSocketAddress brokerAddress,
			IBrokerRemoteQuerier brokerQuerier) {
		return sendQueryMessage(QueryMessageTypes.QUERY_MESSAGE_PSSESSIONS, brokerAddress, brokerQuerier);
	}

	@Override
	public Sequence getExceptionInfos(InetSocketAddress brokerAddress,
			IBrokerRemoteQuerier brokerQuerier) {
		return sendQueryMessage(QueryMessageTypes.QUERY_MESSAGE_EXCEPTIONS, brokerAddress, brokerQuerier);
	}
	
	@Override
	public Sequence getSessions(InetSocketAddress brokerAddress,
			IBrokerRemoteQuerier brokerQuerier) {
		
		return sendQueryMessage(QueryMessageTypes.QUERY_MESSAGE_ISESSIONS, brokerAddress, brokerQuerier);
	}

	@Override
	public Sequence getOpState(InetSocketAddress brokerAddress,
			IBrokerRemoteQuerier brokerQuerier) {
		return sendQueryMessage(QueryMessageTypes.QUERY_MESSAGE_OPSTATE, brokerAddress, brokerQuerier);
	}

	@Override
	public Sequence getComponentState(InetSocketAddress brokerAddress,
			IBrokerRemoteQuerier brokerQuerier) {
		return sendQueryMessage(QueryMessageTypes.QUERY_MESSAGE_COMPONENTSTATE, brokerAddress, brokerQuerier);
	}

	@Override
	public Sequence getSubscriptions(InetSocketAddress brokerAddress,
			IBrokerRemoteQuerier brokerQuerier) {
		return sendQueryMessage(QueryMessageTypes.QUERY_MESSAGE_SUBSCRIPTIONS, brokerAddress, brokerQuerier);
	}

	@Override
	public Sequence getTopologyLinks(InetSocketAddress brokerAddress,
			IBrokerRemoteQuerier brokerQuerier) {
		return sendQueryMessage(QueryMessageTypes.QUERY_MESSAGE_TOPOLOGY_LINKS, brokerAddress, brokerQuerier);
	}
	
	private PubForwardingStrategy overrideBrokerForwardingStrategy(PubForwardingStrategy brokerForwardingStrategy, Properties arguments) {
		PubForwardingStrategy specifiedBrokerStrategy = PropertyGrabber.getStrategy(arguments);
		PubForwardingStrategy retStrategy = specifiedBrokerStrategy==null ?
				brokerForwardingStrategy : specifiedBrokerStrategy;
		return retStrategy;
	}

	@Override
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueJoin(NodeTypes nodeType, InetSocketAddress brokerAddress, 
			InetSocketAddress brokerJoinPointAddress, PubForwardingStrategy brokerForwardingStrategy,
			IBrokerRemoteQuerier brokerQuerier, Properties arguments) {
		brokerForwardingStrategy = overrideBrokerForwardingStrategy(brokerForwardingStrategy, arguments);
		NodeInstantiationData<?> nodeInitiationData = new NodeInstantiationData(nodeType.getXClass(), brokerAddress, BrokerOpState.BRKR_PUBSUB_JOIN, brokerJoinPointAddress, brokerForwardingStrategy, arguments);
		SequenceUpdateable<IMessageSequenceUpdateListener> sequence = sendControlMessage(ControlMessageTypes.CTRL_MESSAGE_JOIN, brokerAddress, nodeInitiationData, brokerQuerier);
		
		sequence.attachComment("issueJoin_" + brokerAddress); 
		return sequence;
	}

	@Override
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueKill(InetSocketAddress brokerAddress,
			IBrokerRemoteQuerier brokerQuerier) {
		NodeInstantiationData<?> nodeInitiationData = null;
		SequenceUpdateable<IMessageSequenceUpdateListener> sequence = sendControlMessage(ControlMessageTypes.CTRL_MESSAGE_KILL, brokerAddress, nodeInitiationData, brokerQuerier);

		sequence.attachComment("issueKill_" + brokerAddress); 
		return sequence;
	}
	
	@Override
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueLoadPreparedSubs(InetSocketAddress brokerAddress,
			IBrokerRemoteQuerier brokerQuerier) {
		NodeInstantiationData<?> nodeInitiationData = null;
		SequenceUpdateable<IMessageSequenceUpdateListener> sequence =
			sendControlMessage(ControlMessageTypes.CTRL_MESSAGE_LOAD_PREPARED_SUBS, brokerAddress, nodeInitiationData, brokerQuerier);
		
		sequence.attachComment("issueLoadPrepareSubs_" + brokerAddress); 
		return sequence;
	}

	@Override
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueBye(InetSocketAddress brokerAddress,
			IBrokerRemoteQuerier brokerQuerier) {
		NodeInstantiationData<?> nodeInitiationData = null;
		SequenceUpdateable<IMessageSequenceUpdateListener> sequence =
			sendControlMessage(ControlMessageTypes.CTRL_MESSAGE_BYE, brokerAddress, nodeInitiationData, brokerQuerier);
		
		sequence.attachComment("issueBye_" + brokerAddress); 
		return sequence;
	}

	@Override
	public SequenceUpdateable<IMessageSequenceUpdateListener> issuePause(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier) {
		NodeInstantiationData<?> nodeInitiationData = null;
		SequenceUpdateable<IMessageSequenceUpdateListener> sequence =
			sendControlMessage(ControlMessageTypes.CTRL_MESSAGE_PAUSE, brokerAddress, nodeInitiationData, brokerQuerier);
		
		sequence.attachComment("issuePause_" + brokerAddress); 
		return sequence;
	}

	@Override
	public SequenceUpdateable<IMessageSequenceUpdateListener> issuePlay(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier) {
		NodeInstantiationData<?> nodeInitiationData = null;
		SequenceUpdateable<IMessageSequenceUpdateListener> sequence =
			sendControlMessage(ControlMessageTypes.CTRL_MESSAGE_PLAY, brokerAddress, nodeInitiationData, brokerQuerier);
		
		sequence.attachComment("issuePlay_" + brokerAddress); 
		return sequence;
	}

	@Override
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueSpeedCommand(InetSocketAddress brokerAddress, int delay, IBrokerRemoteQuerier brokerQuerier) {
		Properties props = new Properties();
		try{
		props.load(new StringReader(SimpleFilePublisher.PROPERTY_DELAY+"="+delay));
		}catch(IOException iox) {iox.printStackTrace(); return null;}
		
		NodeInstantiationData<?> nodeInitiationData = new NodeInstantiationData(null, null, null, null, null, props);
		SequenceUpdateable<IMessageSequenceUpdateListener> sequence = sendControlMessage(ControlMessageTypes.CTRL_MESSAGE_SPEED, brokerAddress, nodeInitiationData, brokerQuerier);
		
		sequence.attachComment("issueSpeed_" + delay + "_" + brokerAddress); 
		return sequence;
	}
	
	@Override
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueRecover(NodeTypes nodeType, InetSocketAddress brokerAddress,
			PubForwardingStrategy brokerForwardingStrategy, IBrokerRemoteQuerier brokerQuerier, Properties arguments) {
		brokerForwardingStrategy = overrideBrokerForwardingStrategy(brokerForwardingStrategy, arguments);
		NodeInstantiationData<?> nodeInitiationData = new NodeInstantiationData(nodeType.getXClass(), brokerAddress, BrokerOpState.BRKR_RECOVERY, null, brokerForwardingStrategy, arguments);
		return sendControlMessage(ControlMessageTypes.CTRL_MESSAGE_RECOVER, brokerAddress, nodeInitiationData, brokerQuerier);
	}

	@Override
	public String getListenerName() {
		return toString();
	}

	protected void handleMessage(Message message) {
		
//		InetSocketAddress senderAddress = message.getFrom();
//		IMessageSender sender = _messageSenders.get(senderAddress);
//		AckMessage ack = AckMessage.createAckMessage(message);
//		if(ack != null && sender!=null) {
//			sender.sendMessage(ack);
//		}
//		if(AckMessage.class.isInstance(message) && sender!=null)
//			sender.messageConfirmed((AckMessage)message);
			
		if(_timestamp.isDuplicate(message.getSourceInstanceID())) {
			return;
		}

		if(ControlMessage.class.isInstance(message))
		{
			ControlMessage<?> controlMessage = (ControlMessage<?>)message;
			handleControlMessage(controlMessage);
		}
		
		else if(InfoMessage.class.isInstance(message))
		{
			InfoMessage infoMessage = (InfoMessage)message;
			handleInfoMessage(infoMessage);
		}
		
		else if(QueryMessage.class.isInstance(message))
		{
			QueryMessage queryMessage = (QueryMessage) message;
			handleQueryMessage(queryMessage);
		}
		
		else if(ErrorMessage.class.isInstance(message))
		{
			ErrorMessage errorMessage = (ErrorMessage) message;
			handleErrorMessage(errorMessage);
		}

		else
			throw new UnsupportedOperationException("Don't know how to handle: " + message);

	}
	
	private Sequence sendQueryMessage(QueryMessageTypes queryType, InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier) {
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return null;
		
		InetSocketAddress brokerContollerAddress = brokerRemoteCache.getBrokerControllerAddress();
		QueryMessage qMessage = new QueryMessage(brokerContollerAddress, queryType, null);
		IMessageSender messageSender = _messageSenders.get(brokerContollerAddress);
		if(messageSender == null)
			return null;
		messageSender.sendMessage(qMessage);
		
		Sequence reqSeq = qMessage.getSourceInstanceID();
		synchronized (_brokerQuerierRequestSequences) {
			_brokerQuerierRequestSequences.put(reqSeq, brokerQuerier);
		}
		
		return reqSeq;
	}
	
	private SequenceUpdateable<IMessageSequenceUpdateListener> sendControlMessage(ControlMessageTypes controlType, TCommand tCommand, InetSocketAddress brokerAddress, NodeInstantiationData<?> nodeInitiationData, String argument, IBrokerRemoteQuerier brokerQuerier) {
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return null;
		
		InetSocketAddress brokerContollerAddress = brokerRemoteCache.getBrokerControllerAddress();
		ControlMessage<?> controlMessage = new ControlMessage(brokerContollerAddress, controlType, nodeInitiationData, tCommand, argument);
		IMessageSender messageSender = _messageSenders.get(brokerContollerAddress);
		if(messageSender == null)
			return null;
		messageSender.sendMessage(controlMessage);
		
		SequenceUpdateable<IMessageSequenceUpdateListener> reqSeq = controlMessage.getSourceInstanceID();
		synchronized (_brokerQuerierRequestSequences) {
			_brokerQuerierRequestSequences.put(reqSeq, brokerQuerier);
		}
		
		return reqSeq;
	}
	
	private SequenceUpdateable<IMessageSequenceUpdateListener> sendControlMessage(ControlMessageTypes controlType, InetSocketAddress brokerAddress, NodeInstantiationData<?> nodeInitiationData, IBrokerRemoteQuerier brokerQuerier) {
		return sendControlMessage(controlType, null, brokerAddress, nodeInitiationData, null, brokerQuerier);
	}

	private void handleInfoMessage(InfoMessage infoMessage) {
		LoggerFactory.getLogger().debug(this, "Handling" + infoMessage);
		
		Sequence remoteSequence = infoMessage.getSourceInstanceID();
		BrokerInfoTypes infoType = infoMessage.getInfoType();
		InetSocketAddress brokerControllerAddress = infoMessage.getFrom();
		IBrokerInfo[] brokerInfos = infoMessage.getBrokerInfos();
		
		InetSocketAddress brokerAddress = getBrokerAddress(brokerControllerAddress);
		if(brokerAddress == null)
			return;
		
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return;
		
		switch(infoType) {
		case BROKER_INFO_PS_SESSIONS:
			brokerRemoteCache.updatePSSessionInfos((PSSessionInfo[])brokerInfos, remoteSequence);
			break;

		case BROKER_INFO_SESSIONS:
			brokerRemoteCache.updateISessionInfos((ISessionInfo[])brokerInfos, remoteSequence);
			break;
			
		case BROKER_INFO_STATISTICS:
			throw new UnsupportedOperationException("No statistics yet implemented.");
			
		case BROKER_INFO_OP_STATUS:
			brokerRemoteCache.updateStatus((BrokerInfoOpState)brokerInfos[0], remoteSequence);
			break;
			
		case BROKER_INFO_COMPONENT_STATUS:
			brokerRemoteCache.updateStatus((BrokerInfoComponentStatus)brokerInfos[0], remoteSequence);
			break;
			
		case BROKER_INFO_SUBSCRIPTIONS:
			brokerRemoteCache.updateSubscriptions((SubscriptionInfo[])brokerInfos, remoteSequence);
			break;

		case BROKER_INFO_JOINS:
			brokerRemoteCache.updateTopologyLinks((JoinInfo[]) brokerInfos, remoteSequence);
			break;
			
		case BROKER_INFO_ERROR:
			ErrorMessage err = (ErrorMessage) infoMessage;
			LoggerFactory.getLogger().infoX(this, err.getException(), infoMessage.toString());
			break;
			
		default:
			throw new UnsupportedOperationException("Unknown Information type: " + infoMessage);
		}
		
		notifyOverlayNodeUpdateListeners(brokerAddress, infoType);
		
		Sequence requestSequence = infoMessage.getReplyToSequence();
		if(requestSequence != null)
		{
			IBrokerRemoteQuerier brokerQuerier = null;
			synchronized (_brokerQuerierRequestSequences) {
				brokerQuerier = _brokerQuerierRequestSequences.get(requestSequence);
			}
			if(brokerQuerier == null)
				return;
			boolean remove = brokerQuerier.newInfoAvailable(requestSequence, infoMessage);
			if(remove) {
				synchronized (_brokerQuerierRequestSequences) {
					brokerQuerier = _brokerQuerierRequestSequences.remove(requestSequence);

					_outRequestSequenceSetNew.remove(requestSequence);
					_outRequestSequenceSetOld.remove(requestSequence);
				}
			}
		}
	}
	
	private void notifyOverlayNodeUpdateListeners(InetSocketAddress brokerAddress, BrokerInfoTypes infoType) {
		synchronized(_overlayNodeUpdateListeners) {
			Set<IOverlayNodeUpdateListener> listenersSet = _overlayNodeUpdateListeners.get(brokerAddress);
			if(listenersSet == null)
				return;
			
			for(Iterator<IOverlayNodeUpdateListener> listenersIt = listenersSet.iterator(); listenersIt.hasNext() ;) {
				IOverlayNodeUpdateListener listener = listenersIt.next();
				listener.overlayNodeUpdated(brokerAddress, infoType);
			}
		}
	}

	private void handleControlMessage(ControlMessage<?> controlMessage) {
		throw new UnsupportedOperationException("This is a BrokerControllerCentre: it must not receive control messages! :: " + controlMessage);
	}
	
	private void handleQueryMessage(QueryMessage queryMessage) {
		throw new UnsupportedOperationException("This is a BrokerControllerCentre: it must not receive query messages! :: " + queryMessage);
	}
	
	private void handleErrorMessage(ErrorMessage errorMessage) {
//		Sequence remoteSequence = errorMessage.getSourceInstanceID();
		Sequence requestSequence = errorMessage.getReplyToSequence();
//		ErrorMessageTypes errorType = errorMessage.getErrorType();
		InetSocketAddress brokerControllerAddress = errorMessage.getFrom();
		
		InetSocketAddress brokerAddress = getBrokerAddress(brokerControllerAddress);
		if(brokerAddress == null)
			return;
		
		IBrokerRemoteCache brokerRemoteCache = _controllableBrokers.get(brokerAddress);
		if(brokerRemoteCache == null)
			return;
		
		brokerRemoteCache.receivedErrorMessage(errorMessage);

		if(requestSequence != null)
		{
			IBrokerRemoteQuerier brokerQuerier = null;
			synchronized (_brokerQuerierRequestSequences) {
				brokerQuerier = _brokerQuerierRequestSequences.remove(requestSequence);
				if(brokerQuerier == null)
					return;
				
				_outRequestSequenceSetNew.remove(requestSequence);
				_outRequestSequenceSetOld.remove(requestSequence);
			}
			brokerQuerier.errorOccured(requestSequence, errorMessage);
		}
	}
	
	private void createSimpleGUI() {
		JFrame jFrame = new JFrame();
		jFrame.setLayout(new FlowLayout());
		jFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {System.exit(0);}
		});
		
		JButton joinButton = new JButton("Join");
		joinButton.addActionListener(this);
		jFrame.add(joinButton);
		
		JButton killButton = new JButton("Kill");
		killButton.addActionListener(this);
		jFrame.add(killButton);
		
		JButton recoverButton = new JButton("Recover");
		recoverButton.addActionListener(this);
		jFrame.add(recoverButton);
		
		jFrame.pack();
		jFrame.setVisible(true);
	}

	public static void main2(String [] argv) {
		InetAddress brokerCtrlCntrAddress = LocalAddressGrabber.grabAllUp4LoopbackAddresses().iterator().next();
		InetSocketAddress brokerAddress = BrokerController.BROKER_BROKER_CONTROLLER_ADDRESSES[0][0];
		InetSocketAddress brokerControllerAddress = BrokerController.BROKER_BROKER_CONTROLLER_ADDRESSES[0][1];
		
		IBrokerControllerCentre brokerCtrlCntr = new BrokerControllerCentreImp(new InetSocketAddress(brokerCtrlCntrAddress, INIT_PORT));
		brokerCtrlCntr.prepareAndStart();
		
		brokerCtrlCntr.addBroker(brokerAddress, brokerControllerAddress);
		
		((BrokerControllerCentreImp)brokerCtrlCntr).createSimpleGUI();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JComponent src = (JComponent) e.getSource();
		if(src.getClass() == JButton.class) {
			JButton button = (JButton) src;
			String strButton = button.getText();
			if(strButton.equalsIgnoreCase("Join"))
			{
				InetSocketAddress brokerAddress = BrokerController.BROKER_BROKER_CONTROLLER_ADDRESSES[0][0];
				issueJoin(NodeTypes.NODE_BROKER, brokerAddress, null, null, null, null);
			}
			else if(strButton.equalsIgnoreCase("Kill"))
			{
				InetSocketAddress brokerAddress = BrokerController.BROKER_BROKER_CONTROLLER_ADDRESSES[0][0];
				issueKill(brokerAddress, null);
			}
			else if(strButton.equalsIgnoreCase("Recover"))
			{
				InetSocketAddress brokerAddress = BrokerController.BROKER_BROKER_CONTROLLER_ADDRESSES[0][0];
				issueRecover(NodeTypes.NODE_BROKER, brokerAddress, null, null, null);
			}
			
		}
	}

	private void removeAllBrokers() {
		synchronized (_brokerQuerierRequestSequences) {
			_brokerQuerierRequestSequences.clear();
			_outRequestSequenceSetNew.clear();
			_outRequestSequenceSetOld.clear();
		}
		synchronized (_controllableBrokers) {
			_controllableBrokers.clear();
			_usedAddresses.clear();
		}
	}

	@Override
	public void clearAllBrokers() {
		removeAllBrokers();
		((MessageListener)this).clear();
		synchronized (_controllableBrokers) {
			Set<Entry<InetSocketAddress, IMessageSender>> allSenders = _messageSenders.entrySet();
			Iterator<Entry<InetSocketAddress, IMessageSender>> allSendersIt = allSenders.iterator();
			while ( allSendersIt.hasNext()) {
				Entry<InetSocketAddress, IMessageSender> entry = allSendersIt.next();
				IMessageSender messageSender = entry.getValue();
				if(messageSender != null) 
					messageSender.clear();
			}
		}
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_BROKER_CONTROLLER_CENTRE;
	}

	@Override
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueTCommandMark(InetSocketAddress brokerAddress, String markName,
			IBrokerRemoteQuerier brokerQuerier) {
		NodeInstantiationData<?> nodeInitiationData = null;
		TCommand tCommand = PacketFactory.createTCommandMarkMessage(markName);
		return sendControlMessage(ControlMessageTypes.CTRL_MESSAGE_TCOMMAND, tCommand, brokerAddress, nodeInitiationData, markName, brokerQuerier);
	}

	@Override
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueTCommandDisseminate(InetSocketAddress brokerAddress, String markName,
			IBrokerRemoteQuerier brokerQuerier) {
		NodeInstantiationData<?> nodeInitiationData = null;
		TCommand tCommand = PacketFactory.createTCommandDisseminateMessage(markName);
		return sendControlMessage(ControlMessageTypes.CTRL_MESSAGE_TCOMMAND, tCommand, brokerAddress, nodeInitiationData, markName, brokerQuerier);
	}
	
	private final Map<InetSocketAddress, Set<IOverlayNodeUpdateListener>> _overlayNodeUpdateListeners = new HashMap<InetSocketAddress, Set<IOverlayNodeUpdateListener>>();
	@Override
	public void registerOverlayNodeUpdateListener(IOverlayNodeUpdateListener overlayNodeUpdateListener, InetSocketAddress remote) {
		synchronized(_overlayNodeUpdateListeners) {
			Set<IOverlayNodeUpdateListener> listeners = _overlayNodeUpdateListeners.get(remote);
			if(listeners == null) {
				listeners = new HashSet<IOverlayNodeUpdateListener>();
				_overlayNodeUpdateListeners.put(remote, listeners);
			}
			
			listeners.add(overlayNodeUpdateListener);
		}
	}

	@Override
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueBFTMessageManipulate(
			InetSocketAddress brokerControllerAddress, IBrokerRemoteQuerier brokerQuerier,
			ExtendedFailureTimelineEvents extendedCommand) {
		return sendControlMessage(ControlMessageTypes.CTRL_MESSAGE_BFT_MSG_MANIPULATE, extendedCommand.getBFTMessageManipulator(), brokerControllerAddress, null, "EMPTY", brokerQuerier);
	}

}
