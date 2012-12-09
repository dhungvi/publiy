package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class ConnectionEvent_ProcessIncompleteBreakedWatchList extends ConnectionEvent {

	public ConnectionEvent_ProcessIncompleteBreakedWatchList(LocalSequencer localSequencer) {
		super(localSequencer, ConnectionEventType.CONNECTION_EVENT_PROCESS_INCOMPLETE_BREAKED_WATCH_LIST);
	}

	@Override
	public String toString() {
		return _eventType.toString();
	}
}
