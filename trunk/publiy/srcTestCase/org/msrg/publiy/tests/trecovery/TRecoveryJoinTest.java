package org.msrg.publiy.tests.trecovery;

import java.net.InetSocketAddress;
import java.util.BitSet;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.NodeCache;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.tests.commons.FileCommons;
import org.msrg.publiy.tests.commons.NodeCacheBitSet;
import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;

import junit.framework.TestCase;

public class TRecoveryJoinTest extends TestCase {
	
	protected final static String TOP_FILENAME = "." + FileUtils.separatorChar + "misc" + FileUtils.separatorChar + "testdata" + FileUtils.separatorChar + "tops" + FileUtils.separatorChar + "OverlayManagerTest_1.top";
	protected InetSocketAddress[] _allAddresses;
	protected NodeCache[] _nodes;
	protected static final int _delta = 5;
	static {
		Broker.setDelta(_delta);
	}
	
	public TRecoveryJoinTest(){
		InetSocketAddress dummyAddress = new InetSocketAddress("localhost", 50000);
		LocalSequencer localSequencer = LocalSequencer.init(null, dummyAddress);
		
		_allAddresses = FileCommons.getAllInetAddressesFromLines(localSequencer, TOP_FILENAME);
	}
	
	@Override
	protected void setUp() { }
	
	@Override
	protected void tearDown() { }
	
	public void testTRecoveryMorph(InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(_delta);
		LocalSequencer localSequencer = brokerShadow.getLocalSequencer();
		TRecovery_Join[] trjs = ConnectionManagerFactory.readRecoveryTopology(localSequencer, TOP_FILENAME);
		IOverlayManager overlayManager = new OverlayManager(brokerShadow);
		overlayManager.applyAllJoinSummary(trjs);
		_nodes = overlayManager.initializeOverlayManager();
		ISession _session = ISession.createDummySession(brokerShadow);
		_session.setRemoteCC(remoteAddress);
		
		TRecovery_Join[] trjsLocal = overlayManager.getSummaryFor(localAddress);
		for ( int i=0 ; i<trjsLocal.length ; i++ )
		{
			TRecovery_Join trj = trjsLocal[i];
			IRawPacket raw = trj.morph(_session, overlayManager);
			
			InetSocketAddress addr1 = trj.getJoiningNode();
			InetSocketAddress addr2 = trj.getJoinPoint();
			// see if raw is null or not
			TRecovery_Join[] trjsSummary = overlayManager.getSummaryFor(remoteAddress);
			int distance = computeProximity(overlayManager, localAddress, remoteAddress, addr1, addr2);
			
			if ( raw == null )
			{
				assertTrue ( (distance > _delta + 1) || !isTRecoveryPresent(trjsSummary, trj) );
			}
			else
			{
				assertTrue (distance <= _delta + 1);
				assertTrue ( isTRecoveryPresent(trjsSummary, trj) );
			}
		}
	}
	
	public void testApplySummary(){
//		TRJ[2010=>2011]TRJ[2009=>2010]TRJ[2001=>2010]TRJ[2019=>2010]
		InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2020);
		InetSocketAddress addr10 = new InetSocketAddress("127.0.0.1", 2010);
		InetSocketAddress addr11 = new InetSocketAddress("127.0.0.1", 2011);
		InetSocketAddress addr1  = new InetSocketAddress("127.0.0.1", 2001);
		InetSocketAddress addr9  = new InetSocketAddress("127.0.0.1", 2009);
		InetSocketAddress addr19 = new InetSocketAddress("127.0.0.1", 2019);
		
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(_delta);
		IOverlayManager overlayManager = new OverlayManager(brokerShadow);
		LocalSequencer localSequencer = brokerShadow.getLocalSequencer();
		overlayManager.applySummary(new TRecovery_Join(localSequencer.getNext(), localAddress, NodeTypes.NODE_BROKER, addr11, NodeTypes.NODE_BROKER));
		
