package org.msrg.publiy.utils.log;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class LoggingSourceRegistry implements Serializable {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 5296173441312532871L;
	
	private long _registeredLoggingSourceBitArray = 0;
	public static int MAX_SUPPORTED_LOGGING_SOURCE_BIT_POSITION = 63;
	public static int MIN_SUPPORTED_LOGGING_SOURCE_BIT_POSITION = 0;
	
	public static boolean isValidLoggingSourceBitPosition(int position){
		return (position>=MIN_SUPPORTED_LOGGING_SOURCE_BIT_POSITION) 
			&& (position<=MAX_SUPPORTED_LOGGING_SOURCE_BIT_POSITION);
	}
	
	// Customize below for additional flexibility.
	public static LoggingSource[] DEFAULT_LOGGING_ENABLED_SOURCES1 = {
	};
	
	public void registerLoggingSource(LoggingSource ls){
		synchronized (this) {
			long oneVector = 1;
			oneVector = (oneVector << ls.getBitPoisition());
			_registeredLoggingSourceBitArray = _registeredLoggingSourceBitArray | oneVector;
		}
	}
	
	public boolean isRegistered(LoggingSource ls){
		synchronized (this) {
			long oneVector = 1;
			oneVector = (oneVector<<ls.getBitPoisition());
			return (_registeredLoggingSourceBitArray&oneVector) != 0;
		}
	}
	
	public void deregisterLoggingSource(LoggingSource ls){
		synchronized (this) {
			long oneVector = 1;
			oneVector = ~(oneVector<<ls.getBitPoisition());
			_registeredLoggingSourceBitArray = _registeredLoggingSourceBitArray & oneVector;
		}
	}
	
	public LoggingSource[] getAllRegisteredLoggingSources(){
		synchronized (this) {
			Set<LoggingSource> registeredLS = new HashSet<LoggingSource>();
			LoggingSource[] allLS = LoggingSource.values();
			for ( int i=0 ; i<allLS.length ; i++ )
				if ( isRegistered(allLS[i]) )
					registeredLS.add(allLS[i]);
			
			return registeredLS.toArray(new LoggingSource[0]);
		}
	}
	
//	public static void main(String[] argv){
//		LoggingSourceRegistry lsr = new LoggingSourceRegistry();
//		lsr.registerLoggingSource(LoggingSource.LOG_SRC_BROKER);
//		boolean b1 = lsr.isRegistered(LoggingSource.LOG_SRC_BROKER_CONTROLLER);
//		boolean b2 = lsr.isRegistered(LoggingSource.LOG_SRC_BROKER);
//		lsr.registerLoggingSource(LoggingSource.LOG_SRC_BROKER_CONTROLLER);
//		lsr.deregisterLoggingSource(LoggingSource.LOG_SRC_BROKER);
//		boolean b3 = lsr.isRegistered(LoggingSource.LOG_SRC_BROKER_CONTROLLER);
//		boolean b4 = lsr.isRegistered(LoggingSource.LOG_SRC_BROKER);
//		
//		System.out.println(b1 + " " + b2 + " " + b3 + " " + b4);
//	}
}
