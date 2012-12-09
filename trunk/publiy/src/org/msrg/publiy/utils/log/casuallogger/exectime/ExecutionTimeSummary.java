package org.msrg.publiy.utils.log.casuallogger.exectime;

public class ExecutionTimeSummary {

	public int _numberOfExecutions;
	public long _durationOfExecutions;
	public boolean _finalized;
	
	public ExecutionTimeSummary(ExecutionTimeSummary summary) {
		this(summary._numberOfExecutions, summary._durationOfExecutions, summary._finalized);
	}
	
	public ExecutionTimeSummary(int numberOfExecutions, long durationOfExecutions, boolean finalized) {
		_numberOfExecutions = numberOfExecutions;
		_durationOfExecutions = durationOfExecutions;
		_finalized = finalized;
	}
	
	public int getNumberOfExecutions() {
		return _numberOfExecutions;
	}
	
	public long getDurationOfExecutions () {
		return _durationOfExecutions;
	}
	
	@Override
	public String toString() {
		return (_durationOfExecutions + " " + _numberOfExecutions);
	}
}
