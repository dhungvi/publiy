package org.msrg.publiy.broker.internaltimer;

import org.msrg.publiy.utils.SystemTime;

public class BrokerInternalTimer {
	
	private static long _startTime = -1;
	
	public static synchronized void start(boolean failOnRestart) {
		if ( _startTime != -1 && failOnRestart )
			throw new IllegalStateException("BIT already started");
		
		if ( _startTime != -1 )
			return;
		
		_startTime = SystemTime.nanoTime();
	}
	
	public static void start() {
		start(false);
	}
	
	public static BrokerInternalTimerReading read() {
		if ( _startTime == -1 )
			throw new IllegalStateException("BIT not started");

		long currTimeNano = SystemTime.nanoTime();
		return new BrokerInternalTimerReading(_startTime, currTimeNano);
	}
	
	public static void inform(String ... strs) {
		System.out.print("@" + BrokerInternalTimer.read() + ": ");
		for(String str : strs)
			System.out.print(str);
		System.out.println();
	}
}
