package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

public interface IBFTIssuerProxyRepo {

	public InetSocketAddress getLocalVerifierAddress();
	public IBFTIssuerProxyRepo registerIssuerProxy(IBFTIssuerProxy iProxy);
	public IBFTIssuerProxy getIssuerProxy(InetSocketAddress iAddr);
	
}
