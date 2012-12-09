package org.msrg.publiy.communication.core.niobinding;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

public class DestroyConnectionTask extends BrokerTimerTask implements ILoggerSource {

	private IConInfoNonListening _conInfoNL;
	
	public DestroyConnectionTask(IConInfoNonListening conInfoNL) {
		super(BrokerTimerTaskType.BTimerTask_DestroyConnection);
		_conInfoNL = conInfoNL;
		
		super.logCreation();
	}
	
	@Override
	public void run() {
		super.run();
		try{
			INIOBinding nioBinding = _conInfoNL.getNIOBinding();
			nioBinding.destroyConnection(_conInfoNL);
		}catch(Exception ex){
			LoggerFactory.getLogger().infoX(this, ex, "Exception in '" + this + "'");
		}
	}

	@Override
	public String toStringDetails() {
		return "" + _conInfoNL;
	}

}
