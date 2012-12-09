package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ITimestamp;
import org.msrg.publiy.pubsub.core.messagequeue.Timestamp;
import org.msrg.publiy.pubsub.core.packets.multicast.Dack_Bundle;
import org.msrg.publiy.pubsub.core.packets.multicast.TDack;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Depart;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import junit.framework.TestCase;

public class OverlayManagerTest extends TestCase {

	protected final InetSocketAddress []_a = new InetSocketAddress[20];

	@Override
	public void setUp() {
		for ( int i=0 ; i<_a.length ; i++ )
			_a[i] = new InetSocketAddress("127.0.0.1", 1000 + i);
		BrokerInternalTimer.start();
	}

	@Override
	public void tearDown() { }
	
	protected IOverlayManager prepareOverlayManager(IBrokerShadow brokerShadow) {
		LocalSequencer  localSequencer = brokerShadow.getLocalSequencer();
		OverlayManager overlayManager = new OverlayManager(brokerShadow);
		TRecovery_Join[] trjs = new TRecovery_Join[_a.length];
		trjs[0] = new TRecovery_Join(localSequencer.getNext(), _a[2], NodeTypes.NODE_BROKER, _a[1], NodeTypes.NODE_BROKER);
		trjs[9] = new TRecovery_Join(localSequencer.getNext(), _a[3], NodeTypes.NODE_BROKER, _a[0], NodeTypes.NODE_BROKER);
		trjs[1] = new TRecovery_Join(localSequencer.getNext(), _a[5], NodeTypes.NODE_BROKER, _a[8], NodeTypes.NODE_BROKER);
		trjs[2] = new TRecovery_Join(localSequencer.getNext(), _a[8], NodeTypes.NODE_BROKER, _a[9], NodeTypes.NODE_BROKER);
		trjs[3] = new TRecovery_Join(localSequencer.getNext(), _a[11], NodeTypes.NODE_BROKER, _a[10], NodeTypes.NODE_BROKER);
		trjs[4] = new TRecovery_Join(localSequencer.getNext(), _a[1], NodeTypes.NODE_BROKER, _a[10], NodeTypes.NODE_BROKER);
		trjs[5] = new TRecovery_Join(localSequencer.getNext(), _a[19], NodeTypes.NODE_BROKER, _a[10], NodeTypes.NODE_BROKER);
		trjs[6] = new TRecovery_Join(localSequencer.getNext(), _a[10], NodeTypes.NODE_BROKER, _a[9], NodeTypes.NODE_BROKER);
		trjs[7] = new TRecovery_Join(localSequencer.getNext(), _a[9], NodeTypes.NODE_BROKER, _a[0], NodeTypes.NODE_BROKER);

		overlayManager.applyAllJoinSummary(trjs);
		return overlayManager;
	}
	
	public void testDepart() {
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _a[0]).setDelta(Broker.DELTA);
		LocalSequencer  localSequencer = brokerShadow.getLocalSequencer();

		TMulticast_Join [] tmjs = new TMulticast_Join[8];
		tmjs[0] = new TMulticast_Join(_a[1], NodeTypes.NODE_BROKER, _a[0], NodeTypes.NODE_BROKER, localSequencer.getNext());
		tmjs[1] = new TMulticast_Join(_a[2], NodeTypes.NODE_BROKER, _a[1], NodeTypes.NODE_BROKER, localSequencer.getNext());
		tmjs[2] = new TMulticast_Join(_a[3], NodeTypes.NODE_BROKER, _a[1], NodeTypes.NODE_BROKER, localSequencer.getNext());
		tmjs[3] = new TMulticast_Join(_a[4], NodeTypes.NODE_BROKER, _a[0], NodeTypes.NODE_BROKER, localSequencer.getNext());
		tmjs[4] = new TMulticast_Join(_a[5], NodeTypes.NODE_BROKER, _a[4], NodeTypes.NODE_BROKER, localSequencer.getNext());
		tmjs[5] = new TMulticast_Join(_a[6], NodeTypes.NODE_BROKER, _a[4], NodeTypes.NODE_BROKER, localSequencer.getNext());
		tmjs[6] = new TMulticast_Join(_a[7], NodeTypes.NODE_BROKER, _a[3], NodeTypes.NODE_BROKER, localSequencer.getNext());
		tmjs[7] = new TMulticast_Join(_a[8], NodeTypes.NODE_BROKER, _a[3], NodeTypes.NODE_BROKER, localSequencer.getNext());

		TMulticast_Depart [] tmds = new TMulticast_Depart[8];
		tmds[7] = new TMulticast_Depart(_a[4], _a[0], localSequencer.getNext());
		tmds[6] = new TMulticast_Depart(_a[1], _a[0], localSequencer.getNext());
		tmds[5] = new TMulticast_Depart(_a[3], _a[1], localSequencer.getNext());
		tmds[4] = new TMulticast_Depart(_a[2], _a[1], localSequencer.getNext());
		tmds[3] = new TMulticast_Depart(_a[5], _a[4], localSequencer.getNext());
		tmds[2] = new TMulticast_Depart(_a[6], _a[4], localSequencer.getNext());
		tmds[1] = new TMulticast_Depart(_a[7], _a[3], localSequencer.getNext());
		tmds[0] = new TMulticast_Depart(_a[8], _a[3], localSequencer.getNext());
		
