package org.msrg.publiy.broker.core.connectionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Depart;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_UnSubscribe;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;

import org.msrg.publiy.broker.core.IConnectionManagerPS;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.client.subscriber.ISubscriptionListener;
import org.msrg.publiy.communication.core.niobinding.IConInfoListening;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.packet.types.TConf_Ack;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.ISessionManagerPS;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.communication.core.sessions.SessionTypes;

public class ConnectionManagerPS extends ConnectionManager implements IConnectionManagerPS {
	
	protected ConnectionManagerPS(
			String connectionManagerName,
			ConnectionManagerTypes type,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue,
			TRecovery_Join[] trjs) throws IOException{
		super(connectionManagerName, type, broker, brokerShadow, overlayManager, subscriptionManager, messageQueue, trjs);
	}	
	
	@Override
	public ISession getDefaultOutgoingSession(InetSocketAddress remote) {
		return ISessionManager.getPubSubSession(_brokerShadow, remote, false);
	}
	
	protected boolean performConnectionManagerTypeSpecificPreRegistration(ISession newSession) {
		return true;
	}
	
	public static ConnectionManagerPS createSecretConnectionManagerPS(
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager, IMessageQueue messageQueue) throws IOException{
		return new ConnectionManagerPS(brokerShadow, overlayManager, subscriptionManager, messageQueue);
	}
	
	protected ConnectionManagerPS(
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue) throws IOException{
		this("ConManPS-" + getDefaultBrokerName(brokerShadow),
			ConnectionManagerTypes.CONNECTION_MANAGER_PUBSUB, null, brokerShadow, overlayManager, subscriptionManager, messageQueue, null);
	}
	
	@Override
	protected ISessionManager createSessionManager(LocalSequencer localSequencer) {
		return new ISessionManagerPS(localSequencer, _nioBinding, this);
	}
	
	protected ConnectionManagerPS(ConnectionManagerJoin conManJoin) {
		this(conManJoin, "ConManPS-", ConnectionManagerTypes.CONNECTION_MANAGER_PUBSUB);
	}
	
	public ConnectionManagerPS(ConnectionManagerJoin conManJoin, String connManName, ConnectionManagerTypes connManType) {
		super((conManJoin._brokerShadow != null) ?
				(connManName + conManJoin._brokerShadow.getBrokerID()) :
				(connManName + getDefaultBrokerName(conManJoin._localSequencer)),
				connManType, conManJoin);
		
		LoggerFactory.getLogger().debug(this, "Applying temp session rep data of joinSessionInProgress: " + conManJoin._joinSessionInProgress);
		if(conManJoin._joinSessionInProgress != null) {
			applyTempSessionRepositoryData(conManJoin._joinSessionInProgress);
			if(conManJoin._joinSessionInProgress.getSessionConnectionType() == SessionConnectionType.S_CON_T_ACTIVE)
				conManJoin._joinSessionInProgress.setSessionConnectionType(SessionConnectionType.S_CON_T_UNKNOWN);

			registerSession(conManJoin._joinSessionInProgress);
		}
		
		Set<Entry<InetSocketAddress, ISession>> ent1 = conManJoin._pendingSessionsWhileJoin.entrySet();
		Iterator<Entry<InetSocketAddress, ISession>> entIt1 = ent1.iterator();
		while ( entIt1.hasNext()) {
			ISession pendingSession = entIt1.next().getValue();

			registerSession(pendingSession);
			applyTempSessionRepositoryData(pendingSession);
		}
		
		Set<ISession> set = getSessionsOfSessionConnectionType(SessionConnectionType.S_CON_T_ACTIVE);
		Iterator<ISession> it2 = set.iterator();
		while ( it2.hasNext()) {
			ISession recoverySession = it2.next();
			applyTempSessionRepositoryData(recoverySession);
		}
	}
	
	protected ConnectionManagerPS(ConnectionManagerRecovery conManRecovery) {
		this(conManRecovery, "ConManPS-", ConnectionManagerTypes.CONNECTION_MANAGER_PUBSUB);
	}
	
	public ConnectionManagerPS(ConnectionManagerRecovery conManRecovery, String connManName, ConnectionManagerTypes connManType) {
		super((conManRecovery._brokerShadow != null) ?
				(connManName + conManRecovery._brokerShadow.getBrokerID()) :
				(connManName + getDefaultBrokerName(conManRecovery._localSequencer)),
				connManType, conManRecovery);
		
		Set<ISession> activeSet = getSessionsOfSessionConnectionType(SessionConnectionType.S_CON_T_ACTIVE);
		Iterator<ISession> it = activeSet.iterator();
		while ( it.hasNext())
		{
			ISession recoverySession = it.next();
			applyTempSessionRepositoryData(recoverySession);
			recoverySession.clearTempRepositoryData();
		}
		
		ISession[] sessions = getAllSessionsPrivately();
		for ( int i=0 ; i<sessions.length ; i++) {
			InetSocketAddress remote = sessions[i].getRemoteAddress();
			Sequence lastReceivedSequence = getLastReceivedSequence(remote);
			_sessionManager.updateSessionTypeAfterLocalRecoveryIsFinished(_brokerShadow, sessions[i], lastReceivedSequence);
		}
	}
	
