package org.msrg.publiy.networkcodes.engine;

import java.io.IOException;
import java.io.StringWriter;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.utils.log.casuallogger.AbstractCasualLogger;

public class CodingEngineLogger extends AbstractCasualLogger {

	public static final int SAMPLING_INTERVAL = 1000;
	
	protected final IBrokerShadow _brokerShadow;
	protected final String _codingEngineLogFilename;

	protected int _decodeStarted = 0, _decodeFinishedSuccess = 0, _decodeFinishedFailure = 0;
	protected int _codingStarted = 0, _codingFinished = 0;
	protected int _codingPieceDiscarded = 0;
	protected int _inverseStarted = 0, _inverseFinishedSuccess = 0, _inverseFinishedFailure = 0;
	
	public CodingEngineLogger(BrokerShadow brokerShadow){
		brokerShadow.setCodingEngineLogger(this);
		_brokerShadow = brokerShadow;
		_codingEngineLogFilename = _brokerShadow.getCodingEngineLogFilename();
		if(_codingEngineLogFilename == null)
			throw new NullPointerException();
	}
	
	@Override
	public void initializeFile() {
		super.initializeFile();
		BrokerInternalTimer.inform("initializeFile: " + this);
	}
	
	@Override
	protected String getFileName() {
		return _codingEngineLogFilename;
	}
	
	@Override
	protected void runMe() throws IOException {
		synchronized (_lock) {
			if(_firstTime){
				int i=0;
				String headerLine = "#TIME(" + (++i) + ")\t" +
						"CODE_BEG(" + (++i) + ")\tCODE_END(" + (++i) + ")\t" +
						"DCOD_BEG(" + (++i) + ")\tDCOD_SUCC(" + (++i) + ")\tDCOD_FAIL(" + (++i) + ")\t" +
						"PIECES_DISCARDED(" + (++i) + ")\t" +
						"INV_BEG(" + (++i) + ")\tINV_SUCC(" + (++i) + ")\tINV_FAIL(" + (++i) + ")\n";
				_logFileWriter.write(headerLine);
				_firstTime = false;
			}
			
			StringWriter stringWriter = new StringWriter();
			stringWriter.append(printSkippedTimes());

			stringWriter.append(
					BrokerInternalTimer.read().toString() + "\t" +
					_codingStarted + "\t" + _codingFinished + "\t" +
					_decodeStarted + "\t" + _decodeFinishedSuccess + "\t" + _decodeFinishedFailure + "\t" +
					_codingPieceDiscarded + "\t" +
					_inverseStarted + "\t" + _inverseFinishedSuccess + "\t" + _inverseFinishedFailure);
			
			_logFileWriter.write(stringWriter.toString());
			_logFileWriter.write('\n');
			
			_codingStarted = 0;
			_codingFinished = 0;
			_decodeStarted = 0;
			_decodeFinishedSuccess = 0;
			_decodeFinishedFailure = 0;
			_codingPieceDiscarded = 0;
			_inverseStarted = 0;
			_inverseFinishedSuccess = 0;
			_inverseFinishedFailure = 0;
		}
	}

	public final void codingPieceDiscarded() {
		synchronized(_lock) {
			_codingPieceDiscarded++;
		}
	}
	
	public final void codingStarted() {
		synchronized(_lock) {
			_codingStarted++;
		}
	}
	
	public final void decodingStarted() {
		synchronized(_lock) {
			_decodeStarted++;
		}
	}
	
	public final void codingFinished() {
		synchronized(_lock) {
			_codingFinished++;
		}
	}

	public final void inverseStarted() {
		synchronized(_lock) {
			_inverseStarted++;
		}
	}

	public final void inverseFinished(boolean success) {
		synchronized(_lock) {
			if(success)
				_inverseFinishedSuccess++;
			else
				_inverseFinishedFailure++;
		}
	}
	
	public final void decodingFinished(boolean success) {
		synchronized(_lock) {
			if(success)
				_decodeFinishedSuccess++;
			else
				_decodeFinishedFailure++;
		}
	}
	
	@Override
	public String toString() {
		return "CodingEngineLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	public boolean isEnabled() {
		boolean ret = _brokerShadow.isNC() && super.isEnabled();
		if(!ret)
			throw new IllegalStateException(_brokerShadow.isNC() + " vs. " + super.isEnabled());
		
		return ret;
	}
	
	@Override
	protected int getDefaultLoggingInterval(){
		return SAMPLING_INTERVAL;
	}
}
