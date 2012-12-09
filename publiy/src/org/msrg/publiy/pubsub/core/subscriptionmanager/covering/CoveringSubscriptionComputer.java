package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;

public class CoveringSubscriptionComputer {

	public static Set<MockCoveringSubscription>[] divideAndGetCoveringSubscriptions(int groups, Set<MockCoveringSubscription> subSet) {
		TreeSet<MockCoveringSubscription>[] subGroups = new TreeSet[groups];
		int i=0;
		
		for(i=0 ; i<groups ; i++)
			subGroups[i] = new TreeSet<MockCoveringSubscription>();
			
		i=0;
		for(MockCoveringSubscription sub : subSet) {
			if (MockMultipliedCoveringSubscription.class.isAssignableFrom(sub.getClass())) {
				for(MockCoveringSubscription subInternal : ((MockMultipliedCoveringSubscription)sub)._subs)
					subGroups[i++%groups].add(subInternal);
			}
			subGroups[i++%groups].add(sub);
		}
		
		
		Set<MockCoveringSubscription>[] subCoveringGroups = new HashSet[groups];
		for(i=0; i<groups ; i++)
			subCoveringGroups[i] = getCoveringSubscriptions(subGroups[i]);
		
		return subCoveringGroups;
	}
	
	public static StandaloneCoveringSubscription insertNewSubscriptionIntoCoveringSubscriptions(
			Subscription newSubscrption, List<StandaloneCoveringSubscription> coveringSubscriptions) {
		SubscriptionEntry newSubscrptionEntry = new SubscriptionEntry(null, newSubscrption, null, false);
		StandaloneCoveringSubscription newCoveringSubscription =
			insertNewSubscriptionIntoCoveringSubscriptions(newSubscrptionEntry, coveringSubscriptions);
		
		return newCoveringSubscription;
	}
	
	public static StandaloneCoveringSubscription insertNewSubscriptionIntoCoveringSubscriptions(
				SubscriptionEntry newSubscrptionEntry, List<StandaloneCoveringSubscription> existingCoveringSubscriptions) {
		StandaloneCoveringSubscription newCoveringSubscription = newSubscrptionEntry._standaloneCoveringSub;
		
		for(StandaloneCoveringSubscription existingCoveringSubscription : existingCoveringSubscriptions)
		{
			Subscription computedCoveringSub =
				SubscrptionCoveringComputer.computeCovering(
						newCoveringSubscription._subscription,
						existingCoveringSubscription._subscription);
			
			if(computedCoveringSub == newCoveringSubscription._subscription)
				newCoveringSubscription.addCoveredSubscription(existingCoveringSubscription);
			
			computedCoveringSub =
				SubscrptionCoveringComputer.computeCovering(
						existingCoveringSubscription._subscription,
						newCoveringSubscription._subscription);
			
			if(computedCoveringSub == existingCoveringSubscription._subscription)
				existingCoveringSubscription.addCoveredSubscription(newCoveringSubscription);
		}
		
		existingCoveringSubscriptions.add(newCoveringSubscription);
		return newCoveringSubscription;
	}
	
	private static void getCoveringSubscriptionEntries(SubscriptionEntry coveringSubEntry) 
			throws ConcurrentModificationException {
		StandaloneCoveringSubscription coveringSub = coveringSubEntry._standaloneCoveringSub;
		for(MockCoveringSubscription coveredSubscription : coveringSub._coveredSubscriptions) {
			SubscriptionEntry coveredSubscriptionEntry =
				((StandaloneCoveringSubscription)coveredSubscription)._subscriptionEntry;
			if(coveringSub != coveredSubscription && 
					coveredSubscriptionEntry._activeFrom == coveringSubEntry._activeFrom)
				coveredSubscriptionEntry.setActiveFrom(null);
		}
	}
	
	static final int MAX_RETRY_CONCURRENT_MODIFIFATION = 3;
	static final int RETRY_CONCURRENT_MODIFIFATION_SLEEP = 20;
	
	public static List<SubscriptionEntry> getCoveringSubscriptionEntries(List<SubscriptionEntry> subSet) {
		for(SubscriptionEntry coveringSubEntry : subSet)
		{
			if(coveringSubEntry._activeFrom == null)
				continue;
			
			for(int i=0; i<MAX_RETRY_CONCURRENT_MODIFIFATION ; i++)
			{
				try {
					getCoveringSubscriptionEntries(coveringSubEntry);
					break;
				} catch(ConcurrentModificationException cmEx) {
					cmEx.printStackTrace();
				}
				
				try {
					Thread.sleep(RETRY_CONCURRENT_MODIFIFATION_SLEEP);
				} catch(InterruptedException itx) {
					itx.printStackTrace();
					break;
				}
			}
		}
		
		return subSet;
	}
	
	public static Set<CoveringSubscription> getCoveringSubscriptions(
			Collection<CoveringSubscription> subSet, Map<Integer, CoveringSubscription> materializedSubSet) {
		Set<MockCoveringSubscription> coveringSubSet = new HashSet<MockCoveringSubscription>();
		Set<MockCoveringSubscription> coveredSubSet = new HashSet<MockCoveringSubscription>();
		
		for(CoveringSubscription coveringSub : subSet) {
			MockCoveringSubscription sub = coveringSub.getMockCoveringSubscription();
			if(coveredSubSet.contains(sub))
				continue;
			
			coveringSubSet.add(sub);
			
			Set<MockCoveringSubscription> coveredSubSets = new HashSet<MockCoveringSubscription>(sub._coveredSubscriptions);
			while(!coveredSubSets.isEmpty()) {
				Set<MockCoveringSubscription> nextCoveredSubSets = new HashSet<MockCoveringSubscription>();
				for(MockCoveringSubscription coveredSub : coveredSubSets) {
					coveredSubSet.add(coveredSub);
					coveringSubSet.remove(coveredSub);
					nextCoveredSubSets.addAll(coveredSub._coveredSubscriptions);
				}
				
				coveredSubSets = nextCoveredSubSets;
			}
		}
		
		Set<CoveringSubscription> retCoveringSubscriptions = new HashSet<CoveringSubscription>();
		for(MockCoveringSubscription mockSub : coveringSubSet)
			retCoveringSubscriptions.add(materializedSubSet.get(mockSub._subscriptionId));
		
		return retCoveringSubscriptions;
	}
	
	public static Set<MockCoveringSubscription> getCoveringSubscriptions(TreeSet<MockCoveringSubscription> subSet) {
		Set<MockCoveringSubscription> coveringSubSet = new HashSet<MockCoveringSubscription>();
		Set<MockCoveringSubscription> coveredSubSet = new HashSet<MockCoveringSubscription>();
		
		for(MockCoveringSubscription sub : subSet) {
			if(coveredSubSet.contains(sub))
				continue;
			
			coveringSubSet.add(sub);
			
			Set<MockCoveringSubscription> coveredSubSets = new HashSet<MockCoveringSubscription>(sub._coveredSubscriptions);
			while(!coveredSubSets.isEmpty()) {
				Set<MockCoveringSubscription> nextCoveredSubSets = new HashSet<MockCoveringSubscription>();
				for(MockCoveringSubscription coveredSub : coveredSubSets) {
					coveredSubSet.add(coveredSub);
					coveringSubSet.remove(coveredSub);
					nextCoveredSubSets.addAll(coveredSub._coveredSubscriptions);
				}
				
				coveredSubSets = nextCoveredSubSets;
			}
		}
		
		return coveringSubSet;
	}
}
