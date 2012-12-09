package org.msrg.publiy.pubsub.core.subscriptionmanager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.client.subscriber.DefaultSubscriptionListener;
import org.msrg.publiy.client.subscriber.ISubscriptionListener;
import org.msrg.publiy.publishSubscribe.Subscription;


public class LocalSubscriptionEntry extends SubscriptionEntry {

	ISubscriptionListener _localSubscriptionListener;

	public LocalSubscriptionEntry(
			Sequence seqID, Subscription subscription, InetSocketAddress from) {
		this(seqID, subscription, from, DefaultSubscriptionListener.getInstance());
	}

	public LocalSubscriptionEntry(
			Sequence seqID, Subscription subscription, InetSocketAddress from,
			ISubscriptionListener localSubscriptionListener) {
		super(seqID, subscription, from, true);
		_localSubscriptionListener = localSubscriptionListener;
		
		if(_localSubscriptionListener == null)
			throw new NullPointerException();
	}
	
	@Override
	public boolean isLocal() {
		return true;
	}
}
