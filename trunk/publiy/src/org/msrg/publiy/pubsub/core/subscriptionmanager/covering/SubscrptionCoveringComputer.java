package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import java.util.HashMap;
import java.util.Map;

import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;


public class SubscrptionCoveringComputer {
	
	public static Subscription computeCovering(Subscription sub1, Subscription sub2) {
		Map<String, AttributeBound> attributeBoundMap1 = createAttributeBoundMap(sub1);
		Map<String, AttributeBound> attributeBoundMap2 = createAttributeBoundMap(sub2);
		
		if(attributeBoundMapCovers(attributeBoundMap1, attributeBoundMap2))
			return sub1;
		else if(attributeBoundMapCovers(attributeBoundMap2, attributeBoundMap1))
			return sub2;
		else
			return null;
	}

	public static Subscription computeCoveringMerged(Subscription sub1, Subscription sub2) {
		Map<String, AttributeBound> attributeBoundMap1 = createAttributeBoundMap(sub1);
		Map<String, AttributeBound> attributeBoundMap2 = createAttributeBoundMap(sub2);

		Subscription sub = new Subscription();
		for(AttributeBound bound1 : attributeBoundMap1.values()) {
			AttributeBound bound2 = attributeBoundMap2.get(bound1._attribute);
			if(bound2 == null)
				return null;
			
			AttributeBound bound = merge(bound1, bound2);
			if(bound == null)
				return null;
			for(SimplePredicate sp : bound.getPredicates())
				if(sp != null)
					sub.addPredicate(sp);
		}
		
		return sub;
	}

	private static AttributeBound merge(AttributeBound bound1, AttributeBound bound2) {
		String attribute = bound1._attribute;
		if(!attribute.equals(bound2._attribute))
			throw new IllegalArgumentException();

		if(bound1.getLower() > bound2.getLower()) {
			AttributeBound bound = bound1;
			bound1 = bound2;
			bound2 = bound;
		}
		
		if(bound1.getUpper() <= bound2.getLower())
			return null;
		int upper1 = bound1.getUpper();
		int upper2 = bound2.getUpper();
		
		AttributeBound bound = new AttributeBound(attribute);
		bound.setLower(bound1.getLower());
		
		if(upper1 > upper2)
			bound.setUpper(upper1);
		else
			bound.setUpper(upper2);
		
		return bound;
	}

	public static boolean attributeBoundMapCovers(
			Map<String, AttributeBound> attributeBoundMap1, Map<String, AttributeBound> attributeBoundMap2) {
		for(AttributeBound bound1 : attributeBoundMap1.values()) {
			AttributeBound bound2 = attributeBoundMap2.get(bound1._attribute);
			if(bound2 == null)
				return false;
			
			if(!bound1.covers(bound2))
				return false;
		}

		return true;
	}
	
	public static Map<String, AttributeBound> createAttributeBoundMap(Subscription sub) {
		Map<String, AttributeBound> attributeBounds = new HashMap<String, AttributeBound>();
		for(SimplePredicate sp : sub._predicates) {
			String attribute = sp._attribute;
			AttributeBound bound = attributeBounds.get(attribute);
			if(bound == null) {
				bound = new AttributeBound(attribute);
				attributeBounds.put(attribute, bound);
			}
			
			bound.updateAttributeBound(sp);
		}
		
		return attributeBounds;
	}
	
	public static void main(String[] argv) {
		Subscription sub1 = new Subscription();
		Subscription sub2 = new Subscription();
		
		SimplePredicate sp1A = SimplePredicate.buildSimplePredicate("A", '=', 1);
		SimplePredicate sp2A = SimplePredicate.buildSimplePredicate("A", '=', 1);

		SimplePredicate sp1BL = SimplePredicate.buildSimplePredicate("B", '<', 2);
		SimplePredicate sp1BG = SimplePredicate.buildSimplePredicate("B", '>', -1);
		
		SimplePredicate sp2BL = SimplePredicate.buildSimplePredicate("B", '<', 1);
		SimplePredicate sp2BG = SimplePredicate.buildSimplePredicate("B", '>', -1);

		SimplePredicate sp1CL = SimplePredicate.buildSimplePredicate("C", '<', 3);
		SimplePredicate sp1CG = SimplePredicate.buildSimplePredicate("C", '>', -1);
		
		SimplePredicate sp2CL = SimplePredicate.buildSimplePredicate("C", '<', 10);
		SimplePredicate sp2CG = SimplePredicate.buildSimplePredicate("C", '>', 2);

		sub1.addPredicate(sp1A); sub2.addPredicate(sp2A);
		sub1.addPredicate(sp1BL); sub2.addPredicate(sp2BL);
		sub1.addPredicate(sp1BG); sub2.addPredicate(sp2BG);
		sub1.addPredicate(sp1CL); sub2.addPredicate(sp2CL);
		sub1.addPredicate(sp1CG); sub2.addPredicate(sp2CG);
		
		{
			Subscription coveringSub = computeCovering(sub1, sub2);
			Subscription coveringSubRevers = computeCovering(sub2, sub1);
			System.out.println("CoveringSub: " + coveringSub);
			System.out.println("\t" + coveringSub + "\n\t" + coveringSubRevers);
		}
		//
		{
			Subscription mergedSub = computeCoveringMerged(sub1, sub2);
			Subscription mergedSubRevers = computeCoveringMerged(sub2, sub1);
			System.out.println("mergedSub: " + mergedSub);
			System.out.println("\t" + mergedSub + "\n\t" + mergedSubRevers);
		}
	}
}
