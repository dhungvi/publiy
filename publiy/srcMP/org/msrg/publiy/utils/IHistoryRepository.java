package org.msrg.publiy.utils;

import java.net.InetSocketAddress;

import org.msrg.publiy.pubsub.multipath.loadweights.ProfilerBucket;


public interface IHistoryRepository {
	
	public ElementTotal<LimittedSizeList<ProfilerBucket>> getHistoryOfType(HistoryType hType, InetSocketAddress remote);

	public void logMessage(HistoryType hType, InetSocketAddress remote, int size);

}
