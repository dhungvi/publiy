package org.msrg.publiy.broker.core.connectionManager;

import java.util.TimerTask;

import org.msrg.publiy.broker.networkcoding.connectionManager.client.ConnectionManagerNC_Client;

public class TimerTask_ProcessIncompleteUnbreakedWatchList extends TimerTask {

	final ConnectionManagerNC_Client _connManNC;
	
	public TimerTask_ProcessIncompleteUnbreakedWatchList(ConnectionManagerNC_Client connManNC) {
		_connManNC = connManNC;
	}
	
	@Override
	public void run() {
		_connManNC.processIncompleteUnBreakedWatchList();
	}
	
	@Override
	public String toString() {
		return "TimerTask_ProcessIncompleteWatchList";
	}
}
