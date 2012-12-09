package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

import org.msrg.publiy.utils.log.casuallogger.BFTDSALogger;

public class PsuedoSequencePair extends SequencePair {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -1540493352664984839L;

	public PsuedoSequencePair(BFTDSALogger dsaLogger, byte[] digest, IBFTIssuerProxy iProxy, long epoch, int order, InetSocketAddress vAddr, int lastReceivedOrder, int lastDiscardedOrder, byte[] dsaNeverToBeVerified) {
		super(null, digest, iProxy, epoch, order, vAddr, lastReceivedOrder, lastDiscardedOrder, dsaNeverToBeVerified);
		
		if(dsaLogger != null)
			dsaLogger.dsaVerifyAvoided();
	}
	
	@Override
	public boolean verifyDSA(byte[] dsaToVerify, byte[] digest, IBFTIssuerProxy iProxy, long epoch, int order, InetSocketAddress vAddr, int lastReceivedOrder, int lastDiscardedOrder) {
		return true;
	}
	
	@Override
	public boolean isValid(byte[] digest) {
		throw new UnsupportedOperationException("How should I know! Haa?!");
	}

	// This is tricky, since super.equals checks obj's class type against SequencePair (i,e, superclass).
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public boolean canValidateDigest() {
		return false;
	}
}
