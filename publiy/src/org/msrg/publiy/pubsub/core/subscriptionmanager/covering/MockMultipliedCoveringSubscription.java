package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

public class MockMultipliedCoveringSubscription extends
		MockCoveringSubscription {
	
	private final int _multiplier;
	public final MockCoveringSubscription[] _subs;
	
	MockMultipliedCoveringSubscription(int multiplier, int level) {
		super(level);
		
		_multiplier = 0;
		_subs = new MockCoveringSubscription[_multiplier];
		for(int i=0 ; i<_multiplier ; i++) {
			_subs[i] = createCoveringSubscription(level);
			
			for(int j=0 ; j<i ; j++)
				_subs[i].addCoveredSubscription(_subs[j]);
			_subs[i].addCoveredSubscription(this);
		}
	}
	
	MockCoveringSubscription[] getAllSubscriptions() {
		return _subs;
	}
	
	protected MockCoveringSubscription createCoveringSubscription(int level) {
		return new MockCoveringSubscription(level);
	}
	
	@Override
	public int count() {
		return _multiplier + 1;
	}
	
	@Override
	protected final void addCoveredSubscription(MockSubscription coveredSub) {
		for(MockCoveringSubscription sub1 : _subs)
		{
			if(MockMultipliedCoveringSubscription.class.isAssignableFrom(coveredSub.getClass()))
			{
				MockMultipliedCoveringSubscription multipliedCoveredSub = (MockMultipliedCoveringSubscription) coveredSub;
				for(MockCoveringSubscription sub2 : multipliedCoveredSub._subs) {
					if(MockMultipliedCoveringSubscription.class.isAssignableFrom(sub2.getClass())) {
						MockMultipliedCoveringSubscription sub3 = (MockMultipliedCoveringSubscription) sub2;
						for(MockCoveringSubscription sub4 : sub3._subs)
							sub1.addCoveredSubscription(sub4);
					}
					sub1.addCoveredSubscription(sub2);
				}
			}
			
			sub1.addCoveredSubscription(coveredSub);
		}
		
		super.addCoveredSubscription(coveredSub);
	}
	
	@Override
	public String toStringPrefix() {
		return super.toStringPrefix() + "*" + _multiplier;
	}
}
