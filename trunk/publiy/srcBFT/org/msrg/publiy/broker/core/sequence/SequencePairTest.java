package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.msrg.raccoon.utils.BytesUtil;


import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.utils.security.dsa.DSAKeyGen;
import org.msrg.publiy.utils.security.dsa.DSAUtils;

import junit.framework.TestCase;

public class SequencePairTest extends TestCase {

	final int _delta = 1;
	
	SequencePair _sp_1_0_0_1_1_replica;
	SequencePair _sp_1_0_0_1_1;
	SequencePair _sp_1_0_0_1_2;
	SequencePair _sp_1_0_0_2_1;
	SequencePair _sp_1_0_1_1_1;
	SequencePair _sp_1_1_0_1_1;
	SequencePair _sp_1_1_1_1_1;
	SequencePair _sp_2_0_0_1_1;
	List<SequencePair> _sps = new LinkedList<SequencePair>();
	Map<InetSocketAddress, KeyPair> _allKeys = new HashMap<InetSocketAddress, KeyPair>();
	
	InetSocketAddress _i1 = new InetSocketAddress("127.0.0.1", 10001);
	InetSocketAddress _i2 = new InetSocketAddress("127.0.0.1", 10002);
	InetSocketAddress _v1 = new InetSocketAddress("127.0.0.1", 10003);
	InetSocketAddress _v2 = new InetSocketAddress("127.0.0.1", 10004);
	byte[] _digest1_replica = {11, 12, 13, 14};
	byte[] _digest1 = {11, 12, 13, 14};
	byte[] _digest2 = {21, 22, 23, 24};
	Map<SequencePair, byte[]> _digests = new HashMap<SequencePair, byte[]>();
	
	KeyPair _keypair1;
	KeyPair _keypair2;
	IBFTIssuer _issuer1;
	IBFTIssuer _issuer2;
	IBFTVerifier _verifier;
	IBFTBrokerShadow _vBrokerShadow;
	IBFTIssuerProxy _iProxy1;
	IBFTIssuerProxy _iProxy2;
	IBFTIssuerProxyRepo _iProxyRepo;
			
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
		
		_keypair1 = DSAKeyGen.getInstance().generateKeys(null, null);
		_keypair2 = DSAKeyGen.getInstance().generateKeys(null, null);
		_issuer1 = new SimpleBFTIssuer(_i1, _keypair1.getPrivate(), null);
		_issuer2 = new SimpleBFTIssuer(_i2, _keypair2.getPrivate(), null);
		_verifier = new SimpleBFTVerifier(_v1);
		_vBrokerShadow = new BFTBrokerShadow(NodeTypes.NODE_BROKER, _delta, _v1, null, null, "NONE", null);
		_iProxy1 = new SimpleBFTIssuerProxy(null, _verifier, _i1, _keypair1.getPublic());
		_iProxy2 = new SimpleBFTIssuerProxy(null, _verifier, _i2, _keypair2.getPublic());
		_iProxyRepo = new SimpleBFTIssuerProxyRepo((BFTBrokerShadow)_vBrokerShadow).registerIssuerProxy(_iProxy1).registerIssuerProxy(_iProxy2);

		_allKeys.put(_i1, _keypair1);
		_allKeys.put(_i2, _keypair2);
		
		_sp_1_0_0_1_1_replica = new SequencePair(_issuer1, 0L, 0, _v1, _digest1_replica, 10, 10);
		_digests.put(_sp_1_0_0_1_1_replica, _digest1_replica);
		
		_sp_1_0_0_1_1 = new SequencePair(_issuer1, 0, 0, _v1, _digest1, 10, 10);
		_digests.put(_sp_1_0_0_1_1, _digest1);
		
		_sp_1_0_0_1_2 = new SequencePair(_issuer1, 0, 0, _v1, _digest2);
		_digests.put(_sp_1_0_0_1_2, _digest2);
		
