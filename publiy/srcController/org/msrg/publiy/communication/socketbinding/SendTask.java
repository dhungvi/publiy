package org.msrg.publiy.communication.socketbinding;

import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

public class SendTask extends BrokerTimerTask {

	private IMessageSender _messageSender;
	
	public SendTask(IMessageSender messageSender){
		super(BrokerTimerTaskType.BTimerTask_Send);
		_messageSender = messageSender;
		
		super.logCreation();
	}
	
	public void run() {
		super.run();
		_messageSender.resend();
	}

	@Override
	public String toStringDetails() {
		return "";
	}

}
