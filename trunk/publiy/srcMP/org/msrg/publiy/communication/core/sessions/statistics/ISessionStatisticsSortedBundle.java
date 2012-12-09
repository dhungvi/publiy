package org.msrg.publiy.communication.core.sessions.statistics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.communication.core.sessions.ISessionMP;

public class ISessionStatisticsSortedBundle {
	private static DateFormat DATE_FORMATE = new SimpleDateFormat("mm:ss");
	private final Date _now = new Date();
	private final SortedSet<ISessionGainStatistics> _sortedGains = new TreeSet<ISessionGainStatistics>();
	private final SortedSet<ISessionInPublicationStatistics> _sortedInPublications = new TreeSet<ISessionInPublicationStatistics>();
	private final SortedSet<ISessionOutPublicationStatistics> _sortedOutPublications = new TreeSet<ISessionOutPublicationStatistics>();
	private final SortedSet<ISessionRankStatistics> _sortedRanks = new TreeSet<ISessionRankStatistics>();
	
	public ISessionStatisticsSortedBundle(ISessionMP[] sessions){
		for ( int i=0 ; i<sessions.length ; i++ )
		{
			if ( sessions[i] == null )
				continue;
			
			ISessionGainStatistics sessionGain = ISessionGainStatistics.getISessionGainStatistics(sessions[i]);
			if ( sessionGain == null )
				LoggerFactory.getLogger().warn(LoggingSource.LOG_SRC_SESSION_MAN_MP, "SessionGain not set for: " + sessions[i]);
			else
				_sortedGains.add(sessionGain);
			
			ISessionInPublicationStatistics sessionInPublications = new ISessionInPublicationStatistics(sessions[i]);
			_sortedInPublications.add(sessionInPublications);
			
			ISessionOutPublicationStatistics sessionOutPublications = new ISessionOutPublicationStatistics(sessions[i]);
			_sortedOutPublications.add(sessionOutPublications);
		}
		
		int rank = 0;
		ISessionGainStatistics[] gainsStatisticsArray = _sortedGains.toArray(new ISessionGainStatistics[0]);
		for ( int i=1 ; i<=gainsStatisticsArray.length ; i++ ){
			ISessionMP session = gainsStatisticsArray[gainsStatisticsArray.length-i]._session;
			ISessionRankStatistics sessionRank = new ISessionRankStatistics(session, rank++);
			_sortedRanks.add(sessionRank);
		}
	}
	
	public ISessionGainStatistics[] getSessionsSortedByGain(){
		return _sortedGains.toArray(new ISessionGainStatistics[0]);
	}
	
	public ISessionOutPublicationStatistics[] getSessionsSortedByOutPublications(){
		return _sortedOutPublications.toArray(new ISessionOutPublicationStatistics[0]);
	}
	
	public ISessionInPublicationStatistics[] getSessionsSortedByInPublications(){
		return _sortedInPublications.toArray(new ISessionInPublicationStatistics[0]);
	}
	
	public ISessionRankStatistics[] getSessionsSortedByRank(){
		return _sortedRanks.toArray(new ISessionRankStatistics[0]);
	}

	@Override
	public String toString(){
		String str = "Sorted Bundle (@" + BrokerInternalTimer.read() + ")";
		
		str += "\n==GAINS: ";
		for ( Iterator<ISessionGainStatistics> sortedGainsIt = _sortedGains.iterator(); sortedGainsIt.hasNext() ; )
			str += sortedGainsIt.next().toString() + ((sortedGainsIt.hasNext())?", ":"");
		
		str += "\n==OUTPUB: ";
		for ( Iterator<ISessionOutPublicationStatistics> outPublicationsIt = _sortedOutPublications.iterator(); outPublicationsIt.hasNext() ; )
			str += outPublicationsIt.next().toString() + ((outPublicationsIt.hasNext())?", ":"");
		
		str += "\n==RANKS: ";
		for ( Iterator<ISessionRankStatistics> sortedRanksIt = _sortedRanks.iterator(); sortedRanksIt.hasNext() ; )
			str += sortedRanksIt.next().toString() + ((sortedRanksIt.hasNext())?", ":"");
		
		return str + "\n";
	}
}
