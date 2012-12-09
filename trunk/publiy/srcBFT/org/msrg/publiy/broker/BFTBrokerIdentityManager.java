package org.msrg.publiy.broker;

import java.net.InetSocketAddress;

public class BFTBrokerIdentityManager extends BrokerIdentityManager {

	public BFTBrokerIdentityManager(InetSocketAddress localAddress, int delta) {
		super(localAddress, delta);
	}

}
