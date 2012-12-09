package org.msrg.publiy.tests.trecovery;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.NodeCache;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;

public class TRecoveryJoinTester extends TRecoveryJoinTest {
	
	public static NodeCache[] _nodes;
	
	public void testAll() {
//		TRecoveryJoinTest.
		testApplySummary();
		
		InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2010);
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(_delta);
		ISession session = ISession.createDummySession(brokerShadow);
		InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2020);
		InetSocketAddress addr1 = new InetSocketAddress("127.0.0.1", 2010);
		InetSocketAddress addr2 = new InetSocketAddress("127.0.0.1", 2009);
		
		TRecovery_Join[] trjs = ConnectionManagerFactory.readRecoveryTopology(brokerShadow.getLocalSequencer(), TRecoveryJoinTest.TOP_FILENAME);
		IOverlayManager overlayManager = new OverlayManager(brokerShadow);
		overlayManager.applyAllJoinSummary(trjs);
		overlayManager.initializeOverlayManager();
//		System.out.println(overlayManager);
		
		TRecovery_Join trj = new TRecovery_Join(brokerShadow.getLocalSequencer().getNext(), addr2, NodeTypes.NODE_BROKER, addr1, NodeTypes.NODE_BROKER);
		session.setRemoteCC(remoteAddress);
		IRawPacket raw = trj.morph(session, overlayManager);
		
		_nodes = overlayManager.initializeOverlayManager();
		
		int distance = 
//				TRecoveryJoinTest.
				computeProximity(overlayManager, localAddress, remoteAddress, addr1, addr2);
		System.out.println("Local: " + localAddress.getPort() + "\n" +
							"Remote: " + remoteAddress.getPort() + "\n" +
							"Dist [" + addr1.getPort() + "_" + addr2.getPort() + "]=" + 
							distance);
	}
}
