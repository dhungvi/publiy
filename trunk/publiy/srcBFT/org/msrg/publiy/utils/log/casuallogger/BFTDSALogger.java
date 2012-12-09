package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;

import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBFTBrokerShadow;

import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class BFTDSALogger extends AbstractCasualLogger {
	
	public static final int SAMPLING_INTERVAL = 1000;
	
	protected long _lastWrite = 0;
	protected int _sign = 0;
	protected int _verifySuccess = 0;
	protected int _verifyFailed = 0;
	protected int _verifyAvoided = 0;
	
	protected final IBFTBrokerShadow _bftBrokerShadow;
	protected final String _logFilename;
	
	public BFTDSALogger(BFTBrokerShadow bftBrokerShadow) {
		_bftBrokerShadow = bftBrokerShadow;
		bftBrokerShadow.setBFTDSALogger(this);
		_logFilename = _bftBrokerShadow.getBFTDSALogFilename();
	}
	
	@Override
	public String toString() {
		return "DSALogger-" + _bftBrokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected String getFileName() {
		return _logFilename;
	}

	@Override
	protected int getDefaultLoggingInterval() {
		return SAMPLING_INTERVAL;
	}
	
	@Override
	protected void runMe() throws IOException {
		synchronized (_lock) {
			if(_firstTime) {
				String headerLine = "#TIME SIGN VERIFY-SUCCESS VERIFY-FAILVERIFY-AVOIDED\n";
				_logFileWriter.write(headerLine);
				_firstTime = false;
			}
			
			long currTime = SystemTime.currentTimeMillis();
			StringBuilder sb = new StringBuilder();
			sb.append(BrokerInternalTimer.read().toString());
			
			sb.append(' ');
			sb.append(_sign);
			sb.append(' ');
			sb.append(_verifySuccess);
			sb.append(' ');
			sb.append(_verifyFailed);
			sb.append(' ');
			sb.append(_verifyAvoided);
			sb.append('\n');
			
			_sign = _verifyFailed = _verifySuccess = _verifyAvoided = 0;
			
			_logFileWriter.write(sb.toString());
			
			_lastWrite = currTime;
		}
	}
	
	public void dsaSigned() {
		synchronized(_lock) {
			_sign++;
		}
	}
	
	public void dsaVerify(boolean succeeded) {
		synchronized(_lock) {
			if(succeeded)
				_verifySuccess++;
			else
				_verifyFailed++;
		}
	}
	
	@Override
	public boolean isEnabled() {
		return Broker.LOG_TRAFFIC && super.isEnabled();
	}

	public void dsaVerifyAvoided() {
		synchronized(_lock) {
			_verifyAvoided++;
		}		
	}
}
