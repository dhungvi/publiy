package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.security.PrivateKey;

import org.msrg.publiy.utils.log.casuallogger.BFTDSALogger;

public class SimpleBFTIssuer implements IBFTIssuer {

	protected final InetSocketAddress _iAddr;
	protected final PrivateKey _prikey;
	protected final BFTDSALogger _bftDSALogger;
	
	public SimpleBFTIssuer(InetSocketAddress iAddr, PrivateKey prikey, BFTDSALogger bftDSALogger) {
		_iAddr = iAddr;
		_prikey = prikey;
		_bftDSALogger = bftDSALogger;
	}
	
	@Override
	public InetSocketAddress getIssuerAddress() {
		return _iAddr;
	}

	@Override
	public PrivateKey getPrivateKey() {
		return _prikey;
	}

	@Override
	public BFTDSALogger getBFTDSALogger() {
		return _bftDSALogger;
	}
}
