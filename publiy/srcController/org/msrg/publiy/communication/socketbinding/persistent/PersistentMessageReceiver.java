package org.msrg.publiy.communication.socketbinding.persistent;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.msrg.publiy.broker.controller.message.AckMessage;

import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.communication.socketbinding.IMessageReceiverListener;
import org.msrg.publiy.communication.socketbinding.Message;
import org.msrg.publiy.communication.socketbinding.MessageReceiver;

public class PersistentMessageReceiver extends MessageReceiver{

	public PersistentMessageReceiver(IMessageReceiverListener messageListener, int receivePort) {
		super(messageListener, receivePort);
	}
	
	private final static int MAX_LISTENING_SOCKET_RETRY = 10;
	private final static int LISTENING_SOCKET_RETRY_TIMEOUT = 1000;
	private ServerSocket initListeningSocket(){
		ServerSocket serverSocket = null;
		for ( int i=0; i<MAX_LISTENING_SOCKET_RETRY ; i++ )
		{
			try{
				serverSocket = new ServerSocket(_receivePort);
				serverSocket.setSoTimeout(LISTENING_TIMEOUT);
				
				try{Thread.sleep(LISTENING_SOCKET_RETRY_TIMEOUT);}catch(InterruptedException itx){}
			}catch(IOException iox){
				iox.printStackTrace();
				continue;
			}
			
			return serverSocket;
		}
		return null;
	}
	public void run() {
		ServerSocket serverSocket = initListeningSocket();
		if ( serverSocket == null || serverSocket.isClosed() )
			throw new IllegalStateException();
		
		while ( serverSocket!=null )
		{
		try{
			Socket client = serverSocket.accept();
			InputStream cis = client.getInputStream();
			ObjectInputStream cois = new ObjectInputStream(cis);
			OutputStream cos = client.getOutputStream();
			ObjectOutputStream coos = new ObjectOutputStream(cos);
			Object received = null;
			
			while ( client != null & client.isConnected() ){
				try{
					received = cois.readObject();
					
					if ( received != null ){
						Message receivedMessage = (Message)received;
						LoggerFactory.getLogger().debug(this, "Message received: " + received);
						_messageListener.gotMessage(receivedMessage);
						
						// Don't Forget To Send Ack
						AckMessage ackMessage = AckMessage.createAckMessage(receivedMessage);
						coos.writeObject(ackMessage);
						coos.flush();
					}

				}catch( StreamCorruptedException scx ){
					scx.printStackTrace();
					try{coos.writeObject(Boolean.FALSE);}catch(IOException iox){}
					break;
				}catch( IOException iox){
					iox.printStackTrace();
					try{coos.writeObject(Boolean.FALSE);}catch(IOException iox2){}
					break;
				}catch ( ClassNotFoundException nfx){
					nfx.printStackTrace();
					try{coos.writeObject(Boolean.FALSE);}catch(IOException iox){}
					LoggerFactory.getLogger().error(this, "Unknown typed message received: " + received);
				}
			}
			
			try{if ( cos!=null ) cos.close(); } catch(IOException iox){}
			try{if ( client!=null ) client.close();}catch(IOException iox) {}
			
		}catch(SocketTimeoutException stox ){
		}catch( StreamCorruptedException scx ){
			scx.printStackTrace();
		}catch( IOException iox){
			iox.printStackTrace();
		}
	}
	}
}
