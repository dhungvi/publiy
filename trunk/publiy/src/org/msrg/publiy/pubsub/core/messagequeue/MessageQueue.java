package org.msrg.publiy.pubsub.core.messagequeue;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.PSSessionInfo;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.communication.core.packet.types.TConf_Ack;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionLocal;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.SessionTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.ITimestamp;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.multicast.Conf_Ack;
import org.msrg.publiy.pubsub.core.packets.multicast.TDack;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastAnnotatorFactory;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Depart;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_UnSubscribe;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecoveryTypes;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.utils.annotations.AnnotationEvent;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.ViolationsLogger;
import org.msrg.publiy.utils.log.casuallogger.exectime.ExecutionTimeType;
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeEntity;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;

public class MessageQueue extends AbstractMessageQueue {
	private boolean _allowedToConfirm = true;
	private final Object _lock = new Object();
	
	private final String _psOverlayDumpFileName;
	private int _cleanUpCounter;
	private static final int MAX_CLEANUP_COUNTER = 1500;
//	private int _cleanUpForcedGCCounter;
//	private static final int MAX_CLEANUP_FORCED_GC_COUNTER = 1500000;

	protected final ViolationsLogger _violationsLogger;
	
	protected IMessageQueueNode _head;
	protected IMessageQueueNode _tail;
	protected Map<TMulticast, IMessageQueueNode> _mQSet;
	
	public MessageQueue(IBrokerShadow brokerShadow, IConnectionManager connectionManager, IOverlayManager overlayManager, ISubscriptionManager subscriptionManager, boolean allowedToConfirm) {
		super(brokerShadow, connectionManager, overlayManager, subscriptionManager);
		
		_psOverlayDumpFileName = brokerShadow.getPSOverlayDumpFileName();
		_head = null;
		_tail = null;
		_mQSet = new HashMap<TMulticast, IMessageQueueNode>();
		_allowedToConfirm = allowedToConfirm;

		_violationsLogger = _brokerShadow.getViolationsLogger();
		
		dumpPSTop();
	}
	
	public MessageQueue(IConnectionManager connectionManager, IMessageQueue oldMq) {
		super(connectionManager, (AbstractMessageQueue) oldMq);
		
		initializeFromOldMessageQueue((MessageQueue)oldMq);
		
		MessageQueue oldMQ = (MessageQueue)oldMq;
		
		_violationsLogger = oldMQ._violationsLogger;
		_psOverlayDumpFileName = oldMQ._psOverlayDumpFileName;
		
		_proceedAllInProgress = oldMQ._proceedAllInProgress;
		_revisitProceedAllAfterFinish = oldMQ._revisitProceedAllAfterFinish;

		Set<Entry<InetSocketAddress, PSSession>> set = _psSessions.entrySet();
		Iterator<Entry<InetSocketAddress, PSSession>> it = set.iterator();
		while(it.hasNext()) {
			Entry<InetSocketAddress, PSSession> entry = it.next();
			PSSession psSession = entry.getValue();
			psSession.swapMessageQueue(this, _connectionManager.getOverlayManager());
		}
		
		dumpPSTop();
	}
	
	protected void initializeFromOldMessageQueue(MessageQueue oldMQ) {
		_head = oldMQ._head;
		oldMQ._head = null;
		
		_tail = oldMQ._tail;
		oldMQ._tail = null;
		
		_mQSet = oldMQ._mQSet;
		oldMQ._mQSet = null;

		_allowedToConfirm = oldMQ._allowedToConfirm;
		oldMQ._allowedToConfirm = false;

		changeNodeReferences((MessageQueueNode)_head);
	}
	
	protected void checkLegitViolating(TMulticast_Publish tmpClone, InetSocketAddress remote) {
		if(_violationsLogger == null)
			return;
		
		_violationsLogger.legitimacyViolatingPublication(getOverlayManager(), tmpClone, remote);
	}

	protected final void checkLegitViolated(TMulticast_Publish tmpClone, InetSocketAddress remote) {
		if(_violationsLogger == null || tmpClone == null)
			return;
		
		_violationsLogger.legitimacyViolatedPublication(getOverlayManager(), tmpClone, remote);
	}

