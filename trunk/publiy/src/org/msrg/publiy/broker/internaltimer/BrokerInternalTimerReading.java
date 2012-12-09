package org.msrg.publiy.broker.internaltimer;

public class BrokerInternalTimerReading implements Comparable<BrokerInternalTimerReading> {

	public final long _startTime;
	public final long _readingTime;
	
	public static int DEFAULT_TIME_SECS_WIDTH = 6;
	
	BrokerInternalTimerReading(long startTime, long readingTime) {
		_startTime = startTime;
		_readingTime = readingTime;
	}
	
	public long timeMillis() {
		return (_readingTime - _startTime) / 1000000;
	}

	public long timeSecs() {
		return (_readingTime - _startTime) / 1000000000;
	}
	
	@Override
	public String toString() {
		return timeSecsString(DEFAULT_TIME_SECS_WIDTH);
	}
	
	public String timeSecsString(int width) {
		return formatSecsString(timeSecs(), width);
	}
	
	public static String formatSecsString(long sec) {
		return formatSecsString(sec, DEFAULT_TIME_SECS_WIDTH);
	}
	
	public static String formatSecsString(long sec, int width) {
		return String.format("%" + width + "d", sec).replace(' ', '0');
	}

	@Override
	public int compareTo(BrokerInternalTimerReading o) {
		if(this._readingTime < o._readingTime)
			return -1;
		else if(this._readingTime > o._readingTime)
			return +1;
		else
			return 0;
	}
	
}
