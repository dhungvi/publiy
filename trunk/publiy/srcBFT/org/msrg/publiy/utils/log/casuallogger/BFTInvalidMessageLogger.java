package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;

import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimerReading;

public class BFTInvalidMessageLogger extends AbstractCasualLogger {

	public static final int SAMPLING_INTERVAL = 1000;
	
	protected long _lastWrite = 0;

	protected final List<InvalidMessage> _invalidMessages =
			new LinkedList<InvalidMessage>();
	
	protected final IBFTBrokerShadow _bftBrokerShadow;
	protected final BrokerIdentityManager _idMan;
	protected final String _invalidMessageLogFilename;

	public BFTInvalidMessageLogger(BFTBrokerShadow bftBrokerShadow) {
		_bftBrokerShadow = bftBrokerShadow;
		bftBrokerShadow.setBFTInvalidMessageLogger(this);
		_invalidMessageLogFilename =
				_bftBrokerShadow.getBFTInvalidMessageLogFilename();
		_idMan = _bftBrokerShadow.getBrokerIdentityManager();
	}
	
	@Override
	public String toString() {
		return "BFTSuspicionLogger-" + _bftBrokerShadow.getBrokerID() +
				"[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected String getFileName() {
		return _invalidMessageLogFilename;
	}

	@Override
	protected int getDefaultLoggingInterval() {
		return SAMPLING_INTERVAL;
	}
	
	@Override
	protected void runMe() throws IOException {
		synchronized (_lock) {
			if(_firstTime) {
				String headerLine = "#TIME (suspecion)*\n";
				_logFileWriter.write(headerLine);
				_firstTime = false;
			}
			
			
			long currTime = SystemTime.currentTimeMillis();
			StringBuilder sb = new StringBuilder();
			
			for(InvalidMessage invalidMessage : _invalidMessages) {
				sb.append(invalidMessage.toString(_idMan));
				sb.append('\n');
			}
			
			_invalidMessages.clear();
			
			_logFileWriter.write(sb.toString());
			_lastWrite = currTime;
		}
	}
	
	public void invalidMessage(TMulticast_Publish_BFT tm) {
		synchronized(_lock) {
			_invalidMessages.add(new InvalidMessage(tm));
		}
	}
}

class InvalidMessage {
	protected final BrokerInternalTimerReading _time;
	protected final TMulticast_Publish_BFT _tmBFT;
	
	InvalidMessage(TMulticast_Publish_BFT tmBFT) {
		_time = BrokerInternalTimer.read();
		_tmBFT = tmBFT;
	}
	
	public String toString(BrokerIdentityManager idMan) {
		return _time + " " + _tmBFT;
	}
}
