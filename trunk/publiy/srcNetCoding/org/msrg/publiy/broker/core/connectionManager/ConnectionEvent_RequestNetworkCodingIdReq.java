package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class ConnectionEvent_RequestNetworkCodingIdReq extends ConnectionEvent {

	public final Content _content;
	
	public ConnectionEvent_RequestNetworkCodingIdReq(
			LocalSequencer localSequence, Content content) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_SEND_CONTENTID_REQ);
		
		_content = content;
	}

	@Override
	public String toString() {
		return "CONNECTION_EVENT_SEND_CONTENTID_REQ: " + _content.getSourceSequence().toStringVeryVeryShort();
	}
	
}
