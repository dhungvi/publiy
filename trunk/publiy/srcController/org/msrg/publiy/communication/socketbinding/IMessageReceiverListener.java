package org.msrg.publiy.communication.socketbinding;

public interface IMessageReceiverListener {

	public int getListeningPort();
	public void gotMessage(Message message);
	public String getListenerName();
	
	public void clear();
	
}
