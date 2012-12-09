package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

public class SimpleBFTVerifier implements IBFTVerifier {

	protected final InetSocketAddress _vAddr;
	
	public SimpleBFTVerifier(InetSocketAddress vAddr) {
		_vAddr = vAddr;
	}
	
	@Override
	public InetSocketAddress getVerifierAddress() {
		return _vAddr;
	}

}
