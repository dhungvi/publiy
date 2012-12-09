package org.msrg.publiy.communication.socketbinding;

import java.io.Serializable;
import java.net.InetSocketAddress;

import org.msrg.publiy.broker.controller.sequence.IMessageSequenceUpdateListener;
import org.msrg.publiy.broker.controller.sequence.LocalControllerSequencer;
import org.msrg.publiy.broker.controller.sequence.SequenceUpdateStatus;
import org.msrg.publiy.broker.controller.sequence.SequenceUpdateable;
import org.msrg.publiy.broker.core.sequence.Sequence;

public abstract class Message implements Serializable{

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = -4957303599146356060L;
	private static transient int MAX_RETRY = 5;
	
//	transient private static LocalControllerSequencer _localControllerSequencer = ;
	protected final InetSocketAddress _to;
	protected InetSocketAddress _redirectedreplyAddress = null;
	public final SequenceUpdateable<IMessageSequenceUpdateListener> _sourceInstanceID;
	protected final Sequence _sequence; 
	protected final MessageTypes _messageType;
	
	private transient int _retries = MAX_RETRY;
	
	public void minimizeRetries(){
		_retries = 2;
	}
	
	public Message(MessageTypes messageType, InetSocketAddress to){
		this(messageType, to, LocalControllerSequencer.getLocalControllerSequencer().getNextMessageSequence());
	}
	
	protected Message(MessageTypes messageType, InetSocketAddress to, SequenceUpdateable<IMessageSequenceUpdateListener> sourceInstanceId){
		if ( to == null )
			throw new IllegalArgumentException("To must not be null: " + to);
		_sourceInstanceID = sourceInstanceId;//LocalControllerSequencer.getLocalControllerSequencer().getNextMessageSequence();
		_to = to;
		_sequence = _sourceInstanceID;
		_messageType = messageType;
	}
	
	public MessageTypes getMessageType(){
		return _messageType;
	}
	
	public SequenceUpdateable<IMessageSequenceUpdateListener> getSourceInstanceID(){
		return _sourceInstanceID;
	}
	
	public InetSocketAddress getTo(){
		return _to;
	}
	
	public InetSocketAddress getFrom(){
		return _sequence.getAddress();
	}
	
	public int hashCode(){
		return _sourceInstanceID.hashCode();
	}
	
	public boolean needsConfirmation(){
		return true;
	}
	
	public boolean equals(Object obj){
		if ( obj == null )
			return false;
		
		if ( Message.class.isInstance(obj) 
//				|| AckMessage.class.isInstance(obj) 
		){
			
			Message msg = (Message) obj;
			return msg._sourceInstanceID.equals(_sourceInstanceID);
		}
		
		return false;
	}
	
	public String toStringSequence(){
		return getClass().getName()+"["+_sourceInstanceID+"]-["+_sequence+"]";
	}
	public abstract String toString();
	
	public boolean retrying(){
		if ( _retries == 0 )
			return false;
		_retries--;
		return true;
	}
	
	private boolean isvalidSequenceStatus(SequenceUpdateStatus newStatus){
		switch(newStatus){
		
		case SEQ_UPDATE_STATUS_NOT_UPDATED:
		case SEQ_UPDATE_STATUS_INVALID: 
		case SEQ_UPDATE_STATUS_CONNECTING:
		case SEQ_UPDATE_STATUS_CONNECTED:
		case SEQ_UPDATE_STATUS_DISCONNECTED:
		case SEQ_UPDATE_STATUS_MESSAGE_BEING_SENT:
		case SEQ_UPDATE_STATUS_MESSAGE_SENT:
		case SEQ_UPDATE_STATUS_MESSAGE_SEND_ERROR:
			return true;

		default: 
			return false;
		}
	}
	
	public void updateSequence(SequenceUpdateStatus newStatus){
		if ( isvalidSequenceStatus(newStatus) )
			_sourceInstanceID.updateSequence(newStatus);
		else
			throw new IllegalArgumentException("A message does not accept this new status: " + newStatus);
	}
	
	public void setRedirectedReplyAddress(InetSocketAddress redirectedreplyAddress){
		_redirectedreplyAddress = redirectedreplyAddress;
	}
	
	public InetSocketAddress getReplyAddress(){
		InetSocketAddress replyAddress;
		if ( _redirectedreplyAddress != null )
			replyAddress = _redirectedreplyAddress;
		else
			replyAddress = getFrom();
		
		return replyAddress;
	}
}
