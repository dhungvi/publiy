package org.msrg.publiy.client.subscriber;

import org.msrg.publiy.broker.info.SubscriptionInfo;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;

public interface ISubscriber extends ISubscriptionListener {

	public TMulticast_Subscribe subscribe(Subscription subscription, ISubscriptionListener subscriptionListener);//I_TM_Confirmation_Listener tmConfirmationListener);
	public SubscriptionInfo[] getUnconfirmedSubscriptions();
	public SubscriptionInfo[] getConfirmedSubscriptionInfos();
	
}
