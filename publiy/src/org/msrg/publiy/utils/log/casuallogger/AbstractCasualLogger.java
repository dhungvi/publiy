package org.msrg.publiy.utils.log.casuallogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimerReading;

import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

public abstract class AbstractCasualLogger implements ILoggerSource, ICasualLogger {

	final int DEFAULT_LOGGING_INTERVAL = 5000;
	final int DEFAULT_FORCED_FLUSH_INTERVAL = 10000;
	
	private long _lastLoggingTime = 0;
	private long _lastForcedFlushTime = 0;
	
	protected final Object _lock = new Object();
	protected boolean _problem = false;
	protected boolean _initialized = false;
	
	protected boolean _firstTime = true;
	protected int _fileWriteCount = 0;
//	protected DateFormat _df = LogEntry.DATE_FORMATE;
	
	protected Writer _logFileWriter;
	
	protected int getDefaultLoggingInterval() {
		return DEFAULT_LOGGING_INTERVAL;
	}
	
	int getDefaultForcedFlushInterval() {
		return DEFAULT_FORCED_FLUSH_INTERVAL;
	}
	
	@Override
	public boolean isEnabled() {
		return !_problem;
	}

	private long _lastTimePrinted = 0;
	protected String printSkippedTimes() {
		long time = BrokerInternalTimer.read().timeSecs();
		String str = "";
		for(long i=_lastTimePrinted+1 ; i<time ; i++)
			str += (BrokerInternalTimerReading.formatSecsString(i) + '\n');
		
		_lastTimePrinted = time;
		return str;
	}
	
	@Override
	public abstract String toString();
	protected abstract String getFileName();
	protected abstract void runMe() throws IOException;
	
	@Override
	public void initializeFile() {
//		synchronized (_lock)
		{
			if(_problem || _initialized)
				return;
			
			String filename = getFileName();
			if(filename == null) {
				_initialized = true;
				return;
			}
			
			File logfile = new File(filename);
			try {
				_logFileWriter = new FileWriter(logfile);
				_initialized = true;
				return;
			} catch (IOException iox) {
				problemOccured("Initialzing the file failed for: " + this, iox);
			}
			
			_initialized = false;
		}
	}
	
	@Override
	public boolean performLogging() {
		synchronized (_lock) 
		{
			if(!_initialized)
				initializeFile();
			if(!isEnabled())
				return false;
						
			long sinceLastLogging = SystemTime.currentTimeMillis() - _lastLoggingTime;
			if(sinceLastLogging < getDefaultLoggingInterval())
				return false;
			
			_lastLoggingTime = SystemTime.currentTimeMillis();
			
			try {
				if(isEnabled()) {
					_fileWriteCount++;
					runMe();
				}
			} catch (IOException iox) {
				problemOccured("Performing log failed for: " + this , iox);
				return false;
			}
			return true;
		}
	}

	protected void problemOccured(String message, Exception ex) {
		LoggerFactory.getLogger().warn(this, "Problem in casual logger: " + this + " --- " + message + "_" + ex);
		_problem = true;
	}
	
	@Override
	public boolean forceFlush() {
		synchronized (_lock) 
		{
			if(!_initialized)
				initializeFile();
			if(!isEnabled())
				return false;
			
			_lastForcedFlushTime = SystemTime.currentTimeMillis();
			if(_logFileWriter != null)
				try {
					_logFileWriter.flush();
				} catch (IOException iox) {
					problemOccured("Forcing flush failed for: " + this , iox);
					return false;
				}
			
			return true;
		}
	}
	
	@Override
	public final long getNextDesiredLoggingTime() {
		synchronized (_lock) 
		{
			long sinceLastLog = SystemTime.currentTimeMillis() - _lastLoggingTime;
			long remainingTime = getDefaultLoggingInterval() - sinceLastLog;
			if(remainingTime < 0)
				return 0;
			else
				return remainingTime; 
		}
	}
	
	@Override
	public final long getNextDesiredForcedFlushTime() {
		synchronized (_lock) 
		{
			long sinceLastForcedFlush = SystemTime.currentTimeMillis() - _lastForcedFlushTime;
			long remainingTime = getDefaultForcedFlushInterval() - sinceLastForcedFlush;
			if(remainingTime < 0)
				return 0;
			else
				return remainingTime; 
		}
	}

	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_CPU_PROFILER;
	}
}
