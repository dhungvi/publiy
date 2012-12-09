package org.msrg.publiy.utils.log.casuallogger.exectime;

public class MutableExecutionTimeSummary extends ExecutionTimeSummary {

	public MutableExecutionTimeSummary(int numberOfExecutions,
			long durationOfExecutions) {
		super(numberOfExecutions, durationOfExecutions, false);
	}
	
	public void addToNumberOfExecutions(ExecutionTimeSummary summary) {
		_numberOfExecutions += summary._numberOfExecutions;
		_durationOfExecutions += summary._durationOfExecutions;
	}
	
	public void reset() {
		_durationOfExecutions = _numberOfExecutions = 0;
		_finalized = false;
	}
	
}
