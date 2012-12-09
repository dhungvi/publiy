package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.IPSCodedBatch;

public class ConnectionEvent_DecodedContentReady extends ConnectionEvent {

	public final boolean _success;
	public final IPSCodedBatch _psDecodedBatch;
	
	public ConnectionEvent_DecodedContentReady(
			LocalSequencer localSequence, boolean success, IPSCodedBatch psDecodedBatch) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_DECODED_CONTENT_READY);
		
		_success = success;
		_psDecodedBatch = psDecodedBatch;
	}

	@Override
	public String toString() {
		return "CONNECTION_EVENT_DECODED_CONTENT_READY";
	}
}
