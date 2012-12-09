package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.security.PrivateKey;

import org.msrg.publiy.utils.log.casuallogger.BFTDSALogger;

public interface IBFTIssuer {

	public InetSocketAddress getIssuerAddress();
	public PrivateKey getPrivateKey();

	public BFTDSALogger getBFTDSALogger();
	
}
