package org.msrg.publiy.pubsub.core.messagequeue;

import org.msrg.publiy.broker.PubForwardingStrategy;

public interface IMessageQueueMP {

	public PubForwardingStrategy getPublicationForwardingStrategy();
	
}
