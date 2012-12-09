package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class StatisticsLogger extends AbstractCasualLogger {
	@SuppressWarnings("unused")
	private long _lastWrite = SystemTime.currentTimeMillis();
	public boolean _compact = false; 
	public static final int SAMPLING_INTERVAL = 1000;
	public static final int FORCE_FILE_FLUSH = 5;

	protected IBrokerShadow _brokerShadow;
	protected final String _statisticsLogFilename;
	
	private int _totalLocalSubscriptionCount = 0;
	private int _totalSubscriptionCount = 0;
	
	private int _bwInavailabilityCount = 0;
	
	private int _publishCount = 0;
	private int _deliveredCount = 0;
	private int _duplicateCount = 0;
	
	private int _topologyMapSize = 0;
	private int _timestampMapSize = 0;
	
	private int _numberOfMachings = 0;
	private int _machingsTime = 0;
	private int _numberOfSubsMatched = 0;
	private int _numberOfPredicatesMatched = -1;
	private int _numberOfSubsCovered = 0;
	private int _numberOfPredicatesCovered = -1;

	public StatisticsLogger(BrokerShadow broker) {
		broker.setStatisticsLogger(this);
		_brokerShadow = broker;
		_statisticsLogFilename = _brokerShadow.getStatisticsLogFilename();
	}
	
	protected DecimalFormat _decf2 = new DecimalFormat ("0.00");
	protected DecimalFormat _decf3 = new DecimalFormat ("0.000");
	@Override
	protected void runMe() throws IOException {
		synchronized (_lock) {
			if(_firstTime) {
				writeHeader(0, _logFileWriter);
				_logFileWriter.write('\n');
				_firstTime = false;
			}
		
			long currTime = SystemTime.currentTimeMillis();
			String logLine = createLogLinePrivately();

			reinitVariables();
			
			_logFileWriter.write(logLine);
			_logFileWriter.write('\n');
			
			_lastWrite = currTime;
		}
	}

	protected String createLogLinePrivately() {
		return 
				BrokerInternalTimer.read().toString() + "\t" +
				_totalSubscriptionCount + "\t" +
				_totalLocalSubscriptionCount + "\t" +
				_publishCount + "\t" +
				_deliveredCount + "\t" +
				_duplicateCount + "\t" +
				_bwInavailabilityCount + "\t" +
				_topologyMapSize + "\t" +
				_timestampMapSize + "\t" +
				_numberOfMachings + "\t" +
				_machingsTime + "\t" +
				_numberOfSubsMatched + "\t" +
				_numberOfPredicatesMatched + "\t" +
				_numberOfSubsCovered + "\t" +
				_numberOfPredicatesCovered;
	}
	
	protected void reinitVariables() {
		_publishCount = 0;
		_deliveredCount = 0;
		_duplicateCount = 0;
		_bwInavailabilityCount = 0;
		_numberOfMachings = 0;
		_machingsTime = 0;
		_numberOfPredicatesMatched = 0;
		_numberOfSubsMatched = 0;
		_numberOfPredicatesCovered = 0;
		_numberOfSubsCovered = 0;
	}

	public void addedNewBWInavailabilityEvent(int count) {
		if(count < 0)
			throw new IllegalArgumentException("Cannot accept negative value: " + count);

		synchronized (_lock) {
			_bwInavailabilityCount += count;
		}
	}
	
	public void addedNewSubscriptions(int count) {
		if(count < 0)
			throw new IllegalArgumentException("Cannot accept negative value: " + count);

		synchronized (_lock) {
			_totalSubscriptionCount += count;
		}
	}
	
	public void removedNewSubscriptions(int count) {
		if(count < 0)
			throw new IllegalArgumentException("Cannot accept negative value: " + count);
		
		synchronized (_lock) {
			if(_totalSubscriptionCount < count)
				throw new IllegalStateException();
			
			_totalSubscriptionCount -= count;
		}
	}

	public void addedNewLocalSubscriptions(int count) {
		if(count < 0)
			throw new IllegalArgumentException("Cannot accept negative value: " + count);

		synchronized (_lock) {
			_totalLocalSubscriptionCount += count;
		}
	}

	public void removedNewLocalSubscriptions(int count) {
		if(count < 0)
			throw new IllegalArgumentException("Cannot accept negative value: " + count);

		synchronized (_lock) {
			if(_totalLocalSubscriptionCount < count)
				throw new IllegalStateException();
			
			_totalLocalSubscriptionCount -= count;
		}
	}
	
	private Set<Sequence> _deliveredSourceSequences = new HashSet<Sequence>();
	
	public void newDelivery(Sequence sourceSequence) {
		if(sourceSequence == null)
			return;
		
		boolean duplicate = false;
		synchronized (_lock) {
			duplicate = !_deliveredSourceSequences.add(sourceSequence);
		}
		
		if(duplicate)
			newDuplicate(1);
		else
			newDelivery(1);
	}
	
	protected void newDuplicate(int count) {
		if(count < 0)
			throw new IllegalArgumentException("Cannot accept negative value: " + count);

		synchronized (_lock) {
			_duplicateCount += count;
		}		
	}
	
	public void newDelivery(int count) {
		if(count < 0)
			throw new IllegalArgumentException("Cannot accept negative value: " + count);

		if(!_brokerShadow.canDeliverMessages())
			return;
		
		synchronized (_lock) {
			_deliveredCount += count;
		}
	}
	
	public void newPublish(int count) {
		if(count < 0)
			throw new IllegalArgumentException("Cannot accept negative value: " + count);

		if(!_brokerShadow.canGenerateMessages())
			return;
		
		synchronized (_lock) {
			_publishCount += count;
		}
	}
	
	public void numberOfSubscriptionsPredicatesMatched(
			long matchingTimeNS, 
			int matchedSubsCount, int matchedPredsCount,
			int coveredSubsCount, int coveredPredsCount) {
		synchronized (_lock) {
			_numberOfMachings++;
			_machingsTime += matchingTimeNS;
			_numberOfSubsMatched += matchedSubsCount;
			_numberOfPredicatesMatched += matchedPredsCount;
			_numberOfSubsCovered += coveredSubsCount;
			_numberOfPredicatesCovered += coveredPredsCount;
		}
	}
	
	protected int writeHeader(int i, Writer ioWriter) throws IOException {
		if(ioWriter == null)
			return i;
		
		String headerLine = ((i == 0) ? "#" : "")
			+ "TIME" + "(" + (++i) + ")\t"
			+ "TOT_SUBS_COUNT" + "(" + (++i) + ")\t"
			+ "TOT_LOCAL_SUBS_COUNT" + "(" + (++i) + ")\t"
			+ "PUBLISH" + "(" + (++i) + ")\t"
			+ "DELIVERED" + "(" + (++i) + ")\t"
			+ "DUPLICATE" + "(" + (++i) + ")\t"
			+ "BW_INAVAILABILITY" + "(" + (++i) + ")\t"
			+ "TOP_MAP_SIZE" + "(" + (++i) + ")\t"
			+ "TS_MAP_SIZE" + "(" + (++i) + ")\t"
			+ "NUM_MATCHING" + "(" + (++i) + ")\t"
			+ "MATCHING_TIME" + "(" + (++i) + ")\t"
			+ "SUBS_MATCHED" + "(" + (++i) + ")\t"
			+ "PREDS_MATCHED" + "(" + (++i) + ")\t"
			+ "SUBS_COVRD" + "(" + (++i) + ")\t"
			+ "PREDS_COVRD" + "(" + (++i) + ")";
		
		ioWriter.write(headerLine);
		return i;
	}

	public void topologyMapSizeUpdated(int topologyMapSize) {
		_topologyMapSize = topologyMapSize;
	}
	
	public void timestampMapSizeUpdated(int timestampMapSize) {
		_timestampMapSize = timestampMapSize;
	}
	
	@Override
	public boolean isEnabled() {
		return Broker.LOG_STATISTICS && super.isEnabled();
	}

	@Override
	protected String getFileName() {
		return _statisticsLogFilename;
	}

	@Override
	public String toString() {
		return "StatisticsLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected int getDefaultLoggingInterval() {
		return SAMPLING_INTERVAL;
	}

	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_STATISTICS_LOGGER;
	}
}
