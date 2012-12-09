package org.msrg.publiy.communication.socketbinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.broker.controller.sequence.IConnectionSequenceUpdateListener;
import org.msrg.publiy.broker.controller.sequence.LocalControllerSequencer;
import org.msrg.publiy.broker.controller.sequence.SequenceUpdateStatus;
import org.msrg.publiy.broker.controller.sequence.SequenceUpdateable;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class MessageSender extends Thread implements IMessageSender, ILoggerSource {
	
	public static int RETRY_INTERVAL = 2050;
	
	protected final SequenceUpdateable<IConnectionSequenceUpdateListener> _connectionUpdateableSequence = LocalControllerSequencer.getLocalControllerSequencer().getNextConnectionSequence();
	
	protected boolean _sendRetryTaskScheduled = false;
	final private Timer _timer;
	final protected List<Message> _sendQueue;
	final protected String _id;

	private InetSocketAddress _redirectedMessageReceiverAddress = null;
	
	protected Socket _client;
	protected OutputStream _cos;
	protected ObjectOutputStream _coos;
	protected InputStream _cis;
	protected ObjectInputStream _cois;
	
	protected static String getNamePrefix(){
		return "MSender-";
	}
	
	protected MessageSender(String id, Timer timer){
		super(id);
		_id = id;
		_sendQueue = new LinkedList<Message>();
		_timer = timer;
		_connectionUpdateableSequence.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_INVALID);
	}
	
	@Override
	public void sendMessage(Message message){
		synchronized( _sendQueue ){
			redirect(message);
			message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_BEING_SENT);
			_sendQueue.add(message);
			LoggerFactory.getLogger().debug(this, "Put '" + message + "' into the SQ: " + _sendQueue);
			_sendQueue.notifyAll();
		}
	}
	
	private synchronized boolean send(Message message){
		message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_BEING_SENT);
		boolean done = false;
		if ( message == null )
			return true;
		
		if ( !message.retrying() ){
			message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_SEND_ERROR);	
			return true;
		}

		InetSocketAddress to = message.getTo();
		InetAddress toAddress = to.getAddress();
		int toPort = to.getPort();
		
		try{
			_client = new Socket(toAddress, toPort);
			_cos = _client.getOutputStream();
			_coos = new ObjectOutputStream(_cos);
			_cis  = _client.getInputStream();
			_cois = new ObjectInputStream(_cis);

			_coos.writeObject(message);
			_coos.flush();
			
			Object receivedObj = _cois.readObject();
			Sequence ackSeq = (Sequence) receivedObj;
			
			if ( message.getSourceInstanceID().equals(ackSeq) ){
				message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_SENT);
				done = true;
			}else
				message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_SEND_ERROR);

			_cis.close();
			_cos.close(); 
			_client.close();
		}catch(IOException iox){
			iox.printStackTrace();
			LoggerFactory.getLogger().debug(this, "Sending: " + message + "... Failed.");
		}catch(ClassNotFoundException iox){
			iox.printStackTrace();
			LoggerFactory.getLogger().debug(this, "Sending: " + message + "... Failed (unknown class.");
		}
		
		if ( done )
			LoggerFactory.getLogger().debug(this, "Sending: " + message + "... Succeeded.");
		else
			message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_SEND_ERROR);
		return done;
	}
	
	// Must already hold '_sendQueue'
	protected void goToSleep(){
		try{_sendQueue.wait();}catch(InterruptedException ix){}
	}
	
	protected void scheduleNewSendRetryTask(){
		scheduleNewSendRetryTask(true);
	}
	
	protected void scheduleNewSendRetryTask(boolean newOrOld){
		if ( newOrOld )
		{	// Schedule new one
			if ( _sendRetryTaskScheduled )
				return;
			_sendRetryTaskScheduled = true;
			_timer.schedule(new SendTask(this), RETRY_INTERVAL);
		}
		else
		{	// Old task done.
			_sendRetryTaskScheduled = false;
		}
	}
	
	public void resend(){
		LoggerFactory.getLogger().debug(this, "Resend ... " + this);
		synchronized (_sendQueue) {
			scheduleNewSendRetryTask(false);
			if ( !_sendQueue.isEmpty() )
				_sendQueue.notify();
		}
	}

	public void run() {
		boolean problem = false;
		boolean remove = false;
		
		LoggerFactory.getLogger().info(this, this.toString() + " is running...");
		while( true ){
			Message message = null;
			synchronized( _sendQueue ){
				if ( problem || _sendQueue.isEmpty() ){
					if ( problem )
						scheduleNewSendRetryTask(true);
					
					goToSleep();
//					try{_sendQueue.wait();}catch(InterruptedException ix){}
				}
				message = _sendQueue.get(0);	
			}
			
			if ( message!=null ){
				remove = send(message);
				if ( remove ){
					remove = false;
					synchronized( _sendQueue ){
						Message removedMessage = _sendQueue.remove(0); //_lastSentMessageButNotAcknowledged
						System.out.println("Going to remove: " + removedMessage);
					}
				}
				else {
					problem = true;
				}
			}
			else
				problem = false;
		}
	}
	
	@Override
	public String toString(){
		return getNamePrefix() + _id;
	}

	@Override
	public void prepareAndStart() {
		start();
	}

	@Override
	public void clear() {
		synchronized (_sendQueue) {
			_sendQueue.clear();	
		}
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_SOCKET_MSG_SENDER;
	}

	@Override
	public SequenceUpdateable<IConnectionSequenceUpdateListener> connect() {
		return _connectionUpdateableSequence;
	}
	
	@Override
	public SequenceUpdateable<IConnectionSequenceUpdateListener> disconnect(){
		return _connectionUpdateableSequence;
	}

	@Override
	public boolean canSendTo(InetSocketAddress clientAddress) {
		return true;
	}

	@Override
	public synchronized void enableRedirectedReplies(InetSocketAddress redirectedMessageReceiverAddress){
		_redirectedMessageReceiverAddress = redirectedMessageReceiverAddress;
	}
	
	protected void redirect(Message message){
		if ( _redirectedMessageReceiverAddress != null )
			message.setRedirectedReplyAddress(_redirectedMessageReceiverAddress);
	}
}
