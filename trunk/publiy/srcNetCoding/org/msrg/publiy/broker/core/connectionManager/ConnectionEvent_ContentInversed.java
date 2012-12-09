package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class ConnectionEvent_ContentInversed extends ConnectionEvent {

	public final Sequence _contentSequence;
	
	public ConnectionEvent_ContentInversed(LocalSequencer localSequence, Sequence contentSequence) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_CONTENT_INVERSED);
		
		_contentSequence = contentSequence;
	}

	@Override
	public String toString() {
		return "CONNECTION_EVENT_CONTENT_INVERSED";
	}

}
