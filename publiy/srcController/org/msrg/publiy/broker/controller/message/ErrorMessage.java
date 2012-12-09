package org.msrg.publiy.broker.controller.message;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.BrokerInfoTypes;

public class ErrorMessage extends InfoMessage {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 1L;
	private ErrorMessageTypes _errorType;
	private Exception _exception;
	
	public ErrorMessage(InetSocketAddress to, ErrorMessageTypes errorType, Sequence requestSequenceId, String arguments){
		super(BrokerInfoTypes.BROKER_INFO_ERROR, arguments, requestSequenceId, to);
		_errorType = errorType;
	}
	
	public ErrorMessageTypes getErrorType(){
		return _errorType;
	}
	
	public void loadException(Exception exception){
		_exception = exception;
		_exception.printStackTrace();
	}
	
	public Exception getException(){
		return _exception;
	}
	
	@Override
	public String toString() {
		return "" + _errorType + "[" + getFrom() + " -> " + getTo() + "]:: EX[" + _exception + "]:: "+ getArguments();
	}

}
