package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

class ConnectionEvent_NodeSuspected extends ConnectionEvent {

	protected final BFTSuspecionReason _reason;
	
	protected ConnectionEvent_NodeSuspected(LocalSequencer localSequence, BFTSuspecionReason reason) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_NODE_SUSPECTED);
		
		_reason = reason;
	}

	@Override
	public String toString() {
		return "CONNECTION_EVENT_NODE_SUSPECTED: " + _reason;
	}
}