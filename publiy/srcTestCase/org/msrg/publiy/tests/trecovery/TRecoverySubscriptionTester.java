package org.msrg.publiy.tests.trecovery;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.NodeCache;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.utils.FileUtils;

import junit.framework.TestCase;

public class TRecoverySubscriptionTester extends TestCase {
	
	protected final static String TOP_FILENAME =
			"." + FileUtils.separatorChar + "misc" + FileUtils.separatorChar + "testdata" + FileUtils.separatorChar + "tops" + FileUtils.separatorChar + "OverlayManagerTest_1.top";

	@Override
	protected void tearDown() { }
	
	public void testFromComputation() {
		InetSocketAddress localAddress2010 = new InetSocketAddress("127.0.0.1", 2010);
		InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2000);
		InetSocketAddress oldFrom = new InetSocketAddress("127.0.0.1", 2012);
		IBrokerShadow brokerShadow2010 = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress2010).setDelta(Broker.DELTA);
		LocalSequencer localSequencer = brokerShadow2010.getLocalSequencer();
		TRecovery_Join[] trjs = ConnectionManagerFactory.readRecoveryTopology(localSequencer, TOP_FILENAME);
		IOverlayManager overlayManager2010 = new OverlayManager(brokerShadow2010);
		overlayManager2010.applyAllJoinSummary(trjs);
		NodeCache[] nodes = overlayManager2010.initializeOverlayManager();
		assertEquals(21, nodes.length);
		
		InetSocketAddress newFrom = 
			TRecoverySubscriptionTest.getNewFrom(overlayManager2010, remoteAddress, oldFrom);
		
		System.out.println(newFrom);
	}

}
