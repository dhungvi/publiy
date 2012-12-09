package org.msrg.publiy.utils.log;

public interface ILoggingSourceRegistrar {

	public void registerLoggingSource(LogLevel logLevel, LoggingSource source);
	public boolean isRegistered(LogLevel logLevel, LoggingSource source);
	public void deregisterLoggingSource(LogLevel logLevel, LoggingSource source);
	public LoggingSource[] getAllRegisteredLoggingSources(LogLevel logLevel);
	
}
