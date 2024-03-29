package org.msrg.publiy.utils.log.trafficcorrelator;

public enum TrafficCorrelationEventType {
	
	DUPLICATE,
	ARRIVED_FROM,
	SENT_TO,
	LOST,
	S_TM_TRANSFER,
	
	S_OUT_PENDING_ENQ,
	S_OUT_PENDING_DEQ,
	
	C_IN_PENDING_ENQ,
	C_IN_PENDING_DEQ,
	C_OUT_PENDING_ENQ,
	C_OUT_PENDING_DEQ,
	
	CONF_ARRIVED_FROM,
	CONF_SENT_TO,
	
	MQ_DUPLICATE,
	MQ_INSERTED,
	MQ_DISCARDED,
	MQ_PROC,
	MQ_PROC_CONF,
	MQ_MQN_CREATED,
	MQ_MQN_CONF_CREATE,
	MQ_COMPUTE_WORKING_SET,
	
	CRITICAL_UPDATE,
	S_RENEW,
	S_REG,
	S_ADD,
	S_DROP,
	S_RMV,
	S_SUBSTITUTE,
	S_REAL_SESSION,
	S_CONNECTION_TYPE,
	S_TYPE,
	S_LST_RCVD,
	
	S_CONNECTION_SET,
	CONNECTION_CREATED,
	CONNECTION_STATE_CHANGED,
	CONNECTION_DISCARDED,
	CONNECTION_DISCONNECTED,
	
	FD_CONNECTION_TIMEOUT,
	
	PSS_ADD,
	PSS_RSET,
	PSS_RMV,
	
	CONFIRMED,
	ACKNOWLEDGED,
	
	FORKED,
	
	END;

}
