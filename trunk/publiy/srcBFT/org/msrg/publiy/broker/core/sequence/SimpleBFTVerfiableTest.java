package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.List;

import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.utils.security.dsa.DSAKeyGen;

import junit.framework.TestCase;

public class SimpleBFTVerfiableTest extends TestCase {

	final int _delta = 1;
	IBFTVerifiable _verifiable;
	byte[] _data = {1, 2, 3, 4};
	InetSocketAddress _sAddr = new InetSocketAddress("127.0.0.1", 1000);
	InetSocketAddress _iAddr = new InetSocketAddress("127.0.0.1", 2000);
	InetSocketAddress _vAddr = new InetSocketAddress("127.0.0.1", 3000);
	KeyPair _keypair;
	IBFTVerifier _verifier;
	IBFTIssuer _issuer;
	IBFTIssuerProxy _iProxy;
	IBFTBrokerShadow _vBrokerShadow;
	IBFTIssuerProxyRepo _iProxyRepo;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
		
		_keypair = DSAKeyGen.getInstance().generateKeys(null, null);
		_verifier = new SimpleBFTVerifier(_vAddr);
		_issuer = new SimpleBFTIssuer(_iAddr, _keypair.getPrivate(), null);
		_iProxy = new SimpleBFTIssuerProxy(null, _verifier, _iAddr, _keypair.getPublic());
		_vBrokerShadow = new BFTBrokerShadow(NodeTypes.NODE_BROKER, _delta, _vAddr, null, null, "NONE", null);
		_iProxyRepo = new SimpleBFTIssuerProxyRepo((BFTBrokerShadow)_vBrokerShadow).registerIssuerProxy(_iProxy);

		IBFTDigestable digestable = new SimpleBFTDigestable(_sAddr, _data);
		_verifiable = new SimpleBFTVerifiable(digestable);
	}
	
	public void testSPAdd() {
		SequencePair sp0 = _verifiable.addSequencePair(_issuer, _vAddr, 0, 0);
		List<SequencePair> lsp = _verifiable.getSequencePairs(_iAddr);
		assertTrue(lsp.size() == 1);
		assertTrue(lsp.get(0) == sp0);
		SequencePair sp = _verifiable.getSequencePair(_iAddr, _vAddr);
		assertEquals(sp, sp0);
		
		SequencePair sp1 = _verifiable.addSequencePair(_issuer, _vAddr, 0, 1);
		lsp = _verifiable.getSequencePairs(_iAddr);
		assertTrue(lsp.size() == 1);
		assertTrue(lsp.get(0) == sp1);

		sp = _verifiable.getSequencePair(_iAddr, _vAddr);
		assertEquals(sp, sp1);
	}
	
	final int maxNumberOfSequencePairs = 500;
	public void testEncodeAndDecode() {
		for(int i=0 ; i<maxNumberOfSequencePairs ; i++)
			_verifiable.addSequencePair(_issuer, _vAddr, 2, i+1);
		
		byte[] data = ((SimpleBFTVerifiable)_verifiable).encode();
		IBFTVerifiable decoded = SimpleBFTVerifiable.decode(null, _iProxyRepo, data, 0);
		assertNotNull(decoded);
		assertEquals(decoded, _verifiable);
		assertFalse(decoded == _verifiable);
		
		data = ((SimpleBFTVerifiable)_verifiable).encode();
		decoded = SimpleBFTVerifiable.decode(null, _iProxyRepo, data, 0);
		assertNotNull(decoded);
		assertEquals(decoded, _verifiable);
		assertFalse(decoded == _verifiable);
	}
}
