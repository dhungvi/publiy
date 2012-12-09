package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import java.util.Map;
import java.util.Set;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngine;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngineImp;

import junit.framework.TestCase;

public class CoveringMockSubscriptionGeneratorTest extends TestCase {

	protected final static boolean DEBUG = false;
	protected final static int SUB_GROUPS = 15;
	protected static MatchingEngine _me = new MatchingEngineImp();;
	
	protected Set<MockCoveringSubscription> _subSet;
	protected Map<Integer, CoveringSubscription> _materializedSubSet;
	
	public void setUp() {
//		CoveringDescriptor coveringDescriptor = makeCoveringDescriptor();
//		CoveringDescriptor coveringDescriptor = makeRandomCoveringDescriptor(10, 30000, 0.95);
		CoveringDescriptor coveringDescriptor =
			CoveringLevelNormalMultiplier.makeRandomMultpliedCoveringDescriptor
//			(10, 400, 0.1, 0.01, 1, 0);
			(200, 40000, 0.1, 0.01, 1, 0);

		_subSet = CoveringMockSubscriptionGenerator.generate(coveringDescriptor);
		_materializedSubSet = CoveringMockSubscriptionGenerator.materializeCoveringSubscriptions(_subSet);
		
		System.out.println("Total(" + _subSet.size() + " v. " + coveringDescriptor.getCount() + " v. " + coveringDescriptor.getUnmultipliedCount() + ") " + (DEBUG?_subSet:""));
	}
	
	public static int checkPubsForSubscriptions(
			Publication[] pubs, Map<Integer, CoveringSubscription> materializedSubSet) {
		
		int totalPubMatching = 0;
		CoveringSubscription[] coveringSubs = materializedSubSet.values().toArray(new CoveringSubscription[0]);
		for(Publication pub : pubs) {
			int pubMatchingCount = 0;
			
			for(CoveringSubscription coveredSub : coveringSubs)
			{
				if(_me.match(coveredSub._subscription, pub)) {
					pubMatchingCount ++;
					for(MockCoveringSubscription mockCoveringSub : coveredSub._coveringSubscriptions) {
						CoveringSubscription coveringSub = materializedSubSet.get(mockCoveringSub._subscriptionId);
						if(!_me.match(coveringSub._subscription, pub))
							throw new IllegalStateException(
									"Covering sub (" + coveringSub._subscription + ") does not match pub (" + pub + ") that matches covered sub(" + coveredSub._subscription + ") !");
					}
				}
			}
			
			if(pubMatchingCount == 0)
				throw new IllegalStateException("Publication matches no subscription!");
			
			totalPubMatching += pubMatchingCount;
		}
		
		return totalPubMatching;
	}

	protected static boolean checkMaterializedSubs(Map<Integer, CoveringSubscription> materializedSubSet) {
		for(CoveringSubscription coveringSub : materializedSubSet.values()) {
			Subscription sub1 = coveringSub._subscription;
			
			for(MockCoveringSubscription mockCoveredSub : coveringSub._coveredSubscriptions) {
				CoveringSubscription coveredSub = materializedSubSet.get(mockCoveredSub._subscriptionId);
				if(coveredSub._level == coveringSub._level)
					continue;
				
				Subscription sub2 = coveredSub._subscription;
				Subscription sub0 = SubscrptionCoveringComputer.computeCovering(sub1, sub2);
				if(sub0 != sub1)
					throw new IllegalStateException();
			}
		}
		
		return true;
	}

	public void testMaterializedSubscriptions() {
		checkMaterializedSubs(_materializedSubSet);
		CoveringMockSubscriptionGenerator.writeOutMaterializedSubscriptions(_materializedSubSet);
	}
	
	public void testGeneratedPublications() {
		Publication[] pubs = CoveringMockSubscriptionGenerator.generatePubsForSubscriptions(_me, 10000, _materializedSubSet.values());
		int totalMatchingCount = checkPubsForSubscriptions(pubs, _materializedSubSet);
		System.out.println("Total Publication Count: " + pubs.length);
		System.out.println("Matching Count: " + totalMatchingCount);
	}
}
