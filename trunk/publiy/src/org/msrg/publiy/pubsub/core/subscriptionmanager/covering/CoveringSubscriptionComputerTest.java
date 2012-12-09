package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.msrg.publiy.publishSubscribe.Subscription;

import junit.framework.TestCase;


public class CoveringSubscriptionComputerTest extends TestCase {

	protected List<StandaloneCoveringSubscription> _standaloneCoveringSubscriptions;
	protected CoveringSubscription[] _materializedCoveringSubscription;
	protected Set<MockCoveringSubscription> _subSet;
	protected Map<Integer, CoveringSubscription> _materializedSubSet;
	
	public void setUp() {
		logTime("SetUp(): BEGIN...");
		CoveringDescriptor coveringDescriptor =
//			CoveringLevelNormalMultiplier.makeRandomMultpliedCoveringDescriptor(200, 40000, 0.1, 0.01, 1, 0);
			CoveringLevelNormalMultiplier.makeRandomMultpliedCoveringDescriptor(20, 1000, 0.1, 0.01, 1, 0);

		_subSet = CoveringMockSubscriptionGenerator.generate(coveringDescriptor);
		_materializedSubSet = CoveringMockSubscriptionGenerator.materializeCoveringSubscriptions(_subSet);
		
		System.out.println("Total(" + _subSet.size() + " v. " + coveringDescriptor.getCount() + " v. " + coveringDescriptor.getUnmultipliedCount() + ") ");
		logTime("SetUp(): MATERIAlIZE END...");
		
		_standaloneCoveringSubscriptions = new LinkedList<StandaloneCoveringSubscription>();
		_materializedCoveringSubscription = _materializedSubSet.values().toArray(new CoveringSubscription[0]);
	
		for(int i=0 ; i<_materializedCoveringSubscription.length ; i++)
		{
			CoveringSubscription coveringSub = _materializedCoveringSubscription[i];
			Subscription newSubscrption = coveringSub._subscription;
//			StandaloneCoveringSubscription standaloneCoveringSub =
				CoveringSubscriptionComputer.insertNewSubscriptionIntoCoveringSubscriptions(
						newSubscrption, _standaloneCoveringSubscriptions);
		}
		logTime("SetUp(): STANDALONE SUBS END...");
	}
	
	public static StandaloneCoveringSubscription findSubscription(
			Subscription sub, List<StandaloneCoveringSubscription> subscriptions) {
		for(StandaloneCoveringSubscription subscription : subscriptions)
			if(sub.equalsExactly(subscription._subscription))
				return subscription;
		
		return null;
	}
	
	public int findSubscription(Subscription sub, StandaloneCoveringSubscription[] subscriptions) {
		for(int i=0; i<subscriptions.length ; i++)
			if(sub.equalsExactly(subscriptions[i]._subscription))
				return i;
		return -1;
	}
	
	public void logTime(String msg) {
		System.out.println("@" + new Date() + "\t" + msg);
	}
	
	public void testSubscriptionInsertion() {
		logTime("testSubscriptionInsertion(): BEGIN...");
		StandaloneCoveringSubscription[] standaloneCoveringSubscriptionsArray =
			_standaloneCoveringSubscriptions.toArray(new StandaloneCoveringSubscription[0]);
		assertTrue(_materializedCoveringSubscription.length == standaloneCoveringSubscriptionsArray.length);
		
		for(int i=0 ; i<_materializedCoveringSubscription.length ; i++)
		{
			checkStandaloneConveringSubscription(standaloneCoveringSubscriptionsArray[i], _materializedCoveringSubscription[i]);
		}
		logTime("testSubscriptionInsertion(): END...");
	}

	private void checkStandaloneConveringSubscription(
			CoveringSubscription coveringSubscription1,
			CoveringSubscription coveringSubscription2) {
		assertTrue(coveringSubscription1._subscription.equalsExactly(coveringSubscription2._subscription));
		if(coveringSubscription1.coveredCount() != coveringSubscription2.coveredCount())
			System.out.println("=================\n" + coveringSubscription1 + "\n\t" + coveringSubscription1.getMockCoveringSubscription().getCoveredSubscriptions() + "\n" + coveringSubscription2 + "\n\t" + coveringSubscription2.getMockCoveringSubscription().getCoveredSubscriptions());

		assertTrue(coveringSubscription1.coveredCount() >= coveringSubscription2.coveredCount());
		assertTrue(coveringSubscription1.coveringCount() >= coveringSubscription2.coveringCount());

		for(MockCoveringSubscription mockCoveredSubscription2 : coveringSubscription2._coveredSubscriptions)
		{
			CoveringSubscription coveredSubscription2 = _materializedSubSet.get(mockCoveredSubscription2._subscriptionId);
			boolean foundEqualSubscription = false;
			for(MockCoveringSubscription mockCoveredSubscription1 : coveringSubscription1._coveredSubscriptions)
			{
				CoveringSubscription coveredSubscription1 = (CoveringSubscription) mockCoveredSubscription1;
				if(coveredSubscription1._subscription.equalsExactly(coveredSubscription2._subscription)) {
					foundEqualSubscription = true;
					break;
				}
			}
			assertTrue(foundEqualSubscription);// || coveredSubscription2._subscription._predicates.size() == 0);
		}
	}
}
