package org.msrg.publiy.broker.core.connectionManager;

public enum ConsiderationStatus {
	
	S_CONSIDERATION_FAILED					("CONSIDER_FAILED"			, 120000),
	S_CONSIDERATION_INITIATED				("CONSIDER_INITATED"		, 20000),
	S_CONSIDERATION_ONGOING					("CONSIDER_ONGOING"			, Integer.MAX_VALUE),
	S_CONSIDERATION_SLOW_HIGH				("CONSIDER_SLOW_HIGH"		, 120000),
	S_CONSIDERATION_SLOW_LOW				("CONSIDER_SLOW_LOW"		, 120000),
	S_CONSIDERATION_FAST_LOW				("CONSIDER_FAST_LOW"		, 120000);
	
	public final String _NAME;
	public final int _VALIDITY_PERIOD;
	
	private ConsiderationStatus(String name, int validityPeriod){
		_NAME = name;
		_VALIDITY_PERIOD = validityPeriod;
	}
	
	@Override
	public String toString(){
		return _NAME;
	}
}
