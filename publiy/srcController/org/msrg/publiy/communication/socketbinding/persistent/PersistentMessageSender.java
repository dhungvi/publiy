package org.msrg.publiy.communication.socketbinding.persistent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import org.msrg.publiy.broker.controller.message.AckMessage;
import org.msrg.publiy.broker.controller.sequence.IConnectionSequenceUpdateListener;
import org.msrg.publiy.broker.controller.sequence.SequenceUpdateStatus;
import org.msrg.publiy.broker.controller.sequence.SequenceUpdateable;

import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.communication.socketbinding.Message;
import org.msrg.publiy.communication.socketbinding.MessageSender;

public class PersistentMessageSender extends MessageSender {

	final int MAX_SEND_AT_A_TIME = 10;
	final int MAX_RECEIVE_AT_A_TIME = 10;
	
	private boolean _connected = false;
	private boolean _connect = false;
	private final InetSocketAddress _clientAddress;
	private final List<Message> _awaitingAckMessageQueue;
	
	public PersistentMessageSender(String id, Timer timer, InetSocketAddress clientAddress) {
		super(id, timer);
		if ( clientAddress == null )
			throw new IllegalArgumentException("clientAddress cannot be null.");
		
		_clientAddress = clientAddress;
		_awaitingAckMessageQueue = new LinkedList<Message>();
	}
	
