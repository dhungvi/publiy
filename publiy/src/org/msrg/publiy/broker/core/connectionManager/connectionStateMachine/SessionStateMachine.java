package org.msrg.publiy.broker.core.connectionManager.connectionStateMachine;

import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;

public class SessionStateMachine extends AbstractSessionStateMachine {

	@Override
	protected void processIncomingSession(SessionActions sessionActions,
			ISession session) {
		SessionAction action = new SessionAction(session, SessionActionTypes.SESSION_A_SESSION_ADD);
		sessionActions.addSessionAction(action);
	}

	@Override
	protected void processSessionMakeOutgoing(SessionActions sessionActions,
			ISession session) {
		SessionAction sessionAddAction = new SessionAction(session, SessionActionTypes.SESSION_A_SESSION_ADD);
		sessionActions.addSessionAction(sessionAddAction);
		
		SessionAction makeOutgoingAction = new SessionAction(session, SessionActionTypes.SESSION_A_MAKE_OUTGOING);
		sessionActions.addSessionAction(makeOutgoingAction);
	}

	@Override
	protected void processSessionConnected(SessionActions sessionActions, ISession session) {
		addSessionResetRetryAction(sessionActions, session);
	}

	@Override
	protected void processSessionDecandidate(SessionActions sessionActions,
			ISession session) {

	}

	@Override
	protected void processSessionDisconnected(SessionActions sessionActions,
			ISession session) {
		SessionConnectionType sessionConnectionType = session.getSessionConnectionType();
	}

	@Override
	protected void processSessionJoin(SessionActions sessionActions,
			ISession session) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void processSessionMakeCandidate(SessionActions sessionActions,
			ISession session) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void processSessionMakeSoft(SessionActions sessionActions,
			ISession session) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void processSessionRegistered(SessionActions sessionActions,
			ISession session) {
		// TODO Auto-generated method stub

	}
}
