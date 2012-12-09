package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.IBFTBrokerShadow;

public class SimpleBFTIssuerProxyRepo implements IBFTIssuerProxyRepo {

	protected final IBFTBrokerShadow _brokerShadow;
	
	public SimpleBFTIssuerProxyRepo(BFTBrokerShadow brokerShadow) {
		_brokerShadow = brokerShadow;
		brokerShadow.setIssuerProxyRepo(this);
	}

	protected final Map<InetSocketAddress, IBFTIssuerProxy> _map = new HashMap<InetSocketAddress, IBFTIssuerProxy>();
	
	@Override
	public IBFTIssuerProxyRepo registerIssuerProxy(IBFTIssuerProxy iProxy) {
		// We allow below for testing!
//		if(_brokerShadow.getLocalAddress().equals(iProxy.getIssuerAddress()))
//			throw new IllegalStateException(_brokerShadow.getLocalAddress().toString() + " vs. " + iProxy.getIssuerAddress());
		
		if(_map.put(iProxy.getIssuerAddress(), iProxy) != null)
			throw new IllegalStateException();
		
		return this;
	}
	
	@Override
	public IBFTIssuerProxy getIssuerProxy(InetSocketAddress iAddr) {
		return _map.get(iAddr);
	}

	@Override
	public InetSocketAddress getLocalVerifierAddress() {
		return _brokerShadow.getLocalAddress();
	}
}