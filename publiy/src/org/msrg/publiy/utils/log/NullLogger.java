package org.msrg.publiy.utils.log;

import java.util.List;

public class NullLogger implements ILogger {

	private String _name;
	
	NullLogger(String name){
		_name = name;
	}
	
	@Override
	public List<LogEntry> collectLogs() {
		return null;
	}

	@Override
	public void debug(ILoggerSource source, String ... msg) {
		return;
	}

	@Override
	public void debug(LoggingSource source, String ... msg) {
		return;
	}

	@Override
	public void error(ILoggerSource source, String ... msg) {
		return;
	}

	@Override
	public void error(LoggingSource source, String ... msg) {
		return;
	}

	@Override
	public void warn(ILoggerSource source, String ... msg) {
		return;
	}

	@Override
	public void warn(LoggingSource source, String ... msg) {
		return;
	}
	
	@Override
	public LoggerTypes getLoggerType() {
		return LoggerTypes.LOGGER_NULL_LOGGER;
	}

	@Override
	public void info(ILoggerSource source, String ... msg) {
		return;
	}

	@Override
	public void info(LoggingSource source, String ... msg) {
		return;
	}

	@Override
	public void infoX(ILoggerSource source, Exception ex, String ... msg) {
		return;
	}

	@Override
	public void infoX(LoggingSource source, Exception ex, String ... msg) {
		return;
	}

	@Override
	public void special(ILoggerSource source, String ... msg) {
		return;
	}

	@Override
	public void special(LoggingSource source, String ... msg) {
		return;
	}

	@Override
	public void deregisterLoggingSource(LogLevel logLevel, LoggingSource source) {
		return;
	}

	@Override
	public LoggingSource[] getAllRegisteredLoggingSources(LogLevel logLevel) {
		return null;
	}

	@Override
	public boolean isRegistered(LogLevel logLevel, LoggingSource source) {
		return false;
	}

	@Override
	public void registerLoggingSource(LogLevel logLevel, LoggingSource source) {
		return;
	}

	@Override
	public String toString(){
		return "NullLogger(" + _name + ")";
	}
}
