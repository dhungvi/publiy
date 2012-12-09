package org.msrg.publiy.communication.core.sessions.statistics;
import org.msrg.publiy.communication.core.sessions.ISessionMP;


public class ISessionRankStatistics extends ISessionStatistics {
	private static int _VALIDITY_PERIOD = 10000;
	
	ISessionRankStatistics(ISessionMP session, int rank) {
		super(session, _VALIDITY_PERIOD, rank);
	}
}
