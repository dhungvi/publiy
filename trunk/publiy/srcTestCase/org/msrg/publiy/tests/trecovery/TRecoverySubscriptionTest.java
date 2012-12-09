package org.msrg.publiy.tests.trecovery;

import java.net.InetSocketAddress;
import java.util.BitSet;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.NodeCache;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.tests.commons.FileCommons;
import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import junit.framework.TestCase;

public class TRecoverySubscriptionTest extends TestCase {
	
	final static String TOP_FILENAME = "." + FileUtils.separatorChar + "misc" + FileUtils.separatorChar + "testdata" + FileUtils.separatorChar + "tops" + FileUtils.separatorChar + "OverlayManagerTest_1.top";
	private InetSocketAddress[] _allAddresses;
	private NodeCache[] _nodes;
	protected static final int _delta = 5;
	
	public TRecoverySubscriptionTest() {
		System.setProperty("Broker.DELTA", "" + _delta);
		InetSocketAddress dummyAddress = new InetSocketAddress("localhost", 50000);
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, dummyAddress);
		
		_allAddresses = FileCommons.getAllInetAddressesFromLines(brokerShadow.getLocalSequencer(), TOP_FILENAME);
	}
	
	public void testTRecoveryMorph(InetSocketAddress localAddress, InetSocketAddress remoteAddress){
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(_delta);
		LocalSequencer localSequencer = brokerShadow.getLocalSequencer();
		TRecovery_Join[] trjs = ConnectionManagerFactory.readRecoveryTopology(localSequencer, TOP_FILENAME);
		IOverlayManager overlayManager = new OverlayManager(brokerShadow);
		overlayManager.applyAllJoinSummary(trjs);
		_nodes = overlayManager.initializeOverlayManager();
		
		for ( int i=0 ; i<_nodes.length ; i++ ){
			InetSocketAddress oldFrom = _nodes[i].getAddress();
			InetSocketAddress testersNewFrom = getNewFrom(overlayManager, remoteAddress, oldFrom);
			InetSocketAddress overlaysNewFrom =
					overlayManager.getNewFromForMorphedMessage(remoteAddress, oldFrom);
			
			assert (testersNewFrom==null && overlaysNewFrom==null) || (overlaysNewFrom.equals(testersNewFrom));
//			System.out.println("L:" + localAddress + ", R:" + remoteAddress + " _ " + oldFrom + " \t" + testersNewFrom + " == " + overlaysNewFrom);
		}
	}
	
	public static InetSocketAddress getNewFrom(IOverlayManager overlay, 
					InetSocketAddress remoteAddress, InetSocketAddress oldFrom)
	{
		NodeCache remoteNode = overlay.getNodeCache(remoteAddress);
		NodeCache oldFromNode = overlay.getNodeCache(oldFrom);
		
		assert remoteNode != null;
		assert oldFrom != null;
		
		BitSet remoteClosersNL = (BitSet)remoteNode.getClosers().clone();
		remoteClosersNL.clear(0);
		BitSet oldFromClosersNL = (BitSet)oldFromNode.getClosers().clone();
		oldFromClosersNL.clear(0);

		if ( remoteClosersNL.intersects(oldFromClosersNL) )
			return null;
		
		int remoteDist = remoteNode._distance;
		int oldFromDist = oldFromNode._distance;
		int index = (remoteDist+oldFromDist+1<=_delta+1) ?
				0 : remoteDist - (_delta+1-oldFromDist);
		
		Path<NodeCache> pathFromOldFrom = oldFromNode._cachedFromPath;
		InetSocketAddress newFrom = pathFromOldFrom.get(index).getAddress();
		
		return newFrom;
	}
	
	public void testAll(){
		for ( int i=0 ; i<_allAddresses.length ; i++ )
			for ( int j=0 ; j<_allAddresses.length ; j++ )
				testTRecoveryMorph(_allAddresses[i], _allAddresses[j]);
	}
}

