package org.msrg.publiy.pubsub.core.packets.recovery;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;

import junit.framework.TestCase;

public class TRecoveryTest extends TestCase {

	protected LocalSequencer _localSequencer;

	@Override
	public void setUp() {
		 _localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 1000));
	}

	@Override
	public void tearDown() { }
	
	public void testWrapUnwrapTRecovery() {
		Subscription subscription = new Subscription();
		SimplePredicate sp = SimplePredicate.buildSimplePredicate("salam", '=', 10001);
		subscription.addPredicate(sp);
		subscription.addPredicate(sp);
		subscription.addPredicate(sp);
		subscription.addPredicate(sp);
		subscription.addPredicate(sp);
		
		TRecovery trs = new TRecovery_Join(_localSequencer.getNext(), Broker.bAddress1, NodeTypes.NODE_PUBLISHER, Broker.bAddress12, NodeTypes.NODE_BROKER);
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, trs);
		TRecovery trs2 = (TRecovery) PacketFactory.unwrapObject(null, raw);
		assertEquals(trs, trs2);
		assertEquals(trs2, trs);
	}

	public void testWrapUnwrapTRecoveryLast() {
		TRecovery tm1 = TRecovery.getLastTRecoveryJoin(_localSequencer.getNext());
		
		System.out.println(tm1.getObjectType());
		IRawPacket raw2 = PacketFactory.wrapObject(_localSequencer, tm1);
		TRecovery tm2 = (TRecovery) PacketFactory.unwrapObject(null, raw2, true);
		assertEquals(tm1, tm2);
		assertEquals(tm1.getObjectType(), tm2.getObjectType());
	}
}
