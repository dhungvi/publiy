package org.msrg.publiy.utils;

public class SystemTime {

	private static long _absoluteMilliTimeValue = -1;

	public static long currentTimeMillis() {
		if (_absoluteMilliTimeValue < 0)
			return System.currentTimeMillis();
		else
			return _absoluteMilliTimeValue;
	}

	public static long nanoTime() {
		if (_absoluteMilliTimeValue < 0)
			return System.nanoTime();
		else
			return _absoluteMilliTimeValue * 1000 * 1000;
	}
	
	public static void setSystemTime(long absoluteMillisTimeValue) {
		if (absoluteMillisTimeValue < 0)
			throw new IllegalStateException("Negative time value!");
		
		if (_absoluteMilliTimeValue > absoluteMillisTimeValue)
			throw new IllegalStateException("Cannot go back in time!");
		
		_absoluteMilliTimeValue = absoluteMillisTimeValue;
	}
	
	public static void resetTime() {
		_absoluteMilliTimeValue = 0;
	}
}