		IOverlayManager overlayMan = new OverlayManager(brokerShadow);
		
		for ( int i=0 ; i<tmjs.length ; i++ )
			overlayMan.handleMessage(tmjs[i]);
		assertEquals(9, overlayMan.getNeighborhoodSize());
		System.out.println(overlayMan);
		
		for ( int i=0 ; i<tmds.length ; i++ )
			overlayMan.handleMessage(tmds[i]);
		assertEquals(1, overlayMan.getNeighborhoodSize());

//		overlayMan.dumpOverlay();
//		System.out.println("*****************************************************");
//		System.out.println("*****************************************************");
//		
//		TRecovery_Join [] summary = overlayMan.getSummary(overlayMan.getLocalAddress());
//		TRecovery_Join [] summary2 = new TRecovery_Join[summary.length];
//		for ( int i=0 ; i<summary.length ; i++ ){
//			IRawPacket raw = PacketFactory.wrapObject(summary[i]);
//			ByteBuffer bdy = raw.getBody();
//			summary2[i] = new TRecovery_Join(bdy);
//		}
//
//		System.out.println(overlayMan);
//		System.out.println("*****************************************************");
		
		{
//		OverlayManager.renewLocalNode();
//		IOverlayManager overlayMan2 = new OverlayManager(Broker.BROKER_RECOVERY_TOPOLOGY_FILE + "-2");
//		
//		for ( int i=0 ; i<summary2.length ; i++ )
//			overlayMan2.applySummary(summary2[i]);
//		
//		System.out.println(overlayMan2);
//		System.out.println("*****************************************************");
//		
//		OverlayManager.renewLocalNode();
		}
		
//		IOverlayManager overlayMan3 = new OverlayManager(Broker.BROKER_RECOVERY_TOPOLOGY_FILE + "-3"); 
//		overlayMan3.applyAllJoinSummary(summary2);
//		System.out.println(overlayMan3);
	}
	
	public void testLargeEnoughDeltaForCenterNodesOnly() {
		final int delta = 2;
		final InetSocketAddress centerNode = _a[10];
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, centerNode).setDelta(delta);
		IOverlayManager overlayManager = prepareOverlayManager(brokerShadow);
		assertEquals(10, overlayManager.getNeighborhoodSize());
		
		TRecovery_Join[] summaries = overlayManager.getSummaryFor(_a[1]);
		assertEquals(5, summaries.length);
	}

	public void testLargeEnoughDeltaForAll() {
		final int delta = 3;
		final InetSocketAddress centerNode = _a[1];
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, centerNode).setDelta(delta);
		IOverlayManager overlayManager = prepareOverlayManager(brokerShadow);
		assertEquals(10, overlayManager.getNeighborhoodSize());
		
		TRecovery_Join[] summaries = overlayManager.getSummaryFor(_a[1]);
		assertEquals(9, summaries.length);
	}

	public void testDack() {
		final int delta = 3;
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _a[1]).setDelta(delta);
		IOverlayManager overlayManager = prepareOverlayManager(brokerShadow);

		Sequence seq1_2 = Sequence.getPsuedoSequence(_a[2], 2);
		Sequence seq1_3 = Sequence.getPsuedoSequence(_a[3], 3);
		Sequence seq1_4 = Sequence.getPsuedoSequence(_a[4], 4);
		Sequence seq1_8 = Sequence.getPsuedoSequence(_a[8], 5);
		Sequence seq1_5 = Sequence.getPsuedoSequence(_a[5], 6);
		Sequence seq1_6 = Sequence.getPsuedoSequence(_a[6], 6);
		Sequence seq1_7 = Sequence.getPsuedoSequence(_a[7], 6);
		Sequence seq1_9 = Sequence.getPsuedoSequence(_a[9], 6);
		Sequence[] sequences1 = {seq1_2,seq1_3,seq1_4,seq1_5,seq1_6,seq1_7,seq1_8,seq1_9};
		
		Sequence seq5_1 = Sequence.getPsuedoSequence(_a[1], 7);
		Sequence seq5_2 = Sequence.getPsuedoSequence(_a[2], 8);
		Sequence seq5_3 = Sequence.getPsuedoSequence(_a[3], 9);
		Sequence seq5_4 = Sequence.getPsuedoSequence(_a[4], 10);
		Sequence seq5_6 = Sequence.getPsuedoSequence(_a[6], 11);
		Sequence seq5_7 = Sequence.getPsuedoSequence(_a[7], 12);
		Sequence seq5_9 = Sequence.getPsuedoSequence(_a[9], 13);
		Sequence[] sequences5 = {seq5_1,seq5_2,seq5_3,seq5_4,seq5_6,seq5_7,seq5_9};
		
		Sequence seq6_1 = Sequence.getPsuedoSequence(_a[1], 14);
		Sequence seq6_2 = Sequence.getPsuedoSequence(_a[2], 15);
		Sequence seq6_5 = Sequence.getPsuedoSequence(_a[5], 16);
		Sequence seq6_7 = Sequence.getPsuedoSequence(_a[7], 17);
		Sequence seq6_9 = Sequence.getPsuedoSequence(_a[9], 18);
		Sequence[] sequences6 = {seq6_1,seq6_2,seq6_5,seq6_7,seq6_9};
		
		Sequence seq7_1 = Sequence.getPsuedoSequence(_a[1], 19);
		Sequence seq7_2 = Sequence.getPsuedoSequence(_a[2], 20);
		Sequence seq7_5 = Sequence.getPsuedoSequence(_a[5], 21);
		Sequence seq7_7 = Sequence.getPsuedoSequence(_a[7], 22);
		Sequence seq7_9 = Sequence.getPsuedoSequence(_a[9], 23);
		Sequence[] sequences7 = {seq7_1,seq7_2,seq7_5,seq7_7,seq7_9};
		
		Sequence seq9_1 = Sequence.getPsuedoSequence(_a[1], 24);
		Sequence seq9_5 = Sequence.getPsuedoSequence(_a[5], 25);
		Sequence seq9_6 = Sequence.getPsuedoSequence(_a[6], 26);
		Sequence seq9_7 = Sequence.getPsuedoSequence(_a[7], 27);
		Sequence[] sequences9 = {seq9_1,seq9_5,seq9_6,seq9_7};
		
		Dack_Bundle bundle1 = new Dack_Bundle(_a[1], sequences1);
		Dack_Bundle bundle5 = new Dack_Bundle(_a[5], sequences5);
		Dack_Bundle bundle6 = new Dack_Bundle(_a[6], sequences6);
		Dack_Bundle bundle7 = new Dack_Bundle(_a[7], sequences7);
		Dack_Bundle bundle9 = new Dack_Bundle(_a[9], sequences9);
		
		Dack_Bundle[] bundles = {bundle1, bundle5, bundle6, bundle7, bundle9};
		TDack dack = new TDack(bundles, bundles);
		
		overlayManager.updateFromTDack(dack);
		Dack_Bundle[][] preparedBundles1_2 = overlayManager.prepareOutgoingArrivedSequences(_a[2]);
		TDack preparedDack1_2 = new TDack(preparedBundles1_2[0], preparedBundles1_2[1]);
		System.out.println(preparedDack1_2); // OK
		
		overlayManager.updateAllNodesCache(null, null);
		
		ITimestamp arrTimestamp = new Timestamp(brokerShadow);
		ITimestamp dscTimestamp = new Timestamp(brokerShadow);
		
		Sequence seq5 = Sequence.getPsuedoSequence(_a[5], 900);
		Sequence[] seqs1 = {Sequence.getPsuedoSequence(_a[1], 900)};
		
		Dack_Bundle[] bundles2 = {new Dack_Bundle(_a[5], seqs1)};
		TDack dack2 = new TDack(new Dack_Bundle[]{new Dack_Bundle(null, null)}, bundles2);
		overlayManager.updateFromTDack(dack2);
		assertFalse(arrTimestamp.isDuplicate(seq5));
		
		overlayManager.updateAllNodesCache(null, null);
		boolean safe = overlayManager.safeToDiscard(_a[5], Sequence.getPsuedoSequence(_a[1], 900));
		System.out.println("Safe: " + safe); // OK
		
		overlayManager.updateAllNodesCache(arrTimestamp, dscTimestamp);
		Dack_Bundle[][] preparedBundles1_5 = overlayManager.prepareOutgoingArrivedSequences(_a[5]);
		TDack preparedDack1_5 = new TDack(preparedBundles1_5[0], preparedBundles1_5[1]);
		System.out.println(preparedDack1_5); // OK
		
