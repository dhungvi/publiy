package org.msrg.publiy.communication.socketbinding;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;


public abstract class MessageListener extends Thread implements IMessageReceiverListener, ILoggerSource {

	private List<Message> _messageQueue;
	private InetSocketAddress _localAddress;

	public MessageListener(InetSocketAddress localAddress, String id){
		super(id);
		_localAddress = localAddress;

		_messageQueue = new LinkedList<Message>();
	}
	
	@Override
	public final int getListeningPort() {
		return _localAddress.getPort();
	}

	@Override
	public final void gotMessage(Message message) {
		synchronized (_messageQueue) {
			LoggerFactory.getLogger().debug(this, "Received " + message);
			_messageQueue.add(message);
			_messageQueue.notify();
		}
	}
	
	@Override
	public String toString() {
		return "MessageListener(" + _localAddress + ")";
	}
	
	@Override
	public final void run(){
		LoggerFactory.getLogger().info(this, this + " is running ... ");
		while (true){
			Message message;
			
			synchronized (_messageQueue) {
				if ( _messageQueue.isEmpty() )
					try{_messageQueue.wait();}catch(InterruptedException itx){}
				
				message = _messageQueue.remove(0);
			}
			
			handleMessage(message);
		}
	}

	@Override
	public final void clear(){
		synchronized (_messageQueue) {
			_messageQueue.clear();
		}
	}
	
	protected abstract void handleMessage(Message message);
}
