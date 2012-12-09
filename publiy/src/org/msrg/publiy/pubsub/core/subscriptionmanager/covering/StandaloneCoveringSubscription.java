package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;

public class StandaloneCoveringSubscription extends CoveringSubscription {

	public final SubscriptionEntry _subscriptionEntry;
	
	public StandaloneCoveringSubscription(SubscriptionEntry subscriptionEntry) {
		super();

		PRESERVE_OVERING_ORDERED = false;
		_mockSubscription = this;
		_subscriptionEntry = subscriptionEntry;
	}
	
//	protected void setMockCoveringSubscription(MockCoveringSubscription mockSubscription) {
//		_mockSubscription = mockSubscription;
//	}
	
	@Override
	public int compareTo(MockSubscription o) {
		if(o == this)
			return 0;
		else
			return -1;
	}
}
