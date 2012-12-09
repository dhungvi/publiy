package org.msrg.publiy.broker.core.connectionManager;

import java.util.TimerTask;

public class TimerTask_ProcessNextFlow extends TimerTask {

	final ConnectionManagerNC _connManNC;
	
	public TimerTask_ProcessNextFlow(ConnectionManagerNC connManNC) {
		_connManNC = connManNC;
	}
	
	@Override
	public void run() {
		_connManNC.processNextFlowEvent();
	}
	
	@Override
	public String toString() {
		return "TimerTask_ProcessNextFlow";
	}

}
