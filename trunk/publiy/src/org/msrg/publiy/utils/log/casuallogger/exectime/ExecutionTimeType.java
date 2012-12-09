package org.msrg.publiy.utils.log.casuallogger.exectime;

public enum ExecutionTimeType {

	// TYPES BEGIN.
	EXEC_TIME_PROCEED							 ("PROCEED"),
	EXEC_TIME_PATH_COMPUTATION					 ("PATH_COMP"),
	EXEC_TIME_CHECKOUT_PUB						 ("CHKOT_P"),
	EXEC_TIME_MATCHING							 ("MTCHING"),
	EXEC_TIME_MQ_MATCHING						 ("MQ_MRPH"),
	EXEC_TIME_WORKING_OVERLAY_MAN_CREATE		 ("W_OVR_M"),
	EXEC_TIME_WORKING_SUBSCRIPTION_MAN_CREATE	 ("W_SUB_M"),
	EXEC_TIME_CONSIDERATION						 ("CONSIDR"),
	EXEC_TIME_COVERING_COMPUTATION_TIME			 ("COV_CMP"),
	;
	// TYPES END.
	
	private final String _str;

	ExecutionTimeType(String str) {
		_str = str;
	}
	
	public String toStringShort() {
		return _str;
	}
	
	public boolean isEnabled() {
		return true;
	}
	
}
