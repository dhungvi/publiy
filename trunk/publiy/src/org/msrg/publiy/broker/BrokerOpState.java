package org.msrg.publiy.broker;

public enum BrokerOpState {

	BRKR_UNKNOWN, 
	
	BRKR_RECOVERY,
	BRKR_PUBSUB_JOIN,
	BRKR_PUBSUB_PS,
	BRKR_PUBSUB_DEPART,
	
	BRKR_END;
	
}
