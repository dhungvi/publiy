package org.msrg.publiy.pubsub.core.overlaymanager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;

import org.msrg.raccoon.utils.BytesUtil;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.BFTPublication;
import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.PropertyGrabber;


import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBFTSuspectedRepo;
import org.msrg.publiy.broker.SimpleBFTSuspectedRepo;
import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.IBFTVerifier;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.SequencePair;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxyRepo;
import org.msrg.publiy.broker.core.sequence.SimpleBFTVerifier;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import junit.framework.TestCase;

public class BFTOverlayNodeTest extends TestCase {

	final int _delta = 3;
	IBFTBrokerShadow _bftBrokerShadow;
	IBFTOverlayNode _bftNode;
	IBFTOverlayManager _bftOverlayManager;
	
	protected final String _tempdir = "." + FileUtils.separatorChar + "temp-" + getClass().getCanonicalName();
	protected final String _keysdir = _tempdir + FileUtils.separatorChar + "keys";
	
	protected final int _numNodes = 10;
	protected final String _ipstr = "127.0.0.1";
	protected final String _nodeNamePrefix = "n-";
	protected final int _portOffset = 1000;

	protected final InetSocketAddress _sAddr = new InetSocketAddress(_ipstr, _portOffset + 0);
	protected final InetSocketAddress _lAddr = new InetSocketAddress(_ipstr, _portOffset + 1);
	protected final InetSocketAddress _vAddr = new InetSocketAddress(_ipstr, _portOffset + 2);
	protected final BFTPublication _pub1 = new BFTPublication(_sAddr).addPredicate("attr1", 10).addStringPredicate("sattr1", "value100");
	protected final BFTPublication _pub2 = new BFTPublication(_sAddr).addPredicate("attr1", 10).addStringPredicate("sattr1", "value101");

	protected IBFTSuspectedRepo _suspectedRepo;
	protected BrokerIdentityManager _idMan;
	protected LocalSequencer _localSequencer;
	
	@Override
	public void setUp() throws IOException {
		BrokerInternalTimer.start();
		assertTrue(FileUtils.prepareTopologyDirectory(_tempdir, _keysdir, _numNodes, _nodeNamePrefix, _ipstr, _portOffset));
		String identityfilename = _tempdir + FileUtils.separatorChar + "identityfile";
		Properties arguments = new Properties();
		arguments.setProperty(PropertyGrabber.PROPERTY_KEYS_DIR, _keysdir);
		_bftBrokerShadow =
				(IBFTBrokerShadow) new BFTBrokerShadow(
						NodeTypes.NODE_BROKER, _delta, _lAddr, _tempdir, _keysdir, identityfilename, arguments);
		_localSequencer = _bftBrokerShadow.getLocalSequencer();
		
		new SimpleBFTIssuerProxyRepo((BFTBrokerShadow) _bftBrokerShadow);
		_suspectedRepo = new SimpleBFTSuspectedRepo((BFTBrokerShadow)_bftBrokerShadow);
		
		_bftOverlayManager = new BFTOverlayManager(_bftBrokerShadow);
		_bftNode = new BFTOverlayNode(_localSequencer, _vAddr, NodeTypes.NODE_BROKER, _bftOverlayManager);
	}
	
	@Override
	public void tearDown() {
		assertTrue(FileUtils.deleteDirectory(_tempdir));
	}
	
	public void testSuspectedReasons() {
		IBFTOverlayManager bftOverlayMan = _bftOverlayManager;//new BFTOverlayManager(_bftBrokerShadow);
		IBFTOverlayNode node1 = new BFTOverlayNode(_localSequencer, _vAddr, NodeTypes.NODE_BROKER, bftOverlayMan);
		assertFalse(node1.isSuspected());
		_suspectedRepo.suspect(new BFTSuspecionReason(false, true, node1, null, "Some reason 1"));
		assertTrue(node1.isSuspected());
		List<BFTSuspecionReason> reasons = _suspectedRepo.getReasons(node1);
		assertNotNull(reasons);
		assertEquals(1, reasons.size());
		
		_suspectedRepo.suspect(new BFTSuspecionReason(false, true, node1, null, "Some reason 2"));
		reasons = _suspectedRepo.getReasons(node1);
		assertNotNull(reasons);
		assertEquals(2, reasons.size());
		assertEquals("Some reason 1", reasons.get(0)._reason);
		assertEquals("Some reason 2", reasons.get(1)._reason);
	}
	
	public void testVerify() {
		IBFTOverlayManager bftOverlayMan = _bftOverlayManager; //new BFTOverlayManager(_bftBrokerShadow);
		IBFTOverlayNode node1 = new BFTOverlayNode(_localSequencer, _vAddr, NodeTypes.NODE_BROKER, bftOverlayMan);
		assertTrue(node1.getVerifierAddress().equals(_vAddr));
		assertNull(node1.getLastLocallySeenSequencePairFromProxy());
		SequencePair sp1 = node1.issueNextSequencePair(_pub1, null);
		assertTrue(BytesUtil.compareByteArray(sp1.getdigest(), _pub1.getDigest()));
		assertEquals(_lAddr, sp1.getIssuerAddress());
		assertEquals(_vAddr, sp1.getVerifierAddress());
		IBFTVerifier _verifier = new SimpleBFTVerifier(_vAddr);
		IBFTIssuerProxy _iProxy = new SimpleBFTIssuerProxy(null, _verifier, _lAddr, _bftBrokerShadow.getKeyManager());
//		assertTrue(sp1.verifyDSA(sp1.sign(_brokerShadow.getKeyManager().getPrivateKey(_lAddr)), _pub1, _iProxy, _verifier, 0L, 1));
		
		SequencePair sp2 = node1.issueNextSequencePair(_pub1, null);
		assertTrue(BytesUtil.compareByteArray(sp1.getdigest(), _pub1.getDigest()));
		assertEquals(_lAddr, sp2.getIssuerAddress());
		assertEquals(_vAddr, sp2.getVerifierAddress());
//		assertTrue(sp2.verifyDSA(sp2.sign(_brokerShadow.getKeyManager().getPrivateKey(_lAddr)), _pub2, _iProxy, _verifier, 0L, 1));
	}
}