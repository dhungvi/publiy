package org.msrg.publiy.broker.core.connectionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


import org.msrg.publiy.client.subscriber.ISubscriptionListener;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.communication.core.packet.types.SessionInitiationTypes;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiationBFT;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.ISessionManagerBFT;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.communication.core.sessions.SessionTypes;

import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.IBFTSubscriptionManager;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.messagequeue.BFTMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.IBFTMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.MQCleanupInfo;
import org.msrg.publiy.pubsub.core.overlaymanager.IBFTOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT_Dack;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.BFTSuspicionLogger;
import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBFTSuspectable;
import org.msrg.publiy.broker.IBFTSuspicionListener;
import org.msrg.publiy.broker.IBFTSuspectedRepo;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.BFTConnectionManagerJoin;
import org.msrg.publiy.broker.core.IBFTConnectionManager;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class BFTConnectionManager extends ConnectionManagerPS implements IBFTConnectionManager, IBFTSuspicionListener {

	public final static long BFT_DACK_SEND_INTERVAL = 8 * 1000;
	public final static long BFT_DACK_RECEIVE_CHECKING_INTERVAL = 1000;
	public final static long BFT_DACK_RECEIVE_INTERVAL = (long) Math.ceil(BFT_DACK_SEND_INTERVAL * 2.5);
	public final static long PURGE_MQ_INTERVAL = BFT_DACK_SEND_INTERVAL * 3;
	
	protected final IBFTSuspectedRepo _bftSuspicionRepo;
	protected final IBFTBrokerShadow _bftBrokerShadow;
	protected final IBFTOverlayManager _bftOverlayManager;
	protected final BFTSuspicionLogger _bftSuspicionLogger;
	protected final ISessionManagerBFT _bftSessionManager;

	protected BFTConnectionManager(BFTConnectionManagerJoin conManJoin) {
		super(conManJoin);
		
		_bftSuspicionRepo = conManJoin.getSuspicionRepo();
		_bftSuspicionRepo.registerSuspicionListener(this);
		_bftBrokerShadow = conManJoin.getBrokerShadow();
		_bftOverlayManager = (IBFTOverlayManager) _overlayManager;
		_bftSuspicionLogger = _bftBrokerShadow.getBFTSuspecionLogger();
		
		_bftSessionManager = (ISessionManagerBFT) _sessionManager;
	}

	public BFTConnectionManager(BFTConnectionManagerJoin conManJoin,
			String connManName, ConnectionManagerTypes connManType) {
		super(conManJoin, connManName, connManType);
		
		_bftSuspicionRepo = conManJoin.getSuspicionRepo();
		_bftSuspicionRepo.registerSuspicionListener(this);
		_bftBrokerShadow = conManJoin.getBrokerShadow();
		_bftOverlayManager = (IBFTOverlayManager) _overlayManager;
		_bftSuspicionLogger = _bftBrokerShadow.getBFTSuspecionLogger();
		
		_bftSessionManager = (ISessionManagerBFT) _sessionManager;
	}

	protected BFTConnectionManager(
			String connectionManagerName,
			ConnectionManagerTypes type,
			IBroker broker,
			IBFTBrokerShadow bftBrokerShadow,
			IBFTOverlayManager bftOverlayManager,
			IBFTSubscriptionManager bftSubscriptionManager,
			IBFTMessageQueue bftMessageQueue,
			TRecovery_Join[] trjs)
			throws IOException {
		super(connectionManagerName, type, broker, bftBrokerShadow,
				bftOverlayManager, bftSubscriptionManager, bftMessageQueue, trjs);
		
		_bftBrokerShadow = bftBrokerShadow;
		_bftSuspicionLogger = _bftBrokerShadow.getBFTSuspecionLogger();
		_bftSuspicionRepo = bftBrokerShadow.getBFTSuspectedRepo();
		_bftSuspicionRepo.registerSuspicionListener(this);
		_bftOverlayManager = bftOverlayManager;
		
		_bftSessionManager = (ISessionManagerBFT) _sessionManager;
	}

	@Override
	protected void connectionManagerJustStarted() {
		super.connectionManagerJustStarted();
		
		scheduleNextSendBFTDackTask();
		scheduleNextReceiveBFTDackTask();
		scheduleNextPurgeMQ();
	}
	
	@Override
	protected void scheduleNextPurgeMQ() {
		PurgeMQTimerTask purgeMQTimerTask = new PurgeMQTimerTask(this);
		scheduleTaskWithTimer(purgeMQTimerTask, PURGE_MQ_INTERVAL);
	}

	@Override
	public void purgeMQ() {
		ConnectionEvent_purgeMQ connEventPurgeMQ = new ConnectionEvent_purgeMQ(_localSequencer);
		addConnectionEventHead(connEventPurgeMQ);
		
		scheduleNextPurgeMQ();
	}
	
	protected void scheduleNextSendBFTDackTask() {
		BFTDackSendTask bftDackSendTask = new BFTDackSendTask(this);
		_maintanenceManager.scheduleMaintenanceTask(bftDackSendTask, BFT_DACK_SEND_INTERVAL);
	}

	@Override
	protected final IMessageQueue createMessageQueue(
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager, boolean allowToConfirm) {
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_MQ, "Creating BFT-MQ");
		IMessageQueue mq = new BFTMessageQueue(
				(IBFTBrokerShadow) brokerShadow,
				this,
				(IBFTOverlayManager) overlayManager,
				(IBFTSubscriptionManager) subscriptionManager);
		if(!allowToConfirm)
			throw new IllegalArgumentException();
		mq.allowConfirmations(allowToConfirm);
		return mq;
	}
	
	public void sendBFTDack() {
		ConnectionEvent_BFTDackSend event = new ConnectionEvent_BFTDackSend(_localSequencer);
		addConnectionEvent(event);
		
		scheduleNextSendBFTDackTask();
	}

	@Override
	protected IMessageQueue createMessageQueue(IBrokerShadow brokerShadow, IMessageQueue mq) {
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_MQ, "Creating BFT-MQ");
		return  new BFTMessageQueue((IBFTBrokerShadow) brokerShadow, this, mq);
	}

	@Override
	protected void handleMulticastMessage(ISession session, TMulticast tm) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, "HanldlePS: " + tm);
	
		performMulticastMessageSendersTest(session, tm);
		TMulticastTypes type = tm.getType();
		switch(type) {
		case T_MULTICAST_PUBLICATION_BFT:
			_mq.addNewMessage(tm);
			break;

		case T_MULTICAST_PUBLICATION_BFT_DACK:
			_mq.addNewMessage(tm);
			break;
			
		default:
			super.handleMulticastMessage(session, tm);
		}
	}
	
	@Override
	public void tmConfirmed(TMulticast tm) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_BFT:
			throw new UnsupportedOperationException("TMConfirmed: " + tm);
			
		default:
			super.tmConfirmed(tm);
		}
	}

	@Override
	public void setLastReceivedSequence2(ISession sesion, Sequence seq, boolean doProceed, boolean initializeMQNode) {
		return;
	}
	
	@Override
	protected final void handleConnectionEvent_sendDack(ConnectionEvent_sendDack connEvent) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected void handleConnectionEvent_purgeMQ(ConnectionEvent_purgeMQ connEvent) {
		long purgeStartTime = SystemTime.currentTimeMillis();
		MQCleanupInfo purgeInfo = _mq.purge();
		LoggerFactory.getLogger().info(this, "Purging MQ took " + (SystemTime.currentTimeMillis() - purgeStartTime) + " ms to discard " + purgeInfo + ".");
	}

	public void scheduleNextReceiveBFTDackTask() {
		BFTDackReceiveTask bftDackReceiveTask = new BFTDackReceiveTask(this);
		_maintanenceManager.scheduleMaintenanceTask(bftDackReceiveTask, BFT_DACK_RECEIVE_CHECKING_INTERVAL);
	}
	
	public void receiveBFTDackTask() {
		ConnectionEvent_BFTDackReceiveTimeout event = new ConnectionEvent_BFTDackReceiveTimeout(_localSequencer);
		addConnectionEvent(event);

		scheduleNextReceiveBFTDackTask();
	}
	
	@Override
	protected boolean handleConnectionEvent_Special(ConnectionEvent connEvent) {
		switch(connEvent._eventType) {
		case CONNECTION_EVENT_PURGE_MQ:
		{
			handleConnectionEvent_purgeMQ((ConnectionEvent_purgeMQ) connEvent);
		}
		return true;
			
		case CONNECTION_EVENT_BFT_DACK_RECEIVE:
		{
			Set<InetSocketAddress> timedoutRemotes = _bftOverlayManager.whoseDacksAreNotReceived();
			processTimedoutDack(timedoutRemotes);
		}
		return true;
			
		case CONNECTION_EVENT_BFT_DACK_SEND:
		{
			TMulticast_Publish_BFT_Dack bftDack = new TMulticast_Publish_BFT_Dack(_localAddress, _localSequencer.getNext());
			_bftOverlayManager.issueLocalMessageSequencePair(bftDack);
			_mq.addNewMessage(bftDack);
		}
		return true;
			
		case CONNECTION_EVENT_NODE_SUSPECTED:
		{
			ConnectionEvent_NodeSuspected nodeSuspectedEvent = (ConnectionEvent_NodeSuspected) connEvent;
			BFTSuspecionReason reason = nodeSuspectedEvent._reason;
			synchronized(_sessionsLock) {
				suspectNodePrivately(reason);
			}
		}
		return true;
			
		default:
			return super.handleConnectionEvent_Special(connEvent);
		}
	}
	
	protected void processTimedoutDack(Set<InetSocketAddress> timedoutRemotes) {
		LoggerFactory.getLogger().info(this, "DACK of these remotes are timedout: ",
				Writers.write(_brokerShadow.getBrokerIdentityManager(), timedoutRemotes));
		Map<InetSocketAddress, InetSocketAddress> affectedToSuspectedMap =
				new HashMap<InetSocketAddress, InetSocketAddress>();
		for(InetSocketAddress timedoutRemote : timedoutRemotes) {
			ISession sessionToTimedoutRemote = getSessionPrivately(timedoutRemote);
			if(sessionToTimedoutRemote != null) {
				if(sessionToTimedoutRemote.isLocallyActive())
					affectedToSuspectedMap.put(timedoutRemote, timedoutRemote);
				continue;
			}
			
			Path<INode> pathFromTimedoutRemote = _bftOverlayManager.getPathFrom(timedoutRemote);
			InetSocketAddress closestReachableIntermediateRemote = timedoutRemote;
			for(InetSocketAddress intermediateRemote : pathFromTimedoutRemote.getAddresses()) {
				ISession session = getSessionPrivately(intermediateRemote);
				if(session != null)
					if(session.isLocallyActive())
						closestReachableIntermediateRemote = intermediateRemote;
			}
			affectedToSuspectedMap.put(timedoutRemote, closestReachableIntermediateRemote);
		}
		
		
		for(Entry<InetSocketAddress, InetSocketAddress> entry : affectedToSuspectedMap.entrySet()) {
			InetSocketAddress affected = entry.getKey();
			InetSocketAddress suspect = entry.getValue();
			IBFTSuspectable suspectable = _bftOverlayManager.getSuspectable(suspect);
			BFTSuspecionReason reason =
					new BFTSuspecionReason(true, true,
							suspectable, affected, "Dack of downstream/itself timedout.");
			_bftSuspicionRepo.suspect(reason);
		}
	}

	protected void suspectNodePrivately(BFTSuspecionReason reason) {
		if(_bftSuspicionLogger != null)
			_bftSuspicionLogger.newNodeSuspected(reason);
		
		if(!_bftBrokerShadow.reactToMisbehaviors())
			return;
		
		if(!reason.isThisANewSuspicion())
			return;
		
		if(!reason.reactToThis())
			return;
		
		InetSocketAddress affectedRemote = reason._affectedRemote;
		InetSocketAddress suspectedRemote = reason._suspectable.getAddress();
		
		// We do not change the status of ISession to `suspectedRemote'
		InetSocketAddress[] suspectedNeighbors =
				_bftOverlayManager.getNeighbors(suspectedRemote);
		for(int i=1 ; i<suspectedNeighbors.length ; i++) {
			InetSocketAddress suspectedNeighbor = suspectedNeighbors[i];
			if(suspectedNeighbor == null)
				continue;
			
			if(!reason.affects(suspectedNeighbor) && !suspectedRemote.equals(affectedRemote))
				continue;
			
			ISession existingBypassingSession =
					getSessionPrivately(suspectedNeighbor);
			if(existingBypassingSession != null)
				if(existingBypassingSession.getSessionType() != SessionTypes.ST_END)
					continue;
			
			ISession bypassingSession = getDefaultOutgoingSession(suspectedNeighbor);
			setSessionsConnectionType(bypassingSession, SessionConnectionType.S_CON_T_BYPASSING_AND_INACTIVATING);
			addSessionPrivately(suspectedNeighbor, bypassingSession);	
			
			LoggerFactory.getLogger().info(this, "Connecting to bypassing node: " + suspectedNeighbor);
			IConInfoNonListening<?> newConInfoNL = _nioBinding.makeOutgoingConnection(bypassingSession , this, this, this, suspectedNeighbor);
			if(newConInfoNL == null) {
				ISessionManagerBFT.setType(bypassingSession, SessionTypes.ST_END);
			}
		}
	}

	@Override
	protected void performConnectionManagerTypeSpecificPostRegistration(ISession newSession) {
		super.performConnectionManagerTypeSpecificPostRegistration(newSession);
	}
	
	@Override
	protected void performConnectionManagerTypeSpecificPostFailed(ISession session) {
		super.performConnectionManagerTypeSpecificPostFailed(session);
	}

	@Override
	public TMulticast_Subscribe issueSubscription(
			Subscription subscription, ISubscriptionListener subscriptionListener) {
		return super.issueSubscription(subscription, subscriptionListener);
	}
	
	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_CON_MAN_BFT;
	}
	
	@Override
	protected void sendAllToRecoveringAndNotEndRecovery(ISession session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean upgradeUnjoinedSessionToActive(InetSocketAddress completedJoiningAddress) {
		return super.upgradeUnjoinedSessionToActive(completedJoiningAddress);
	}
	
	@Override
	protected ISessionManager createSessionManager(LocalSequencer localSequencer) {
		return new ISessionManagerBFT(localSequencer, _nioBinding, this);
	}
	
	@Override
	public ISession getDefaultOutgoingSession(InetSocketAddress remote) {
		return ISessionManagerBFT.getPubSubSession(_bftBrokerShadow, remote, false);
	}
	
	@Override
	protected boolean performConnectionManagerTypeSpecificPreRegistration(ISession newSession) {
		return true;
	}
	
	@Override
	public void nodeSuspected(BFTSuspecionReason reason) {
		ConnectionEvent event = new ConnectionEvent_NodeSuspected(_localSequencer, reason);
		addConnectionEvent(event);
	}

	@Override
	public IBFTSuspectedRepo getSuspicionRepo() {
		return _bftSuspicionRepo;
	}
	
	@Override
	public IBFTBrokerShadow getBrokerShadow() {
		return _bftBrokerShadow;
	}

	@Override
	public void sendTMulticast(TMulticast tm, ITMConfirmationListener tmConfirmationListener) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_BFT:
			_bftOverlayManager.issueLocalMessageSequencePair((TMulticast_Publish_BFT)tm);
			break;
			
		default:
			break;
		}
		
		super.sendTMulticast(tm, tmConfirmationListener);
	}
	
	@Override
	public void sendTMulticast(TMulticast[] tms, ITMConfirmationListener tmConfirmationListener) {
		for(TMulticast tm : tms) {
			switch(tm.getType()) {
			case T_MULTICAST_PUBLICATION_BFT:
				_bftOverlayManager.issueLocalMessageSequencePair((TMulticast_Publish_BFT)tm);
				break;
				
			default:
				break;
			}
		}
		super.sendTMulticast(tms, tmConfirmationListener);
	}
	
	public long getDackReceiveTimeout(InetSocketAddress remote) {
		return _bftOverlayManager.getDackReceiveTimeout(remote);
	}

	@Override
	protected void checkFartherSessionsWhileRegister(ISession survivedSession, Set<ISession> mqAddedSessions, Set<ISession> mqRemovedSessions) {
		return;
	}
	
	@Override
	protected boolean checkCloserSessionsWhileRegister(ISession survivedSession, Set<ISession> mqAddedSessions, Set<ISession> mqRemovedSessions) {
		SessionConnectionType sessionConnectionType = survivedSession.getSessionConnectionType();
		switch(sessionConnectionType) {
		case S_CON_T_BYPASSING_AND_INACTIVATING:
			setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_BYPASSING_AND_INACTIVE);
			checkBypassingInactiveSession(survivedSession);
//			mqAddedSessions.add(survivedSession);
			return true;
			
		case S_CON_T_BYPASSING_AND_ACTIVE:
//			mqAddedSessions.add(survivedSession);
//			setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_BYPASSING_AND_ACTIVE);
			return true;
//			throw new IllegalStateException(survivedSession.toString());
			
		default:
			break;
		}
		
		InetSocketAddress remote = survivedSession.getRemoteAddress();
		Path<INode> pathFromRemoteSession = _overlayManager.getPathFrom(remote);
		if(pathFromRemoteSession == null) {
			// This can only become a S_CON_T_UNJOINED.
			setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_UNJOINED);
			mqAddedSessions.add(survivedSession);
			return true;
		}

		InetSocketAddress [] pathAddresses = pathFromRemoteSession.getAddresses();
		if(pathAddresses.length == 1) {
			setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_ACTIVE);
			mqAddedSessions.add(survivedSession);
			return true;
		}
		
		setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_BYPASSING_AND_INACTIVE);
		checkBypassingInactiveSession(survivedSession);
