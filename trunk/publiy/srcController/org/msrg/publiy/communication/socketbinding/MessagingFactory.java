package org.msrg.publiy.communication.socketbinding;

import java.net.InetSocketAddress;
import java.util.Timer;

import org.msrg.publiy.communication.socketbinding.persistent.PersistentMessageReceiver;
import org.msrg.publiy.communication.socketbinding.persistent.PersistentMessageSender;

public class MessagingFactory {
	
	private static Class<?> DEFAULT_MESSAGE_SENDER = PersistentMessageSender.class;
	private static Class<?> DEFAULT_MESSAGE_RECEIVER = PersistentMessageReceiver.class;
	
	public static MessageSender getNewMessageSender(String id, Timer timer, InetSocketAddress clientAddress){
		return getNewMessageSender(DEFAULT_MESSAGE_SENDER, id, timer, clientAddress);
	}
	
	public static MessageSender getNewMessageSender(Class<?> mSenderClass, String id, Timer timer, InetSocketAddress clientAddress){
		System.out.println("Creating message sender: " + id + " to " + clientAddress);
		if ( mSenderClass == MessageSender.class )
			return new MessageSender(id, timer);
		
		else if ( mSenderClass == PersistentMessageSender.class )
			return new PersistentMessageSender(id, timer, clientAddress);
		
		else
			throw new UnsupportedOperationException("Donno how to instanciate this message sender type: " + mSenderClass);
	}
	
	public static MessageReceiver getNewMessageReceiver(IMessageReceiverListener messageListener, int receivePort){
		return getNewMessageReceiver(DEFAULT_MESSAGE_RECEIVER, messageListener, receivePort);
	}

	public static MessageReceiver getNewMessageReceiver(Class<?> mReceiverClass, IMessageReceiverListener messageListener, int receivePort){
		System.out.println("Creating message receiver: " + messageListener + " at " + receivePort);
		if ( mReceiverClass == MessageReceiver.class )
			return new MessageReceiver(messageListener, receivePort);
		
		else if ( mReceiverClass == PersistentMessageReceiver.class )
			return new PersistentMessageReceiver(messageListener, receivePort);
		
		else 
			throw new UnsupportedOperationException("Donno how to instanciate this message receiver type: " + mReceiverClass);
	}
}
