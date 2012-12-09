package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

import org.msrg.publiy.broker.core.IConnectionManager;

public class ForcedConfAckTimerTask extends BrokerTimerTask {

	final IConnectionManager _connectionManager;
	
	ForcedConfAckTimerTask(IConnectionManager connectionManager){
		super(BrokerTimerTaskType.BTimerTask_ForcedConfAck);
		_connectionManager = connectionManager;
		
		super.logCreation();
	}
	
	@Override
	public void run() {
		super.run();
		_connectionManager.insertForceConfAckEvent();
	}

	@Override
	public String toStringDetails() {
		return "";
	}

}
