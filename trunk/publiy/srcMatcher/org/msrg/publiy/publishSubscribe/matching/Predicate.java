package org.msrg.publiy.publishSubscribe.matching;

public interface Predicate {
	public boolean evaluate(Predicate p);
	public boolean overlap(Predicate p);
}
