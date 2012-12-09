package org.msrg.publiy.broker.core.flowManager;

public enum FlowPriority {

	ULTIMATE		(COUNTER._COUNTER++, "U"),			// broker-to-broker
	HIGH			(COUNTER._COUNTER++, "H"),			// broker-to-seedingclients before min
	MEDIUM			(COUNTER._COUNTER++, "M"),			// broker-to-seedingclients after min
	LOW				(COUNTER._COUNTER++, "L");			// *-to-nonseeding clients

	public final String _str;
	public final int _index;
	
	FlowPriority(int index, String str) {
		_str = str;
		_index = index;
	}
	
	@Override
	public String toString() {
		return _str;
	}
}


class COUNTER {
	static int _COUNTER = 0;
}