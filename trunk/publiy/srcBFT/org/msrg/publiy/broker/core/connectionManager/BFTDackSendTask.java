package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

class BFTDackSendTask extends BrokerTimerTask {

	protected final BFTConnectionManager _bftConnectionManager;
	
	public BFTDackSendTask(BFTConnectionManager bftConnectionManager) {
		super(BrokerTimerTaskType.BTimerTask_BFT_DACK_Send);
		
		_bftConnectionManager = bftConnectionManager;
	}

	@Override
	public void run() {
		_bftConnectionManager.sendBFTDack();
	}
	
	@Override
	public String toStringDetails() {
		return _bftConnectionManager.getLocalAddress().toString();
	}
}
