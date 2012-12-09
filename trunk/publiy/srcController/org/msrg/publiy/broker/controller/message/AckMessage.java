package org.msrg.publiy.broker.controller.message;

import org.msrg.publiy.communication.socketbinding.Message;
import org.msrg.publiy.communication.socketbinding.MessageTypes;

public class AckMessage extends Message {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 63808648843468353L;

	public static AckMessage createAckMessage(Message message) {
		if ( message.needsConfirmation() )
			return new AckMessage(message);		
		else 
			return null;
	}
	
	private AckMessage(Message message) {
		super(MessageTypes.MESSAGE_TYPE_ACK, message.getFrom(), message.getSourceInstanceID());
	}

	@Override
	public String toString() {
		return "Ack of: " + _sourceInstanceID;
	}
	
	@Override
	public boolean needsConfirmation(){
		return false;
	}
	
	@Override
	public boolean equals(Object obj){
		if ( obj == null )
			return false;
		
		if ( Message.class.isInstance(obj) ){
			
			Message msg = (Message) obj;
			return msg._sourceInstanceID.equals(_sourceInstanceID);
		}
		
		return false;
	}
}
