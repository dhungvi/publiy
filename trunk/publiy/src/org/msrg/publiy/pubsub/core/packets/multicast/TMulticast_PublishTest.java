package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.publishSubscribe.Publication;

import junit.framework.TestCase;

public class TMulticast_PublishTest extends TestCase {
	protected Publication _pub;
	protected TMulticast_Publish _tmPub;
	protected InetSocketAddress _from = Sequence.getRandomAddress();
	protected byte[] _payload = {1, 2, 3, 4};
	protected LocalSequencer _localSequencer;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start(false);
		_localSequencer = LocalSequencer.init(null, _from);
		_pub = createPublication().
				addPredicate("ATTR1", 12).
				addStringPredicate("ATTRSTR", "HI");
		_tmPub = createTMulticast_Publish(_pub, _from);
	}
	
	@Override
	public void tearDown() { }
	
	protected TMulticast_Publish createTMulticast_Publish(Publication pub, InetSocketAddress from) {
		return new TMulticast_Publish(pub, from, _payload, _localSequencer.getNext()).getShiftedClone(1, _localSequencer.getNext());
	}
	
	protected Publication createPublication() {
		return new Publication();
	}
	
	public void testTMulticastSubscribeEncodeDecode() {
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, _tmPub);
		IPacketable packet = PacketFactory.unwrapObject(null, raw, true);
		
		assertTrue(_tmPub != packet);
		assertTrue(_tmPub.equals(packet));
		assertTrue(packet.equals(_tmPub));
	}
}
