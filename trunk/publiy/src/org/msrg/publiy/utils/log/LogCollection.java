package org.msrg.publiy.utils.log;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

class LogCollection implements Serializable {
	
	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 918680662764774003L;
	private List<LogEntry> _collectedLogs = new LinkedList<LogEntry>();
	private final Sequence _fromSeq;
	private final Date _fromTime;
	private Sequence _lastSeq;
	private Date _lastTime;
	
	public LogCollection(LocalSequencer localSequencer) {
		_fromTime =new Date();
		_fromSeq = localSequencer.getNext();
	}
	
	public void appendLog(LocalSequencer localSequencer, LogLevel logLevel, LoggingSource source, String ... logStr){
		synchronized (this) {
			LogEntry logEntry = new LogEntry(localSequencer, logLevel, source, logStr);
			_collectedLogs.add(logEntry);
			_lastSeq = localSequencer.getNext();
			_lastTime = new Date();
		}
	}
	
	public int getCollectedLogSize(){
		synchronized (this) {
			return _collectedLogs.size();
		}
	}
	
	public Sequence getFromSequence(){
		return _fromSeq;
	}
	
	public Date getFromTime(){
		return _fromTime;
	}
	
	public Sequence getLastSequence(){
		return _lastSeq;
	}
	
	public Date getLastTime(){
		return _lastTime;
	}	
	
	public List<LogEntry> getCollectedLogs(){
		synchronized (this) {
			List<LogEntry> retCollection = _collectedLogs;
			_collectedLogs = new LinkedList<LogEntry>();
			return retCollection;
		}
	}

}
