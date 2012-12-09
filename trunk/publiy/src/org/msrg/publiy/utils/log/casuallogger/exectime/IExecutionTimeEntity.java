package org.msrg.publiy.utils.log.casuallogger.exectime;

public interface IExecutionTimeEntity {

	void reset();
	void executionStarted ();
	void executionEnded ();	
	void executionEnded (boolean finalized, boolean flatten);	
	void finalized(boolean flatten);
	void discard();
	ExecutionTimeType getType ();
	
	public ExecutionTimeSummary getExecutionTimeSummary();
	
	public String toString();
//	public String toStringShortAndReset ();
	
}

