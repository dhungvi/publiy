package org.msrg.publiy.client.subscriber;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

public class SubscriberTask extends BrokerTimerTask implements ILoggerSource {

	SimpleFileSubscriber _simpleFileSubscriber;
	
	public SubscriberTask(SimpleFileSubscriber simpleFileSubscriber){
		super(BrokerTimerTaskType.BTimerTask_Subscribe);
		_simpleFileSubscriber = simpleFileSubscriber;
		
		super.logCreation();
	}

	@Override
	public void run() {
		super.run();
		try{
			_simpleFileSubscriber.subscribeNext();
		}catch(Exception ex){
			LoggerFactory.getLogger().infoX(this, ex, "Exception in '" + this + "'");
		}
	}

	@Override
	public String toStringDetails() {
		return "";
	}
}