		_sp_1_0_0_2_1 = new SequencePair(_issuer1, 0, 0, _v2, _digest1);
		_digests.put(_sp_1_0_0_2_1, _digest1);
		
		_sp_1_0_1_1_1 = new SequencePair(_issuer1, 0, 1, _v1, _digest1);
		_digests.put(_sp_1_0_1_1_1, _digest1);
		
		_sp_1_1_0_1_1 = new SequencePair(_issuer1, 10, 0, _v1, _digest1);
		_digests.put(_sp_1_1_0_1_1, _digest1);
		
		_sp_1_1_1_1_1 = new SequencePair(_issuer1, 10, 1, _v1, _digest1);
		_digests.put(_sp_1_1_1_1_1, _digest1);
		
		_sp_2_0_0_1_1 = new SequencePair(_issuer2, 0, 0, _v1, _digest1);
		_digests.put(_sp_2_0_0_1_1, _digest1);
		
		_sps.add(_sp_1_0_0_1_1_replica);
		_sps.add(_sp_1_1_0_1_1);
		_sps.add(_sp_1_0_0_1_1);
		_sps.add(_sp_1_0_0_1_2);
		_sps.add(_sp_1_0_0_2_1);
		_sps.add(_sp_2_0_0_1_1);
	}
	
	public void testEquals() {
		assertTrue(_sp_1_0_0_1_1.equals(_sp_1_0_0_1_1_replica));
		assertTrue(_sp_1_0_0_1_1.equals(_sp_1_0_0_1_1_replica));
		
		assertTrue(_sp_1_0_0_1_1.equals(_sp_1_0_0_1_1));
		assertFalse(_sp_1_0_0_1_1.equals(_sp_1_0_0_1_2));
		assertFalse(_sp_1_0_0_1_1.equals(_sp_1_0_0_2_1));
		assertFalse(_sp_1_0_0_1_1.equals(_sp_1_0_1_1_1));
		assertFalse(_sp_1_0_0_1_1.equals(_sp_1_1_0_1_1));
		assertFalse(_sp_1_0_0_1_1.equals(_sp_2_0_0_1_1));
	}
	
	public void testComparison() {
		assertTrue(_sp_1_0_1_1_1.succeeds(_sp_1_0_0_1_1));
		assertTrue(_sp_1_1_0_1_1.succeeds(_sp_1_0_1_1_1));
		assertTrue(_sp_1_1_1_1_1.succeeds(_sp_1_0_1_1_1));
		assertFalse(_sp_1_0_0_1_1.succeeds(_sp_1_0_1_1_1));
		assertFalse(_sp_1_0_1_1_1.succeeds(_sp_1_1_0_1_1));
		assertFalse(_sp_1_0_1_1_1.succeeds(_sp_1_1_1_1_1));
		
		assertFalse(_sp_1_0_0_1_1.succeeds(_sp_1_0_0_1_1));
		assertFalse(_sp_1_0_1_1_1.succeeds(_sp_1_0_1_1_1));
		assertFalse(_sp_1_1_1_1_1.succeeds(_sp_1_1_1_1_1));
		
		assertTrue(_sp_1_0_0_1_1.succeedsOrEquals(_sp_1_0_0_1_1));
		assertTrue(_sp_1_0_1_1_1.succeedsOrEquals(_sp_1_0_1_1_1));
		assertTrue(_sp_1_1_1_1_1.succeedsOrEquals(_sp_1_1_1_1_1));
		
		assertFalse(_sp_1_0_0_1_1.succeeds(_sp_1_0_0_1_1));
		assertFalse(_sp_1_0_0_1_1.succeeds(_sp_1_0_0_1_2));
	}
	
	public void testEncodeAndDecode() {
 		for(SequencePair sp : _sps) {
 			byte[] spInArray = sp.encode();
			PrivateKey prikey =_allKeys.get(sp.getIssuerAddress()).getPrivate();
			byte[] sign = DSAUtils.getInstance().signData(sp._data, prikey);
			SequencePair dec = SequencePair.decode(null, _iProxyRepo, spInArray, 0);
			assertEquals(sp, dec);
			assertEquals(dec, sp);
			
			if(dec.canValidateDigest())
				assertTrue(dec.isValid(_digests.get(sp)));
			BytesUtil.compareByteArray(sign, dec._dsa);
			assertNotNull(dec._dsa);
		}
	}

	public void testEncodeAndDecodeWithLastReceivedDiscardedInfo() {
 		for(SequencePair sp : _sps) {
			byte[] spInArray = sp.encode();
			PrivateKey prikey =_allKeys.get(sp.getIssuerAddress()).getPrivate();
			byte[] sign = DSAUtils.getInstance().signData(sp._data, prikey);
			SequencePair dec = SequencePair.decode(null, _iProxyRepo, spInArray, 0);
			assertEquals(sp, dec);
			assertEquals(dec, sp);
			if(dec.canValidateDigest())
				assertTrue(dec.isValid(_digests.get(sp)));
			BytesUtil.compareByteArray(sign, dec._dsa);
			assertNotNull(dec._dsa);
		}
	}
	
	public void testEncodeAndDecodeFail() {
		SequencePairForTest sp1 = new SequencePairForTest(_issuer1, 100, 200, _v1, _digest1);
		SequencePairForTest sp2 = new SequencePairForTest(_issuer2, 100, 200, _v1, _digest1);
		
		sp1.loadDSA(sp2._dsa);
		byte[] spInArray = sp1.encode();
		SequencePair dec = SequencePair.decode(null, _iProxyRepo, spInArray, 0);
		assertFalse(dec.isValid(_digest1));
		assertNull(dec._dsa);

		byte[] newDSA = {1, 2, 3};
		sp1.loadDSA(newDSA);
		spInArray = sp1.encode();
		SequencePair decFail = SequencePair.decode(null, _iProxyRepo, spInArray, 0);
		assertFalse(decFail.isValid(_digest1));
		assertNull(decFail._dsa);
	}
}

