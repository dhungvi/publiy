package org.msrg.publiy.utils.timer;

import java.util.Timer;
import java.util.TimerTask;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimerReading;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

public abstract class BrokerTimerTask extends TimerTask implements ILoggerSource {

	public final BrokerTimerTaskType _brokerTimerType;
	public final BrokerInternalTimerReading _creationTime;
	public final long _delay;
	
	public BrokerTimerTask(BrokerTimerTaskType brokerTimerType) {
		this(brokerTimerType, null, -1);
	}
	
	BrokerTimerTask(BrokerTimerTaskType brokerTimerType, Timer timer, long delay) {
		_brokerTimerType = brokerTimerType;
		_creationTime = BrokerInternalTimer.read();
		_delay = delay;
		if(timer!=null)
			timer.schedule(this, delay);
	}
	
	protected void logCreation() {
		if(_brokerTimerType._doLogCreation && (!Broker.RELEASE || Broker.DEBUG))
			LoggerFactory.getLogger().info(this, "Creating task: " + this);
	}
	
	@Override
	public void run() {
		logRun();
	}
	
	protected void logRun() {
		if(_brokerTimerType._doLogRun) // && (!Broker.RELEASE || Broker.DEBUG))
			LoggerFactory.getLogger().info(this, "Running task: " + this + " @" + BrokerInternalTimer.read());
	}

	@Override
	public final LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_BTimerTask;
	}

	@Override
	public final String toString() {
		return _brokerTimerType.toString() + "@" + _creationTime + ":" + _delay + "{" + toStringDetails() + "}";
	}
	
	public abstract String toStringDetails();
}
