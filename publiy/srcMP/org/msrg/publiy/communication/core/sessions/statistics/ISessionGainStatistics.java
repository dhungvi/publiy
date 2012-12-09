package org.msrg.publiy.communication.core.sessions.statistics;
import org.msrg.publiy.communication.core.sessions.ISessionMP;

public class ISessionGainStatistics extends ISessionStatistics {
	private static int _VALIDITY_PERIOD = 10000;
	
	private ISessionGainStatistics(ISessionMP session) {
		super(session, _VALIDITY_PERIOD, session.getTotalOutPublications() * (session.getDistance()));
		
		if ( !session.isDistanceSet() )
			throw new IllegalStateException("Distance not set: " + session);
	}
	
	static ISessionGainStatistics getISessionGainStatistics(ISessionMP session){
		if ( session != null && session.isDistanceSet() )
			return new ISessionGainStatistics(session);
		else
			return null;
	}
}
