package org.msrg.publiy.utils.log;

import java.util.List;

public class ConsoleLogger implements ILogger {
	
	protected final String _name;
	protected final LoggingSourceRegistry _debugLoggingSourceRegistry = new LoggingSourceRegistry();
	protected final LoggingSourceRegistry _infoLoggingSourceRegistry = new LoggingSourceRegistry();
	protected final LoggingSourceRegistry _infoxLoggingSourceRegistry = new LoggingSourceRegistry();
	protected final LoggingSourceRegistry _errorLoggingSourceRegistry = new LoggingSourceRegistry();
	protected final LoggingSourceRegistry _warnLoggingSourceRegistry = new LoggingSourceRegistry();
	
	protected ConsoleLogger(String name){
		if ( name == null )
			_name = getLoggerType().toString();
		else
			_name = name;
	}
	
	public String toString(){
		return "Logger: " + _name;
	}
	
	@Override
	public List<LogEntry> collectLogs(){
		return null;
	}
	
	@Override
	public void error(ILoggerSource source, String ... msg){
		error(source.getLogSource(), msg);
	}
	
	@Override
	public void error(LoggingSource source, String ... msg){
		if ( ! isRegistered(LogLevel.LOG_ERROR, source) )
			return;
		String logStr = LogEntry.format(LogLevel.LOG_ERROR, source, null, msg);
		System.err.println(logStr);
	}
	
	@Override
	public void warn(ILoggerSource source, String ... msg){
		warn(source.getLogSource(), msg);
	}
	
	@Override
	public void warn(LoggingSource source, String ... msg){
		if ( ! isRegistered(LogLevel.LOG_WARN, source) )
			return;
		String logStr = LogEntry.format(LogLevel.LOG_WARN, source, null, msg);
		System.err.println(logStr);
	}

	@Override
	public void infoX(LoggingSource source, Exception ex, String ... msg){
		if ( ! isRegistered(LogLevel.LOG_INFOX, source) )
			return;
		String logStr = LogEntry.format(LogLevel.LOG_INFOX, source, ex, msg);
		System.out.println(logStr);
	}
	
	@Override
	public void infoX(ILoggerSource source, Exception ex, String ... msg){
		infoX(source.getLogSource(), ex, msg);
	}
	
	@Override
	public void info(ILoggerSource source, String ... msg){
		info(source.getLogSource(), msg);
	}

	@Override
	public void info(LoggingSource source, String ... msg){
		if ( ! isRegistered(LogLevel.LOG_INFO, source) )
			return;
		String logStr = LogEntry.format(LogLevel.LOG_INFO, source, null, msg);
		System.out.println(logStr);
	}

	@Override
	public void special(ILoggerSource source, String ... msg){
		int line = new Exception().getStackTrace()[1].getLineNumber();
		special(source.getLogSource(), "" + line + msg);
	}

	@Override
	public void special(LoggingSource source, String ... msg){
		if ( ! isRegistered(LogLevel.LOG_SPECIAL, source) )
			return;
		String logStr = LogEntry.format(LogLevel.LOG_SPECIAL, source, null, msg);
		System.out.println(logStr);
	}
	
	@Override
	public void debug(ILoggerSource source, String ... msg){
		debug(source.getLogSource(), msg);
	}

	@Override
	public void debug(LoggingSource source, String ... msg) {
		if ( ! isRegistered(LogLevel.LOG_DEBUG, source) )
			return;
		String logStr = LogEntry.format(LogLevel.LOG_DEBUG, source, null, msg);
		System.out.println(logStr);
	}
	
	@Override
	public LoggerTypes getLoggerType(){
		return LoggerTypes.LOGGER_CONSOLE;
	}

	public static void main(String[] argv){
		Exception x = new Exception("Hello msg.");
		ConsoleLogger logger = new ConsoleLogger("");
		logger.infoX(LoggingSource.LOG_SRC_DUMMY_SOURCE, x, "This is an exception.");
	}

	@Override
	public void deregisterLoggingSource(LogLevel logLevel, LoggingSource source) {
		switch(logLevel){
		case LOG_DEBUG:
			_debugLoggingSourceRegistry.deregisterLoggingSource(source);
			return;

		case LOG_INFO:
			_infoLoggingSourceRegistry.deregisterLoggingSource(source);
			return;
			
		case LOG_INFOX:
			_infoxLoggingSourceRegistry.deregisterLoggingSource(source);
			return;
			
		case LOG_ERROR:
			_errorLoggingSourceRegistry.deregisterLoggingSource(source);
			return;
			
		case LOG_WARN:
			_warnLoggingSourceRegistry.deregisterLoggingSource(source);
			return;
			
		case LOG_EXCEPTION:
		case LOG_SPECIAL:
			return;
			
		default:
			throw new UnsupportedOperationException(logLevel + " " + source);
		}
	}

	@Override
	public LoggingSource[] getAllRegisteredLoggingSources(LogLevel logLevel) {
		switch(logLevel){
		case LOG_DEBUG:
			return _debugLoggingSourceRegistry.getAllRegisteredLoggingSources();

		case LOG_INFO:
			return _infoLoggingSourceRegistry.getAllRegisteredLoggingSources();
			
		case LOG_INFOX:
			_infoxLoggingSourceRegistry.getAllRegisteredLoggingSources();
			
		case LOG_ERROR:
			_errorLoggingSourceRegistry.getAllRegisteredLoggingSources();
			
		case LOG_WARN:
			_warnLoggingSourceRegistry.getAllRegisteredLoggingSources();
			
		case LOG_EXCEPTION:
			return LoggingSource.values();
			
		case LOG_SPECIAL:
			return LoggingSource.values();
			
		default:
			throw new UnsupportedOperationException(logLevel + "");
		}
	}

	@Override
	public boolean isRegistered(LogLevel logLevel, LoggingSource source) {
		if ( logLevel == LogLevel.LOG_DEBUG )
		{
			return _debugLoggingSourceRegistry.isRegistered(source);
		}
		else if ( logLevel == LogLevel.LOG_INFO )
		{
			return _infoLoggingSourceRegistry.isRegistered(source);
		}
		else if ( logLevel == LogLevel.LOG_INFOX )
		{
			return _infoxLoggingSourceRegistry.isRegistered(source);
		}
		else if ( logLevel == LogLevel.LOG_ERROR )
		{
			return _errorLoggingSourceRegistry.isRegistered(source);
		}
		
		else if ( logLevel == LogLevel.LOG_EXCEPTION )
		{
			return true;
		}
		
		else if ( logLevel == LogLevel.LOG_SPECIAL )
		{
			return true;
		}
		
		return false;
	}

	@Override
	public void registerLoggingSource(LogLevel logLevel, LoggingSource source) {
		switch(logLevel){
		case LOG_DEBUG:
			_debugLoggingSourceRegistry.registerLoggingSource(source);
			return;

		case LOG_INFO:
			_infoLoggingSourceRegistry.registerLoggingSource(source);
			return;
			
		case LOG_INFOX:
			_infoxLoggingSourceRegistry.registerLoggingSource(source);
			return;
			
		case LOG_ERROR:
			_errorLoggingSourceRegistry.registerLoggingSource(source);
			return;
			
		case LOG_WARN:
			_warnLoggingSourceRegistry.registerLoggingSource(source);
			return;
			
		case LOG_EXCEPTION:
		case LOG_SPECIAL:
			return;
			
		default:
			throw new UnsupportedOperationException(logLevel + " " + source);
		}
	}
	
}
