package org.msrg.publiy.publishSubscribe.matching;

import java.io.Serializable;

public class SimpleStringPredicate implements Serializable {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -1690219687466510476L;
	public final String _attribute;
	public final char _operator;
	public final String _strValue;

	private SimpleStringPredicate(String attribute, char operator, String strValue){
		_attribute = attribute;
		_operator = operator;
		_strValue = strValue;		
	}

	public static SimpleStringPredicate buildSimpleStringPredicate(String attribute, char operator, String strValue){
		if (operator != '~')
			throw new UnsupportedOperationException("Invalid operator: " + operator);
		
		return new SimpleStringPredicate(attribute, operator, strValue);
	}

	@Override
	public int hashCode(){
		return toString().hashCode();
	}
	
	@Override
	public String toString(){
		return _attribute + _operator + _strValue;		
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!this.getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		SimpleStringPredicate spObj = (SimpleStringPredicate) obj;
		if(!_attribute.equals(spObj._attribute))
			return false;
		if(!_strValue.equals(spObj._strValue))
			return false;
		if(_operator != spObj._operator)
			return false;
		
		return true;
	}
	
}
