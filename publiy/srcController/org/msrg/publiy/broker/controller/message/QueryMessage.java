package org.msrg.publiy.broker.controller.message;

import java.net.InetSocketAddress;

import org.msrg.publiy.communication.socketbinding.Message;
import org.msrg.publiy.communication.socketbinding.MessageTypes;

public class QueryMessage extends Message {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 1L;
	private QueryMessageTypes _queryType;
	private String _arguments;
	
	public QueryMessage(InetSocketAddress to, QueryMessageTypes queryType, String arguments){
		super(MessageTypes.MESSAGE_TYPE_QUERY, to);
		_queryType = queryType;
		_arguments = arguments;
	}
	
	public void loadArguments(String arguments){
		_arguments = arguments;
	}

	public QueryMessageTypes getQueryType(){
		return _queryType;
	}
	
	public String getArguments(){
		return _arguments;
	}
	
	@Override
	public String toString() {
		return "" + _queryType + "[" + getFrom() + " -> " + getTo() + "]:: " + _arguments;
	}

}
