package org.msrg.publiy.pubsub.multipath.loadweights;

import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

import org.msrg.publiy.broker.core.connectionManager.IConnectionManagerMP;

public class LoadWeightSendTask extends BrokerTimerTask {

	private IConnectionManagerMP _connMan;
	
	public LoadWeightSendTask(IConnectionManagerMP connMan) {
		super(BrokerTimerTaskType.BTimerTask_LoadWeight);
		_connMan = connMan;
		
		super.logCreation();
	}
	
	@Override
	public void run() {
		super.run();
		try{
			_connMan.sendLoadWeights();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	@Override
	public String toStringDetails() {
		return "";
	}

}
