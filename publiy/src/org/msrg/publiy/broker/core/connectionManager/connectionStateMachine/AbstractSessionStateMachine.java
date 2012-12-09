package org.msrg.publiy.broker.core.connectionManager.connectionStateMachine;

import org.msrg.publiy.communication.core.sessions.ISession;

public abstract class AbstractSessionStateMachine {

	SessionActions processSessionEvents(
			GlobalSessions globalConnections,
			ISession session,
			SessionEventType event) {
		SessionActions sessionActions = new SessionActions(globalConnections);
		switch (event) {
		case SESSION_E_MAKE_OUTCOMING:
			processSessionMakeOutgoing(sessionActions, session);
			break;
			
		case SESSION_E_CONNECTED:
			processSessionConnected(sessionActions, session);
			break;
			
		case SESSION_E_INCOMING:
			processIncomingSession(sessionActions, session);
			break;

		case SESSION_E_JOINING:
			processSessionJoin(sessionActions, session);
			break;
			
		case SESSION_E_REGISTERED:
			processSessionRegistered(sessionActions, session);
			break;
			
		case SESSION_E_DISCONNECTED:
			processSessionDisconnected(sessionActions, session);
			break;
			
		case SESSION_E_MAKE_CANDIDATE:
			processSessionMakeCandidate(sessionActions, session);
			break;
			
		case SESSION_E_MAKE_SOFT:
			processSessionMakeSoft(sessionActions, session);
			break;
			
		case SESSION_E_DECANDIDATE:
			processSessionDecandidate(sessionActions, session);
			break;
			
		default:
			throw new UnsupportedOperationException("Unknown session event: " + event);
		}
		
		return sessionActions;
	}
	
	protected void addSessionResetRetryAction(SessionActions sessionActions, ISession session) {
		SessionAction action = new SessionAction(session, SessionActionTypes.SESSION_A_RESET_RETRY);
		sessionActions.addSessionAction(action);
	}
	


	protected abstract void processSessionMakeOutgoing(
			SessionActions sessionActions, ISession session);
	protected abstract void processSessionConnected(
			SessionActions sessionActions, ISession session);
	protected abstract void processIncomingSession(
			SessionActions sessionActions, ISession session);
	protected abstract void processSessionJoin(
			SessionActions sessionActions, ISession session);
	protected abstract void processSessionRegistered(
			SessionActions sessionActions, ISession session);
	protected abstract void processSessionDisconnected(
			SessionActions sessionActions, ISession session);
	protected abstract void processSessionMakeCandidate(
			SessionActions sessionActions, ISession session);
	protected abstract void processSessionMakeSoft(
			SessionActions sessionActions, ISession session);
	protected abstract void processSessionDecandidate(
			SessionActions sessionActions, ISession session);
}
