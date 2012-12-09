package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.msrg.publiy.pubsub.multipath.loadweights.LoadProfiler;
import org.msrg.publiy.pubsub.multipath.loadweights.ProfilerBucket;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.utils.ElementTotal;
import org.msrg.publiy.utils.HistoryType;
import org.msrg.publiy.utils.IHistoryRepository;
import org.msrg.publiy.utils.LimittedSizeList;

public class MessageProfilerLogger extends AbstractCasualLogger implements IHistoryRepository {
	public static final int SAMPLING_INTERVAL = 1000;
	public static final int PROFILER_CACHE_SIZE = 5;
	
	private final LoadProfiler _inLoadProfiler;
	private final LoadProfiler _outLoadProfiler;
	private final IBrokerShadow _brokerShadow;
	private final String _pubProfilerLogFilename;
	
	public MessageProfilerLogger(BrokerShadow brokerShadow){
		brokerShadow.setMessageProfilerLogger(this);
		_brokerShadow = brokerShadow;
		_pubProfilerLogFilename = _brokerShadow.getMessageProfilerLogFilename();
		_inLoadProfiler = new LoadProfiler(PROFILER_CACHE_SIZE, SAMPLING_INTERVAL);
		_outLoadProfiler = new LoadProfiler(PROFILER_CACHE_SIZE, SAMPLING_INTERVAL);
	}
	
	@Override
	protected String getFileName() {
		return _pubProfilerLogFilename;
	}
	
	public double getInputPublicationRate() {
		ElementTotal<LimittedSizeList<ProfilerBucket>> elementsTotal = getTotalInputPublication();
		return (elementsTotal == null ? 0 : elementsTotal.getAverage());
	}
	
	public ElementTotal<LimittedSizeList<ProfilerBucket>> getTotalInputPublication() {
		return getTotalInputPublication(null);
	}
	
	public ElementTotal<LimittedSizeList<ProfilerBucket>> getTotalInputPublication(InetSocketAddress remote) {
		return _inLoadProfiler.getHistoryOfType(HistoryType.HIST_T_PUB_IN_BUTLAST, remote);
	}
	
	public double getOutputPublicationRate() {
		ElementTotal<LimittedSizeList<ProfilerBucket>> elementsTotal = getTotalOutputPublication();
		return (elementsTotal == null ? 0 : elementsTotal.getAverage());
	}
	
	
	public ElementTotal<LimittedSizeList<ProfilerBucket>> getTotalOutputPublication() {
		return getTotalOutputPublication(null);
	}

	public ElementTotal<LimittedSizeList<ProfilerBucket>> getTotalOutputPublication(InetSocketAddress remote) {
		return _outLoadProfiler.getHistoryOfType(HistoryType.HIST_T_PUB_OUT_BUTLAST, remote);
	}

	@Override
	protected void runMe() throws IOException {
		String time = BrokerInternalTimer.read().toString();
		
		_logFileWriter.write("IN  " + time + " ");
		_inLoadProfiler.writeBucketLists(_logFileWriter, HistoryType.HIST_T_PUB_IN_BUTLAST);
		_logFileWriter.write("\n");

		_logFileWriter.write("OUT " + time + " ");
		_outLoadProfiler.writeBucketLists(_logFileWriter, HistoryType.HIST_T_PUB_OUT_BUTLAST);
		_logFileWriter.write("\n");
	}
	
	@Override
	public ElementTotal<LimittedSizeList<ProfilerBucket>> getHistoryOfType(HistoryType hType, InetSocketAddress remote){
		switch (hType) {
		case HIST_T_PUB_IN_BUTLAST:
		case HIST_T_PUB_IN_WITHLAST:
		case HIST_T_SUB_IN_BUTLAST:
		case HIST_T_SUB_IN_WITHLAST:
			return _inLoadProfiler.getHistoryOfType(hType, remote);

		case HIST_T_PUB_OUT_BUTLAST:
		case HIST_T_PUB_OUT_WITHLAST:
		case HIST_T_SUB_OUT_BUTLAST:
		case HIST_T_SUB_OUT_WITHLAST:
			return _outLoadProfiler.getHistoryOfType(hType, remote);
			
		default:
			throw new UnsupportedOperationException("Unsupported history type: " + hType);
		}
	}
	
	@Override
	public String toString() {
		return "ProfLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected int getDefaultLoggingInterval(){
		return SAMPLING_INTERVAL;
	}

	@Override
	public void logMessage(HistoryType hType, InetSocketAddress remote, int bytes) {
		switch (hType) {
		case HIST_T_PUB_IN_BUTLAST:
		case HIST_T_PUB_IN_WITHLAST:
			_inLoadProfiler.logMessage(hType, remote, bytes);
			break;

		case HIST_T_PUB_OUT_BUTLAST:
		case HIST_T_PUB_OUT_WITHLAST:
			_outLoadProfiler.logMessage(hType, remote, bytes);
			break;

		case HIST_T_SUB_IN_BUTLAST:
		case HIST_T_SUB_IN_WITHLAST:
		case HIST_T_SUB_OUT_BUTLAST:
		case HIST_T_SUB_OUT_WITHLAST:
//			throw new UnsupportedOperationException("Unknown type: " + hType);
			
		default:
			break;
		}		
	}
}