class SequencePairForTest extends SequencePair {

	/**
	 * Auto generated.
	 */
	private static final long serialVersionUID = -2842379593405696472L;

	public SequencePairForTest(byte[] digest, IBFTIssuerProxy iProxy,
			long epoch, int order, InetSocketAddress vAddr, int lastReceivedOrder, int lastDiscardedOrder,
			byte[] dsaToBeVerified) {
		super(null, digest, iProxy, epoch, order, vAddr, lastReceivedOrder, lastDiscardedOrder, dsaToBeVerified);
	}
	
	protected SequencePairForTest(IBFTIssuer issuer, long epoch, int order,
			InetSocketAddress vAddr, byte[] digest) {
		super(issuer, epoch, order, vAddr, digest);
	}
	
	public SequencePairForTest(IBFTIssuer issuer, long epoch, int order,
			InetSocketAddress vAddr, IBFTDigestable digestable) {
		super(issuer, epoch, order, vAddr, digestable);
	}
	
	public SequencePairForTest(IBFTIssuerProxyRepo iProxyRepo, byte[] digest,
			InetSocketAddress iAddr, long epoch, int order,
			InetSocketAddress vAddr, int lastReceivedOrder, int lastDiscardedOrder, byte[] dsaToBeVerified) {
		super(iProxyRepo, digest, iAddr, epoch, order, vAddr, lastReceivedOrder, lastDiscardedOrder, dsaToBeVerified);
	}

	public SequencePairForTest(IBFTIssuerProxyRepo iProxyRepo, byte[] data,
			int offset) {
		super(iProxyRepo, data, offset);
	}

	protected void loadDSA(byte[] newDSA) {
		_dsa = newDSA;
		_dataAndDsa = null;
		encode();
	}
}