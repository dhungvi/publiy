package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.security.KeyPair;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.utils.security.dsa.DSAKeyGen;

import junit.framework.TestCase;

public class SimpleBFTDackVerifierProxyTest extends TestCase {
	
	// Original Sequence Pair issuer (NOTE: the roles of issuer and verifier for SP and dack flip)
	protected final InetSocketAddress _iAddr = new InetSocketAddress("127.0.0.1", 1000);
	protected final InetSocketAddress _vAddr = new InetSocketAddress("127.0.0.1", 1001);
	protected final InetSocketAddress _dackIssuerAddr = _vAddr;
	protected final InetSocketAddress _dackVerifierAddr = _iAddr;

	KeyPair _keypair1 = DSAKeyGen.getInstance().generateKeys(null, null);
	protected IBFTIssuer _spIssuer = new SimpleBFTIssuer(_iAddr, _keypair1.getPrivate(), null);
	protected IBFTDackVerifier _dackVerifier = new SimpleBFTDackVerifier(_dackIssuerAddr, _dackVerifierAddr);
	
	// DACK issuer and verifier
	KeyPair _keypair2 = DSAKeyGen.getInstance().generateKeys(null, null);
	protected final IBFTIssuer _dackIssuer = new SimpleBFTIssuer(_dackIssuerAddr, _keypair2.getPrivate(), null);
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
	}

	public void testDackReceived() {
		throw new UnsupportedOperationException();
//		SequencePair sp1 = new SequencePair(_spIssuer, 100, 10, _vAddr, 0, 0);
//		SequencePair sp2 = new SequencePair(_spIssuer, 100, 10, _vAddr, 10, 10);
//		SequencePair sp3 = new SequencePair(_spIssuer, 100, 10, _vAddr, 100, 100);
//
//		assertTrue(_dackVerifier.isReceivedByVerifier(sp1));
//		assertFalse(_dackVerifier.isReceivedByVerifier(sp2));
//		assertFalse(_dackVerifier.isReceivedByVerifier(sp3));
//		assertTrue(_dackVerifier.isDiscardedByVerifier(sp1));
//		assertFalse(_dackVerifier.isDiscardedByVerifier(sp2));
//		assertFalse(_dackVerifier.isDiscardedByVerifier(sp3));
//		
//		_dackVerifier.dackReceived(sp1);
//		assertTrue(_dackVerifier.isReceivedByVerifier(sp1));
//		assertTrue(_dackVerifier.isDiscardedByVerifier(sp1));
//		assertFalse(_dackVerifier.isReceivedByVerifier(sp2));
//		assertTrue(_dackVerifier.isDiscardedByVerifier(sp1));
//		assertFalse(_dackVerifier.isDiscardedByVerifier(sp2));
//		assertFalse(_dackVerifier.isDiscardedByVerifier(sp3));
//
//		_dackVerifier.dackReceived(sp2);
//		assertTrue(_dackVerifier.isReceivedByVerifier(sp1));
//		assertTrue(_dackVerifier.isReceivedByVerifier(sp2));
//		assertFalse(_dackVerifier.isReceivedByVerifier(sp3));
//		assertTrue(_dackVerifier.isDiscardedByVerifier(sp1));
//		assertTrue(_dackVerifier.isDiscardedByVerifier(sp2));
//		assertFalse(_dackVerifier.isDiscardedByVerifier(sp3));
//
//		_dackVerifier.dackReceived(sp3);
//		assertTrue(_dackVerifier.isReceivedByVerifier(sp1));
//		assertTrue(_dackVerifier.isReceivedByVerifier(sp2));
//		assertTrue(_dackVerifier.isReceivedByVerifier(sp3));
//		assertTrue(_dackVerifier.isDiscardedByVerifier(sp1));
//		assertTrue(_dackVerifier.isDiscardedByVerifier(sp2));
//		assertTrue(_dackVerifier.isDiscardedByVerifier(sp3));
	}
}
