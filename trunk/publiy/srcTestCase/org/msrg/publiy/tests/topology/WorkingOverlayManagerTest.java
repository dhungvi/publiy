package org.msrg.publiy.tests.topology;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.NodeCache;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.overlaymanager.WorkingOverlayManager;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.tests.commons.FileCommons;
import org.msrg.publiy.utils.FileUtils;
import junit.framework.TestCase;

public class WorkingOverlayManagerTest extends TestCase {
	
	private final static String TOP_FILENAME = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "tops" + FileUtils.separatorChar + "OverlayManagerTest_1.top";

	protected InetSocketAddress[] _allAddresses;
	protected final int _delta = 5;
	
	@Override
	protected void setUp() throws Exception {
		Broker.setDelta(_delta);
		BrokerInternalTimer.start();
		InetSocketAddress dummyAddress = new InetSocketAddress("localhost", 50000);
		LocalSequencer localSequencer = LocalSequencer.init(null, dummyAddress);
		
		_allAddresses = FileCommons.getAllInetAddressesFromLines(localSequencer, TOP_FILENAME);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testAll() {
		InetSocketAddress localAddress = Broker.b2Addresses[10];
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(Broker.DELTA).setMP(true);
		LocalSequencer localSequencer = brokerShadow.getLocalSequencer();
		
		TRecovery_Join[] trjs = ConnectionManagerFactory.readRecoveryTopology(localSequencer, TOP_FILENAME);
		IOverlayManager overlayManager = new OverlayManager(brokerShadow);
		overlayManager.applyAllJoinSummary(trjs);
		overlayManager.initializeOverlayManager();
		
//		System.out.println(Broker.DELTA + " " + overlayManager);
		
		Set<InetSocketAddress> activeRemoteAddressesSet = new HashSet<InetSocketAddress>();
		activeRemoteAddressesSet.add(Broker.b2Addresses[20]);
		activeRemoteAddressesSet.add(Broker.b2Addresses[11]);
		activeRemoteAddressesSet.add(Broker.b2Addresses[3]);
		activeRemoteAddressesSet.add(Broker.b2Addresses[00]);
		IOverlayManager wOverlayManager = new WorkingOverlayManager(0, overlayManager, activeRemoteAddressesSet);
//		System.out.println(Broker.DELTA + " " + wOverlayManager);
		
		NodeCache[]cNodes = wOverlayManager.initializeOverlayManager();
		assertTrue(activeRemoteAddressesSet.size() == cNodes.length); // local has been added to activeRemoteAddressesSet
	}
	
	public void testGeneral(InetSocketAddress localAddress, IOverlayManager overlayManager){
		assertEquals(localAddress, overlayManager.getLocalAddress());
		assertEquals(_allAddresses.length, overlayManager.getTopologyLinks().length);
	}
	
	public void testReachability(InetSocketAddress localAddress, IOverlayManager overlayManager) {
		InetSocketAddress[] neighbors = overlayManager.getNeighbors(localAddress);
		for ( int i=0 ; i<_allAddresses.length ; i++ )
		{
			InetSocketAddress remoteAddress = _allAddresses[i];
			Path<INode> path = overlayManager.getPathFrom(remoteAddress);
			assertNotNull(path);
			for ( int j=0 ; j<neighbors.length ; j++ )
				if ( neighbors[j].equals(remoteAddress))
					assertEquals(true, path.getLength() == 1);
		}
	}
}
