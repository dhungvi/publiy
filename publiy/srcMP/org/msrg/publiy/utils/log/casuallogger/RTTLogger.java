package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.utils.ElementTotal;
import org.msrg.publiy.utils.LimittedSizeList;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.communication.core.packet.types.TPingReply;

public class RTTLogger extends AbstractCasualLogger {
	
	public static final int SAMPLING_INTERVAL = 1000;
	public static final int FORCE_FILE_FLUSH = 5;
	public static final int RTT_CACHE_SIZE = 5;
	
	private IBrokerShadow _brokerShadow;
	private Map<InetSocketAddress, LimittedSizeList<TPingReply>> _recentReplies = new HashMap<InetSocketAddress, LimittedSizeList<TPingReply>>();
	private String _rttLogFilename;
	
	public RTTLogger(BrokerShadow brokerShadow) {
		brokerShadow.setRTTLogger(this);
		_brokerShadow = brokerShadow;
		_rttLogFilename = _brokerShadow.getRTTLogFilename();
	}
	
	@Override
	protected String getFileName() {
		return _rttLogFilename;
	}

	protected DecimalFormat _decf2 = new DecimalFormat ("0.00");
	protected DecimalFormat _decf3 = new DecimalFormat ("0.000");
	
	@Override
	protected void runMe() throws IOException {
		synchronized(_recentReplies){
			if (!_initialized)
				return;
			
			String logLine = BrokerInternalTimer.read().toString() + "{";
			
			Iterator<Map.Entry<InetSocketAddress, LimittedSizeList<TPingReply>>> it = _recentReplies.entrySet().iterator();
			while ( it.hasNext() ){
				Map.Entry<InetSocketAddress, LimittedSizeList<TPingReply>> entry = it.next();
				InetSocketAddress remote = entry.getKey();
				LimittedSizeList<TPingReply> tpingReplies = entry.getValue();

				double avgRTT = getAverageRTT(tpingReplies);
				double avgCPU = getAverageCPU(tpingReplies);
				double avgInPubRate = getAverageInputPublicationRate(tpingReplies);
				double avgOutPubRate = getAverageOutputPublicationRate(tpingReplies);
				
				logLine += Writers.write(remote) +
						"=[" +
						_decf2.format(avgRTT) +
						":" +
						_decf2.format(avgCPU) +
						":" +
						_decf2.format(avgInPubRate) +
						":" +
						_decf2.format(avgOutPubRate) +
						(it.hasNext()? "] ":"]}\n");
			}
			
			_logFileWriter.write(logLine);
		}
	}
	
	public void updatefromTPingReply(InetSocketAddress remote, TPingReply pingReply){
		LimittedSizeList<TPingReply> pingReplies;
		synchronized(_recentReplies){
			pingReplies = _recentReplies.get(remote);
			if ( pingReplies == null ) {
				pingReplies = new LimittedSizeList<TPingReply>(new TPingReply[RTT_CACHE_SIZE]);
				_recentReplies.put(remote, pingReplies);
			}
		}

		pingReplies.append(pingReply);
	}
	
	public double getAverageRTT(InetSocketAddress remote){
		LimittedSizeList<TPingReply> pingReplies;
		synchronized(_recentReplies){
			pingReplies = _recentReplies.get(remote);
			if ( pingReplies == null )
				return -1;
		}
		
		return getAverageRTT(pingReplies);
	}
	
	private double getAverageRTT(LimittedSizeList<TPingReply> pingReplies){
		return getAverageI(pingReplies, 0);
	}
	
	public double getAverageCPU(InetSocketAddress remote){
		LimittedSizeList<TPingReply> pingReplies;
		synchronized(_recentReplies){
			pingReplies = _recentReplies.get(remote);
			if ( pingReplies == null )
				return 0;
		}
		
		return getAverageCPU(pingReplies);
	}
	
	private double getAverageCPU(LimittedSizeList<TPingReply> pingReplies){
		return getAverageI(pingReplies, 1);
	}

	public double getAverageInputPublicationRate(InetSocketAddress remote){
		LimittedSizeList<TPingReply> pingReplies;
		synchronized(_recentReplies){
			pingReplies = _recentReplies.get(remote);
			if ( pingReplies == null )
				return 0;
		}
		
		return getAverageInputPublicationRate(pingReplies);
	}
	
	private double getAverageInputPublicationRate(LimittedSizeList<TPingReply> pingReplies){
		return getAverageI(pingReplies, 2);
	}
	
	public double getAverageOutputPublicationRate(InetSocketAddress remote){
		LimittedSizeList<TPingReply> pingReplies;
		synchronized(_recentReplies){
			pingReplies = _recentReplies.get(remote);
			if ( pingReplies == null )
				return 0;
		}
		
		return getAverageOutputPublicationRate(pingReplies);
	}
	
	private double getAverageOutputPublicationRate(LimittedSizeList<TPingReply> pingReplies){
		return getAverageI(pingReplies, 3);
	}

	private double getAverageI(LimittedSizeList<TPingReply> pingReplies, int i){
		ElementTotal<LimittedSizeList<TPingReply>> avg = pingReplies.getElementsTotal(i);
		if ( avg == null )
			return -1;
		
		if ( avg._latestTime + Broker.TPING_TIMEOUT_INTERVAL < SystemTime.currentTimeMillis() )
			return 0; // Broker.TPING_TIMEOUT_INTERVAL;
		else 
			return avg.getAverage(); //_total;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
	
	@Override
	public String toString() {
		return "RTTLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected int getDefaultLoggingInterval(){
		return SAMPLING_INTERVAL;
	}
}