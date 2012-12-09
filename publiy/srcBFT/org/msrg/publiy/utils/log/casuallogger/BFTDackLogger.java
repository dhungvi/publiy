package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBFTBrokerShadow;

import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.core.sequence.SequencePair;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class BFTDackLogger extends AbstractCasualLogger {
	
	public static final int SAMPLING_INTERVAL = 1000;
	
	protected long _lastWrite = 0;

	protected final IBFTBrokerShadow _bftBrokerShadow;
	protected final String _bftDackFilename;
	protected final InetSocketAddress _localAddress;
	protected final Map<InetSocketAddress, SequencePair> _registeredDackSequencePairs =
			new HashMap<InetSocketAddress, SequencePair>();
	protected final Map<InetSocketAddress, Long> _registeredDackSequencePairsTimes =
			new HashMap<InetSocketAddress, Long>();
	protected final BrokerIdentityManager _idMan;
			
	public BFTDackLogger(BFTBrokerShadow bftBrokerShadow) {
		bftBrokerShadow.setBFTDackLogger(this);
		_bftBrokerShadow = bftBrokerShadow;
		_bftDackFilename = _bftBrokerShadow.getBFTDackLogFilename();
		_localAddress = _bftBrokerShadow.getLocalAddress();
		_idMan = _bftBrokerShadow.getBrokerIdentityManager();
	}

	@Override
	public String toString() {
		return "BFTDackLogger-" + _bftBrokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected String getFileName() {
		return _bftDackFilename;
	}

	@Override
	protected int getDefaultLoggingInterval() {
		return SAMPLING_INTERVAL;
	}
	
	@Override
	protected void runMe() throws IOException {
		synchronized (_lock) {
			if(_firstTime) {
				String headerLine = "#TIME (sp[time-since-last-dack-receipt])*\n";
				_logFileWriter.write(headerLine);
				_firstTime = false;
			}
			
			
			long currTime = SystemTime.currentTimeMillis();
			StringBuilder sb = new StringBuilder();
			sb.append(BrokerInternalTimer.read().toString());
			
			for(Entry<InetSocketAddress, SequencePair> dackedSequencePairEntry : _registeredDackSequencePairs.entrySet()) {
				InetSocketAddress dackIssuer = dackedSequencePairEntry.getKey();
				long receiptTime = _registeredDackSequencePairsTimes.get(dackIssuer);
				SequencePair sp = dackedSequencePairEntry.getValue();
				sb.append(' ');
				sb.append(sp.toString(_idMan));
				sb.append("[" + ((currTime - receiptTime) / 1000) + "]");
			}
			
			sb.append('\n');
			_logFileWriter.write(sb.toString());
			
			_lastWrite = currTime;
		}
	}
	
	public void dackSequencePairRegistered(SequencePair sp) {
		if(!sp.getVerifierAddress().equals(_localAddress))
			throw new IllegalArgumentException(sp + " vs. " + _localAddress);
		
		InetSocketAddress dackIssuer = sp.getIssuerAddress();
		synchronized(_lock) {
			long currTime = SystemTime.currentTimeMillis();
			_registeredDackSequencePairsTimes.put(dackIssuer, currTime);
			SequencePair prevDackSequencePair = _registeredDackSequencePairs.put(dackIssuer, sp);
			if(prevDackSequencePair == null)
				return;
			
			if(prevDackSequencePair._lastDiscardedOrder > sp._lastDiscardedOrder)
				throw new IllegalStateException(prevDackSequencePair + " vs. " + sp);
			
			if(prevDackSequencePair._lastReceivedOrder > sp._lastReceivedOrder)
				throw new IllegalStateException(prevDackSequencePair + " vs. " + sp);
		}
	}
	
	@Override
	public boolean isEnabled() {
		return Broker.LOG_TRAFFIC && super.isEnabled();
	}
}
