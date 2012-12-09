package org.msrg.publiy.pubsub.core;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Set;

import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_UnSubscribe;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.pubsub.core.subscriptionmanager.LocalSubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;

public interface ISubscriptionManager extends ISubscriptionManagerQueriable {
	
	public InetSocketAddress getLocalAddress();
	public TRecovery_Subscription[] getAllSummary();
	public TRecovery_Subscription[] getAllSummary(InetSocketAddress remote, IOverlayManager overlayManager);
	public TRecovery_Subscription[] getLocalSummary(InetSocketAddress remote, IOverlayManager overlayManager);
	public void addNewSubscriptionEntry(SubscriptionEntry newSubEntry);
	public void addNewLocalSubscriptionEntry(LocalSubscriptionEntry newSubEntry);
	public void applySummary(TRecovery_Subscription trs);
	public boolean dumpSubscriptions(String dumpFileName);	
	
	public Set<InetSocketAddress> getMatchingSet(Publication publication);
	public Set<InetSocketAddress> getLocalMatchingSet(Publication publication);
	public Set<Subscription> getMatchingSubscriptionSet(Publication publication);
	
//	public void handleLocalMessage(TMulticast_Subscribe tms, ISubscriptionListener subscriptionListener);
	public void handleMessage(TMulticast_Subscribe tms);
	public void handleMessage(TMulticast_UnSubscribe tmus);
	public void applyAllSubscriptionSummary(TRecovery_Subscription[] trss);
	public void informLocalSubscribers(TMulticast_Publish tmp);
	String getSubscriptionsDumpFilename();
	
	public Collection<LocalSubscriptionEntry> getLocalSubscriptionEntries();
	public Collection<SubscriptionEntry> getSubscriptionEntries();
	
	public IBrokerShadow getBrokerShadow();

}
