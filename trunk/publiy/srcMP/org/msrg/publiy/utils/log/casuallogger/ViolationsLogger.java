package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;

import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class ViolationsLogger extends AbstractCasualLogger {

	@SuppressWarnings("unused")
	private long _lastWrite = SystemTime.currentTimeMillis();
	public static final int SAMPLING_INTERVAL = 1000;
	public static final int FORCE_FILE_FLUSH = 5;
	private final int REQUIRED_SEQ_VECTOR_LENGTH_FOR_CHECKING_LEGITIMACY;
	
	protected final Set<InetSocketAddress> _downstreamNodesOfIllegitimateMessageReceivers;
	
	private int _deltaViolatingPubs = 0;
	private int _legitimacyViolatingPubs = 0;
	private int _legitimacyViolatedPubs = 0;
	
	private final IBrokerShadow _brokerShadow;
	private final String _violationsLogFilename;
	
	public ViolationsLogger(BrokerShadow brokerShadow) {
		brokerShadow.setViolationsLogger(this);
		_brokerShadow = brokerShadow;
		REQUIRED_SEQ_VECTOR_LENGTH_FOR_CHECKING_LEGITIMACY = 3 * _brokerShadow.getDelta() + 1;
		_violationsLogFilename = _brokerShadow.getViolationsLogFilename();
		_downstreamNodesOfIllegitimateMessageReceivers = new HashSet<InetSocketAddress>();
	}
	
	@Override
	protected String getFileName() {
		return _violationsLogFilename;
	}

	protected void writeHeader(Writer ioWriter) throws IOException {
		if (ioWriter == null)
			return;
		
		String headerLine =
			"TIME" +
			"\tDT_VIO_P" +
			"\tLG_VING_P" +
			"\tLG_VIOED" +
			"\n";
		ioWriter.write(headerLine);
	}

	@Override
	protected void runMe() throws IOException {
		synchronized (_lock) {
			if ( _firstTime ){
				writeHeader(_logFileWriter);
				_firstTime = false;
			}
		
			long currTime = SystemTime.currentTimeMillis();
			String logLine =
				BrokerInternalTimer.read().toString() +
				"\t" + _deltaViolatingPubs +
				"\t" + _legitimacyViolatingPubs +
				"\t" + _legitimacyViolatedPubs +
				"\n";
			
			_deltaViolatingPubs = 0;
			_legitimacyViolatingPubs = 0;
			_legitimacyViolatedPubs = 0;
			
			_logFileWriter.write(logLine);
			_lastWrite = currTime;
		}
	}
	
	public void legitimacyViolatingPublication(IOverlayManager overlayManager, TMulticast tm, InetSocketAddress remote) {
		if(tm.getLegitimacyViolating() || tm.getLegitimacyViolated())
			return;
		
		int pathLength = tm.getPathLength();
		Sequence[] seqV = tm.getSequenceVector();
		if(seqV.length != REQUIRED_SEQ_VECTOR_LENGTH_FOR_CHECKING_LEGITIMACY)
			throw new IllegalStateException("SeqVector's length is less than " + REQUIRED_SEQ_VECTOR_LENGTH_FOR_CHECKING_LEGITIMACY + " (" + seqV.length + ")");
		
		int nullCount = 0;
		for(int i=0 ; i<seqV.length && i<pathLength ; i++)
			if(seqV[i] == null)
				nullCount++;
		
		if(nullCount <= Broker.DELTA)
			return;
		
		Set<InetSocketAddress> downstreamNodesOfIllegitimateMessageReceivers =
			overlayManager.getFartherNeighbors(remote);
		tm.setLegitimacyViolating(true);

		synchronized(_lock){
			int initialSize = _downstreamNodesOfIllegitimateMessageReceivers.size();
			for(InetSocketAddress downstreamNode : downstreamNodesOfIllegitimateMessageReceivers)
				_downstreamNodesOfIllegitimateMessageReceivers.add(downstreamNode);
			
			int endingSize = _downstreamNodesOfIllegitimateMessageReceivers.size();
			
			if(initialSize != endingSize)
				System.out.println("@" + BrokerInternalTimer.read() + ": " + 
						"LEGIT_VIOLATING_DOWNSTREAMS: " + _downstreamNodesOfIllegitimateMessageReceivers + " " + 
						Writers.write(seqV));

			_legitimacyViolatingPubs++;
		}
	}
	
	public void legitimacyViolatedPublication(IOverlayManager overlayManager, TMulticast tm, InetSocketAddress remote) {
		if(tm.getLegitimacyViolating() || tm.getLegitimacyViolated())
			return;
		
		if(!_downstreamNodesOfIllegitimateMessageReceivers.contains(remote))
			return;

		tm.setLegitimacyViolated(true);
		synchronized(_lock){
			_legitimacyViolatedPubs++;
		}
	}

	public void deltaViolatingPublication() {
		synchronized(_lock){
			_deltaViolatingPubs++;
		}
	}

	@Override
	public boolean isEnabled() {
		return Broker.LOG_VIOLATIONS;
	}
	
	@Override
	public String toString() {
		return "VIOLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected int getDefaultLoggingInterval(){
		return SAMPLING_INTERVAL;
	}
}
