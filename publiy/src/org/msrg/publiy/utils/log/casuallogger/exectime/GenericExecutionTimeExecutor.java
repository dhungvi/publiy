package org.msrg.publiy.utils.log.casuallogger.exectime;

public class GenericExecutionTimeExecutor implements IExecutionTimeExecutorInternal {
	
	final private Object _lock = new Object();
	final protected ExecutionTimeType[] _types;
	final protected MutableExecutionTimeSummary _mutableExecTimeSummary;
	
	public GenericExecutionTimeExecutor(ExecutionTimeType type) {
		_types = new ExecutionTimeType[1];
		_types[0] = type;
		_mutableExecTimeSummary = new MutableExecutionTimeSummary(0, 0);
	}
	
	@Override
	public void executionFinalized(IExecutionTimeEntity entity) {
		synchronized (_lock) {
			_mutableExecTimeSummary.addToNumberOfExecutions(entity.getExecutionTimeSummary());
		}
	}

	@Override
	public ExecutionTimeSummary getExecutionTimeSummary(ExecutionTimeType type, boolean reset) {
		checkType(type);
		synchronized (_lock) {
			ExecutionTimeSummary returnSummary = new ExecutionTimeSummary(_mutableExecTimeSummary);
			if (reset)
				_mutableExecTimeSummary.reset();
			
			return returnSummary;
		}
	}
	
	public boolean checkType(ExecutionTimeType type) {
		if (type == null || type != _types[0])
			throw new IllegalArgumentException("Only " + _types[0] + " is supported. Not " + type);
		
		return true;
	}

	@Override
	public String getHeader(ExecutionTimeType type) {
		checkType(type);
		return _types[0].toStringShort();
	}

	@Override
	public ExecutionTimeSummary[] getExecutionTimeSummaryForAllTypes(boolean reset) {
		ExecutionTimeSummary[] summaries = new ExecutionTimeSummary[1];
		summaries[0] = getExecutionTimeSummary(_types[0], reset);
		return summaries;
	}

	@Override
	public String getHeaderForAllTypes() {
		return getHeader(_types[0]);
	}

	@Override
	public ExecutionTimeType[] getSupportedTypes() {
		return _types;
	}

}
