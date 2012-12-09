package org.msrg.publiy.utils.log.casuallogger;

public interface ICpuCasualReader extends ICasualLogger {

	public double getAverageCpuPerc();
	public long getUsedHeapMemory();
	
}
