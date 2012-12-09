package org.msrg.publiy.broker.core.bwenforcer;

public class UnlimittedBWEnforcer extends BWEnforcer {
	
	UnlimittedBWEnforcer() {
		super();
	}
	
	@Override
	public boolean hasRemainingBW() {
		return true;
	}
}
