package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.multipath.IWorkingManager;

public interface IWorkingOverlayManager extends IOverlayManager, IWorkingManager{
	
	public IOverlayManager getMasterOverlayManager();
	public InetSocketAddress mapsToWokringNodeAddress(InetSocketAddress masterNode);
	public InetSocketAddress mapsToRealWokringNodeAddress(InetSocketAddress masterNodeAddress);
	public WorkingRemoteSet computeSoftLinksAddresses(InOutBWEnforcer bwEnforcer, PubForwardingStrategy forwardingStrategy, WorkingRemoteSet matching);
	
//	public Set<InetSocketAddress> subSetMapsToRealNode(Set<InetSocketAddress> remoteSet, InetSocketAddress mappedNode);

	public boolean isReal(InetSocketAddress remote);
}
