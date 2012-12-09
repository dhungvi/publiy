package org.msrg.publiy.publishSubscribe.matching;

import org.msrg.publiy.publishSubscribe.Advertisement;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;

public interface MatchingEngine {
	
	public boolean evaluatePredicate(SimplePredicate sp, String attribute, int value);
	public boolean overlapPredicates(SimplePredicate sp, SimplePredicate p);
	public boolean anyOverlap(Advertisement adv, SimplePredicate sp);
	public boolean match(Advertisement adv, Subscription sub);
	public boolean match(Advertisement adv, Publication pub);
	public boolean match(Subscription sub, Publication pub);
		
}
