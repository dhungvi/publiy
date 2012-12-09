package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

import org.msrg.publiy.broker.core.IConnectionManager;

public class SendDackTimerTask extends BrokerTimerTask {

	private IConnectionManager _connMan;
	
	SendDackTimerTask(IConnectionManager connMan) {
		super(BrokerTimerTaskType.BTimerTask_SendDack);
		_connMan = connMan;
		
		super.logCreation();
	}
	
	@Override
	public void run() {
		super.run();
		try{
			_connMan.sendDackOnAllSessions();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	@Override
	public String toStringDetails() {
		return "";
	}

}
