package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.IConnectionManagerPS;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.IWorkingOverlayManager;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.subscriptionmanager.multipath.IWorkingSubscriptionManager;

public interface IConnectionManagerMP extends IConnectionManagerPS {

	public IWorkingOverlayManager getWorkingOverlayManager();
	public IWorkingSubscriptionManager getWorkingSubscriptionManager();
	
	public void updateWorkingOverlayManager(IOverlayManager overlayManager, TMulticast tm);
	public void updateWorkingSubscriptionManager(TMulticast tm);
	public PubForwardingStrategy getPublicationForwardingStrategy();
	
	public void sendLoadWeights();
	
}