	@Override
	public Sequence getLastReceivedSequence(InetSocketAddress remote) {
		return _arrivalTimestamp.getLastReceivedSequence(remote);
	}
	
	@Override
	public void replaceSessions(Set<ISession> oldSessions, Set<ISession> newSessions) {
		synchronized (_lock)
		{
			replaceSessionsPrivately(oldSessions, newSessions);

			if(!Broker.RELEASE)
				LoggerFactory.getLogger().info(this, 
						"PSSessions: " + _psSessions + 
						" OLD:" + ((oldSessions==null)?null:Writers.write(oldSessions.toArray(new ISession[0]))) +
						" NEW:" + ((newSessions==null)?null:Writers.write(newSessions.toArray(new ISession[0])))
						);
		}
	}
	
	protected void replaceSessionsPrivately(Set<ISession> oldSessions, Set<ISession> newSessions) {
		int added = addSessions(newSessions);
		int removed = removeSessions(oldSessions);
		
		dumpPSTop();

		if(added == removed)
			return;
		else if(added > removed)
			return;
		if(added < removed) {
			checkAllForConfirmations();
		}
	}

	@Override
	public boolean dumpPSTop() {
		if(_psOverlayDumpFileName==null)
			return true;
		try{
			FileWriter fwriter = new FileWriter(_psOverlayDumpFileName, true);
			fwriter.write("\n" + BrokerInternalTimer.read() + "\n");

			String str = dumpPSTopToString("");
			fwriter.write(str);
			
			for(int i=0 ; i<40 ; i++)
				fwriter.write("=");
			
			fwriter.close();
		}catch (IOException iox) {
			return false;
		}
		return true;
	}
	
	protected final String dumpPSTopToString(String comment) {
		synchronized(_lock) {
			String str = "";
			Set<Entry<InetSocketAddress, PSSession>> psSet = _psSessions.entrySet();
			Iterator<Entry<InetSocketAddress, PSSession>> psIt = psSet.iterator();
			while(psIt.hasNext()) {
				Entry<InetSocketAddress, PSSession> entry = psIt.next();
				PSSession psSession = entry.getValue();
				str += (comment + "\t" + psSession + ",");
			}
			
			return str + "\n";
		}
	}

	private void changeNodeReferences(MessageQueueNode mqn) {
		while(mqn != null) {
			mqn._mQ = this;
			mqn = (MessageQueueNode) mqn.getNext();
		}
	}
	
	IMessageQueueNode getRealHead() {
		return _head;
	}
	
	@Deprecated
	IMessageQueueNode getBehindHead(Sequence lastSequence, InetSocketAddress remote, InetSocketAddress[] remotesInBetween) {
		proceedHead();
		
		IMessageQueueNode head = _head;
		
		if(lastSequence == null)
			return null;
		else if(head == null)
			return null;
		else if(head.getNodeSequence().succeeds(lastSequence))
			return null;
		
		boolean breakOuter = false; 
		
		while(head!=null && lastSequence.succeeds(head.getNodeSequence()) && !breakOuter) {
			head.passThrough(remote);
			
			if(head.isConfirmed() == false)
			{
				for(int i=0 ; i<remotesInBetween.length ; i++)
					if(head.checkedOutSetContains(remotesInBetween[i])) {
						breakOuter = true;
						break;
					}
			}
			
			if(head.getNext() == null)
				return head;
			else if(lastSequence.succeeds(head.getNext().getNodeSequence())) {
				head = head.getNext();
			}
			else
				return head;
		}
		
		if(head != null && head.getNodeSequence().succeeds(lastSequence))
			return null;
		else
			return head;
	}
	
	protected IMessageQueueNode getBehindHead(Sequence lastSequence, ISession session, boolean passThrough) {
		InetSocketAddress remote = session.getRemoteAddress();
		proceedHead();
		
		IMessageQueueNode head = getRealHead();
		if(lastSequence == null || head == null)
			return null;
		
		IMessageQueueNode last = null;
		try
		{
			do{
				if(!head.isConfirmed() && !lastSequence.succeedsOrEquals(head.getNodeSequence()))
					return last;
				if(passThrough) {
					if(Broker.CORRELATE)
						TrafficCorrelator.getInstance().sessionVisited(session, head.getTMulticast(), getPSSessionSize());
					
					head.passThrough(remote);
				}
				last = head;
				head = head.getNext();
			}while(head != null);
		}catch(Exception x) {
			System.out.println("ERROR: lastSeenSequence: " + lastSequence + " current position: " + head + " last: " + last + " remote: " + remote); 
		}
		
		return last;
	}

