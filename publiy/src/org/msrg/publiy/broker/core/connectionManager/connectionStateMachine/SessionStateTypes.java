package org.msrg.publiy.broker.core.connectionManager.connectionStateMachine;

public enum SessionStateTypes {
	
	Session_S_UNKNOWN,

	Session_S_ACTIVE,
	Session_S_ACTIVATING,
	Session_S_INACTIVE,
	Session_S_INACTIVATING,

	Session_S_SOFT,
	Session_S_SOFTING,
	Session_S_CANDIDATE,

}
