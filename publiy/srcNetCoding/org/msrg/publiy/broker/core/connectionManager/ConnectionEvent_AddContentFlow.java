package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class ConnectionEvent_AddContentFlow extends ConnectionEvent {

	public final Content _content;
	
	public ConnectionEvent_AddContentFlow(LocalSequencer localSequence, Content content) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_ADD_CONTENT_FLOW);
		
		_content = content;
	}

	@Override
	public String toString(){
		return "CONNECTION_EVENT_ADD_CONTENT_FLOW" + _content;
	}
}
