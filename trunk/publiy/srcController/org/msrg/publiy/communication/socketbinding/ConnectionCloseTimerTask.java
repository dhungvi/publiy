package org.msrg.publiy.communication.socketbinding;

import java.io.IOException;
import java.net.Socket;
import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

public class ConnectionCloseTimerTask extends BrokerTimerTask {

	public static final int DELAY = 5000;
	
	private final Socket _socket;
	
	ConnectionCloseTimerTask(Socket socket) {
		super(BrokerTimerTaskType.BTimerTask_ConnectionClose);
		_socket = socket;
		
		super.logCreation();
	}
	
	@Override
	public void run() {
		super.run();
		try{
			_socket.close();
		}catch(IOException iox ){}
	}

	@Override
	public String toStringDetails() {
		return "" + _socket;
	}

}
