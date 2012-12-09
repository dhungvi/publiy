package org.msrg.publiy.communication.socketbinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Timer;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;


public class MessageReceiver extends Thread implements ILoggerSource {
	public static final int LISTENING_TIMEOUT = 1000;
	
	protected IMessageReceiverListener _messageListener;
	private Timer _timer;
	protected final int _receivePort;
	
	public MessageReceiver(IMessageReceiverListener messageListener, int receivePort){
		_messageListener = messageListener;
		_receivePort = receivePort;
		_timer = new Timer(this.toString(), true);
	}

	public void run() {
		try{
			LoggerFactory.getLogger().info(this, this.toString() + " is running...");
			ServerSocket serverSocket = new ServerSocket(_receivePort);
			serverSocket.setSoTimeout(LISTENING_TIMEOUT);
			while ( true ){
				Socket client = null;
				try{
					client = serverSocket.accept();
					InputStream cis = client.getInputStream();
					ObjectInputStream cois = new ObjectInputStream(cis);
					OutputStream cos = client.getOutputStream();
					ObjectOutputStream coos = new ObjectOutputStream(cos);

					Object received = cois.readObject();
					if ( received != null ){
						Message receivedMessage = (Message)received;
						LoggerFactory.getLogger().debug(this, "Message received: " + received);
						_messageListener.gotMessage(receivedMessage);
						
						// Send ack
						coos.writeObject(receivedMessage.getSourceInstanceID());
					}
					else
						System.out.println("READ NULL");

					ConnectionCloseTimerTask connectionCloseTimerTask = new ConnectionCloseTimerTask(client);
					_timer.schedule(connectionCloseTimerTask, ConnectionCloseTimerTask.DELAY);
					
					cis.close();
					client.close();
				}catch(SocketTimeoutException stox ){
				}catch( StreamCorruptedException scx ){
					scx.printStackTrace();
				}catch( IOException iox){
					iox.printStackTrace();
				}catch ( ClassNotFoundException nfx){
					nfx.printStackTrace();
				}
				
				if ( client != null )
					try{client.close();}catch(Exception x){};
			}
		}catch( StreamCorruptedException scx ){
			scx.printStackTrace();
		}catch( IOException iox){
			iox.printStackTrace();
		}
	}
	
	public void prepareAndStart(){
		start();
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_SOCKET_MSG_RECEIVEDER;
	}
}
