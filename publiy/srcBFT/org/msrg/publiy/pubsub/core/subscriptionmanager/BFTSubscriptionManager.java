package org.msrg.publiy.pubsub.core.subscriptionmanager;

import org.msrg.publiy.pubsub.core.IBFTSubscriptionManager;
import org.msrg.publiy.broker.IBFTBrokerShadow;

public class BFTSubscriptionManager extends SubscriptionManager implements IBFTSubscriptionManager {

	public BFTSubscriptionManager(IBFTBrokerShadow brokerShadow, String dumpFileName, boolean shouldLog) {
		super(brokerShadow, dumpFileName, shouldLog);
	}

}
