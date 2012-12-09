package org.msrg.publiy.broker.core.connectionManager.connectionStateMachine;

import java.util.LinkedList;
import java.util.List;

public class SessionActions {
	
	private final GlobalSessions _globalSessions;
	private final List<SessionAction> _actions = new LinkedList<SessionAction>();
	
	SessionActions(GlobalSessions globalConnections) {
		_globalSessions = globalConnections;
	}

	GlobalSessions getGlobalSessions() {
		return _globalSessions;
	}
	
	void addSessionAction(SessionAction action) {
		_actions.add(action);
	}
}