	private void receive(Message message) throws IOException, ClassNotFoundException{
		Object receivedObj = null;
		
		receivedObj = _cois.readObject();
		
		if ( AckMessage.class.isInstance(receivedObj) ){
			AckMessage ackMessage = (AckMessage) receivedObj;
			
			if ( ackMessage.equals(message) ){
				message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_SENT);
			}else{
				message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_SEND_ERROR);
				throw new IllegalStateException("Did not receive thr right (" + message.getSourceInstanceID().toStringLong() + ") ack message: " + ackMessage._sourceInstanceID.toStringLong());
			}
		}
		else
			throw new IllegalStateException("Received unknown message type: " + receivedObj);
			
	}
	
	private boolean send(Message message) throws IOException{
		message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_BEING_SENT);
		if ( message == null )
			return false;
		
		if ( !message.retrying() ){
			message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_SEND_ERROR);	
			return false;
		}

		InetSocketAddress to = message.getTo();
		if ( _clientAddress != to )
			throw new IllegalStateException("Message is to a wrong client (!" + _client + "): " + message);
		
		_coos.writeObject(message);
		
		return true;
	}
	
	private void sendMany() throws IOException{
		int i = 0;
		do{
			Message message;
			synchronized( _sendQueue ){
				if ( _sendQueue.isEmpty() || !_connected )
					return;
				message = _sendQueue.get(0);
			}
			boolean sent = send(message);
			
			synchronized( _sendQueue ){
				if ( sent )
					_awaitingAckMessageQueue.add(message);
				Message message2 = _sendQueue.remove(0);
				if ( !message2.equals(message) )
					throw new IllegalStateException(message2 + " != " + message);
			}
			
		}while ( i++<MAX_SEND_AT_A_TIME );
	}
	
	private void receiveMany() throws IOException, ClassNotFoundException{
		if ( !_connected ){
			LoggerFactory.getLogger().debug(this, "ReceiveMany - " + this.toString() + ": NOT CONNECTED");
			return;
		}
		else
			LoggerFactory.getLogger().debug(this, "ReceivingMany");
		
		int i = 0;
		do{
			if ( _awaitingAckMessageQueue.isEmpty() || !_connected )
				return;
			Message message = _awaitingAckMessageQueue.get(0);
			try{
				receive(message);
			}catch(Exception x){x.printStackTrace();}
			finally{
				_awaitingAckMessageQueue.remove(0);
			}
			LoggerFactory.getLogger().debug(this, "Received one ack, let's see: " + _awaitingAckMessageQueue);
		}while ( i++<MAX_RECEIVE_AT_A_TIME );
	}
	
	@Override
	protected void goToSleep(){
		LoggerFactory.getLogger().debug(this, "Going to sleeeeep: " + _connect + "_" + _sendQueue.size());
		super.goToSleep();
		LoggerFactory.getLogger().debug(this, "Waking up from sleeeeep: " + _connect + "_" + _sendQueue.size());
	}
	
	@Override
	public void run() {
		LoggerFactory.getLogger().info(this, this.toString() + " is running...");
		while( true )
		{
			synchronized( _sendQueue )
			{
				if ( !_connected )
				{
					if ( _connect )
						bringupConnection();
					else
						bringdownConnection(true);
				}
				if ( _sendRetryTaskScheduled  || _sendQueue.isEmpty() ){
					goToSleep();
				}
			}
			
			try{
				sendMany();
				receiveMany();
			}catch(IOException iox){
				synchronized (_sendQueue) {
					bringdownConnection(false);
					_connectionUpdateableSequence.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_DISCONNECTING);
					scheduleNewSendRetryTask();
				}
			}catch(ClassNotFoundException cnfx){
				throw new IllegalStateException(cnfx);
			}
		}
	}
	
	// Must already hold '_sendQueue'
	private void bringdownConnection(boolean discard){
		LoggerFactory.getLogger().debug(this, "Bringing down connection [" + discard + "] - " + this.toString());
		if ( discard )
		{
			Iterator<Message> awaitingAckIt = _awaitingAckMessageQueue.iterator();
			while ( awaitingAckIt.hasNext() ){
				Message message = awaitingAckIt.next();
				message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_INVALID);
			}
			_awaitingAckMessageQueue.clear();
			
			Iterator<Message> sendQueutIt = _sendQueue.iterator();
			while ( sendQueutIt.hasNext() ){
				Message message = sendQueutIt.next();
				message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_INVALID);
			}
			_sendQueue.clear();
		}
		else
		{
			Iterator<Message> awaitingAckIt = _awaitingAckMessageQueue.iterator();
			while ( awaitingAckIt.hasNext() ){
				Message message = awaitingAckIt.next();
				_sendQueue.add(message);
				LoggerFactory.getLogger().debug(this, "Put into the SQ: " + _sendQueue);
			}
			_awaitingAckMessageQueue.clear();
			
			Iterator<Message> sendQueutIt = _sendQueue.iterator();
			while ( sendQueutIt.hasNext() ){
				Message message = sendQueutIt.next();
				message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_SEND_ERROR);
			}
		}
		
		try{
			if ( _cis != null )
				_cis.close();
			if ( _cos != null )
				_cos.close(); 
			if ( _client != null )
				_client.close();
		}catch(IOException iox){}

		_cis = null;
		_cos = null;
		_client = null;
		_connected = false;
		_coos = null;
		_connectionUpdateableSequence.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_DISCONNECTED);
	}

	// Must already hold '_sendQueue'
	private void bringupConnection(){
		LoggerFactory.getLogger().debug(this, "Bringing up connection [" + _connect + "] - " + this.toString());
		try{
			if ( _client == null || !_client.isConnected() )
			{
				_connectionUpdateableSequence.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_CONNECTING);
				_client = new Socket(_clientAddress.getAddress(), _clientAddress.getPort());
				_cis = _client.getInputStream();
				_cos = _client.getOutputStream();
				_coos = new ObjectOutputStream(_cos);
				_cois = new ObjectInputStream(_cis);
				_connected = true;
			}
			else
				_connected = false;
		}catch(IOException iox){
			LoggerFactory.getLogger().info(this, "Failed to create connection: " + this);
			iox.printStackTrace();
			bringdownConnection(false);
			scheduleNewSendRetryTask();
		}
		
		if ( _client != null && _client.isConnected() )
			_connectionUpdateableSequence.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_CONNECTED);
		else
			_connectionUpdateableSequence.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_DISCONNECTED);
	}
	
	protected static String getNamePrefix(){
		return "PMSender-";
	}
	
	@Override
	public SequenceUpdateable<IConnectionSequenceUpdateListener> connect() {
		synchronized (_sendQueue) {
			_connect = true;
			_sendQueue.notify();
		}
		
		return _connectionUpdateableSequence;
	}

	@Override
	public SequenceUpdateable<IConnectionSequenceUpdateListener> disconnect(){
		synchronized (_sendQueue) {
			_connect = false;
			_sendQueue.notify();
		}
		
		return _connectionUpdateableSequence;
	}

	@Override
	public boolean canSendTo(InetSocketAddress clientAddress) {
		return _clientAddress.equals(clientAddress);
	}

	@Override
	public String toString(){
		return super.toString() + " TO: " + _clientAddress;
	}

	@Override
	public void sendMessage(Message message){
		super.sendMessage(message);
		
		synchronized (_sendQueue) {
			if ( !_connected )
				message.updateSequence(SequenceUpdateStatus.SEQ_UPDATE_STATUS_MESSAGE_SEND_ERROR);
		}
	}
}
