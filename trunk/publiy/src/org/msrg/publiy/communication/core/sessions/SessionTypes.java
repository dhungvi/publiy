package org.msrg.publiy.communication.core.sessions;

import java.io.Serializable;

public enum SessionTypes implements Serializable {
	
	ST_DUMMY					(false		, false	, "DUMMY"),

	ST_UNINITIATED				(false		, false	, "UNINIT"),
	
	ST_PUBSUB_INITIATED			(false		, false	, "PSINIT"),
	ST_PUBSUB_CONNECTED			(false		, false	, "PSCONN"),
	ST_RECOVERY_INITIATED		(false		, false	, "RINIT"),
	ST_RECOVERY_CONNECTED		(false		, false	, "RCONN"),
	
	ST_PEER_UNKNOWN				(false		, false	, "UNKNOWN"),
	
	ST__PUBSUB_PEER_PUBSUB		(true		, false	, "PS/PS"),
	ST__PUBSUB_PEER_RECOVERY	(true		, true	, "PS/R"),
	ST__RECOVERY_PEER_PUBSUB	(true		, false	, "R/PS"),
	ST__RECOVERY_PEER_RECOVERY	(true		, true	, "R/R"),
	
	ST_END						(false		, false	, "END");
	
	public final boolean _pubSubEnabled;
	public final String _shortName;
	public final boolean _isPeerRecovering;
	
	SessionTypes(boolean pubSubEnabled, boolean isPeerRecovering, String shortName){
		_pubSubEnabled = pubSubEnabled;
		_isPeerRecovering = isPeerRecovering;
		_shortName = shortName;
	}

	public String toStringShort() {
		return _shortName;
	}
	
}
