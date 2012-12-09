package org.msrg.publiy.pubsub.core.packets.recovery;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.publishSubscribe.Subscription;

public class TRecovery_Subscription_withActiveFrom extends
		TRecovery_Subscription {

	protected final InetSocketAddress _activeFrom;
	
	public TRecovery_Subscription_withActiveFrom(Subscription subscription,
			Sequence sourceSequence, InetSocketAddress from, InetSocketAddress activeFrom) {
		super(subscription, sourceSequence, from);
		
		_activeFrom = activeFrom;
	}

	public InetSocketAddress getActiveFrom() {
		return _activeFrom;
	}
}