		TRecovery_Join[] trjs = {
				new TRecovery_Join(localSequencer.getNext(), addr10, NodeTypes.NODE_BROKER, addr11, NodeTypes.NODE_BROKER),
				new TRecovery_Join(localSequencer.getNext(), addr1 , NodeTypes.NODE_BROKER, addr10, NodeTypes.NODE_BROKER),
				new TRecovery_Join(localSequencer.getNext(), addr9 , NodeTypes.NODE_BROKER, addr10, NodeTypes.NODE_BROKER),
				new TRecovery_Join(localSequencer.getNext(), addr19, NodeTypes.NODE_BROKER, addr10, NodeTypes.NODE_BROKER),
				};
		overlayManager.applyAllJoinSummary(trjs);
		System.out.println(overlayManager);
	}
	
	public void testAddress(InetSocketAddress localAddress, InetSocketAddress remoteAddress){
		LocalSequencer localSequencer = LocalSequencer.init(null, localAddress);
		TRecovery_Join[] trjs = ConnectionManagerFactory.readRecoveryTopology(localSequencer, TOP_FILENAME);
		IOverlayManager overlayManager = new OverlayManager(new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(_delta));
		overlayManager.applyAllJoinSummary(trjs);
		_nodes = overlayManager.initializeOverlayManager();
		
		TRecovery_Join[] localTrjsSummary = overlayManager.getSummaryFor(localAddress);
		TRecovery_Join[] trjsSummary = overlayManager.getSummaryFor(remoteAddress);
		for ( int i=0 ; i<trjsSummary.length ; i++ )
		{
			TRecovery_Join trj = trjsSummary[i];
			assertTrue(isTRecoveryPresent(localTrjsSummary, trj));
			
			InetSocketAddress addr1 = trj.getJoiningNode();
			InetSocketAddress addr2 = trj.getJoinPoint();
			
			testProximity(overlayManager, localAddress, remoteAddress, addr1, addr2);
			System.out.println(trj);
		}
		
		for ( int i=0 ; i<localTrjsSummary.length ; i++ )
		{
			TRecovery_Join trj = localTrjsSummary[i];
			InetSocketAddress addr1 = trj.getJoiningNode();
			InetSocketAddress addr2 = trj.getJoinPoint();
			
			Path<INode> path1 = overlayManager.getPathFrom(addr1);
			Path<INode> path2 = overlayManager.getPathFrom(addr2);
			Path<INode> pathR = overlayManager.getPathFrom(remoteAddress);
			
			if ( path1.intersect(pathR) || path2.intersect(pathR) && 
					!(remoteAddress.equals(addr1) || remoteAddress.equals(addr2)) )
			{
				// if it was in trjsSummary this is an error
				boolean existed = isTRecoveryPresent(trjsSummary, trj);
				if ( existed )
					existed = true;
				assertFalse(existed);
			}
			else
			{
				if ( computeProximity(overlayManager, localAddress, remoteAddress, addr1, addr2) <= _delta + 1 )
				{
					// it must be in trsjSummary
					boolean existed = isTRecoveryPresent(trjsSummary, trj);
					assertTrue(existed);
				}
			}				
		}
	}
	
	private boolean isTRecoveryPresent(TRecovery_Join[] trjsSummary,
			TRecovery_Join trj) {
		for ( int j=0 ; j<trjsSummary.length ; j++ )
			if ( 
					(trjsSummary[j].getJoiningNode().equals(trj.getJoiningNode()) 
				  && trjsSummary[j].getJoinPoint().equals(trj.getJoinPoint()) )
				  ||
				    (trjsSummary[j].getJoinPoint().equals(trj.getJoiningNode()) 
				  && trjsSummary[j].getJoiningNode().equals(trj.getJoinPoint()) )
				)
				return true;
		
		return false;
	}

