package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBFTBrokerShadow;

public class BFTSuspicionLogger extends AbstractCasualLogger {

	public static final int SAMPLING_INTERVAL = 1000;
	
	protected long _lastWrite = 0;

	protected final List<BFTSuspecionReason> reasons = new LinkedList<BFTSuspecionReason>();
	
	protected final IBFTBrokerShadow _bftBrokerShadow;
	protected final BFTSuspicionLogger _bftSuspicionLogger;
	protected final BrokerIdentityManager _idMan;
	protected final String _suspicionLogFilename;

	public BFTSuspicionLogger(BFTBrokerShadow bftBrokerShadow) {
		_bftBrokerShadow = bftBrokerShadow;
		bftBrokerShadow.setBFTSuspicionLogger(this);
		_suspicionLogFilename = _bftBrokerShadow.getBFTSuspicionLogFilename();
		_idMan = _bftBrokerShadow.getBrokerIdentityManager();
		_bftSuspicionLogger = _bftBrokerShadow.getBFTSuspecionLogger();
	}
	
	@Override
	public String toString() {
		return "BFTSuspicionLogger-" + _bftBrokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected String getFileName() {
		return _suspicionLogFilename;
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
			
			for(BFTSuspecionReason reason : reasons) {
				sb.append(reason.toString(_idMan));
				sb.append('\n');
			}
			
			reasons.clear();
			
			_logFileWriter.write(sb.toString());
			_lastWrite = currTime;
		}
	}
	
	public void newNodeSuspected(BFTSuspecionReason reason) {
		synchronized(_lock) {
			reasons.add(reason);
		}
	}
}
