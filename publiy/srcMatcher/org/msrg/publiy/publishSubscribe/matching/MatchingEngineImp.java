package org.msrg.publiy.publishSubscribe.matching;

import java.util.List;
import java.util.Map.Entry;

import org.msrg.publiy.publishSubscribe.Advertisement;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;



public class MatchingEngineImp implements MatchingEngine {
	
	public boolean evaluateStringPredicate(SimpleStringPredicate sp, String attribute, String strValue) {
		if(attribute == null)
			return false;
		if(attribute.compareToIgnoreCase(sp._attribute) != 0)
			return false;
		
		switch(sp._operator){
		case '~':
			return (sp._strValue.equals(strValue));
			
		default:
			throw new UnsupportedOperationException("Invalid string operator: " + sp._operator);
		}
	}
	
	public boolean evaluatePredicate(SimplePredicate sp, String attribute, int value) {
		if ( attribute==null)
			return false;
		if ( attribute.compareToIgnoreCase(sp._attribute) != 0)
			return false;
		switch (sp._operator ){
		case '<':
			return (sp._intValue > value);
		case '>':
			return (sp._intValue < value);
		case '=':
			return (sp._intValue == value);
		case '!':
			return (sp._intValue != value);
		case '%':
			return ((value % sp._intValue) == 0);
		}
		return false;
	}

	public boolean overlapPredicates(SimpleStringPredicate ssp1, SimpleStringPredicate ssp2) {
		if(ssp1._operator != ssp2._operator)
			return false;
		
		return evaluateStringPredicate(ssp1, ssp2._attribute, ssp2._strValue);
	}
	
	public boolean overlapPredicates(SimplePredicate sp, SimplePredicate p) {
		if ( p==null)
			return false;
		if ( p._attribute.compareToIgnoreCase(sp._attribute) != 0)
			return false;

		switch (sp._operator){
		case '%':
				return true;
				
		case '<':
			switch (p._operator){
			case '=':
				return  ( sp._intValue > p._intValue);
			case '!':
				return false;
			case '<':
				return true;
			case '>':
				return (p._intValue < sp._intValue);
			}
			return false;
			
		case '>':
			switch (p._operator){
			case '=':
				return  ( sp._intValue < p._intValue);
			case '!':
				return false;
			case '>':
				return true;
			case '<':
				return (p._intValue > sp._intValue);
			}
			return false;
			
		case '!':
			if ( p._operator == '=')
				return ( sp._intValue != p._intValue);
			else
				return false;
			
		case '=':
			switch(p._operator){
			case  '=':
				return (sp._intValue == p._intValue);
			case '<':
				return (sp._intValue < p._intValue);
			case '>':
				return (sp._intValue > p._intValue);
			case '!':
				return (sp._intValue != p._intValue);
			}
			return false;
			
		}
		return false;
	}
	
	public boolean anyOverlap(Advertisement adv, SimpleStringPredicate ssp){
		for(SimpleStringPredicate ssp2 : adv._stringPredicates)
			if(overlapPredicates(ssp, ssp2))
				return true;
		
		return false;
	}

	public boolean anyOverlap(Advertisement adv, SimplePredicate sp){
		for(SimplePredicate sp2 : adv._predicates)
			if(overlapPredicates(sp, sp2))
				return true;
		
		return false;
	}

	public boolean match(Advertisement adv, Subscription sub){
		if ( adv == null || sub == null )
			throw new NullPointerException(adv + " vs. " + sub);
		
		for(SimplePredicate sp : sub._predicates)
			if (anyOverlap(adv, sp)==false)
				return false;
		
		for(SimpleStringPredicate ssp : sub._stringPredicates)
			if (anyOverlap(adv, ssp)==false)
				return false;
		
		return true;
	}
	