//		mqAddedSessions.add(survivedSession);
		return true;
	}

	protected void checkBypassingInactiveSession(ISession byppassingInactiveSession) {
		if(byppassingInactiveSession.getSessionConnectionType() != SessionConnectionType.S_CON_T_BYPASSING_AND_INACTIVE)
			throw new IllegalArgumentException(byppassingInactiveSession + " must be " + SessionConnectionType.S_CON_T_BYPASSING_AND_INACTIVE);
		
		InetSocketAddress remote = byppassingInactiveSession.getRemoteAddress();
		Path<INode> pathFromRemote = _overlayManager.getPathFrom(remote);
		if(pathFromRemote.getLength() <= 1)
			throw new IllegalStateException("Remote cannot be within distance 1: " + byppassingInactiveSession);
		
		INode immediateNeighborOfRemote = pathFromRemote.get(1);
		IBFTSuspectable immediateSuspectableOfRemote = _bftOverlayManager.getSuspectable(immediateNeighborOfRemote.getAddress());
		if(!immediateSuspectableOfRemote.isSuspected())
			return;
		
		TSessionInitiationBFT tInitBft = new TSessionInitiationBFT(_localSequencer, remote, SessionInitiationTypes.SINIT_PUBSUB, null);
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, tInitBft);
		_sessionManager.send(byppassingInactiveSession, raw);
	}
	
	@Override
	protected void checkBWAvailability() {
		return;
	}
	
	@Override
	public InOutBWEnforcer getBWEnforcer() {
		return null;
	}
	
	@Override
	public String getThreadName() {
		return "ConMan-BFT";
	}

	@Override
	public boolean checkOutputBWAvailability() {
		return true;
	}
	
	@Override
	protected boolean handleSpecialMessage(ISession session, IRawPacket raw) {
		PacketableTypes packetType = raw.getType();
		
		switch( packetType) {
		case TSESSIONINITIATIONBFT:
			TSessionInitiationBFT sInitBFT = (TSessionInitiationBFT) PacketFactory.unwrapObject(_brokerShadow, raw, true);
			handleSessionBFTInitMessage(session, sInitBFT);
			return true;
			
		default:
			return super.handleSpecialMessage(session, raw);
		}
	}
	
	protected void handleSessionBFTInitMessage(ISession session, TSessionInitiationBFT sInit) {
		switch(session.getSessionConnectionType()) {
		case S_CON_T_BYPASSING_AND_INACTIVE:
			session.setSessionConnectionType(SessionConnectionType.S_CON_T_BYPASSING_AND_ACTIVE);
			Set<ISession> newSessions = new HashSet<ISession>();
			newSessions.add(session);
			_mq.replaceSessions(null, newSessions);
			break;
			
		default:
			break;
		}
	}
}
