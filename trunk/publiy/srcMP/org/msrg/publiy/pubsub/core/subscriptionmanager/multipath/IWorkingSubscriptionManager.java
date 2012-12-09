package org.msrg.publiy.pubsub.core.subscriptionmanager.multipath;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.multipath.IWorkingManager;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.overlaymanager.WorkingRemoteSet;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_UnSubscribe;

public interface IWorkingSubscriptionManager extends ISubscriptionManager, IWorkingManager {
	
	public ISubscriptionManager getMasterSubscriptionManager();
	public WorkingRemoteSet getMatchingWokringSet(Publication publication);
	public void applySummary(TMulticast_Subscribe tms, Path<INode> pathFromFrom);
	public void applySummary(TMulticast_UnSubscribe tmus);
	WorkingRemoteSet getMatchingWokringSet();
	
}
