package org.msrg.publiy.broker.controller.discovery;

import java.io.IOException;

import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManager;

public class SubscriptionsDiscoverer extends AbstractDiscoverer {

	private final SubscriptionManager _subscriptionManager;
	
	SubscriptionsDiscoverer(SubscriptionManager subscriptionManager){
		_subscriptionManager = subscriptionManager;
	}
	
	@Override
	protected String getFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void runMe() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
