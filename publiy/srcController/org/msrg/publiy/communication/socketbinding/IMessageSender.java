package org.msrg.publiy.communication.socketbinding;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.controller.sequence.IConnectionSequenceUpdateListener;
import org.msrg.publiy.broker.controller.sequence.SequenceUpdateable;


public interface IMessageSender extends Runnable {

	public SequenceUpdateable<IConnectionSequenceUpdateListener> connect();
	public SequenceUpdateable<IConnectionSequenceUpdateListener> disconnect();
	
	public void prepareAndStart();
	public void sendMessage(Message message);
	public void resend();
	
	public void clear();

	public boolean canSendTo(InetSocketAddress clientAddress);
	public void enableRedirectedReplies(InetSocketAddress redirectedMessageReceiverAddress);
	
}