	protected void addConfMessage(TMulticast tm, InetSocketAddress to) {
		if(Broker.DEBUG) 
			LoggerFactory.getLogger().debug(this, "Adding new conf to '" + to + "' : " +  tm + "(" + _psSessions + ")");
		
		Sequence confirmationFromSequence = _localSequencer.getNext();
		TMulticast_Conf tmc = tm.getConfirmation(to, _localAddress, confirmationFromSequence);
		MessageQueueNode_Conf newConfNode = createNewMessageQueueNode_Conf(tmc, confirmationFromSequence, to);
		
		IMessageQueueNode existingMQNode = addNodeToLinkedListInternally(newConfNode, true);
		if((existingMQNode=_mQSet.get(tmc)) != newConfNode)
			throw new IllegalStateException("MISMATCH: ExistingMQNode: " + existingMQNode.toStringLong(false) + " vs. " + newConfNode.toStringLong(false));

		proceedAll();
	}
	
	private IMessageQueueNode addNodeToLinkedListInternally(IMessageQueueNode newNode, boolean replaceInMQ) {
		IMessageQueueNode existingMQNode = _mQSet.put(newNode.getMessage(), newNode);
		if(!replaceInMQ && existingMQNode!=null) {
			_mQSet.put(existingMQNode.getMessage(), existingMQNode);
			return existingMQNode;
		}
		
		if(_tail != null)
			_tail.setNext(newNode);
		
		if(_head == null)
			_head = newNode;

		_tail = newNode;
		
		return existingMQNode;
	}

	public int getHeadToToeSupposedlySize() {
		return _mQSet.size();
	}
	
	public int getHeadToToeUnConfrmedSize() {
		IMessageQueueNode mqn = getRealHead();
		int size = 0;
		
		while(mqn != null) {
			if(!mqn.isConfirmed())
				size++;
			mqn = mqn.getNext();
		}
		
		return size;		
	}
	
	public int getHeadToToeConfrmedSize() {
		IMessageQueueNode mqn = getRealHead();
		int size = 0;
		
		while(mqn != null) {
			if(mqn.isConfirmed())
				size++;
			mqn = mqn.getNext();
		}
		
		return size;		
	}
	
	public int getHeadToToeSize() {
		IMessageQueueNode mqn = getRealHead();
		int size = 0;
		
		while(mqn != null) {
			mqn = mqn.getNext();
			size++;
		}
		
		return size;
	}
	
	void forceGC() {
		System.gc();
	}
	
	protected MessageQueueNode createNewMessageQueueNode(TMulticast tm, ITMConfirmationListener confirmationListener) {
		MessageQueueNode mqn = new MessageQueueNode(this, tm, confirmationListener, _localSequencer.getNext());
		return mqn;
	}
	
	protected MessageQueueNode_Conf createNewMessageQueueNode_Conf(TMulticast_Conf tmc, Sequence confirmationFromSequence, InetSocketAddress to) {
		return new MessageQueueNode_Conf(this, tmc, null, confirmationFromSequence, to);
	}
	
	protected boolean isDuplicate(TMulticast tm) {
		boolean isDuplicate = _arrivalTimestamp.isDuplicate(tm.getSourceSequence());
		if(isDuplicate && Broker.CORRELATE)
			if(Broker.CORRELATE)
				TrafficCorrelator.getInstance().duplicate(_arrivalTimestamp, tm);
		
		return isDuplicate;
	}
	
