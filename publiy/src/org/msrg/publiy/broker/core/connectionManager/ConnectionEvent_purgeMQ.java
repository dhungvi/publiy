package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class ConnectionEvent_purgeMQ extends ConnectionEvent{
	
	public ConnectionEvent_purgeMQ(LocalSequencer localSequence){
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_PURGE_MQ);
	}
	
	public String toString(){
		return "CONNECTION_EVENT_PURGE_MQ";
	}
}
