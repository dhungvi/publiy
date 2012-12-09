package org.msrg.publiy.utils.log.casuallogger.exectime;

public interface IExecutionTimeExecutor {
	
	public ExecutionTimeType[] getSupportedTypes();

	public String getHeader(ExecutionTimeType type);
	public String getHeaderForAllTypes();
	
	public ExecutionTimeSummary[] getExecutionTimeSummaryForAllTypes(boolean reset);
	public ExecutionTimeSummary getExecutionTimeSummary(ExecutionTimeType type, boolean reset);
	
}
