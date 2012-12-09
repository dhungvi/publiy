package org.msrg.publiy.broker.core.connectionManager;

import java.util.TimerTask;

import org.msrg.publiy.broker.networkcoding.connectionManager.client.ConnectionManagerNC_Client;

public class TimerTask_ProcessIncompleteBreakedWatchList extends TimerTask {

	final ConnectionManagerNC_Client _connManNC;
	
	public TimerTask_ProcessIncompleteBreakedWatchList(ConnectionManagerNC_Client connManNC) {
		_connManNC = connManNC;
	}
	
	@Override
	public void run() {
		_connManNC.processIncompleteBreakedWatchList();
	}
	
	@Override
	public String toString() {
		return "TimerTask_ProcessIncompleteWatchList";
	}
}