//	public NodeCache[] initializeOverlayManager(IOverlayManager overlayManager){
//		NodeCache[] nodes = overlayManager.getAllCachedNodes();
//		for ( int i=0 ; i<nodes.length ; i++ )
//			nodes[i].setIndex(i);
//
//		for ( int i=0 ; i<nodes.length ; i++ ){
//			Path<NodeCache> path = nodes[i]._cachedFromPath;
//			for ( int j=0 ; j<path.getLength() ; j++ )
//			{
//				NodeCache closer = path.get(j);
//				nodes[i].setAsCloser(closer.getIndex());
//				closer.setAsFarther(nodes[i].getIndex());
//			}
//		}
//		
//		for ( int i=1 ; i<nodes.length && nodes[i]._distance == 1 ; i++ ){
//			NodeCache neighbor = nodes[i];
//			BitSet farthers = neighbor.getFarthers();
//			
//			BitSet mask = new BitSet(nodes.length);
//			mask.set(0, nodes.length);
//			mask.andNot(farthers);
//			
//			for (int j = farthers.nextSetBit(0); j >= 0; j = farthers.nextSetBit(j+1)){
//				nodes[j].setMaskedClosers(mask);
//			}
//		}
//		
//		return nodes;
//	}
	
	public void testProximity(IOverlayManager overlayManager, InetSocketAddress localAddress,
			InetSocketAddress remoteAddress, InetSocketAddress addr1,
			InetSocketAddress addr2) 
	{
		int pathLength = computeProximity(overlayManager, localAddress, remoteAddress, addr1, addr2);
		assertTrue(pathLength <= _delta + 1);
	}
	
	public int computeProximity(IOverlayManager overlayManager, InetSocketAddress localAddress,
			InetSocketAddress remoteAddress, InetSocketAddress addr1,
			InetSocketAddress addr2) 
	{
		NodeCache ncL = overlayManager.getNodeCache(localAddress);
		NodeCache ncR = overlayManager.getNodeCache(remoteAddress);
		NodeCache nc1 = overlayManager.getNodeCache(addr1);
		NodeCache nc2 = overlayManager.getNodeCache(addr2);
		
		assertTrue(nc1 != null);
		assertTrue(nc2 != null);
		assertTrue(ncL != null);
		assertTrue(ncR != null);
		
		BitSet closers1 = (BitSet) nc1.getClosers().clone();
		BitSet closers1NL = (BitSet) nc1.getClosers().clone();
		closers1NL.clear(0);
		
		BitSet closers2 = (BitSet) nc2.getClosers().clone();
		BitSet closers2NL = (BitSet) nc2.getClosers().clone();
		closers2NL.clear(0);
		
		BitSet closersR = (BitSet) ncR.getClosers().clone();
		BitSet closersRNL = (BitSet) ncR.getClosers().clone();
		closers1NL.clear(0);
		
		BitSet closersL = (BitSet) ncL.getClosers().clone();
		BitSet closersLNL = (BitSet) ncL.getClosers().clone();
		closersLNL.clear(0);
		
		boolean isAddr1FartherOfAddr2;
		InetSocketAddress[] neighbors1 = overlayManager.getNeighbors(addr1);
		if ( neighbors1[0].equals(addr2) )
			isAddr1FartherOfAddr2 = true;
		else
			isAddr1FartherOfAddr2 = false;
			
		boolean isAddr2FartherOfAddr1;
		InetSocketAddress[] neighbors2 = overlayManager.getNeighbors(addr2);
		if ( neighbors2[0].equals(addr1) )
			isAddr2FartherOfAddr1 = true;
		else
			isAddr2FartherOfAddr1 = false;
		
		assertFalse(isAddr2FartherOfAddr1 && isAddr1FartherOfAddr2 );
		assertTrue(isAddr2FartherOfAddr1 || isAddr1FartherOfAddr2 );
		
		assertTrue(closers1NL.intersects(closers2NL) || closers1NL.isEmpty() || closers2NL.isEmpty());
		
		if ( closers1NL.intersects(closersRNL) || closers2NL.intersects(closersRNL) )
		{
			BitSet sharedPath1 = (BitSet)closers1NL.clone();
			sharedPath1.and(closersRNL);
			BitSet sharedPath2 = (BitSet)closers2NL.clone();
			sharedPath2.and(closersRNL);
			
			int distanceFarther = nc1._distance>nc2._distance?nc1._distance:nc2._distance;
			int pathLength;
			
			NodeCache farther = nc1._distance>nc2._distance?nc1:nc2;
			NodeCache closer = nc1._distance<nc2._distance?nc1:nc2;
			if ( farther.getClosers().get(ncR.getIndex()) )
			{
				if ( farther._distance == ncR._distance )
					return 1;
				pathLength = farther._distance - ncR._distance;
			}
			else if ( ncR.getClosers().get(farther.getIndex()) )
			{
				pathLength = ncR._distance - farther._distance + 1;
			}
			else
			{
				int distanceCloserSharedPoint;
				if ( sharedPath1.cardinality() > sharedPath2.cardinality() )
					distanceCloserSharedPoint = sharedPath1.cardinality();
				else
					distanceCloserSharedPoint = sharedPath2.cardinality();
				
				pathLength = ncR._distance - 2*distanceCloserSharedPoint + distanceFarther;
			}
			
			return pathLength;
		}
		else
		{
			// on different directions
			int distance12 = (nc1._distance>nc2._distance)?nc1._distance : nc2._distance;
			int distance = distance12 + ncR._distance;
			return distance;
		}
	}

	private String toStringBV(BitSet bv){
		return NodeCacheBitSet.toStringBV(bv, _nodes);
	}
	
	public String toStringNodeCache(NodeCache node){
		return NodeCacheBitSet.toStringNodeCache(node, _nodes);
	}
	
	public void testAllAddresses() {
//		testAddress(new InetSocketAddress("127.0.0.1", 2001), new InetSocketAddress("127.0.0.1", 2002));
		for ( int i=0 ; i<_allAddresses.length ; i++ ) {
			for ( int j=0 ; j<_allAddresses.length ; j++ ) {
//				testAddress(_allAddresses[i], _allAddresses[j]);
				testTRecoveryMorph(_allAddresses[i], _allAddresses[j]);
			}
		}
	}
}

