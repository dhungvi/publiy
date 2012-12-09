package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class ConnectionEvent_ProcessNextFlow extends ConnectionEvent {

	protected ConnectionEvent_ProcessNextFlow(LocalSequencer localSequencer) {
		super(localSequencer, ConnectionEventType.CONNECTION_EVENT_PROCESS_NEXT_NETCODING_FLOW);
	}

	@Override
	public String toString() {
		return _eventType.toString();
	}
}
