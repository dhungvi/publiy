package org.msrg.publiy.communication.core.sessions.statistics;
import org.msrg.publiy.communication.core.sessions.ISessionMP;

public class ISessionOutPublicationStatistics extends ISessionStatistics {
	private static int _VALIDITY_PERIOD = 10000;
	
	ISessionOutPublicationStatistics(ISessionMP session){
		super(session, _VALIDITY_PERIOD, session.getTotalOutPublications());
	}

}
