package org.msrg.publiy.broker.core;

import java.net.InetSocketAddress;

public interface IConnectionManagerJoin extends IConnectionManager {
	
	public void join(InetSocketAddress remote);
	
}
