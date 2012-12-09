package org.msrg.publiy.publishSubscribe.matching;

import java.io.Serializable;

public class SimplePredicate implements Serializable {
	
	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = -8761367561721765037L;
	public static final String SimplePredicatePatternStr = "\\s*(\\w[\\w\\-]*)\\s*([=<>!%])\\s*(\\-?[0-9]*)\\s*";
	public final String _attribute;
	public final char _operator;
	public final int _intValue;

	private SimplePredicate(String attribute, char operator, int value){
		_attribute = attribute;
		_operator = operator;
		_intValue = value;		
	}

	public static SimplePredicate buildSimplePredicate(String attribute, char operator, int value){
		if ( operator != '=' && operator != '<' && operator != '>' && operator != '!' && operator != '%' )
			return null;
		return new SimplePredicate(attribute, operator, value);
	}

	@Override
	public int hashCode(){
		return toString().hashCode();
	}
	
	@Override
	public String toString(){
		return _attribute + _operator + _intValue;		
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!this.getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		SimplePredicate spObj = (SimplePredicate) obj;
		if(!_attribute.equals(spObj._attribute))
			return false;
		if(_intValue != spObj._intValue)
			return false;
		if(_operator != spObj._operator)
			return false;
		
		return true;
	}
}
