package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

import junit.framework.TestCase;

public class TSessionInitiationTest extends TestCase {

	protected LocalSequencer _localSequencer;
	
	@Override
	public void setUp() {
		_localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.2", 3000));
	}
	
	@Override
	public void tearDown() { }
	
	public void test() {
		TSessionInitiation tSession = new TSessionInitiation (_localSequencer, new InetSocketAddress("127.0.0.1", 2002), SessionInitiationTypes.SINIT_PUBSUB, false, null);
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, tSession);
		TSessionInitiation tSession2 = (TSessionInitiation) PacketFactory.unwrapObject(null, raw);

		assertEquals(tSession, tSession2);
		assertEquals(tSession2, tSession);
		
		assertEquals(tSession2.getObjectType(), raw.getType());
		
		raw.setEncoding((short)0);
		raw.setReceiver(Broker.bAddress12);
		raw.setSender(Broker.bAddress10);
		raw.setSeq(0);
		raw.setSession(0);

		raw.getType();
		assertEquals(tSession.getObjectType(), raw.getType());
		assertEquals(tSession2.getObjectType(), raw.getType());
	}
}