	protected final void checkForDirectJoin(ISession session, TMulticast_Join tmj) {
		InetSocketAddress joinPointAddress = tmj.getJoinPoint();
		InetSocketAddress joiningAddress = tmj.getJoiningNode();

		if(!joinPointAddress.equals(this._localAddress))
			return;

		LoggerFactory.getLogger().debug(this, "checking for direct join (true)");
		Set<ISession> newSessions = new HashSet<ISession>();
		newSessions.add(session);
		getOverlayManager().joinNode(joiningAddress);

		// This is a direct join.
		// Prepare the recovery messages.
		sendAllOverlayEntrySummaries(session);
		sendLastRecoveryJoinMessage(session);
		
		sendAllSubscriptionEntrySummaries(session);
		sendLastRecoverySubscribeMessage(session);

		registerUnjoinedSessionAsJoined(joiningAddress);
//		_mq.replaceSessions(null, newSessions);

	}

	@Override
	public void tmConfirmed(TMulticast tm) {
		LoggerFactory.getLogger().debug(this, "TMConfirmed: " + tm);
		TMulticastTypes type = tm.getType();
		switch(type) {
		case T_MULTICAST_JOIN:
			TMulticast_Join tmj = (TMulticast_Join) tm;
			throw new IllegalStateException("ConManPS::tmConfirmed - ERROR, there should be no join Confs '" + tmj + "'.");

		case T_MULTICAST_DEPART:
		case T_MULTICAST_PUBLICATION_MP:
		case T_MULTICAST_PUBLICATION:
		case T_MULTICAST_SUBSCRIPTION:
		case T_MULTICAST_UNSUBSCRIPTION:
			break;
			
		default:
			throw new IllegalStateException("Unknown TMessage type class: " + tm);
		}
	}
	
