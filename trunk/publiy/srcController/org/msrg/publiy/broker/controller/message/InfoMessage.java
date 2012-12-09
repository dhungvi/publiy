package org.msrg.publiy.broker.controller.message;

import java.net.InetSocketAddress;

import org.msrg.publiy.communication.socketbinding.Message;
import org.msrg.publiy.communication.socketbinding.MessageTypes;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.BrokerInfoTypes;
import org.msrg.publiy.broker.info.IBrokerInfo;

public class InfoMessage extends Message {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 1L;
	private Sequence _replyToSequence;
	private BrokerInfoTypes _infoType;
	private String _arguments;
	private IBrokerInfo[] _brokerInfos;
	
	public InfoMessage(BrokerInfoTypes infoType, String arguments, Sequence replyToSequence, InetSocketAddress to) {
		super(MessageTypes.MESSAGE_TYPE_INFO, to);
		_infoType = infoType;
		_arguments = arguments;
		_replyToSequence = replyToSequence;
	}
	
	public void loadInfos(IBrokerInfo[] brokerInfos){
		_brokerInfos = brokerInfos;
	}
	
	public void loadInfo(IBrokerInfo brokerInfo){
		IBrokerInfo[] brokerInfos = new IBrokerInfo[1];
		brokerInfos[0] = brokerInfo;
		
		loadInfos(brokerInfos);
	}

	
	public IBrokerInfo[] getBrokerInfos(){
		return _brokerInfos;
	}
	
	public void loadArguments(String arguments){
		_arguments = arguments;
	}

	public BrokerInfoTypes getInfoType(){
		return _infoType;
	}
	
	public String getArguments(){
		return _arguments;
	}
	
	public Sequence getReplyToSequence(){
		return _replyToSequence;
	}

	@Override
	public String toString() {
		return "" + _infoType + "[" + getFrom() + " -> " + getTo() + "] replyTo(" + _replyToSequence + "):: " + _brokerInfos + "\t" + _arguments;
	}
}
