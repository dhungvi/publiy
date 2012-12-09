package org.msrg.publiy.broker.core.connectionManager.connectionStateMachine;

import org.msrg.publiy.communication.core.sessions.ISession;

public class SessionAction {
	
	final ISession _session;
	final SessionActionTypes _actionType;

	SessionAction(ISession session, SessionActionTypes actionType) {
		_session = session;
		_actionType = actionType;
	}
}
