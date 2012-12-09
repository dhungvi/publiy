package org.msrg.publiy.broker.core.connectionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.multipath.loadweights.LoadWeightRepository;

import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;

public class ConnectionManagerMP extends AbstractConnectionManagerMP {
	
	protected ConnectionManagerMP(
			String connectionManagerName,
			ConnectionManagerTypes type,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue,
			TRecovery_Join[] trjs,
			LoadWeightRepository loadWeightRepo) throws IOException{
		super(connectionManagerName, type, broker, brokerShadow, overlayManager, subscriptionManager, messageQueue, trjs, loadWeightRepo);
	}
	
	protected ConnectionManagerMP(ConnectionManagerTypes type, ConnectionManagerRecovery conManRecovery) {
		super(type, conManRecovery);
	}
		
	public ConnectionManagerMP(ConnectionManagerRecovery conManRecovery) {
		this(ConnectionManagerTypes.CONNECTION_MANAGER_MULTIPATH, conManRecovery);
	}
	
	public ConnectionManagerMP(ConnectionManagerJoin conManJoin) {
		this(ConnectionManagerTypes.CONNECTION_MANAGER_MULTIPATH, conManJoin);
	}
	
	protected ConnectionManagerMP(ConnectionManagerTypes type, ConnectionManagerJoin conManJoin) {
		super(type, conManJoin);
	}
	
	@Override
	protected void registerSessionPrivately(ISession newSession) {
		if (Broker.CORRELATE)
			TrafficCorrelator.getInstance().sessionRegistered(newSession, true);
		
		InetSocketAddress remote = newSession.getRemoteAddress();
		SessionConnectionType newSessionConnectionType = newSession.getSessionConnectionType();
		ISession oldSession = getSessionPrivately(remote);
		if(oldSession == null)
		{
			if(newSessionConnectionType == SessionConnectionType.S_CON_T_UNKNOWN) {
				super.registerSessionPrivately(newSession);
				return;
			}
			else
				throw new IllegalStateException(newSession + " has no oldSession and is not UNKNOWN");
		}

		Set<ISession> mqRemovedSessions = new HashSet<ISession>(), mqAddedSessions = new HashSet<ISession>();
		SessionConnectionType oldSessionConnectionType = oldSession.getSessionConnectionType();
		switch(oldSessionConnectionType) {
		case S_CON_T_CANDIDATE:
			if(newSession == oldSession)
				throw new IllegalStateException(newSession + " could not be a CANDIDATE");
			
			replaceOldSessionWithNewSession(oldSession, newSession, mqRemovedSessions, mqAddedSessions);
			newSession.transferMessages(oldSession);
			dumpAllSessions();
			_mq.dumpPSTop();
			return;
			
		case S_CON_T_SOFT:
		{
			ISession survivedSession = surviveASessionPrivately(oldSession, newSession);
			if(survivedSession != oldSession) {
				newSession.transferMessages(oldSession);
				survivedSession.setRealSession(survivedSession);
				setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_SOFT);
				_mq.substituteSession(oldSession,survivedSession);
			}
			_mq.proceed(remote);
			dumpAllSessions();
			_mq.dumpPSTop();
			return;
		}
		
		case S_CON_T_SOFTING:
		{
			ISession survivedSession = surviveASessionPrivately(oldSession, newSession);
			if(survivedSession != oldSession) {
				addSessionPrivately(remote, survivedSession);
				newSession.transferMessages(oldSession);
			}
			survivedSession.setRealSession(survivedSession);
			setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_SOFT);
			setLastReceivedSequence2(survivedSession, _localSequencer.getNext(), false, false); // true?
			updateCriticalSessions();
			mqAddedSessions.add(survivedSession);
			_mq.replaceSessions(null, mqAddedSessions);
			_mq.proceed(remote);
			dumpAllSessions();
			return;
		}
			
