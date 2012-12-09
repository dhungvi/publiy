package org.msrg.publiy.broker.core.bwenforcer;

import java.net.InetSocketAddress;

import org.msrg.publiy.pubsub.multipath.loadweights.LoadProfiler;
import org.msrg.publiy.pubsub.multipath.loadweights.ProfilerBucket;
import org.msrg.publiy.utils.ElementTotal;
import org.msrg.publiy.utils.HistoryType;
import org.msrg.publiy.utils.LimittedSizeList;

public class BWLoadProfiler extends LoadProfiler {

	public BWLoadProfiler(int profilerCacheSize, int bucketRefreshRate) {
		super(profilerCacheSize, bucketRefreshRate);
	}

	@Override
	public ElementTotal<LimittedSizeList<ProfilerBucket>> getHistoryOfType(HistoryType hType, InetSocketAddress remote) {
		return super.getHistoryOfType(HistoryType.HIST_T_PUB_IN_BUTLAST, null);
	}
	
	@Override
	public void logMessage(HistoryType hType, InetSocketAddress remote, int bytes){
		super.logMessage(HistoryType.HIST_T_PUB_IN_BUTLAST, null, bytes);
	}
}