	@Override
	protected void handleTConfAckMessage(ISession session, TConf_Ack tConfAck) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, "HanldlePS: " + tConfAck);
		
		_mq.receiveTConfAck(tConfAck);
	}
	
	@Override
	protected void handleMulticastMessage(ISession session, TMulticast tm) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, "HanldlePS: " + tm);
		
		//TODO: URGENT! Remove below
		InetSocketAddress remote = session.getRemoteAddress();
		if(remote != null && !tm.getSenderAddress().equals(remote))
			throw new IllegalStateException(session + " got " + tm);
		
		performMulticastMessageSendersTest(session, tm);
		TMulticastTypes type = tm.getType();
		switch(type) {
		case T_MULTICAST_JOIN:
			TMulticast_Join tmj = (TMulticast_Join) tm;
			checkForDirectJoin(session, tmj);
			_mq.addNewMessage(tm);
			break;
		
		case T_MULTICAST_DEPART:
			TMulticast_Depart tmd = (TMulticast_Depart) tm;
			_mq.addNewMessage(tmd);
			break;

		case T_MULTICAST_PUBLICATION_MP:
		case T_MULTICAST_PUBLICATION:
			TMulticast_Publish tmp = (TMulticast_Publish) tm;
			_mq.addNewMessage(tmp);
			break;
		
		case T_MULTICAST_SUBSCRIPTION:
			TMulticast_Subscribe tms = (TMulticast_Subscribe) tm;
			_mq.addNewMessage(tms);
			break;

		case T_MULTICAST_UNSUBSCRIPTION:
			TMulticast_UnSubscribe tmus = (TMulticast_UnSubscribe) tm;
			_mq.addNewMessage(tmus);
			break;
		
		case T_MULTICAST_CONF:
			TMulticast_Conf conf = (TMulticast_Conf) tm;
			_mq.addNewMessage(conf);
			if(Broker.CORRELATE)
				TrafficCorrelator.getInstance().confirmationInsertedIntoMQ(session, conf);
			
			acknowledgeTMulticast_Conf(session, conf, false);
			break;
		
		default:
			throw new IllegalArgumentException("Unknown TMessage type ('" + type + "') class: " + tm);
		}
	}

	@Override
	protected void handleRecoveryMessage(ISession session, TRecovery tr) {
		if(tr.isLastJoin())
			return;
		
		else if(tr.isLastSubscription())
			return;

		else if(session.isLocallyActive()) {
			IllegalStateException ex = new IllegalStateException("ConnectionManagerPS (should not) recieved from[" + session + "]:" + tr);
			LoggerFactory.getLogger().infoX(this, ex, "Damn it!");
			if(!Broker.RELEASE)
				throw ex;
		}
	}

	@Override
	protected void handleSessionInitMessage(ISession session, TSessionInitiation sInit) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, "HanldlePS: " + sInit);
		
		if(!Broker.RELEASE && !Broker.DEBUG)
			LoggerFactory.getLogger().info(this, "HanldlePS: " + sInit);
		
		_sessionManager.update(_brokerShadow, session, sInit);
	}

	@Override
	public void unsubscribe(TMulticast_UnSubscribe tmus) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, "HanldlePS: " + tmus);

		_mq.addNewMessage(tmus,this);
	}
	
	@Override
	public ConnectionManagerTypes getType() {
		assert _type == ConnectionManagerTypes.CONNECTION_MANAGER_PUBSUB;
		return _type;
	}

	@Override
	protected void sendAllRecoveryDataToRecoveringAndEndRecovery(ISession session) {
		LoggerFactory.getLogger().debug(this, "SendingALLtoRecoveringAndEndRecovery: " + session.getRemoteAddress());
		// Flush all overlay manager to the current session
		sendAllOverlayEntrySummaries(session);
		sendLastRecoveryJoinMessage(session);

		sendAllSubscriptionEntrySummaries(session);
		sendLastRecoverySubscribeMessage(session);		
	}

	@Override
	protected void sendAllToRecoveringAndNotEndRecovery(ISession session) {
		LoggerFactory.getLogger().debug(this, "SendingALLtoRecoveringAndEndRecovery: " + session.getRemoteAddress());
		throw new UnsupportedOperationException("This is a ConManPS - should not use this.");
	}

	@Override
	public boolean upgradeUnjoinedSessionToActive(InetSocketAddress completedJoiningAddress) {
		synchronized (_sessionsLock) {
			LoggerFactory.getLogger().debug(this, "upgrading unjoinedSession To active: " + completedJoiningAddress);
			ISession completedJoiningSession = getSessionPrivately(completedJoiningAddress);
			if(completedJoiningSession == null)
				return false;
			
			SessionConnectionType joiningSessionConnectionType = completedJoiningSession.getSessionConnectionType();
			if(joiningSessionConnectionType == SessionConnectionType.S_CON_T_UNJOINED) {
				completedJoiningSession.setSessionConnectionType(SessionConnectionType.S_CON_T_ACTIVE);
			}
			else
				return false;
		}
		
		return true;
	}

	@Override
	protected void performConnectionManagerTypeSpecificPostRegistration(ISession newSession) {
		SessionTypes sessionType = newSession.getSessionType();
		
		switch(sessionType) {
		case ST__PUBSUB_PEER_PUBSUB:
			return;
		
		case ST__PUBSUB_PEER_RECOVERY:
			sendAllRecoveryDataToRecoveringAndEndRecovery(newSession);
			break;

		default:
			throw new IllegalArgumentException("ConnectionManagerPS cannot register newSession '" + newSession.getRemoteAddress() + "' with SessionType='" + sessionType + "'.");
		}
	}

	@Override
	protected void performConnectionManagerTypeSpecificPostFailed(ISession session) {
		return;
	}

	@Override
	public TMulticast_Subscribe issueSubscription(
			Subscription subscription, ISubscriptionListener subscriptionListener) {
		TMulticast_Subscribe tms = new TMulticast_Subscribe(subscription,_localAddress, _localSequencer);
		tms._subscriber = subscriptionListener;
		
		LoggerFactory.getLogger().debug(this, "Issuing Subscription: " + tms);
		
		sendTMulticast(tms, subscriptionListener);
		return tms;
	}
	
	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_CON_MAN_PS;
	}

	//INIO_A_Listener
	@Override
	public void newIncomingConnection(IConInfoListening<?> conInfoL, IConInfoNonListening<?> newConInfoNL) {
		if(conInfoL != _conInfoL)
			throw new IllegalStateException("ConnectionManger::newIncomingConnection(.) - ERROR, this is not my listening connection.");
		ISessionManager.attachUnknownPeerSession(newConInfoNL, false);
	}

	@Override
	protected void connectionManagerJustStarted() {
		super.connectionManagerJustStarted();
		
		if(_brokerShadow.isFastConf()) {
			scheduleNextPurgeMQ();
			scheduleNextDack();
		}
	}

	@Override
	public InOutBWEnforcer getBWEnforcer() {
		if(_brokerShadow == null)
			return null;
		else
			return _broker.getBWEnforcer();
	}
	
	@Override
	public String getThreadName() {
		return "ConMan-PS";
	}
}