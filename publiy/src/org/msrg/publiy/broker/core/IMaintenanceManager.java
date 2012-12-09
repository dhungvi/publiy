package org.msrg.publiy.broker.core;

import java.util.Timer;
import java.util.TimerTask;

public interface IMaintenanceManager {
	
	Timer getTimer();
	void scheduleNextGC_maintenanceTask();
	boolean scheduleMaintenanceTask(TimerTask task, long delay);
	boolean schedulePeriodicMaintenanceTask(TimerTask task, long delay, long period);

}
