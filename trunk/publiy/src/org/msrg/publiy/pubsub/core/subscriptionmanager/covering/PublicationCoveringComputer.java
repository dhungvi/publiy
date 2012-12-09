package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngine;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;


public class PublicationCoveringComputer {
	
	final Random _rand = new Random(CoveringMockSubscriptionGenerator.SEED);
	
	protected Publication[] generate(int pubCount, Collection<CoveringSubscription> subs) {
		return generate(null, pubCount, subs);
	}
	
	protected Publication[] generate(MatchingEngine me, int pubCount, Collection<CoveringSubscription> subs) {
		Publication[] pubs = new Publication[pubCount];
		CoveringSubscription[] subsArray = subs.toArray(new CoveringSubscription[0]);
		for(int i=0 ; i<pubCount ; i++) {
			pubs[i] = new Publication();
			int j = _rand.nextInt(subsArray.length);
			CoveringSubscription sub = subsArray[j];
			Map<String, AttributeBound> attributesBoundMap = new HashMap<String, AttributeBound>();
			for(SimplePredicate sp : sub.getSubscription()._predicates) {
				AttributeBound attributeBound = attributesBoundMap.get(sp._attribute);
				if(attributeBound == null) {
					attributeBound = new AttributeBound(sp._attribute);
					attributesBoundMap.put(sp._attribute, attributeBound);
				}
				attributeBound.updateAttributeBound(sp);
			}
			
			for(AttributeBound attributeBound : attributesBoundMap.values())
				pubs[i].addPredicate(attributeBound._attribute, attributeBound.getValueWithinRange(_rand));
			
			if(!me.match(sub.getSubscription(), pubs[i]))
				throw new IllegalStateException();
		}
		
		return pubs;
	}
	
	protected Publication[] generate2(int pubCount, Collection<CoveringSubscription> subs) {
		Publication[] pubs = new Publication[pubCount];
		Map<String, AttributeBound> attributeBounds = new HashMap<String, AttributeBound>();
		for(CoveringSubscription sub : subs) {
			for(SimplePredicate sp : sub.getSubscription()._predicates) {
				AttributeBound attributeBound = attributeBounds.get(sp._attribute);
				if(attributeBound == null) {
					attributeBound = new AttributeBound(sp._attribute);
					attributeBounds.put(sp._attribute, attributeBound);
				}
				
				attributeBound.updateAttributeBound(sp);
			}
		}
		
		AttributeBound[] attributesBounds = attributeBounds.values().toArray(new AttributeBound[0]);
		for(int i=0 ; i<pubs.length ; i++) {
			pubs[i] = new Publication();
			
			for(AttributeBound attriBound : attributesBounds)
				pubs[i].addPredicate(attriBound._attribute, attriBound.getValueWithinRange(_rand));
		}
		
		return pubs;
	}
	
	public static void main(String[] argv) {
		
	}
}
