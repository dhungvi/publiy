package org.msrg.publiy.broker.core.maintenance;

import java.util.Timer;
import java.util.TimerTask;


import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.broker.core.IMaintenanceManager;
import org.msrg.publiy.component.IComponent;
import org.msrg.publiy.component.IComponentListener;

public class MaintenanceManagerWithNoBroker implements IMaintenanceManager, ILoggerSource, IComponentListener {
	
	public static final long MAINTENANCE_GC_INTERVAL = 30000;

	protected final Timer _timer;
	protected final String _id;
	
	private boolean _destroyed = false;
	private Object _lock = new Object();
	
	public MaintenanceManagerWithNoBroker(String id){
		_id = id;
		_timer = new Timer("MaintenanceTimer-" + _id);
	}
	
	@Override
	public boolean schedulePeriodicMaintenanceTask(TimerTask task, long delay, long period){
		synchronized (_lock)
		{
			if ( _destroyed )
				return false;
				
			_timer.scheduleAtFixedRate(task, delay, period);
			return true;
		}
	}
	
	@Override
	public boolean scheduleMaintenanceTask(TimerTask task, long delay){
		synchronized (_lock)
		{
			if ( _destroyed )
				return false;
				
			_timer.schedule(task, delay);
			return true;
		}
	}
	
	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_MAINTENANCE_MAN;
	}
	
	@Override
	public void scheduleNextGC_maintenanceTask(){
		GCMaintenanceTask gcTask = GCMaintenanceTask.getNextGCMaintenanceTask(this);
		if ( gcTask != null )
			scheduleMaintenanceTask(gcTask, MAINTENANCE_GC_INTERVAL);
	}

	@Override
	public synchronized void componentStateChanged(IComponent comonent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Timer getTimer() {
		return _timer;
	}

}
