package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT_Dack;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;
import org.msrg.publiy.broker.core.sequence.IBFTDigestable;
import org.msrg.publiy.broker.core.sequence.IBFTVerifiable;
import org.msrg.publiy.broker.core.sequence.IBFTVerifierProxy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.SequencePair;
import org.msrg.publiy.broker.core.sequence.SimpleBFTDigestable;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxyRepo;
import org.msrg.publiy.broker.core.sequence.SimpleBFTVerifiable;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.PropertyGrabber;

public class BFTOverlayManagerTest extends TestCase {
	
	protected final static String TOP_FILENAME_DELTA2 = "." + FileUtils.separatorChar + "misc" + FileUtils.separatorChar + "testdata" + FileUtils.separatorChar + "tops" + FileUtils.separatorChar + "OverlayManagerTest_1.top";
	protected final static String TOP_FILENAME_DELTA1 = "." + FileUtils.separatorChar + "misc" + FileUtils.separatorChar + "testdata" + FileUtils.separatorChar + "tops" + FileUtils.separatorChar + "OverlayManagerTest_1_Smaller.top";

	protected InetSocketAddress[] _allAddresses;

	protected final String _tempdir = "." + FileUtils.separatorChar + "temp-" + getClass().getCanonicalName();
	protected final String _keysdir = _tempdir + FileUtils.separatorChar + "keys";
	
	protected final int _numNodes = 10;
	protected final String _ipstr = "127.0.0.1";
	protected final String _nodeNamePrefix = "n-";
	protected final int _portOffset = 2000;
	
	@Override
	public void setUp() throws Exception {
		BrokerInternalTimer.start();
		_allAddresses = new InetSocketAddress[21];//FileCommons.getAllInetAddressesFromLines(localSequencer, TOP_FILENAME);
		for(int i=0 ; i<_allAddresses.length ; i++)
			_allAddresses[i] = new InetSocketAddress("127.0.0.1", 2000 + i);
		assertTrue(FileUtils.prepareTopologyDirectory(_tempdir, _keysdir, _numNodes, _nodeNamePrefix, _ipstr, _portOffset));
	}
	
