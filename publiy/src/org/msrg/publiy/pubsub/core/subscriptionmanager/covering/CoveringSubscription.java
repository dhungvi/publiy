package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import org.msrg.publiy.broker.core.nodes.OverlayNodeId;
import org.msrg.publiy.publishSubscribe.Subscription;

public class CoveringSubscription extends MockCoveringSubscription {
	
	protected boolean PRESERVE_OVERING_ORDERED = true;
	public Subscription _subscription = null;
	protected OverlayNodeId _srcId = null;

	protected MockCoveringSubscription _mockSubscription;
	
	public CoveringSubscription(MockCoveringSubscription mockCoveringSubscription) {
		super(mockCoveringSubscription._level, mockCoveringSubscription._coveredSubscriptions, mockCoveringSubscription._coveringSubscriptions);
		
		_mockSubscription = mockCoveringSubscription;
	}
	
	protected CoveringSubscription() {
		super(-1);
		_mockSubscription = null;
	}
			
	public OverlayNodeId getSourceNodeId() {
		return _srcId;
	}
	
	public Subscription getSubscription() {
		return _subscription;
	}
	
	@Override
	void addCoveredSubscription(MockSubscription mockSub) {
		super.addCoveredSubscription(mockSub);
		CoveringSubscription mockCoveredSub = (CoveringSubscription) mockSub;
		
		
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
	
	public int count() {
		return 1;
	}
	
	public Subscription getMaterializedSubcsription() {
		return _subscription;
	}
	
	public void materialize(OverlayNodeId srcId, Subscription subscription) {
		if(_subscription != null)
			throw new IllegalStateException();
		
		_subscription = subscription;
		_srcId = srcId;
	}
	
	public void assignSourceId(OverlayNodeId srcId) {
		if(_srcId != null)
			throw new IllegalStateException();

		_srcId = srcId;
	}
	
	@Override
	public String toString() {
//		return "MOCK=" + _mockSubscription + "-{" + _subscription + "}";
		return _subscription.toString();
	}
	
	@Override
	public int compareTo(MockSubscription o) {
		if(_mockSubscription == this)
			return 0;
		
		if(CoveringSubscription.class.isAssignableFrom(o.getClass())) {
			CoveringSubscription coveringSubscription = (CoveringSubscription) o;
			return _mockSubscription.compareTo(coveringSubscription._mockSubscription);
		}
		else
			return _mockSubscription.compareTo(o);
	}
	
	protected MockCoveringSubscription getMockCoveringSubscription() {
		return _mockSubscription;
	}
	
//	protected void setMockCoveringSubscription(MockCoveringSubscription mockSubscription) {
//		throw new UnsupportedOperationException();
//	}
}
