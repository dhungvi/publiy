package org.msrg.publiy.broker.core.connectionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecoveryTypes;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.pubsub.core.packets.recovery.TempSessionRecoveryDataRepository;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.broker.core.IConnectionManagerRecovery;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.client.subscriber.ISubscriptionListener;
import org.msrg.publiy.communication.core.niobinding.IConInfoListening;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionLocal;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.ISessionManagerRecovery;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.communication.core.sessions.SessionTypes;

public class ConnectionManagerRecovery extends ConnectionManagerPS implements IConnectionManagerRecovery {

	protected Set<ISession> _outRecoverySessions = new HashSet<ISession>();
	
	ConnectionManagerRecovery(
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue,
			TRecovery_Join[] trjs) throws IOException {
		super((broker != null) ?
				("ConManRecovery-" + broker.getBrokerID()) :
				("ConManRecovery-" + getDefaultBrokerName(brokerShadow)),
				ConnectionManagerTypes.CONNECTION_MANAGER_RECOVERY, broker, brokerShadow, overlayManager, subscriptionManager, messageQueue, trjs);
	}
	
	@Override
	public ISession getDefaultOutgoingSession(InetSocketAddress remote){
		return ISessionManager.getRecoverySession(_brokerShadow, remote);
	}
	
	@Override
	public void prepareToStart() {
		ISessionLocal endedSessionLocal = _mq.addPSSessionLocalForRecoveryWithENDEDIsession();
		addSessionPrivately(_localAddress, endedSessionLocal);

		// Prepare internal data structures..
		failed(endedSessionLocal);

		// Normally prepareToStart super class
		super.prepareToStart();
	}

	protected void handleMulticastMessage(ISession session, TMulticast tm){
		super.handleMulticastMessage(session, tm);
	}
	
	protected final void sendMulticastsToOutRecovering(TMulticast[] tms, ISession outRecoveringSession){
		for ( int i=0 ; i<tms.length ; i++ ){
			IRawPacket raw = tms[i].morph(_localSequencer, outRecoveringSession);
			if ( raw == null )
				_sessionManager.send(outRecoveringSession, raw);
		}
	}
	
	protected void handleRecoveryMessage(ISession session, TRecovery tr){
		LoggerFactory.getLogger().debug(this, "Handling Recovery message: " + tr);
		session.storeSessionRecoveryData(tr);

		TRecoveryTypes trType = tr.getType();
		switch(trType){
		case T_RECOVERY_JOIN:
		case T_RECOVERY_SUBSCRIPTION:
			_mq.applySummary(tr);
			break;
			
		case T_RECOVERY_LAST_JOIN:
		case T_RECOVERY_LAST_SUBSCRIPTION:
			LoggerFactory.getLogger().debug(this, "Received TRecovery_Last: " + tr + " from: " + session);
			synchronized(_sessionsLock) 
			{
				if ( checkEndOfRecovery() )
					endRecovery();
			}
			return;
			
		default:
			throw new UnsupportedOperationException(tr.toString());
		}
		
		IOverlayManager overlayManager = getOverlayManager();
		Iterator<ISession> it = _outRecoverySessions.iterator();
		while ( it.hasNext() ){
			ISession outRecSession = it.next();
			if ( outRecSession.getRemoteAddress().equals(session.getRemoteAddress()) ){
				continue;
			}
			
			IRawPacket raw = tr.morph(outRecSession, overlayManager);
			if ( trType == TRecoveryTypes.T_RECOVERY_JOIN )
				assert outRecSession.tRecoveryJoinCanBeSent();
			else if ( trType == TRecoveryTypes.T_RECOVERY_SUBSCRIPTION )
				assert outRecSession.tRecoverySubscriptionCanBeSent();
					
			_sessionManager.send(outRecSession, raw);
		}
	}
	
