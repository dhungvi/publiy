package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class ConnectionEvent_BFTDackSend extends ConnectionEvent {

	protected static int COUNTER = 0;
	
	protected final int _counter;
	
	protected ConnectionEvent_BFTDackSend(LocalSequencer localSequence) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_BFT_DACK_SEND);
		_counter = COUNTER++;
	}

	@Override
	public String toString() {
		return "#" + _counter;
	}
}

