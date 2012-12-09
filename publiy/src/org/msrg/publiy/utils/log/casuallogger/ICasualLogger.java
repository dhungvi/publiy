package org.msrg.publiy.utils.log.casuallogger;

public interface ICasualLogger {

	void initializeFile();
	boolean isEnabled();
	boolean performLogging();
	boolean forceFlush();
	long getNextDesiredLoggingTime();
	long getNextDesiredForcedFlushTime();

}
