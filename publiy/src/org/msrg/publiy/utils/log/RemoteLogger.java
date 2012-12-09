package org.msrg.publiy.utils.log;

import java.net.InetSocketAddress;
import java.util.List;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;


public class RemoteLogger extends ConsoleLogger {
	
	/**
	 * Auto Generated
	 */
	protected final LogCollection _logCollection;
	protected final LocalSequencer _localSequencer;
	
	protected RemoteLogger(LocalSequencer localSequencer, String name){
		super(name);
		_localSequencer = localSequencer;
		_logCollection = new LogCollection(_localSequencer);
	}
	
	public String toString(){
		return "RemoteLogger: " + _name;
	}
	
	@Override
	public void error(LoggingSource source, String ... msg){
		if ( ! isRegistered(LogLevel.LOG_ERROR, source) )
			return;
		_logCollection.appendLog(_localSequencer, LogLevel.LOG_ERROR, source, msg);
	}
	
	@Override
	public void infoX(ILoggerSource source, Exception ex, String ... msg){
		if ( ! isRegistered(LogLevel.LOG_INFOX, source.getLogSource()) )
			return;
		_logCollection.appendLog(_localSequencer, LogLevel.LOG_INFOX, source.getLogSource(), msg);
	}
	
	@Override
	public void info(LoggingSource source, String ... msg){
		if ( ! isRegistered(LogLevel.LOG_INFO, source) )
			return;
		_logCollection.appendLog(_localSequencer, LogLevel.LOG_INFO, source, msg);
	}
	
	@Override
	public void special(LoggingSource source, String ... msg){
		if ( ! isRegistered(LogLevel.LOG_SPECIAL, source) )
			return;
		
		_logCollection.appendLog(_localSequencer, LogLevel.LOG_SPECIAL, source, msg);
	}
	
	@Override
	public void debug(LoggingSource source, String ... msg){
		if ( ! isRegistered(LogLevel.LOG_DEBUG, source) )
			return;
		_logCollection.appendLog(_localSequencer, LogLevel.LOG_DEBUG, source, msg);
	}
	
	@Override
	public List<LogEntry> collectLogs(){
		return _logCollection.getCollectedLogs();
	}

	public static void main(String[] argv) {
		LocalSequencer localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 2000));
		Exception x = new Exception("Hello msg.");
		RemoteLogger remoteLogger = new RemoteLogger(localSequencer, "");
		remoteLogger.infoX(LoggingSource.LOG_SRC_DUMMY_SOURCE, x, "This is an exception.");
	}
	
}
