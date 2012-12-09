package org.msrg.publiy.utils.log;

import java.io.Serializable;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.Date;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimerReading;

public class LogEntry implements Serializable {
	
	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 9169088711496929071L;
	protected static int EXCEPTION_STACK_LOOKUP = 10;
	private String [] _logStr;
	private Sequence _localSequence;
//	private Date _localTime;
	private BrokerInternalTimerReading _localTime;
	private LoggingSource _loggingSource;
	private LogLevel _logLevel;
	private Exception _ex;
	
	protected LogEntry(LocalSequencer localSequencer, LogLevel logLevel, LoggingSource loggingSource, Exception ex, String ... str){
		this(localSequencer, logLevel, loggingSource, str);
		_ex = ex;
	}
	
	protected LogEntry(LocalSequencer localSequencer, LogLevel logLevel, LoggingSource loggingSource, String ... str){
		_logStr = str;
		_localTime = BrokerInternalTimer.read();
		_localSequence = localSequencer.getNext();
		_loggingSource = loggingSource;
		_logLevel = logLevel;
	}
	
	public Exception getException(){
		return _ex;
	}
	
	public BrokerInternalTimerReading getLogTime(){
		return _localTime;
	}
	
	public Sequence getLogSequence(){
		return _localSequence;
	}

	public LoggingSource getLoggingSource(){
		return _loggingSource;
	}
	
	public LogLevel getLogLevel(){
		return _logLevel;
	}
	
	static String format(LogLevel logLevel, LoggingSource source, Exception ex, String ... msgs){
		StringBuilder strBuilder = new StringBuilder();
		switch(logLevel){
		case LOG_DEBUG:
		{
			StackTraceElement[] stackTraces = new Exception().getStackTrace();
			strBuilder.append("DBG:" + source + "@" + BrokerInternalTimer.read().toString() + ":" + stackTraces[2].getMethodName() + "(" + stackTraces[2].getLineNumber() + ")");
			for(String msg : msgs)
				strBuilder.append(msg);
			strBuilder.append("\t" + Thread.currentThread().getName());
			break;
		}
		
		case LOG_INFO:
		{
//			long time = System.nanoTime();
			strBuilder.append( "INF:" + source + "@" + BrokerInternalTimer.read().toString() + ":");
			for(String msg : msgs)
				strBuilder.append(msg);
			strBuilder.append("\t" + Thread.currentThread().getName());
			break;
		}
		
		case LOG_ERROR:
		{
			strBuilder.append("ERR:" + source + "@" + BrokerInternalTimer.read().toString() + ":");
			for(String msg : msgs)
				strBuilder.append(msg);
			strBuilder.append("\t" + Thread.currentThread().getName());
			break;
		}

		case LOG_WARN:
		{
			strBuilder.append("WARN:" + source + "@" + BrokerInternalTimer.read().toString() + ":");
			for(String msg : msgs)
				strBuilder.append(msg);
			strBuilder.append("\t" + Thread.currentThread().getName());
			break;
		}

		case LOG_INFOX:
		{
			if ( ex == null )
				return "INFX: null";
			String exStackContent = buildStackTrace(ex);
			strBuilder.append("INFX:");
			for(String msg : msgs)
				strBuilder.append(msg);
			strBuilder.append("@" + BrokerInternalTimer.read().toString() + ":" + ":: EX:[\"" + ex.getMessage() + "\""+ exStackContent + "]" + "\t" + Thread.currentThread().getName());
			break;
		}

		case LOG_SPECIAL:
		{
			strBuilder.append("SPECIAL:" + source + "@" + BrokerInternalTimer.read().toString() + ":");
			for(String msg : msgs)
				strBuilder.append(msg);
			strBuilder.append("\t" + Thread.currentThread().getName());
			break;
		}
		
		default:
			throw new IllegalArgumentException(logLevel + " is not supported.");
		}
		
		return strBuilder.toString();
	}
	
	protected static String buildStackTrace(Exception ex){
		String exStackContent = "";
		StackTraceElement[] stTrace = ex.getStackTrace();
		int maxStackLookup = EXCEPTION_STACK_LOOKUP < stTrace.length?EXCEPTION_STACK_LOOKUP:stTrace.length;
		
		for ( int i=0 ; i<maxStackLookup ; i++ ){
			exStackContent += ( " >>> " + stTrace[i].getClassName() + "." + stTrace[i].getMethodName() + "(" + stTrace[i].getLineNumber() + ")" ); 
		}
		
		return exStackContent;
	}
	
	@Override
	public String toString(){
		return format(_logLevel, _loggingSource, _ex, _logStr);
	}
}
