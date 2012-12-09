package org.msrg.publiy.broker.info;

import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

//extends LogEntry 
public class ExceptionInfo extends IBrokerInfo {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 1148307310425538673L;
	private final Exception _ex;
	private final String _arguments;
	private final LoggingSource _source;
	private final Sequence _localSequence;
	
	public ExceptionInfo(LocalSequencer localSequencer, LoggingSource source, Exception x, String arguments) {
		super(BrokerInfoTypes.BROKER_INFO_EXCEPTIONS);
		_ex = x;
		_arguments = arguments;
		_source = source;
		_localSequence = localSequencer.getNext();
	}
	
	public String getArguments() {
		return _arguments;
	}
	
	public Exception getException() {
		return _ex;
	}
	
	public Sequence getLocalExceptionSequence() {
		return _localSequence;
	}
	
	@Override
	public String toStringPrivately() {
		return "S[" + _source + "]" + getLocalExceptionSequence() 
				+ "-EX:[" + getException() + "]::" + getArguments();
	}
}
