package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.node.NodeTypes;

import junit.framework.TestCase;

public class PathTest extends TestCase {

	protected final int BASE_PORT = 1000;
	protected final InetSocketAddress _localAddress = new InetSocketAddress("127.0.0.1", 500);
	
	@Override
	public void setUp() { }
	
	@Override
	public void tearDown() { }
	
	public void testPathRevserse() {
		LocalSequencer localSequencer = LocalSequencer.init(null, _localAddress);
		int neighborhoodRadious = Broker.DELTA + 1;
		Path<INode> p = Path.createOverlayNodesPath(neighborhoodRadious);
		
		for ( int i=0 ; i<neighborhoodRadious ; i++ ){
			InetSocketAddress addr = new InetSocketAddress("127.0.0.1", BASE_PORT+i);
			INode node = new OverlayNode(localSequencer, addr, NodeTypes.NODE_BROKER);
			p.addNode(node);
		}
		
		InetSocketAddress[] rAddresses = p.getReverseAddresses();
		for ( int i=0 ; i<rAddresses.length ; i++ )
			assertEquals(BASE_PORT + rAddresses.length - i - 1, rAddresses[i].getPort());
	}

}
