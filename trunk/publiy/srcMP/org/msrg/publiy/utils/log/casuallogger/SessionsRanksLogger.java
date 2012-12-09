package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.communication.core.sessions.statistics.ISessionStatisticsSortedBundle;

public class SessionsRanksLogger extends AbstractCasualLogger {
	public static final int SAMPLING_INTERVAL = 1000;
	public static final int PROFILER_CACHE_SIZE = 5;
	
	private final IBrokerShadow _brokerShadow;
	private final String _ranksLogFilename;
	private final List<ISessionStatisticsSortedBundle> _sortedBundles = new LinkedList<ISessionStatisticsSortedBundle>();
	
	public SessionsRanksLogger(BrokerShadow brokerShadow) {
		brokerShadow.setSessionsRanksLogger(this);
		_brokerShadow = brokerShadow;
		_ranksLogFilename = _brokerShadow.getRanksLogFileName();
	}
	
	@Override
	protected String getFileName() {
		return _ranksLogFilename;
	}

	@Override
	protected void runMe() throws IOException {
		synchronized(_sortedBundles){
			Iterator<ISessionStatisticsSortedBundle> sortedBundlesIt = _sortedBundles.iterator();
			while ( sortedBundlesIt.hasNext() ){
				ISessionStatisticsSortedBundle bundle = sortedBundlesIt.next();
				String logLine = bundle.toString();
				_logFileWriter.write(logLine + "\n");
			}
			
			_sortedBundles.clear();
		}
	}

	public void logRanks(ISessionStatisticsSortedBundle sessionsStatisticsBundle){
		synchronized(_sortedBundles){
			_sortedBundles.add(sessionsStatisticsBundle);
		}
	}
	
	@Override
	public String toString() {
		return "RanksLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}
	
	@Override
	protected int getDefaultLoggingInterval(){
		return SAMPLING_INTERVAL;
	}
}
