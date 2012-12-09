package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

import org.msrg.publiy.broker.core.IConnectionManager;

public class PurgeMQTimerTask extends BrokerTimerTask {

	private IConnectionManager _connMan;
	
	PurgeMQTimerTask(IConnectionManager connMan){
		super(BrokerTimerTaskType.BTimerTask_PurgeMQ);
		_connMan = connMan;
		
		super.logCreation();
	}
	
	@Override
	public void run() {
		super.run();
		try{
			_connMan.purgeMQ();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	@Override
	public String toStringDetails() {
		return "";
	}

}
