package org.msrg.publiy.communication.core.niobinding.keepalive;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;
import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

public class FDReceiveTask extends BrokerTimerTask implements ILoggerSource {
	private IConInfoNLKeepAlive _conInfoNLKA;
	
	public FDReceiveTask(IConInfoNLKeepAlive conInfoNLKA){
		super(BrokerTimerTaskType.BTimerTask_FDReceive);
		_conInfoNLKA = conInfoNLKA;
		
		super.logCreation();
	}

	@Override
	public void run() {
		super.run();
		try{
			// We have not heard from the remote party during the FD_RECEIVE_DEADLINE
			//    we decide to close the connection - too sad, too bad :(
			if (Broker.CORRELATE)
				TrafficCorrelator.getInstance().connectionTimedOut(_conInfoNLKA);
			
			_conInfoNLKA.getNIOBinding().renewConnection(_conInfoNLKA.getSession()); //destroyConnection(_conInfoNLKA);
		}catch(Exception ex){
			LoggerFactory.getLogger().infoX(this, ex, "Exception in '" + this + "'");
		}
	}

	@Override
	public String toStringDetails() {
		return "" + _conInfoNLKA;
	}
}
