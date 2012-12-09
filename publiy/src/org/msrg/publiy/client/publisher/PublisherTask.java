package org.msrg.publiy.client.publisher;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

public class PublisherTask extends BrokerTimerTask implements ILoggerSource {

	SimpleFilePublisher _filePubliser;
	static int _counter = 0;
	
	public PublisherTask(SimpleFilePublisher filePubliser) {
		super(BrokerTimerTaskType.BTimerTask_Publish);
		_counter++;
		_filePubliser = filePubliser;
		
		super.logCreation();
	}
	
	@Override
	protected void logCreation() {
		super.logCreation();
	}

	@Override
	protected void logRun() {
		super.logRun();
	}

	@Override
	public void run() {
		super.run();
		try{
			_filePubliser.publishNext();
		}catch(Exception ex) {
			LoggerFactory.getLogger().infoX(this, ex, "Exception in '" + this + "'");
		}
	}
	
	@Override
	public String toStringDetails() {
		return "";
	}
}