//		INode node1 = overlayManager._nodes.get(a[5]);
//		INode node2 = overlayManager._nodes.get(a[1]);
//		
//		Set<INode> nodesCollection = new HashSet<INode>();
//		overlayManager.getFartherNodesOtherThan(nodesCollection, node1, node2, 2);
		
//		System.exit(0);
	
//		List<InetSocketAddress> fartherNeighborsList = overlayManager.getFartherNeighborsOrderedSet(a[2]);
//		System.out.println(fartherNeighborsList); // OK
		
		NodeCache nodeCache1 = overlayManager.getNodeCache(_a[11]);
		System.out.println(nodeCache1); // OK
		NodeCache nodeCache2 = overlayManager.getNodeCache(_a[10]);
		System.out.println(nodeCache2); // OK

		System.out.println(overlayManager.nodesDistnacesThroughLocal(nodeCache1._remoteNode, nodeCache2._remoteNode)); // OK
		System.out.println(nodeCache1._cachedFromPath.intersect(nodeCache2._cachedFromPath)); // OK
		
		INode on = ((OverlayManager)overlayManager)._nodes.get(_a[11]);
		NodeCache nc1 = overlayManager.getNodeCache(_a[11]);
		NodeCache nc2 = ((OverlayManager)overlayManager).getNodeCacheNoCreate(on);
		assertEquals(nc1, nc2);
		assertTrue(nc1 == nc2);
	}

}
