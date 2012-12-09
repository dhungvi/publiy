package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

import org.msrg.publiy.broker.core.IConnectionManager;

public class CandidatesEventTimerTask extends BrokerTimerTask {

	final IConnectionManager _connectionManager;
	
	CandidatesEventTimerTask(IConnectionManager connectionManager){
		super(BrokerTimerTaskType.BTimerTask_Candidate);
		_connectionManager = connectionManager;
		
		super.logCreation();
	}
	
	@Override
	public void run() {
		super.run();
		_connectionManager.insertCandidatesEvent();
	}

	@Override
	public String toStringDetails() {
		return "";
	}
}
