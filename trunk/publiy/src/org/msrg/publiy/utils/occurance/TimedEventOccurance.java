package org.msrg.publiy.utils.occurance;

import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

public class TimedEventOccurance extends IEventOccurance {

	private long _startTime = SystemTime.currentTimeMillis();
	private final long _DURATION;
	private final Object _obj;
	private final String _str;
	private int _testCount = 0;
	
	public TimedEventOccurance(long maxDuration, Object obj, String str) {
		_DURATION = maxDuration;
		_obj = obj;
		_str = str;
	}
	
	public boolean test() {
		synchronized(_lock) {
			_testCount++;

			LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_DUMMY_SOURCE, "TESTING: " + this);
			
			if(isFailed())
				throw new IllegalStateException("" + getStatus());

			long now = SystemTime.currentTimeMillis();
			boolean result = (now <= _startTime + _DURATION);
			
			return result;
		}
	}
	
	public void reset() {
		synchronized(_lock) {
			_startTime = SystemTime.currentTimeMillis();
		}
	}
	
	@Override
	public String toString() {
		long now = SystemTime.currentTimeMillis();
		boolean result = (now <= _startTime + _DURATION);
		return _obj + "_[" + _str + "-" + _testCount + "]" + result + "[" + now + "<=" + _startTime + "+" + _DURATION + "]";
	}
}