	protected void handleSessionInitMessage(ISession session, TSessionInitiation sInit){
		_sessionManager.update(_brokerShadow, session, sInit);
	}

	
	///////////////////////////////////////////////////////////////////////////
	//							PRIVATE METHODS								 //
	///////////////////////////////////////////////////////////////////////////
	private void endRecovery(){
		LoggerFactory.getLogger().info(this, "Ending recovery");
		
		Iterator<ISession> it = _outRecoverySessions.iterator();
		while ( it.hasNext() ){
			ISession outRecSession = it.next();
			
			sendLastRecoveryJoinMessage(outRecSession);
			sendLastRecoverySubscribeMessage(outRecSession);
		}
		
		ISession[] activeSessions = getAllLocallyActiveSessionsPrivately();
		for ( int i=0 ; i<activeSessions.length ; i++ )
			if ( !activeSessions[i]._tempRepositoryObject.isAllRecoveryDataReceived() )
				throw new IllegalStateException("Session not received all recovery data: " + activeSessions[i]);
		
		LoggerFactory.getLogger().info(this, "RecoveryCompleted from locallyActive sessions: " + Writers.write(activeSessions));
		_broker.recoveryComplete();
	}
	
	private boolean checkEndOfRecovery(){
		boolean result = checkEndOfRecoveryPrivately();
		String str = result + "::{";
		if ( result == false ){
			List<ISession> inRecoverySessions = getAllPrimaryActiveSessionsPrivately();
			Iterator<ISession> it = inRecoverySessions.iterator();
			while ( it.hasNext() ){
				ISession session = it.next();
				str += session.getRemoteAddress() + "-"
					+ ((session._tempRepositoryObject==null)?"null":session._tempRepositoryObject.isAllRecoveryDataReceived())
					+ ((it.hasNext())?",":"");
			}
		}
		
		LoggerFactory.getLogger().info(this, "checkEndOfRecovery::" + str + "}");
		
		return result;
	}
	
	private boolean checkEndOfRecoveryPrivately(){
		if ( !_mq.getAllowedToConfirm() )
			return false;
		
//		Set<ISession> inRecoverySessions = _mq.getPSSessionsAsISessions();
		List<ISession> inRecoverySessions = getAllPrimaryActiveSessionsPrivately();
		
		ISession _onlyNonFinishedOutRecoverySession = null;
		Iterator<ISession> it = inRecoverySessions.iterator();
		while ( it.hasNext() ){
			ISession inRecSession = it.next();
			
			if ( inRecSession.getSessionConnectionType() != SessionConnectionType.S_CON_T_ACTIVE )
				return false;
			
			TempSessionRecoveryDataRepository tempSessionRepositotryData = inRecSession.getTempSessionDataRepository();
			if ( tempSessionRepositotryData!= null && tempSessionRepositotryData.isAllRecoveryDataReceived() )
				continue;
			
			if ( _onlyNonFinishedOutRecoverySession != null )
			{
				return false;
			}
			else
			{
				_onlyNonFinishedOutRecoverySession = inRecSession;
			}
		}
		
		if ( _onlyNonFinishedOutRecoverySession == null )
			return true;
	
		if ( _onlyNonFinishedOutRecoverySession.getSessionType() == SessionTypes.ST__RECOVERY_PEER_RECOVERY )
		{
			sendLastRecoveryJoinMessage(_onlyNonFinishedOutRecoverySession);
			sendLastRecoverySubscribeMessage(_onlyNonFinishedOutRecoverySession);
		}
		return false;
	}
	
	///////////////////////////////////////////////////////////////////////////
	//						CONNECTIONMANAGER METHODS						 //
	///////////////////////////////////////////////////////////////////////////
	@Override
	protected void performConnectionManagerTypeSpecificPostRegistration(ISession newSession) {
		SessionTypes sessionType = newSession.getSessionType();
		
		switch( sessionType ){
		case ST__RECOVERY_PEER_PUBSUB: 
			break;

		case ST__RECOVERY_PEER_RECOVERY:
			_outRecoverySessions.add(newSession);
			sendAllToRecoveringAndNotEndRecovery(newSession);
			break;

		default:
			throw new IllegalArgumentException("ConnectionManagerRecovery cannot register newSession '" + newSession.getRemoteAddress() + "' with SessionType='" + sessionType + "'.");
		}
		
		if ( checkEndOfRecovery())
			endRecovery();

	}

	@Override
	protected void sendAllRecoveryDataToRecoveringAndEndRecovery(ISession session) {
		throw new UnsupportedOperationException("ConnectionManagerRecovery has not Recovered, and cannot send'All'ToRecoveringAnd'End'.");
	}
	
