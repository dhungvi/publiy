package org.msrg.publiy.utils.log.casuallogger.exectime;

import org.msrg.publiy.utils.SystemTime;

public class GenericExecutionTimeEntity implements IExecutionTimeEntity {
	
	IExecutionTimeExecutorInternal _execTimeExecutor;
	final ExecutionTimeType _type;
	int _numberOfExecutions = 0;
	long _durationOnExecutions = 0;
	long _lastExecutionStartTime = -1;
	boolean _executionFinalized = false;
	
	public GenericExecutionTimeEntity (IExecutionTimeExecutorInternal execTimeExecutor,ExecutionTimeType type) {
		_execTimeExecutor = execTimeExecutor;
		_type = type;
	}
	
	public void reset() {
		_durationOnExecutions = _numberOfExecutions = 0;
	}
	
	public void executionStarted () {
		if (_executionFinalized)
			throw new IllegalStateException("Execusion has been finalized");
		
		_lastExecutionStartTime = SystemTime.nanoTime();
	}
	
	public void executionEnded () {
		if (_executionFinalized && false)
			throw new IllegalStateException("Execusion has been finalized.");
		
		if (_lastExecutionStartTime <= 0)
			throw new IllegalStateException("Execusion has not been started.");
		
		long executionEndTime = SystemTime.nanoTime();
		_numberOfExecutions ++;
		_durationOnExecutions += (executionEndTime - _lastExecutionStartTime);
		
		_lastExecutionStartTime = -1;
	}
	
	@Override
	public synchronized void finalized(boolean flatten) {
		if (_executionFinalized)
			throw new IllegalStateException("Execusion has been finalized: " + _type);
		
		_executionFinalized = true;
		if (flatten)
			_numberOfExecutions = 1;
		
		if(_execTimeExecutor!=null)
			_execTimeExecutor.executionFinalized(this);
	}
	
	public void discard() {
		_execTimeExecutor = null;
	}
	
	public ExecutionTimeType getType () {
		return _type;
	}
	
	@Override
	public String toString() {
		return "ExecTimeEnt[" + _type + ":" + _numberOfExecutions + ":" + _durationOnExecutions + ":" + _executionFinalized + "]";
	}

	@Override
	public ExecutionTimeSummary getExecutionTimeSummary() {
		return new ExecutionTimeSummary(_numberOfExecutions, _durationOnExecutions, _executionFinalized);
	}

	@Override
	public void executionEnded(boolean finalized, boolean flatten) {
		executionEnded();
		
		if(finalized)
			finalized(flatten);
	}
	
}
