package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import java.util.Random;
import java.util.Set;

import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;


class AttributeBound {
	public final String _attribute;
	private int _lower;
	private int _upper;
	
	AttributeBound(String attribute) {
		_attribute = attribute;
		_lower = Integer.MIN_VALUE;
		_upper = Integer.MAX_VALUE;
	}
	
	public int getLower() {
		return _lower;
	}
	
	public int getUpper() {
		return _upper;
	}
	
	protected void setLower(int lower) {
		_lower = lower;
	}
	
	protected void setUpper(int upper) {
		_upper = upper;
	}
	
	public SimplePredicate[] getPredicates() {
		if(_lower + 1 >= _upper) {
			return new SimplePredicate[0];
		} else if (_lower + 2 == _upper) {
			SimplePredicate[] sps = new SimplePredicate[1];
			sps[0] = SimplePredicate.buildSimplePredicate(_attribute, '=', _lower + 1);
			return sps;
		} else {
			SimplePredicate[] sps = new SimplePredicate[2];
			sps[0] = SimplePredicate.buildSimplePredicate(_attribute, '>', _lower);
			sps[1] = SimplePredicate.buildSimplePredicate(_attribute, '<', _upper);
			
			return sps;
		}
	}
	
	public static AttributeBound createAttributeBound(Set<SimplePredicate> sps) {
		if(sps == null || sps.isEmpty())
			return null;
		
		AttributeBound attrBound = null;
		for(SimplePredicate sp : sps) {
			if(attrBound ==null)
				attrBound = new AttributeBound(sp._attribute);
			
			attrBound.updateAttributeBound(sp);
		}
		
		return attrBound;
	}
	
	protected void updateAttributeBound(SimplePredicate sp) {
		String attribute = sp._attribute;
		int min = Integer.MIN_VALUE;
		int max = Integer.MAX_VALUE;

		if (!attribute.equals(sp._attribute))
			throw new IllegalArgumentException();
		
		switch(sp._operator) {
		case '=':
		{
			if(min == Integer.MIN_VALUE || min > sp._intValue - 1) {
				min = sp._intValue - 1;
				if(_lower == Integer.MIN_VALUE || _lower > min)
					_lower = min;
			}
			
			if(max == Integer.MAX_VALUE || max < sp._intValue + 1) {
				max = sp._intValue + 1;
			if(_upper  == Integer.MAX_VALUE || _upper < max)
				_upper = max;
			}
		}	
		break;
			
		case '<':
		{
			if(max == Integer.MAX_VALUE || max < sp._intValue)
				max = sp._intValue;
			if(_upper  == Integer.MAX_VALUE || _upper < max)
				_upper = max;
		}
		break;
		
		case '>':
		{
			if(min == Integer.MIN_VALUE || min > sp._intValue)
				min = sp._intValue;
			if(_lower == Integer.MIN_VALUE || _lower > min)
				_lower = min;
		}
		break;
			
		default:
			throw new UnsupportedOperationException("" + sp._operator);
		}
	}

	@Override
	public String toString() {
		return _attribute + "(" + _lower + "," + _upper + ")";
	}

	public boolean covers(AttributeBound bound2) {
		if(!_attribute.equals(bound2._attribute))
			throw new IllegalArgumentException(_attribute + " v. " + bound2._attribute);
		
		if(_lower <= bound2._lower && _upper >= bound2._upper)
			return true;
		else
			return false;
	}

	public int getValueWithinRange(Random rand) {
		if(_upper == Integer.MAX_VALUE)
			if(_lower != Integer.MIN_VALUE)
				return _lower + 1 + rand.nextInt(100);
			else
				throw new IllegalStateException();
		
		if(_lower == Integer.MIN_VALUE)
			if(_upper != Integer.MAX_VALUE)
				return _upper - 1 - rand.nextInt(100);
			else
				throw new IllegalStateException();
		
		if(_lower + 1 >= _upper)
			return _lower + 1;
		return _lower + 1 + rand.nextInt(_upper - _lower - 1);
	}
}
