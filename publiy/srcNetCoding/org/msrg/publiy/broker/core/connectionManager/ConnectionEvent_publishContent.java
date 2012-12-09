package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;

public class ConnectionEvent_publishContent extends ConnectionEvent {

	public final PSSourceCodedBatch _psSourceCodedBatch;
	
	public ConnectionEvent_publishContent(
			LocalSequencer localSequence, PSSourceCodedBatch psSourceCodedBatch) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_PUBLISH_CONTENT);
		
		_psSourceCodedBatch = psSourceCodedBatch;
	}

	@Override
	public String toString() {
		return "CONNECTION_EVENT_PUBLISH_CONTENT";
	}

}
