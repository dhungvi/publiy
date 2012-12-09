package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.communication.core.packet.PacketFactory;

import junit.framework.TestCase;

public class TimestampTest extends TestCase {

	protected final InetSocketAddress addr1 = new InetSocketAddress("127.0.0.1",1000);
	protected final InetSocketAddress addr2 = new InetSocketAddress("127.0.0.1",2000);
	protected LocalSequencer _localSequencer1;
	protected final IBrokerShadow _brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, addr1);

	@Override
	public void setUp() {
		 _localSequencer1 = LocalSequencer.init(null, addr1);
	}
	
	@Override
	public void tearDown() { }
	
	public void testDuplicate() {
		TMulticast_Join tm = new TMulticast_Join(addr1, NodeTypes.NODE_BROKER, addr2, NodeTypes.NODE_BROKER, _localSequencer1);
		tm = tm.getShiftedClone(1, _localSequencer1.getNext());
//		tm.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress3, 300));
		TMulticast_Join tmm = new TMulticast_Join(addr1, NodeTypes.NODE_BROKER, addr2, NodeTypes.NODE_BROKER, _localSequencer1);
		
		TMulticast_Join tm1 = (TMulticast_Join) PacketFactory.unwrapObject(_brokerShadow, PacketFactory.wrapObject(_localSequencer1, tm));
		TMulticast_Join tm2 = (TMulticast_Join) PacketFactory.unwrapObject(_brokerShadow, PacketFactory.wrapObject(_localSequencer1, tm1));
//		tm2.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress4, 440));
		TMulticast_Join tm3 = (TMulticast_Join) PacketFactory.unwrapObject(_brokerShadow, PacketFactory.wrapObject(_localSequencer1, tm2));
		
		Timestamp ts = new Timestamp(_brokerShadow);
		
		assertFalse(ts.isDuplicate(tm1));
		assertFalse(ts.isDuplicate(tmm));
		assertTrue(ts.isDuplicate(tm1));
		assertTrue(ts.isDuplicate(tm2));
		assertTrue(ts.isDuplicate(tm3));
	}
}