	@Override
	protected void sendAllToRecoveringAndNotEndRecovery(ISession recoveringSession) {
		// Flush all overlay manager to the current session
		LoggerFactory.getLogger().debug(this, "SendingALLtoRecoveringAndNOTEndRecovery: " + recoveringSession.getRemoteAddress());
		
		InetSocketAddress recoveringRemote = recoveringSession.getRemoteAddress();
		
//		Set<ISession> psSessionsAsSessions = _mq.getPSSessionsAsISessions();
//		Iterator<ISession> psSessionsAsSessionsIt = psSessionsAsSessions.iterator();
		ISession[] allSessions = getAllSessionsPrivately();
		for ( int allSessionsIndex=0 ;allSessionsIndex<allSessions.length ; allSessionsIndex++ )
//		while ( psSessionsAsSessionsIt.hasNext() )
		{
//			ISession session = psSessionsAsSessionsIt.next();
			ISession session = allSessions[allSessionsIndex];
			InetSocketAddress remote = session.getRemoteAddress();
			if ( session.getSessionConnectionType() != SessionConnectionType.S_CON_T_ACTIVE )
				continue;
			
			if ( remote.equals(recoveringRemote) )
				continue;
			
			IOverlayManager overlayManager = getOverlayManager();
			TempSessionRecoveryDataRepository tempRecoveryData = session.getTempSessionDataRepository();
			TRecovery_Join[] trjs = tempRecoveryData.getAllRecoveryJoinMessages();
			for ( int i=0 ; i<trjs.length ; i++ ){
				IRawPacket raw = trjs[i].morph(recoveringSession, overlayManager);
				if ( raw != null)
					_sessionManager.send(recoveringSession, raw);
			}
			
			TRecovery_Subscription[] trss = tempRecoveryData.getAllRecoverySubscriptionMessages();
			for ( int i=0 ; i<trss.length ; i++ ){
				IRawPacket raw = trss[i].morph(recoveringSession, overlayManager);
				if ( raw != null)
					_sessionManager.send(recoveringSession, raw);
			}
		}
	}
	
	@Override
	protected void performConnectionManagerTypeSpecificPostFailed(ISession fSession) {
		_outRecoverySessions.remove(fSession);
		
		if ( checkEndOfRecovery())
			endRecovery();
		
		return;
	}
	
	///////////////////////////////////////////////////////////////////////////
	//						ICONNECTIONMANAGER METHODS						 //
	///////////////////////////////////////////////////////////////////////////
	@Override
	public ConnectionManagerTypes getType(){
		assert _type == ConnectionManagerTypes.CONNECTION_MANAGER_RECOVERY;
		return _type;
	}
	
	@Override
	public void sendTMulticast(TMulticast tm, ITMConfirmationListener tmConfirmationListener) {
		if ( tm.getClass() != TMulticast_Conf.class )
			throw new UnsupportedOperationException("ConManRecovery-" + _brokerShadow.getBrokerID() + " - cannot sendTMulticast message: " + tm);
	}	

	@Override
	public TMulticast_Subscribe issueSubscription(Subscription subscription, ISubscriptionListener subscriptionListener) {
		LoggerFactory.getLogger().debug(this, "issuing Subscription: " + subscription);
		throw new UnsupportedOperationException("ConManRecovery does not support the subscribe operation.");
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	//							ILOGGERSOURCE METHODS						 //
	///////////////////////////////////////////////////////////////////////////
	@Override
	public LoggingSource getLogSource(){
		return LoggingSource.LOG_SRC_CON_MAN_RECOVERY;
	}
	
	///////////////////////////////////////////////////////////////////////////
	//							INIO_A_LISTENER METHODS						 //
	///////////////////////////////////////////////////////////////////////////
	@Override
	public void newIncomingConnection(IConInfoListening<?> conInfoL, IConInfoNonListening<?> newConInfoNL) {
		if ( conInfoL != _conInfoL )
			throw new IllegalStateException("ConnectionManger::newIncomingConnection(.) - ERROR, this is not my listening connection.");
		ISessionManager.attachUnknownPeerSession(newConInfoNL, true);
	}
	
	@Override
	protected ISessionManager createSessionManager(LocalSequencer localSequencer) {
		return new ISessionManagerRecovery(localSequencer, _nioBinding, this);
	}
	
	@Override
	public String getThreadName() {
		return "ConMan-Recovery";
	}
}