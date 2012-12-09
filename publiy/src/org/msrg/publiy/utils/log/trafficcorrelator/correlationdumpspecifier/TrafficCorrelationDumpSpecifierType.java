package org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier;

class PrivateCounter {
	static byte i = 0;
}

public enum TrafficCorrelationDumpSpecifierType {
	DUMP_ALL (PrivateCounter.i++),
	DUMP_ONE (PrivateCounter.i++),
	DUMP_SESSIONS_EVENTS (PrivateCounter.i++);
	
	final byte _byteValue;
	
	TrafficCorrelationDumpSpecifierType(byte byteValue) {
		_byteValue = byteValue;
	}
	
	public static TrafficCorrelationDumpSpecifierType valueOf(byte type) {
		return TrafficCorrelationDumpSpecifierType.values()[type];
	}
}