	public boolean match(Advertisement adv, Publication pub){
		if ( adv == null || pub == null )
			throw new NullPointerException(adv + " vs. " + pub);
		
		for(Entry<String, List<Integer>> entryList : pub._predicates.entrySet()) {
			String attribute = entryList.getKey();
			for(Integer value : entryList.getValue()) {
				SimplePredicate sp = SimplePredicate.buildSimplePredicate(attribute, '=', value);
				if ( anyOverlap(adv, sp) == false )
					return false;
			}
		}
		
		for(Entry<String, List<String>> entryList : pub._stringPredicates.entrySet()) {
			String attribute = entryList.getKey();
			for(String strValue : entryList.getValue()) {
				SimpleStringPredicate ssp =
					SimpleStringPredicate.buildSimpleStringPredicate(attribute, '~', strValue);
				if(!anyOverlap(adv, ssp))
					return false;
			}
		}
		
		return true;
	}

	@Deprecated
	public boolean match_highcostmatching(Subscription sub, Publication pub){
		if ( pub == null || sub == null )
			throw new NullPointerException(pub + " vs. " + sub);
		
		boolean ret = true;
		for(SimplePredicate sp : sub._predicates){
			String att = sp._attribute; 
			List<Integer> valList = pub.getValue(att);
			if ( valList==null ) {
				return false;
			} 
			
			for(Integer value : valList) {
				boolean result = evaluatePredicate(sp, att, value.intValue());
				if ( result == false )
					return false;
			}
		}
		
		return ret;
	}
	
	public boolean match(Subscription sub, Publication pub){
		if ( pub == null || sub == null )
			throw new NullPointerException(pub + " vs. " + sub);
		
		for(SimplePredicate sp : sub._predicates){
			String att = sp._attribute; 
			List<Integer> valList = pub.getValue(att);
			if ( valList==null )
				return false;
			
			boolean result = false;
			for(Integer val : valList) {
				result |= evaluatePredicate(sp, att, val.intValue());
			}
			if(!result)
				return false;
		}

		for(SimpleStringPredicate ssp : sub._stringPredicates){
			String att = ssp._attribute; 
			List<String> strValList = pub.getStringValue(att);
			if(strValList==null)
				return false;
			
			boolean result = false;
			for(String strVal : strValList)
				result |= evaluateStringPredicate(ssp, att, strVal);
			if(!result)
				return false;
		}
		
		return true;
	}


	@Deprecated
	protected static void profilingTiming(){
		int size = 10000;
		Publication [][]pubs = new Publication[size][1000000/size];
		

		Subscription sub = new Subscription();
		
		SimplePredicate sp1 = SimplePredicate.buildSimplePredicate("ATT1", '>', -1);
		SimplePredicate sp2 = SimplePredicate.buildSimplePredicate("ATT2", '>', 0);
		sub.addPredicate(sp1); sub.addPredicate(sp2);
					
		for ( int u=0 ; u<size; u++ ){
			for ( int w=0; w<1000000/size ; w++ ){
				Publication pub = new Publication();
//				SimplePredicate ssp1 = SimplePredicate.buildSimplePredicate("ATT1", '=', 1);
//				SimplePredicate ssp2 = SimplePredicate.buildSimplePredicate("ATT2", '=', 1);
	//			sub.addPredicate(ssp1); pub.addPredicate(ssp2);
				pub.addPredicate("ATT1", 1); pub.addPredicate("ATT2", 2);
	
				pubs[u][0] = pub.getClone();
			}
		}
		MatchingEngine me = new MatchingEngineImp();
		boolean rslt;
			
		long total =  0;
		long last = 0;
		Integer a[] = new Integer[10000000]; 
		for ( int j=0 ; j<10000000; j++ )
			a[j] = 0;
		
		for ( int k=0 ; k<size ; k++ ){
			long t1 = System.nanoTime();
//			for ( int i=0 ; i<100 ; i++ )
			rslt = me.match(sub, pubs[k][0]);
			long t2 = System.nanoTime();
			last = (t2-t1);
			System.out.println(last + " " + rslt);
			total += last;
		}
		System.out.println(total);	
	}
}