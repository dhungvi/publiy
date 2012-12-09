package org.msrg.publiy.publishSubscribe;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;
import org.msrg.publiy.publishSubscribe.matching.SimpleStringPredicate;


public class Advertisement implements Serializable {
	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = -2535842506164532477L;
	public final LinkedList<SimplePredicate> _predicates;
	public final LinkedList<SimpleStringPredicate> _stringPredicates;
	
	public Advertisement() {
		_predicates = new LinkedList<SimplePredicate>();
		_stringPredicates = new LinkedList<SimpleStringPredicate>();
	}	
	
	public void addStringPredicate(SimpleStringPredicate ssp){
		_stringPredicates.add(ssp);
	}

	public void addPredicate(SimplePredicate sp){
		_predicates.add(sp);
	}
	 
	public Set<SimpleStringPredicate> getStringPredicates(String att){
		Set<SimpleStringPredicate> set = new HashSet<SimpleStringPredicate>();
		for(SimpleStringPredicate ssp : _stringPredicates) {
			if ( ssp._attribute.compareToIgnoreCase(att) == 0 )
				set.add(ssp);
		}
		return set;
	}

	public Set<SimplePredicate> getPredicates(String att){
		Set<SimplePredicate> set = new HashSet<SimplePredicate>();
		for(SimplePredicate sp : _predicates) {
			if ( sp._attribute.compareToIgnoreCase(att) == 0 )
				set.add(sp);
		}
		return set;
	}

	public String toString(){
		return "Adv: " + _predicates + _stringPredicates;
	}
	
	public Advertisement getClone(){
		Advertisement newAdvertisement = new Advertisement();
		
		for(SimplePredicate sp : _predicates)
			newAdvertisement.addPredicate(sp);
		
		for(SimpleStringPredicate ssp : _stringPredicates)
			newAdvertisement.addStringPredicate(ssp);
		
		return newAdvertisement;
	}
}