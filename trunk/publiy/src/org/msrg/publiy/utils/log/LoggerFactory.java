package org.msrg.publiy.utils.log;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class LoggerFactory {
	
	static {
		create(null,
				LoggerTypes.LOGGER_CONSOLE, 
				LoggingSource.values(),
				Broker.DEBUG ? LoggingSource.values() : null,
				LoggingSource.values(),
				LoggingSource.values(), 
				LoggingSource.values());
	}
	
	private static ILogger _logger;
	
	public static synchronized void create(LocalSequencer localSequencer,
											LoggerTypes loggerType, 
											LoggingSource[] infoLoggingSources,
											LoggingSource[] debugLoggingSources,
											LoggingSource[] warnLoggingSource,
											LoggingSource[] errorLoggingSources,
											LoggingSource[] infoXLoggingSources) {
		String loggerName = null;//LocalSequencer.getLocalSequencer().getLocalAddress().toString();
		switch(loggerType) {
		case LOGGER_CONSOLE:
			_logger = new ConsoleLogger(loggerName);
			break;
		
		case LOGGER_NULL_LOGGER:
			_logger = new NullLogger(loggerName);
			break;
		
		case LOGGER_REMOTE:
			_logger = new RemoteLogger(localSequencer, loggerName);
			break;
		
		case LOGGER_FILE:
			throw new UnsupportedOperationException();
		
		default:
			throw new UnsupportedOperationException(loggerType + " is not supported.");
		}
		
		modifyLogger(infoLoggingSources, null, debugLoggingSources, null, warnLoggingSource, null, errorLoggingSources, null, infoXLoggingSources, null);
	}
	
	public static void modifyLogger(LoggingSource[] addInfoLoggingSources, LoggingSource[] removeInfoLoggingSources,
									LoggingSource[] addDebugLoggingSources, LoggingSource[] removeDebugLoggingSources,
									LoggingSource[] addWarnLoggingSources, LoggingSource[] removeWarnLoggingSources,
									LoggingSource[] addErrorLoggingSources, LoggingSource[] removeErrorLoggingSources,
									LoggingSource[] addInfoXLoggingSources, LoggingSource[] removeInfoXLoggingSources) {
		synchronized(_logger) {
			if(addInfoLoggingSources != null) {
				for ( int i=0 ; i<addInfoLoggingSources.length ; i++)
					if(addInfoLoggingSources[i]!=null)
						_logger.registerLoggingSource(LogLevel.LOG_INFO, addInfoLoggingSources[i]);
			}
			
			if(removeInfoLoggingSources != null) {
				for ( int i=0 ; i<removeInfoLoggingSources.length ; i++)
					if(removeInfoLoggingSources[i]!=null)
						_logger.deregisterLoggingSource(LogLevel.LOG_INFO, removeInfoLoggingSources[i]);
			}
			
			if(addInfoXLoggingSources != null) {
				for ( int i=0 ; i<addInfoXLoggingSources.length ; i++)
					if(addInfoXLoggingSources[i]!=null)
						_logger.registerLoggingSource(LogLevel.LOG_INFOX, addInfoXLoggingSources[i]);
			}
			
			if(removeInfoXLoggingSources != null) {
				for ( int i=0 ; i<removeInfoXLoggingSources.length ; i++)
					if(removeInfoXLoggingSources[i]!=null)
						_logger.deregisterLoggingSource(LogLevel.LOG_INFOX, removeInfoXLoggingSources[i]);
			}
			
			if(addDebugLoggingSources != null) {
				for ( int i=0 ; i<addDebugLoggingSources.length ; i++)
					if(addDebugLoggingSources[i]!=null)
						_logger.registerLoggingSource(LogLevel.LOG_DEBUG, addDebugLoggingSources[i]);	
			}
			
			if(removeDebugLoggingSources != null) {
				for ( int i=0 ; i<removeDebugLoggingSources.length ; i++)
					if(removeDebugLoggingSources[i]!=null)
						_logger.deregisterLoggingSource(LogLevel.LOG_DEBUG, removeDebugLoggingSources[i]);	
			}
			
			if(addWarnLoggingSources != null) {
				for ( int i=0 ; i<addWarnLoggingSources.length ; i++)
					if(addWarnLoggingSources[i]!=null)
						_logger.registerLoggingSource(LogLevel.LOG_WARN, addWarnLoggingSources[i]);	
			}
			
			if(removeWarnLoggingSources != null) {
				for ( int i=0 ; i<removeWarnLoggingSources.length ; i++)
					if(removeWarnLoggingSources[i]!=null)
						_logger.deregisterLoggingSource(LogLevel.LOG_WARN, removeWarnLoggingSources[i]);	
			}
			
			if(addErrorLoggingSources != null) {
				for ( int i=0 ; i<addErrorLoggingSources.length ; i++)
					if(addErrorLoggingSources[i]!=null)
						_logger.registerLoggingSource(LogLevel.LOG_ERROR, addErrorLoggingSources[i]);
			}
			
			if(removeErrorLoggingSources != null) {
				for ( int i=0 ; i<removeErrorLoggingSources.length ; i++)
					if(removeErrorLoggingSources[i]!=null)
						_logger.deregisterLoggingSource(LogLevel.LOG_ERROR, removeErrorLoggingSources[i]);
			}
		}
	}
	
	public static ILogger getLogger() {
		return _logger;
	}
}
