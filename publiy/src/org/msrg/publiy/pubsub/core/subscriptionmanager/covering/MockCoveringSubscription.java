package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MockCoveringSubscription extends MockSubscription {
	
	protected boolean PRESERVE_OVERING_ORDERED = true;

	protected final List<MockCoveringSubscription> _coveredSubscriptions;
	protected final List<MockCoveringSubscription> _coveringSubscriptions;
	protected final int _level;
	
	protected MockCoveringSubscription(int level, List<MockCoveringSubscription> coveredSubscriptions, List<MockCoveringSubscription> coveringSubscriptions) {
		super();
		_level = level;
		_coveredSubscriptions = coveredSubscriptions;
		_coveringSubscriptions = coveringSubscriptions;
	}
	
	public MockCoveringSubscription(int level) {
		this(level, new LinkedList<MockCoveringSubscription>(), new LinkedList<MockCoveringSubscription>());
	}
	
	@Override
	void addCoveredSubscription(MockSubscription mockSub) {
		super.addCoveredSubscription(mockSub);
		MockCoveringSubscription mockCoveredSub = (MockCoveringSubscription) mockSub;
		
		
		if (PRESERVE_OVERING_ORDERED) {
			if(compareTo(mockCoveredSub) > 0) {
				_coveredSubscriptions.add(mockCoveredSub);
				mockCoveredSub._coveringSubscriptions.add(this);
			}
		}
		else {
			_coveredSubscriptions.add(mockCoveredSub);
			mockCoveredSub._coveringSubscriptions.add(this);
		}
	}
	
	@Override
	public int coveringCount() {
		return _coveringSubscriptions.size();
	}
	
	@Override
	public int coveredCount() {
		return _coveredSubscriptions.size();
	}

	public List<MockCoveringSubscription>  getCoveredSubscriptions() {
		return _coveredSubscriptions;
	}

	public List<MockCoveringSubscription>  getCoveringSubscriptions() {
		return _coveringSubscriptions;
	}
	
	public int count() {
		return 1;
	}
	
	@Override
	public String toString() {
		return toString(CoveringMockSubscriptionGenerator.DEBUG);
	}
	
	protected String toStringPrefix() {
		return super.toString();
	}

	protected String toString(Collection<MockCoveringSubscription> subscriptions) {
		StringWriter writer = new StringWriter();
		boolean first = true;
		for(MockCoveringSubscription mockCoveredSub : subscriptions) {
			writer.append((first?"":'.') + mockCoveredSub.toString(false));
			first = false;
		}
		return writer.toString();
	}
	
	public String toString(boolean showCovered) {
		if(showCovered == false) {
			return super.toString();
		} else {

			return toStringPrefix() +
				"{" + toString(_coveredSubscriptions) + "}"; // +
//				"{" + toString(_coveringSubscriptions) + "}";
		}
	}
}