	@Override
	public void addNewMessage(TMulticast tm, ITMConfirmationListener confirmationListener) {
		synchronized(_lock)
		{
			if(Broker.DEBUG) 
				LoggerFactory.getLogger().debug(this, "Adding new message: " + tm + "(" + _psSessions + ")");			
			
//			if((++_cleanUpForcedGCCounter)%MAX_CLEANUP_FORCED_GC_COUNTER==0)
//				forceGC();

			TMulticastTypes tmType = tm.getType();
			if(tmType == TMulticastTypes.T_MULTICAST_CONF) {
				if((++_cleanUpCounter)%MAX_CLEANUP_COUNTER == 0)
					proceedHeadAndJumpConfirmed();
			
				confirmationReceived((TMulticast_Conf) tm);
				return;
			}
			
			boolean isDuplicate = isDuplicate(tm);
			boolean isDuplicateNonLocalSource = isDuplicate && !_localSequencer.equals(tm.getSourceAddress());
			if(isDuplicateNonLocalSource) {
				duplicateMessageReceived(tm);
				return;
			}
			
			TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(
					_localSequencer, tm, AnnotationEvent.ANNOTATION_EVENT_MQ_ENTER, "Message enters in MQ");

			informLocalSubscribers(tm);
			
			IMessageQueueNode newNode = createNewMessageQueueNode(tm, confirmationListener); 
			
			IMessageQueueNode existingMQNode = addNodeToLinkedListInternally(newNode, false);
			if(existingMQNode != null) {
				existingMQNode.addToSenders(tm.getSenderAddress());
				newNode = existingMQNode;
			}

			proceedAll();
			
			newNode.checkForConfirmation();
			if(Broker.DEBUG) 
				LoggerFactory.getLogger().debug(this, "Added new message in: " + newNode + "(" + newNode.isConfirmed() + ")");
			return;	
		}
	}
	
	protected void duplicateMessageReceived(TMulticast tm) {
		if(!updateSenders(tm)) {
			InetSocketAddress remote = tm.getSenderAddress();
			addConfMessage(tm, remote);
		}
	}
	
	protected void informLocalSubscribers(TMulticast tm) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION:
			getSubscriptionManager().informLocalSubscribers((TMulticast_Publish)tm);
			break;
			
