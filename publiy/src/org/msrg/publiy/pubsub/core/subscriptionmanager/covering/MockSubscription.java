package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

public class MockSubscription implements Comparable<MockSubscription> {

	protected static int _maxUsedSubscriptionId = 0;
	
	public final int _subscriptionId;
	
	private MockSubscription(int subscriptionId) {
		if(subscriptionId >= _maxUsedSubscriptionId)
			_maxUsedSubscriptionId = subscriptionId;

		else
			throw new IllegalArgumentException("Cannot go back in SubsciptionIds: " + _maxUsedSubscriptionId + " vs. "  + subscriptionId);
		
		_subscriptionId = subscriptionId;
	}
	
	MockSubscription() {
		this(_maxUsedSubscriptionId + 1);
	}

	@Override
	public String toString() {
		return "sub[" + _subscriptionId + "]";
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		MockSubscription mockSubObj = (MockSubscription) obj;
		return mockSubObj._subscriptionId == _subscriptionId;
	}
	
	@Override
	public int hashCode() {
		return _subscriptionId;
	}
	
	void addCoveredSubscription(MockSubscription coveredSub) {
//		System.out.println(this + "\tcovers\t" + coveredSub);
	}
	
	public int coveringCount() {
		return 0;
	}
	
	public int coveredCount() {
		return 0;
	}

	@Override
	public int compareTo(MockSubscription o) {
		if(_subscriptionId == o._subscriptionId)
			return 0;
		else if (_subscriptionId > o._subscriptionId)
			return 1;
		else
			return -1;
	}
}
