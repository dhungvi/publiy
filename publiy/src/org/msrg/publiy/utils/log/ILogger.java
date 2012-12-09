package org.msrg.publiy.utils.log;

import java.util.List;


public interface ILogger extends ILoggingSourceRegistrar {

	public void error(ILoggerSource source, String ... msg);
	public void error(LoggingSource source, String ... msg);

	public void warn(ILoggerSource source, String ... msg);
	public void warn(LoggingSource source, String ... msg);

	public void debug(ILoggerSource source, String ... msg);
	public void debug(LoggingSource source, String ... msg);

	public void info(ILoggerSource source, String ... msg);
	public void info(LoggingSource source, String ... msg);
	
	public void infoX(ILoggerSource source, Exception ex, String ... msg);
	public void infoX(LoggingSource source, Exception ex, String ... msg);
	
	public void special(ILoggerSource source, String ... msg);
	public void special(LoggingSource source, String ... msg);
	
	public List<LogEntry> collectLogs();
	
	public LoggerTypes getLoggerType();
}