	protected IBFTOverlayManager initOverlayManager(int delta, InetSocketAddress iAddr, String topologyFilename) {
		LocalSequencer localSequencer = LocalSequencer.init(null, iAddr);
		Properties arguments = new Properties();
		arguments.setProperty(PropertyGrabber.PROPERTY_KEYS_DIR, _keysdir);
		BFTBrokerShadow brokerShadow = new BFTBrokerShadow_ForTest(NodeTypes.NODE_BROKER, delta, 3*delta+1, iAddr, _tempdir, _tempdir, _tempdir + FileUtils.separatorChar + "identityfile", arguments);
		new SimpleBFTIssuerProxyRepo(brokerShadow);

		TRecovery_Join[] trjs = ConnectionManagerFactory.readRecoveryTopology(localSequencer, topologyFilename);
		IBFTOverlayManager bftOverlayMananger = (IBFTOverlayManager) createNewOverlayManager(brokerShadow);
		bftOverlayMananger.applyAllJoinSummary(trjs);
		return bftOverlayMananger;
	}
	
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		assertTrue(FileUtils.deleteDirectory(_tempdir));
	}

	protected IOverlayManager createNewOverlayManager(BFTBrokerShadow bftBrokerShadow) {
		return new BFTOverlayManager(bftBrokerShadow);
	}
	
	public void testComputeDownstreamVerifersSequencePairs() {
		InetSocketAddress _iAddr = new InetSocketAddress(_ipstr, _portOffset + 0);
		InetSocketAddress _sAddr = new InetSocketAddress(_ipstr, _portOffset + 3);
		IBFTOverlayManager _bftOverlayManager = initOverlayManager(2, _iAddr, TOP_FILENAME_DELTA2);
		
		byte[] data = {1, 2, 3, 4, 5};
		IBFTDigestable digestable = new SimpleBFTDigestable(_sAddr, data);
		Set<InetSocketAddress> anchorSet = new HashSet<InetSocketAddress>();
		
		IBFTVerifiable verifiable = new SimpleBFTVerifiable(digestable);

		// Anchor at distance 1 and no downstream node
		anchorSet.add(_allAddresses[12]);
		Map<IBFTVerifierProxy, SequencePair> sequencePairs2012 =
				_bftOverlayManager.computeIntermediateVerifersSequencePairs(_iAddr, true, false, verifiable, anchorSet);
		assertEquals(1, sequencePairs2012.size());
		assertTrue(sequencePairs2012.containsValue(
				new SequencePair(_bftOverlayManager, 0, 1, _allAddresses[12], digestable)));
		anchorSet.clear();
		
		// Anchor at distance 1
		anchorSet.add(_allAddresses[9]);
		Map<IBFTVerifierProxy, SequencePair> sequencePairs2009 =
				_bftOverlayManager.computeIntermediateVerifersSequencePairs(_iAddr, true, false, verifiable, anchorSet);
		assertEquals(1, sequencePairs2009.size());
		assertTrue(sequencePairs2009.containsValue(
				new SequencePair(_bftOverlayManager, 0, 1, _allAddresses[9], digestable)));
		anchorSet.clear();
		
		// Anchor at distance 2
		anchorSet.add(_allAddresses[10]);
		Map<IBFTVerifierProxy, SequencePair> sequencePairs2010 =
				_bftOverlayManager.computeIntermediateVerifersSequencePairs(_iAddr, true, false, verifiable, anchorSet);
		assertEquals(2, sequencePairs2010.size());
		assertTrue(sequencePairs2010.containsValue(
				new SequencePair(_bftOverlayManager, 0, 2, _allAddresses[9], digestable)));
		assertTrue(sequencePairs2010.containsValue(
				new SequencePair(_bftOverlayManager, 0, 1, _allAddresses[10], digestable)));
		anchorSet.clear();
		
		// Anchor at distance 5
		anchorSet.add(_allAddresses[5]);
		Map<IBFTVerifierProxy, SequencePair> sequencePairs2005 =
				_bftOverlayManager.computeIntermediateVerifersSequencePairs(_iAddr, true, false, verifiable, anchorSet);
		assertEquals(5, sequencePairs2005.size());
		assertTrue(sequencePairs2005.containsValue(
				new SequencePair(_bftOverlayManager, 0, 1, _allAddresses[5], digestable)));
		assertTrue(sequencePairs2005.containsValue(
				new SequencePair(_bftOverlayManager, 0, 1, _allAddresses[2], digestable)));
		assertTrue(sequencePairs2005.containsValue(
				new SequencePair(_bftOverlayManager, 0, 1, _allAddresses[11], digestable)));
		assertTrue(sequencePairs2005.containsValue(
				new SequencePair(_bftOverlayManager, 0, 2, _allAddresses[10], digestable)));
		assertTrue(sequencePairs2005.containsValue(
				new SequencePair(_bftOverlayManager, 0, 3, _allAddresses[9], digestable)));
		anchorSet.clear();
		
		// Multiple anchors non-overlapping paths
		anchorSet.add(_allAddresses[10]);
		anchorSet.add(_allAddresses[3]);
		Map<IBFTVerifierProxy, SequencePair> sequencePairs2010_2003 =
				_bftOverlayManager.computeIntermediateVerifersSequencePairs(_iAddr, true, false, verifiable, anchorSet);
		assertEquals(3, sequencePairs2010_2003.size());
		assertTrue(sequencePairs2010_2003.containsValue(
				new SequencePair(_bftOverlayManager, 0, 1, _allAddresses[3], digestable)));
		assertTrue(sequencePairs2010_2003.containsValue(
				new SequencePair(_bftOverlayManager, 0, 3, _allAddresses[10], digestable)));
		assertTrue(sequencePairs2010_2003.containsValue(
				new SequencePair(_bftOverlayManager, 0, 4, _allAddresses[9], digestable)));
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[7], sequencePairs2010_2003).size());
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[12], sequencePairs2010_2003).size());
		assertEquals(1, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[3], sequencePairs2010_2003).size());
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[15], sequencePairs2010_2003).size());
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[2], sequencePairs2010_2003).size());
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[6], sequencePairs2010_2003).size());
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[19], sequencePairs2010_2003).size());
		assertEquals(1, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[10], sequencePairs2010_2003).size());
		assertEquals(2, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[9], sequencePairs2010_2003).size());
		anchorSet.clear();
		
		// Multiple anchors with overlapping paths
		anchorSet.add(_allAddresses[10]);
		anchorSet.add(_allAddresses[19]);
		Map<IBFTVerifierProxy, SequencePair> sequencePairs2010_2019 =
				_bftOverlayManager.computeIntermediateVerifersSequencePairs(_iAddr, true, false, verifiable, anchorSet);
		assertEquals(3, sequencePairs2010_2019.size());
		assertTrue(sequencePairs2010_2019.containsValue(
				new SequencePair(_bftOverlayManager, 0, 1, _allAddresses[19], digestable)));
		assertTrue(sequencePairs2010_2019.containsValue(
				new SequencePair(_bftOverlayManager, 0, 4, _allAddresses[10], digestable)));
		assertTrue(sequencePairs2010_2019.containsValue(
				new SequencePair(_bftOverlayManager, 0, 5, _allAddresses[9], digestable)));
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[7], sequencePairs2010_2019).size());
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[12], sequencePairs2010_2019).size());
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[3], sequencePairs2010_2019).size());
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[15], sequencePairs2010_2019).size());
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[2], sequencePairs2010_2019).size());
		assertEquals(0, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[6], sequencePairs2010_2019).size());
		assertEquals(1, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[19], sequencePairs2010_2019).size());
		assertEquals(2, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[10], sequencePairs2010_2019).size());
		assertEquals(3, _bftOverlayManager.getDownstreamSequencePairsOf(_allAddresses[9], sequencePairs2010_2019).size());
		anchorSet.clear();
	}
	
	public void testGetMatchingSetForBFTDack() {
		InetSocketAddress _iAddr = new InetSocketAddress(_ipstr, _portOffset + 0);
		IBFTOverlayManager bftOverlayManager = initOverlayManager(1, _iAddr, TOP_FILENAME_DELTA1);

		{
			// 2010
			InetSocketAddress hbSource = _allAddresses[10];
			LocalSequencer hbSourceLocalSequencer = LocalSequencer.init(null, hbSource);
			TMulticast_Publish_BFT_Dack bftHB = new TMulticast_Publish_BFT_Dack(hbSource, hbSourceLocalSequencer.getNext());
			Set<InetSocketAddress> matchingSetBFTHB = bftOverlayManager.getMatchingSetForBFTDack(bftHB);
			assertNotNull(matchingSetBFTHB);
			assertFalse(matchingSetBFTHB.contains(bftOverlayManager.getLocalAddress()));
			assertEquals(2, matchingSetBFTHB.size());
			assertTrue(matchingSetBFTHB.contains(_allAddresses[12]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[3]));
		}
		
		{
			// 2012
			InetSocketAddress hbSource = _allAddresses[12];
			LocalSequencer hbSourceLocalSequencer = LocalSequencer.init(null, hbSource);
			TMulticast_Publish_BFT_Dack bftHB = new TMulticast_Publish_BFT_Dack(hbSource, hbSourceLocalSequencer.getNext());
			Set<InetSocketAddress> matchingSetBFTHB = bftOverlayManager.getMatchingSetForBFTDack(bftHB);
			assertNotNull(matchingSetBFTHB);
			assertFalse(matchingSetBFTHB.contains(bftOverlayManager.getLocalAddress()));
			assertEquals(9, matchingSetBFTHB.size());
			assertTrue(matchingSetBFTHB.contains(_allAddresses[3]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[9]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[18]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[6]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[15]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[1]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[11]));
		}
		
		{
			// 2015
			InetSocketAddress hbSource = _allAddresses[15];
			LocalSequencer hbSourceLocalSequencer = LocalSequencer.init(null, hbSource);
			TMulticast_Publish_BFT_Dack bftHB = new TMulticast_Publish_BFT_Dack(hbSource, hbSourceLocalSequencer.getNext());
			Set<InetSocketAddress> matchingSetBFTHB = bftOverlayManager.getMatchingSetForBFTDack(bftHB);
			assertNotNull(matchingSetBFTHB);
			assertFalse(matchingSetBFTHB.contains(bftOverlayManager.getLocalAddress()));
			assertEquals(2, matchingSetBFTHB.size());
			assertTrue(matchingSetBFTHB.contains(_allAddresses[12]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[3]));
		}
		
		{
			// 2015
			InetSocketAddress hbSource = bftOverlayManager.getLocalAddress();
			LocalSequencer hbSourceLocalSequencer = LocalSequencer.init(null, hbSource);
			TMulticast_Publish_BFT_Dack bftHB = new TMulticast_Publish_BFT_Dack(hbSource, hbSourceLocalSequencer.getNext());
			Set<InetSocketAddress> matchingSetBFTHB = bftOverlayManager.getMatchingSetForBFTDack(bftHB);
			assertNotNull(matchingSetBFTHB);
			assertFalse(matchingSetBFTHB.contains(bftOverlayManager.getLocalAddress()));
			assertEquals(16, matchingSetBFTHB.size());
			assertTrue(matchingSetBFTHB.contains(_allAddresses[12]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[18]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[15]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[6]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[3]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[9]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[10]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[1]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[13]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[4]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[10]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[16]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[7]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[11]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[2]));
			assertTrue(matchingSetBFTHB.contains(_allAddresses[20]));
		}
	}
}

class BFTBrokerShadow_ForTest extends BFTBrokerShadow {

	protected final int _nr;
	
	public BFTBrokerShadow_ForTest(NodeTypes nodeType, int delta, int nr,
			InetSocketAddress localAddress, String basedirname,
			String outputdirname, String identityfilename, Properties arguments) {
		super(nodeType, delta, localAddress, basedirname, outputdirname,
				identityfilename, arguments);
		
		_nr = nr;
	}
	
	@Override
	public int getNeighborhoodRadius() {
		return _nr;
	}
}