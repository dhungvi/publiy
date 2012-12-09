package org.msrg.publiy.broker.core.flowManager;

public enum FlowState {

	ACTIVE			("ACT"),
	INACTIVE		("INACT");
	
	public final String _str;
	
	FlowState(String str) {
		_str = str;
	}
	
	@Override
	public String toString() {
		return _str;
	}
}
