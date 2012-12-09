package org.msrg.publiy.broker.core.bwenforcer;

public enum BWMetric {
	
	BW_METRIC_IN_MSG_COUNT,
	BW_METRIC_IN_BYTES,
	
	BW_METRIC_OUT_MSG_COUNT,
	BW_METRIC_OUT_BYTES,

	BW_METRIC_INOUT_MSG_COUNT,
	BW_METRIC_INOUT_BYTES,
	
	BW_METRIC_UNLIMITTED;

	public static BWMetric getBWMetricFromString(String bwMetricStr) {
		return valueOf(bwMetricStr);
	}
}
