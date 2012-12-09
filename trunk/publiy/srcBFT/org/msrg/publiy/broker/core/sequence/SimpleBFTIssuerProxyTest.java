package org.msrg.publiy.broker.core.sequence;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;


import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.security.keys.KeyManager;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.BFTPublication;
import org.msrg.publiy.utils.FileUtils;

import junit.framework.TestCase;

public class SimpleBFTIssuerProxyTest extends TestCase {
	
	protected final String _tempdir = "." + FileUtils.separatorChar + "temp-" + getClass().getCanonicalName();
	protected final String _keydir = _tempdir +  FileUtils.separatorChar + "keys";
	protected final int _delta = 3;
	
	protected KeyManager _keyman;
	protected BrokerIdentityManager _idMan;
	protected final int _portOffset = 1000;
	
	protected InetSocketAddress _sAddr = new InetSocketAddress("127.0.0.1", _portOffset + 0);
	protected InetSocketAddress _iAddr = new InetSocketAddress("127.0.0.1", _portOffset + 1);
	protected InetSocketAddress _vAddr = new InetSocketAddress("127.0.0.1", _portOffset + 2);

	protected IBFTIssuerProxy _iProxy;
	protected IBFTDigestable _pub1 = (IBFTDigestable) new BFTPublication(_sAddr).addPredicate("att1", 10).addStringPredicate("satt1", "hi");
	protected IBFTDigestable _pub2 = (IBFTDigestable) new BFTPublication(_sAddr).addPredicate("att2", 20).addStringPredicate("satt2", "hi");
	protected IBFTDigestable _pub3 = (IBFTDigestable) new BFTPublication(_sAddr).addPredicate("att3", 30).addStringPredicate("satt3", "hi");
	protected IBFTDigestable _pub4 = (IBFTDigestable) new BFTPublication(_sAddr).addPredicate("att4", 40).addStringPredicate("satt4", "hi");

	protected IBFTVerifier _verifier = new SimpleBFTVerifier(_vAddr);
	protected IBFTIssuer _issuer;
	protected IBFTIssuerProxyRepo _iProxyRepo;
	
	@Override
	public void setUp() throws IOException {
		BrokerInternalTimer.start();

		assertTrue(FileUtils.prepareTopologyDirectory(_tempdir, _keydir, 3, "n-", "127.0.0.1", _portOffset));
		String identityfile = _tempdir + FileUtils.separatorChar + "identityfile";
		_idMan = new BrokerIdentityManager(_sAddr, _delta);
		_idMan.loadIdFile(identityfile);
		_keyman = new KeyManager(_keydir, _idMan);

		_issuer  = new SimpleBFTIssuer(_iAddr, _keyman.getPrivateKey(_iAddr), null);
		_iProxy = new SimpleBFTIssuerProxy(_idMan, new SimpleBFTVerifier(_vAddr), _iAddr, _keyman.getPublicKey(_iAddr));
		IBFTBrokerShadow _vBrokerShadow = new BFTBrokerShadow(NodeTypes.NODE_BROKER, _delta, _vAddr, null, null, "NONE", null);
		_iProxyRepo = new SimpleBFTIssuerProxyRepo((BFTBrokerShadow)_vBrokerShadow).registerIssuerProxy(_iProxy);
	}
	
	public void testGeneric() {
		assertEquals(_iProxy.getIssuerAddress(), _iAddr);
		assertEquals(_iProxy.getPublicKey(), _keyman.getPublicKey(_iAddr));
	}
	
	public void testNotTagged() {
		List<BFTSuspecionReason> reasons = new LinkedList<BFTSuspecionReason>();
		SimpleBFTVerifiable verifiable1 = new SimpleBFTVerifiable(_pub1);
		assertEquals(SequencePairVerifyResult.SEQ_PAIR_MISSING, _iProxy.verifySuccession(false, true, null, verifiable1, reasons));
		assertNull(_iProxy.getLastLocallySeenSequencePairFromProxy());
	}
	
	public void testNoSPSeenBefore() {
		List<BFTSuspecionReason> reasons = new LinkedList<BFTSuspecionReason>();
		SimpleBFTVerifiable verifiable1 = new SimpleBFTVerifiable(_pub1);
		SequencePair sp1 = verifiable1.addSequencePair(_issuer, _vAddr, 1, 1);
		assertEquals(SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS, _iProxy.verifySuccession(false, true, null, verifiable1, reasons));
		assertEquals(sp1, _iProxy.getLastLocallySeenSequencePairFromProxy());
	}

	public void testSucceeds() {
		List<BFTSuspecionReason> reasons = new LinkedList<BFTSuspecionReason>();
		SimpleBFTVerifiable verifiable1 = new SimpleBFTVerifiable(_pub1);
		SequencePair sp1 = verifiable1.addSequencePair(_issuer, _vAddr, 1, 1);
		assertEquals(SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS, _iProxy.verifySuccession(false, true, null, verifiable1, reasons));
		assertEquals(sp1, _iProxy.getLastLocallySeenSequencePairFromProxy());

		SimpleBFTVerifiable verifiable2 = new SimpleBFTVerifiable(_pub2);
		SequencePair sp2 = verifiable2.addSequencePair(_issuer, _vAddr, 1, 2);
		assertEquals(SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS, _iProxy.verifySuccession(false, true, null, verifiable2, reasons));
		assertEquals(sp2, _iProxy.getLastLocallySeenSequencePairFromProxy());

		SimpleBFTVerifiable verifiable4 = new SimpleBFTVerifiable(_pub4);
		SequencePair sp4 = verifiable4.addSequencePair(_issuer, _vAddr, 1, 4);
		assertEquals(SequencePairVerifyResult.SEQ_PAIR_JUMPS, _iProxy.verifySuccession(false, true, null, verifiable4, reasons));
		assertEquals(sp2, _iProxy.getLastLocallySeenSequencePairFromProxy());

		SimpleBFTVerifiable verifiable3 = new SimpleBFTVerifiable(_pub3);
		SequencePair sp3 = verifiable3.addSequencePair(_issuer, _vAddr, 1, 3);
		assertEquals(SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS, _iProxy.verifySuccession(false, true, null, verifiable3, reasons));
		assertEquals(sp3, _iProxy.getLastLocallySeenSequencePairFromProxy());

		assertEquals(SequencePairVerifyResult.SEQ_PAIR_EQUALS, _iProxy.verifySuccession(false, true, null, verifiable3, reasons));
		assertEquals(sp3, _iProxy.getLastLocallySeenSequencePairFromProxy());

		assertEquals(SequencePairVerifyResult.SEQ_PAIR_PRECEDES, _iProxy.verifySuccession(false, true, null, verifiable1, reasons));
		assertEquals(sp3, _iProxy.getLastLocallySeenSequencePairFromProxy());
		
		assertEquals(SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS, _iProxy.verifySuccession(false, true, null, verifiable4, reasons));
		assertEquals(sp4, _iProxy.getLastLocallySeenSequencePairFromProxy());
	}
}