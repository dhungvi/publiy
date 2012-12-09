package org.msrg.publiy.pubsub.core.subscriptionmanager.multipath;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.overlaymanager.IWorkingOverlayManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.LocalSubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.covering.CoveringSubscriptionComputer;

public class WorkingSubscriptionManagerWithCovering extends
		WorkingSubscriptionManager {

	public WorkingSubscriptionManagerWithCovering(int workingVersion,
			ISubscriptionManager masterSubManager,
			IWorkingOverlayManager workingOverlayManager) {
		super(workingVersion, masterSubManager, workingOverlayManager);
	}

	protected void addMastersSubEntries(ISubscriptionManager masterSubManager, IWorkingOverlayManager workingOverlayManager) {
		Collection<LocalSubscriptionEntry> coveringLocalSubEntriesList =
			masterSubManager.getLocalSubscriptionEntries();
//			CoveringSubscriptionComputer.getCoveringSubscriptionEntries(masterSubManager.getLocalSubscriptionEntries());
		
		_localSubscriptionEntries.addAll(coveringLocalSubEntriesList);
		
		Map<InetSocketAddress, List<SubscriptionEntry>> fromSubEntriesMap =
			new HashMap<InetSocketAddress, List<SubscriptionEntry>>();
		for (SubscriptionEntry masterSubEntry : masterSubManager.getSubscriptionEntries()) {
			InetSocketAddress masterFrom = masterSubEntry._orgFrom;
			InetSocketAddress workingFrom =
				workingOverlayManager.mapsToWokringNodeAddress(masterFrom);
			if(workingFrom == null)
				workingFrom = workingOverlayManager.isClientOf(masterFrom);
			masterSubEntry.setActiveFrom(workingFrom);
			
			List<SubscriptionEntry> subEntiesList = fromSubEntriesMap.get(workingFrom);
			if(subEntiesList == null) {
				subEntiesList = new LinkedList<SubscriptionEntry>();
				fromSubEntriesMap.put(workingFrom, subEntiesList);
			}
			subEntiesList.add(masterSubEntry);
		}
		
		for(List<SubscriptionEntry> fromSubEntriesList : fromSubEntriesMap.values()) {
			List<SubscriptionEntry> coveringSubEntriesList =
//				fromSubEntriesList;
				CoveringSubscriptionComputer.getCoveringSubscriptionEntries(fromSubEntriesList);
			
			_subscriptionEntries.addAll(coveringSubEntriesList);
		}
	}

}
