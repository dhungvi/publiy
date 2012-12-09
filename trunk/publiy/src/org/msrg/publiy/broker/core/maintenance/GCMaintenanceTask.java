package org.msrg.publiy.broker.core.maintenance;

import org.msrg.publiy.broker.core.IMaintenanceManager;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

class GCMaintenanceTask extends BrokerTimerTask implements ILoggerSource {

	private static final Object _lock = new Object();
	private static GCMaintenanceTask _waitingTask;
	private final IMaintenanceManager _maintenanceManager;
	
	private GCMaintenanceTask(IMaintenanceManager maintenanceManager){
		super(BrokerTimerTaskType.BTimerTask_GC);
		_maintenanceManager = maintenanceManager;
		
		super.logCreation();
	}
	
	static GCMaintenanceTask getNextGCMaintenanceTask(IMaintenanceManager maintenanceManager){
		synchronized (_lock) {
			if ( _waitingTask != null )
				return null;
			
			_waitingTask = new GCMaintenanceTask(maintenanceManager);
			
			return _waitingTask;
		}
	}
	@Override
	public void run() {
		super.run();
		long maxMem = Runtime.getRuntime().maxMemory();
		long freeMem = Runtime.getRuntime().freeMemory();
		LoggerFactory.getLogger().info(this, "Max Mem: " + maxMem + ", Free Mem: " +  freeMem);
		
		System.gc();
		
		synchronized (_lock) {
			if ( _waitingTask != this )
				throw new IllegalStateException("" + this);
			
			_waitingTask = null;
		}
		
		_maintenanceManager.scheduleNextGC_maintenanceTask();
	}

	@Override
	public String toStringDetails() {
		return "";
	}
}
