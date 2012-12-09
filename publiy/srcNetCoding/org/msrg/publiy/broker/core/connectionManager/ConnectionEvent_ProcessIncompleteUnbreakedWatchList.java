package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class ConnectionEvent_ProcessIncompleteUnbreakedWatchList extends ConnectionEvent {

	public ConnectionEvent_ProcessIncompleteUnbreakedWatchList(LocalSequencer localSequencer) {
		super(localSequencer, ConnectionEventType.CONNECTION_EVENT_PROCESS_INCOMPLETE_UNBREAKED_WATCH_LIST);
		
	}

	@Override
	public String toString() {
		return _eventType.toString();
	}

}
