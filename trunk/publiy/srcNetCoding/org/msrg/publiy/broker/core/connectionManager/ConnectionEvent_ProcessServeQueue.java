package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class ConnectionEvent_ProcessServeQueue extends ConnectionEvent {

	public ConnectionEvent_ProcessServeQueue(LocalSequencer localSequencer) {
		super(localSequencer, ConnectionEventType.CONNECTION_EVENT_PROCESS_SERVE_QUEUE);
	}

	@Override
	public String toString() {
		return _eventType.toString();
	}
}
