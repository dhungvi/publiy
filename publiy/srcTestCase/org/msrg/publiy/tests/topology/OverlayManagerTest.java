package org.msrg.publiy.tests.topology;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;


import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.tests.commons.FileCommons;
import org.msrg.publiy.utils.FileUtils;
import junit.framework.TestCase;

public class OverlayManagerTest extends TestCase {
	
	protected final static String TOP_FILENAME = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "tops" + FileUtils.separatorChar + "OverlayManagerTest_1.top";

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

	@Override
	protected void tearDown() { }
	
	public void testAllCenters() {
		for(int i=0 ; i<_allAddresses.length ; i++) {
			InetSocketAddress localAddress = _allAddresses[i];
			LocalSequencer localSequencer = LocalSequencer.init(null, localAddress);
			
			TRecovery_Join[] trjs = ConnectionManagerFactory.readRecoveryTopology(localSequencer, TOP_FILENAME);
			IOverlayManager overlayManager = createNewOverlayManager(localAddress);
			overlayManager.applyAllJoinSummary(trjs);
			
			testGeneral(localAddress, overlayManager);
			testReachability(localAddress, overlayManager);
		}
	}
	
	protected IOverlayManager createNewOverlayManager(InetSocketAddress localAddress) {
		return new OverlayManager(new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(_delta));
	}

	public void testGeneral(InetSocketAddress localAddress, IOverlayManager overlayManager) {
		assertEquals(localAddress, overlayManager.getLocalAddress());
		assertEquals(_allAddresses.length - 1, overlayManager.getTopologyLinks().length);
	}
	
	public void testReachability(InetSocketAddress localAddress, IOverlayManager overlayManager) {
		InetSocketAddress[] neighbors = overlayManager.getNeighbors(localAddress);
		for(int i=0 ; i<_allAddresses.length ; i++) {
			InetSocketAddress remoteAddress = _allAddresses[i];
			Path<INode> path = overlayManager.getPathFrom(remoteAddress);
			assertNotNull(path);
			for(int j=0 ; j<neighbors.length ; j++)
				if(neighbors[j].equals(remoteAddress))
					assertEquals(true, path.getLength() == 1);
		}
	}
	
}
