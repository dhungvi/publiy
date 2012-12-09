package org.msrg.publiy.broker.networkcoding.connectionManager.client;

import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEventType;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class ConnectionEvent_SendPListRequest extends ConnectionEvent {

	public final Sequence _contentSequence;
	public final boolean _fromSource;
	public final int _rows;
	public final int _cols;
	
	public ConnectionEvent_SendPListRequest(
			LocalSequencer localSequencer, Sequence contentSequence,
			boolean fromSource, int rows, int cols) {
		super(localSequencer, ConnectionEventType.CONNECTION_EVENT_SEND_PLIST_REQUESTS);
		
		_contentSequence = contentSequence;
		_fromSource = fromSource;
		_rows = rows;
		_cols = cols;
	}

	@Override
	public String toString() {
		return _eventType.toString() + ":" + _contentSequence;
	}
}
