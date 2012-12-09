package org.msrg.publiy.utils.log;

public class LoggerUtility {
	
	public static LoggingSource[] unmaskLoggingSource(LoggingSource[] sources, LoggingSource[] unmasks){
		LoggingSource[] modifiedSources = sources;
		for ( int i=0 ; i<unmasks.length ; i++ )
			modifiedSources = unmaskLoggingSource(modifiedSources, unmasks[i]);
		
		return modifiedSources;
	}
	
	public static LoggingSource[] unmaskLoggingSource(LoggingSource[] sources, LoggingSource unmask){
		for ( int i=0 ; i<sources.length ; i++ )
			if ( sources[i] == null ){
				sources[i] = unmask;
				return sources;
			}
		
			else if ( sources[i] == unmask )
				return sources;
		
		LoggingSource[] newSources = new LoggingSource[sources.length + 1];
		for ( int i=0 ; i<sources.length ; i++ )
			newSources[i] = sources[i];
		newSources[sources.length + 1] = unmask;
		
		return newSources;
	}
	
	public static void maskLoggingSource(LoggingSource[] sources, LoggingSource[] masks){
		for ( int i=0 ; i<masks.length ; i++ )
			maskLoggingSource(sources, masks[i]);
	}
	
	public static void maskLoggingSource(LoggingSource[] sources, LoggingSource mask){
		for ( int i=0 ; i<sources.length ; i++ )
			if ( sources[i] == mask )
				sources[i] = null;
	}
}