		case S_CON_T_ACTIVATING:
		{
			ISession survivedSession = surviveASessionPrivately(oldSession, newSession);
			if(survivedSession != oldSession) {
				addSessionPrivately(remote, survivedSession);
				newSession.transferMessages(oldSession);
			}
			survivedSession.setRealSession(survivedSession);
			setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_ACTIVE);
			setLastReceivedSequence2(survivedSession, _localSequencer.getNext(), false, false); // true?
			updateCriticalSessions();
			mqAddedSessions.add(survivedSession);
			_mq.replaceSessions(null, mqAddedSessions);
			_mq.proceed(remote);
			dumpAllSessions();
			return;
		}
			
		default:
			super.registerSessionPrivately(newSession);
		}
	}
	
	@Override
	protected void replaceOldSessionWithNewSession(ISession oldSession, ISession newSession, Set<ISession> mqAddedSessions, Set<ISession> mqRemovedSessions) {
		super.replaceOldSessionWithNewSession(oldSession, newSession, mqAddedSessions, mqRemovedSessions);
		
		SessionConnectionType oldSessionConnectionType = oldSession.getSessionConnectionType();
		newSession.setSessionConnectionType(oldSessionConnectionType);
		ISession oldRealSession = oldSession.getRealSession();
		if(oldRealSession == oldSession)
			newSession.setRealSession(newSession);
		else
			newSession.setRealSession(oldRealSession);
		_sessionManager.dropSession(oldSession, false);
		_mq.substituteSession(oldSession, newSession);
	}

	@Override
	protected ISession surviveASessionPrivately(ISession oldSession, ISession newSession) {
		if(oldSession == null) {
			InetSocketAddress remote = newSession.getRemoteAddress();
			addSessionPrivately(remote, newSession);
			return newSession;
		}
		
		if(newSession == oldSession)
			return newSession;
		
		SessionConnectionType oldSessionConnectionType = oldSession.getSessionConnectionType();
		ISession oldRealSession = oldSession.getRealSession();
		switch(oldSessionConnectionType) {
		case S_CON_T_SOFT:
		case S_CON_T_SOFTING:
		{
			newSession.setSessionConnectionType(oldSessionConnectionType);
			if(oldRealSession == oldSession)
				newSession.setRealSession(newSession);
			else
				newSession.setRealSession(oldRealSession);
			addSessionPrivately(newSession.getRemoteAddress(), newSession);
			return newSession;
		}
		
		case S_CON_T_CANDIDATE:
		{
			if(oldRealSession == oldSession)
				throw new IllegalStateException(oldRealSession + " vs. " + newSession);
			newSession.setSessionConnectionType(oldSessionConnectionType);
			newSession.setRealSession(oldRealSession);
			addSessionPrivately(newSession.getRemoteAddress(), newSession);
			return newSession;
		}
		
		default:
			break;
		}
		
		return super.surviveASessionPrivately(oldSession, newSession);
	}

	@Override
	protected void handleConnectionEvent_renewConnection(ConnectionEvent_renewConnection connEvent) {
		ISession session = connEvent._session;
		switch(session.getSessionConnectionType()) {
		case S_CON_T_SOFT:
			session.setSessionConnectionType(SessionConnectionType.S_CON_T_SOFTING);
			session.setRealSession(null);
			updateCriticalSessions();
			Set<ISession> mqRemoveSet = new HashSet<ISession>();
			mqRemoveSet.add(session);
			_mq.replaceSessions(mqRemoveSet, null);
			super.renewSessionsConnection(session);
			return;

		case S_CON_T_CANDIDATE:
			return;

		case S_CON_T_SOFTING:
			super.renewSessionsConnection(session);
			return;

		case S_CON_T_ACTIVE:
			session.setSessionConnectionType(SessionConnectionType.S_CON_T_ACTIVATING);
			super.renewSessionsConnection(session);
			
		default:
			break;
		}

		super.handleConnectionEvent_renewConnection(connEvent);
	}
	
	@Override
	protected void failedSession(ISession failedSession) {
		synchronized(_sessionsLock) {
			switch(failedSession.getSessionConnectionType()) {
			case S_CON_T_CANDIDATE:
			case S_CON_T_SOFTING:
			case S_CON_T_SOFT:
				setSessionsConnectionType(failedSession, SessionConnectionType.S_CON_T_FAILED);
				removeFromAllSessionsPrivately(failedSession.getRemoteAddress());
				Set<ISession> mqRemoveSessions = new HashSet<ISession>();
				mqRemoveSessions.add(failedSession);
				_mq.replaceSessions(mqRemoveSessions, null);
				updateCriticalSessions();
				return;
				
			default:
				break;
			}
		}

		if(failedSession.isLocallyActive()) {
			super.failedSession(failedSession);
			updateCriticalSessions();
		} else {
			super.failedSession(failedSession);
		}
		
		// check all sessionsConsiderationList
		reconsiderAllConsideredSessions();
	}
	
	@Override
	public String getThreadName() {
		return "ConMan-MP";
	}
}