		default:
			break;
		}
	}

	private boolean _proceedAllInProgress = false;
	private boolean _revisitProceedAllAfterFinish = false;
	
	@Override
	public boolean isProceedEnabled() {
		return _proceedAllInProgress;
	}
	
	@Override
	public void disableProceed() {
		_proceedAllInProgress = true;
	}
	
	@Override
	public void enableProceed() {
		_proceedAllInProgress = false;
	}
	
	@Override
	public void proceedAll() {
		synchronized(_lock)
		{
			if(_proceedAllInProgress)
			{
				_revisitProceedAllAfterFinish = true;
				return;
			}
			IExecutionTimeEntity execTimeEntity =
					_brokerShadow.getExecutionTypeEntity(ExecutionTimeType.EXEC_TIME_PROCEED);
			if(execTimeEntity != null)
				execTimeEntity.executionStarted();

			do{
				_revisitProceedAllAfterFinish = false;
				_proceedAllInProgress = true;
				for(PSSession psSession : _psSessions.values()) {
					proceed(psSession, false);
				}
			}while(_revisitProceedAllAfterFinish);
			_proceedAllInProgress = false;
			_revisitProceedAllAfterFinish = false;
			
			if(execTimeEntity != null)
				execTimeEntity.executionEnded(true, false);
			
		}
	}
	
	@Override
	public void proceed(InetSocketAddress remote) {
		synchronized(_lock)
		{
			proceedPrivately(remote);
		}
	}
	
	private void proceedPrivately(InetSocketAddress remote) {
		if(_proceedAllInProgress)
			throw new IllegalStateException("ProceedAll and proceed cannot share a lock!");
		
		PSSession psSession = _psSessions.get(remote);
		if(psSession != null)
			proceed(psSession, false);
	}
	
	@Override
	public void applySummary(TRecovery tr) {
		synchronized (_lock)
		{
			TRecoveryTypes type = tr.getType();
			switch(type) {
			case T_RECOVERY_JOIN:
				considerJoinSummary((TRecovery_Join)tr);
				break;
				
			case T_RECOVERY_SUBSCRIPTION:
				considerSubscriptionSummary((TRecovery_Subscription)tr);
				break;
				
			default:
				return;
			}
		}
	}
	
	protected void considerSubscriptionSummary(TRecovery_Subscription trs) {
		if(Broker.DEBUG) 
			LoggerFactory.getLogger().debug(this, "Applying Subscription Summary: " + trs);
		getSubscriptionManager().applySummary(trs);
	}
	
	protected void considerJoinSummary(TRecovery_Join trj) {
		if(Broker.DEBUG) 
			LoggerFactory.getLogger().debug(this, "Applying Join Summary: " + trj);
		getOverlayManager().applySummary(trj);
	}
	
	public Map<InetSocketAddress, PSSession> getPSSessions() {
		return _psSessions;
	}
	
	private Set<InetSocketAddress> getPSSessionsAsInetSocketAddress() {
		Set<InetSocketAddress> remotes = new HashSet<InetSocketAddress>();
		
//		synchronized (_lock) 
		{
			Set<Entry<InetSocketAddress, PSSession>> psSet = _psSessions.entrySet();
			Iterator<Entry<InetSocketAddress, PSSession>> psIt = psSet.iterator();
			while(psIt.hasNext())
			{
				Entry<InetSocketAddress, PSSession> entry = psIt.next();
				PSSession psSession = entry.getValue();
				ISession session = psSession.getISession();
				InetSocketAddress remote = session.getRemoteAddress();
				remotes.add(remote);
			}
		}
		return remotes;
	}
	
	@Override
	public void substituteSession(ISession oldSession, ISession newSession) {
		synchronized(_lock) {
			if(!Broker.RELEASE)
				LoggerFactory.getLogger().info(this, "Substituting sessions: " + oldSession 
						+ " with: " + newSession + " ALL: " + _psSessions);
			
			if(Broker.CORRELATE)
				TrafficCorrelator.getInstance().substituteSessions(oldSession, newSession);

			InetSocketAddress remote = oldSession.getRemoteAddress();
			if(!newSession.getRemoteAddress().equals(remote))
				throw new IllegalArgumentException(oldSession + " vs. " + newSession);
			
			PSSession psSession = getPSSession(remote);
			if(psSession == null)
				throw new NullPointerException("No PSSession associated w/ " + oldSession + " to be replaced with " + newSession);

			psSession.resetSessionOnly(newSession);
		}
	}
	
	@Override
	public Set<ISession> getPSSessionsAsISessions() {
		Set<ISession> sessions = new HashSet<ISession>();
		synchronized (_lock)
		{
			Set<Entry<InetSocketAddress, PSSession>> psSet = _psSessions.entrySet();
			Iterator<Entry<InetSocketAddress, PSSession>> psIt = psSet.iterator();
			while(psIt.hasNext())
			{
				Entry<InetSocketAddress, PSSession> entry = psIt.next();
				PSSession psSession = entry.getValue();
				ISession session = psSession.getISession();
				sessions.add(session);
			}
		}
		
		return sessions;
	}
	
	@Override
	public void addNewMessage(TMulticast tm) {
		addNewMessage(tm, null);
	}
	
	@Override
	public void receiveTConfAck(TConf_Ack tConfAck) {
		synchronized (_lock)
		{
			if(Broker.DEBUG) 
				LoggerFactory.getLogger().debug(this, "Handling TConfAck: " + tConfAck);
			Conf_Ack[] confAcks = tConfAck.getIndividualConfAcks();
			for(int i=0 ; i<confAcks.length ; i++)
				receiveConfAck(confAcks[i]);
			
			if(	(_cleanUpCounter/MAX_CLEANUP_COUNTER) != 
					((_cleanUpCounter+confAcks.length)/MAX_CLEANUP_COUNTER))
			{
				_cleanUpCounter+=confAcks.length;
				proceedHeadAndJumpConfirmed();
			}
			else
				_cleanUpCounter+=confAcks.length;
		}
	}
	
	private void receiveConfAck(Conf_Ack confAck) {
		IMessageQueueNode node = _mQSet.remove(confAck);
		if(node == null) {
			System.out.println("Warning: confAck not found: " + confAck);
			return;
		}

		if(!MessageQueueNode_Conf.class.isInstance(node))
			throw new IllegalStateException("MessageQueue node of the confAck msg is not MessageQueueNode_Conf. It is a: " + node.toStringLong(false));

		TMulticast tm = node.getTMulticast();
		
		if(tm.getType() != TMulticastTypes.T_MULTICAST_CONF) // TMulticast_Conf.class.isInstance(tm))
			throw new IllegalStateException("Conf_Ack can only be equal to TMulticast_Conf.");
		
		TMulticast_Conf tmc = (TMulticast_Conf) tm;
		
		if(!confAck.equals(tmc))
			throw new IllegalStateException("These should have been equal!!!");
		
		MessageQueueNode_Conf node_conf = (MessageQueueNode_Conf) node;
		confirm(node_conf);
	}
	
	final int getActiveSessionsCount() {
		return _psSessions.size();
	}
	
	@Override
	public int getPSSessionSize() {
		return _psSessions.size();
	}
	
	protected IMessageQueueNode removeFromMQSet(IMessageQueueNode node) {
		node.confirmed(true);
		return _mQSet.remove(node.getMessage().getSourceSequence());
	}
	
	protected void confirm(MessageQueueNode_Conf node) {
		removeFromMQSet(node);
	}
	
	protected void confirm(IMessageQueueNode node) {
		if(_allowedToConfirm == false)
			return;

		// Remote the node from the set
		if(removeFromMQSet(node) == null)
			throw new IllegalStateException("Node which is being confirmed is not in the messagequeue '" + node.toStringLong(false) + "'");
		
		TMulticast tm = node.getTMulticast();
		
		// Apply the message in the routing tables.
		BrokerOpState brokerState = getBrokerOpState();
		if(brokerState == BrokerOpState.BRKR_PUBSUB_PS || brokerState == BrokerOpState.BRKR_PUBSUB_DEPART)
			apply(tm);
		
		// Notify the confirmation listener, if any (typically the broker)
		ITMConfirmationListener confirmationListener = node.getConfirmationListener();
		if(confirmationListener != null) {
			confirmationListener.tmConfirmed(tm);
			
//				node.confirmed(true);
			proceedHead();
			return;
		}
		
		TMulticastTypes tmType = node.getMessage().getType();
		if(tmType == TMulticastTypes.T_MULTICAST_JOIN) {
			InetSocketAddress possibleDirectJoiningAddress = tm.getSenderAddress();
			Path<INode> path = getOverlayManager().getPathFrom(possibleDirectJoiningAddress);
			if(path != null && path.getLength() == 1) {
				boolean isDirectJoin = _connectionManager.upgradeUnjoinedSessionToActive(possibleDirectJoiningAddress);
				if(isDirectJoin) {
//						proceedPrivately(possibleDirectJoiningAddress);
					proceedAll();
				}
			}
		}
		
		sendConfirmations(node);
		
		proceedHead();
	}
	
	protected void sendConfirmations(IMessageQueueNode node) {
		// Send the confirmation to the original sender
		TMulticast tm = node.getTMulticast();
		InetSocketAddress sender = node.getSender();
		if(sender != null)
			addConfMessage(tm, sender);
		else
			throw new IllegalStateException("Sender of '" + tm.toStringTooLong() + "' is null: " + node);
		
		// Send the confirmation to other senders.
		InetSocketAddress senders [] = node.getSenders();
		if(senders != null) {
			int sendersSize = senders.length;
			for(int i=0; i<sendersSize ; i++) {
				addConfMessage(tm, senders[i]);
//					sendConfirmationTo(node.getConfirmation(senders[i]), senders[i]);
			}
		}
	}
	
	@Override
	public void applySpecial(TMulticast tm) {
		apply(tm);
	}
	
	protected void apply(TMulticast tm) {
		TMulticastTypes type = tm.getType();
		switch(type) {
		case T_MULTICAST_JOIN:
			TMulticast_Join tmj = (TMulticast_Join) tm;
			getOverlayManager().handleMessage(tmj);
			break;
		
		case T_MULTICAST_DEPART:
			TMulticast_Depart tmd = (TMulticast_Depart) tm;
			getOverlayManager().handleMessage(tmd);
			break;
		
		case T_MULTICAST_SUBSCRIPTION:
			TMulticast_Subscribe tms = (TMulticast_Subscribe) tm;
			getSubscriptionManager().handleMessage(tms);
			break;

		case T_MULTICAST_UNSUBSCRIPTION:
			TMulticast_UnSubscribe tmus = (TMulticast_UnSubscribe)tm;
			getSubscriptionManager().handleMessage(tmus);
			break;

		default:
			return;
		}
	}
	
	public PSSession getPSSession(InetSocketAddress to) {
		synchronized (_lock)
		{
			return _psSessions.get(to);
		}
	}
	
	private boolean updateSenders(TMulticast tm) {
		IMessageQueueNode mqn = _mQSet.get(tm);
		if(mqn == null)
			return false;
		mqn.addToSenders(tm.getSenderAddress());
		return true;
	}
	
	private void proceed(PSSession psSession, boolean revisitProgressAll) {
		boolean prevProceedInProgress = psSession.setProceeding(true);
		if(prevProceedInProgress) {
			if(_proceedAllInProgress && revisitProgressAll) {
				_revisitProceedAllAfterFinish = true;
			}
			return;
		}

		while(psSession.advance())
			continue;
		
		psSession.setProceeding(false);
	}
	
	protected int addSessions(Set<ISession> newSessions) {
		if(newSessions == null)
			return 0;
		
		int addedSize = 0;		
		Iterator<ISession> newSessionsIt = newSessions.iterator();
		while(newSessionsIt.hasNext())
		{
			ISession newSession = newSessionsIt.next();
			if(Broker.CORRELATE)
				TrafficCorrelator.getInstance().sessionAdded(newSession);
			InetSocketAddress remote = newSession.getRemoteAddress();
			PSSession existingPSSession = _psSessions.get(remote);
			if(existingPSSession!=null)
			{
				ISession existingSession = existingPSSession.getSession();
				if(existingSession != newSession) {
					substituteSession(existingSession, newSession);
				}
				else {
					if(_proceedAllInProgress)
						_revisitProceedAllAfterFinish = true;
					else
						proceed(existingPSSession, true);
				}
			}
			else
			{
				PSSession newPSS = createNewPSSession(newSession);
				_psSessions.put(remote, newPSS);
				if(Broker.CORRELATE)
					TrafficCorrelator.getInstance().psSessionAdded(newPSS);
			
				if(_proceedAllInProgress)
					_revisitProceedAllAfterFinish = true;
				else
					proceed(newPSS, true);
				
				addedSize++;
			}
		}

		return addedSize;
	}
	
	protected PSSession createNewPSSession(ISession newSession) {
		return new PSSession(newSession, this, _overlayManager);
	}

	protected void checkAllForConfirmations() {
		IMessageQueueNode node = getRealHead();
		while(node != null) {
			node.checkForConfirmation();
			
			node = node.getNext();
		}
	}
	
	protected void proceedHead() {
		IMessageQueueNode prev = getRealHead();
		if(prev == null)
			return;
		
		IMessageQueueNode next = prev.getNext();
		
		while(next != null && prev.isConfirmed()) {
			prev = next;
			next = next.getNext();
		}
		
		_head = prev;
		_tail = _head == null ? null : _tail;
		if(Broker.DEBUG) 
			LoggerFactory.getLogger().debug(this, "Head = " + _head);
	}
	
	protected MQCleanupInfo proceedHeadAndJumpConfirmed() {
		proceedHead();
		
		IMessageQueueNode prev = getRealHead();
		if(prev == null)
			return new MQCleanupInfo(0, 0);
		int discardedCount=0;
		int size=0;
				
		IMessageQueueNode next = prev.getNext();
		
		while(next != null && next != _tail) {
			if(next.isConfirmed()) {
				discardedCount++;
				next = next.getNext();
				prev.setNext(next);
			} else {
				size++;
				prev = next;
				next = next.getNext();
			}
		}
		
		if(Broker.DEBUG) 
			LoggerFactory.getLogger().debug(this, "ProceedJump: " + _mQSet.size() + "=" + getHeadToToeSize());
		
		return new MQCleanupInfo(size, discardedCount);
	}
	
	@Override
	public String toString() {
		return toString(false);
	}
	
	public String toString(boolean includeConfirmed) {
		String str = "MQ:[[[[";
		IMessageQueueNode mqn = _head;
		while(mqn != null) {
			if(includeConfirmed || !mqn.isConfirmed())
				str += "\t" + mqn.toStringLong(true) + " \n";
			mqn = mqn.getNext();
		}
			
		return str + "]]]]";
	}
	
	private void confirmationReceived(TMulticast_Conf tmc) {
		IMessageQueueNode mqn = _mQSet.get(tmc.getSourceSequence());
		if(mqn == null)
			return;
		
		if(Broker.DEBUG) 
			LoggerFactory.getLogger().debug(this, "confirmationReceived for node: " + mqn);
		
		mqn.confirmationReceieved(tmc);
	}

	@Override
	public void applyAllSummary(TRecovery_Join[] trjs) {
		synchronized (_lock)
		{
			getOverlayManager().applyAllJoinSummary(trjs);
		}
	}

	@Override
	public void applyAllSummary(TRecovery_Subscription[] trss) {
		getSubscriptionManager().applyAllSubscriptionSummary(trss);
	}
	
	@Override
	public void allowConfirmations(boolean allow) {
		synchronized (_lock)
		{
			LoggerFactory.getLogger().info(this, allow? "Allowing" : "Disallowing", " confirmations" + getPSSessionsAsInetSocketAddress(), "\t Prev value:" + _allowedToConfirm);
			_allowedToConfirm = allow;
			if(_allowedToConfirm)
				checkAllForConfirmations();
		}
	}

	@Override
	public boolean getAllowedToConfirm() {
		return _allowedToConfirm;
	}
	
	@Override
	public ISessionLocal addPSSessionLocalForRecoveryWithENDEDIsession() {
		synchronized(_lock)
		{
			InetSocketAddress localAddress = _localAddress;
			ISessionLocal sessionLocal = ISessionManager.createBaseLocalSession(_brokerShadow, SessionTypes.ST_END);
			PSSessionLocal psSessionLocal = new PSSessionLocal(sessionLocal, this);
			_psSessions.put(localAddress, psSessionLocal);
			if(Broker.CORRELATE)
				TrafficCorrelator.getInstance().psSessionAdded(psSessionLocal);
			
			return sessionLocal;
		}
	}

	@Override
	public PSSessionInfo[] getPSSessionInfos() {
		synchronized(_lock) {
			Map<InetSocketAddress, PSSession> psSessions = _psSessions;
			if(psSessions == null)
				return new PSSessionInfo[0];
			
			PSSessionInfo[] psSessionInfos = new PSSessionInfo[psSessions.size()];
			int index = 0;
			Set<Entry<InetSocketAddress, PSSession>> psSet = psSessions.entrySet();
			Iterator<Entry<InetSocketAddress, PSSession>> psIt = psSet.iterator();
			while(psIt.hasNext()) {
				Entry<InetSocketAddress, PSSession> entry = psIt.next();
				PSSession psSession = entry.getValue();
				psSessionInfos[index++] = psSession.getInfo();
			}
			
			return psSessionInfos;
		}
	}
	
	@Override
	public ITimestamp getDiscardedTimestamp() {
		return null;
	}

	@Override
	public void processTDack(TDack dack) {
		throw new UnsupportedOperationException("This is a normal MQ, donno how to handle this: " + dack);
	}

	@Override
	public MQCleanupInfo purge() {
		throw new UnsupportedOperationException("This is a normal MQ, donno how to purge.");
	}
	
	@Override
	public void resetSession(ISession session, Sequence lastReceivedSequence, boolean initializeMQNode) {
		synchronized (_lock) {
			if(!Broker.RELEASE)
				LoggerFactory.getLogger().info(this, "SPECIAL3 Resetting session: " + session + "_" + lastReceivedSequence + "[" + initializeMQNode + "]");
			
			session.setLastReceivedSequence(lastReceivedSequence);
			InetSocketAddress remote = session.getRemoteAddress();
			PSSession psSession = _psSessions.get(remote);
			if(psSession == null)
				return;
			
			psSession.resetSession(session, initializeMQNode);
		}
	}
	
	@Override
	public void dumpAll(String prepend) {
		LoggerFactory.getLogger().info(this, dumpPSTopToString(prepend));
		LoggerFactory.getLogger().info(this, "MQ_DUMP: " + prepend);

		IMessageQueueNode mqn = _head;
		while(mqn != null) {
			if(!mqn.isConfirmed())
				LoggerFactory.getLogger().info(this, prepend + "\t" + mqn.toStringLong(true));
			mqn = mqn.getNext();
		}
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_MQ;
	}
}
