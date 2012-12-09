package org.msrg.publiy.utils.log.casuallogger;

import org.msrg.publiy.broker.BrokerShadow;

public class FakeCpuCasualReader implements ICpuCasualReader {

	public FakeCpuCasualReader(BrokerShadow brokerShadow) {
		brokerShadow.setCpuCasualReader(this);
	}
	
	@Override
	public double getAverageCpuPerc() {
		return -1;
	}

	@Override
	public long getUsedHeapMemory() {
		return -1;
	}

	@Override
	public void initializeFile() { }

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean performLogging() {
		return true;
	}

	@Override
	public boolean forceFlush() {
		return true;
	}

	@Override
	public long getNextDesiredLoggingTime() {
		return 1000;
	}

	@Override
	public long getNextDesiredForcedFlushTime() {
		return 1000;
	}
}
