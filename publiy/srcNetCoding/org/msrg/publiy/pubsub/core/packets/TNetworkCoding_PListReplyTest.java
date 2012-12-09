package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.util.HashSet;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;


import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import junit.framework.TestCase;

public class TNetworkCoding_PListReplyTest extends TestCase {

	static final int ROWS = 100;
	static final int COLS = 10000;
	
	InetSocketAddress _sender = Sequence.getRandomAddress();
	LocalSequencer _localSequencer = LocalSequencer.init(null, _sender);
	
	public void testEncodeDecode() {
		for(int i=0 ; i<1000 ; i++) {
			Sequence contentSequence = _localSequencer.getNext();
			TNetworkCoding_PListReply tPListReply =
				new TNetworkCoding_PListReply(
						i % 2 == 0,
						_sender, contentSequence,
						ROWS, COLS,
						new HashSet<InetSocketAddress>(), new HashSet<InetSocketAddress>());
			
			IRawPacket raw = PacketFactory.wrapObject(_localSequencer, tPListReply);
			IPacketable decodedPacket = PacketFactory.unwrapObject(null, raw, true);
			TNetworkCoding_PListReply tPListReplyDecoded =
				(TNetworkCoding_PListReply) decodedPacket;
			assertTrue(tPListReply != decodedPacket);
			assertTrue(decodedPacket.equals(tPListReply));
			assertTrue(tPListReply.equals(decodedPacket));
			assertTrue(tPListReply._rows == ROWS);
			assertTrue(tPListReply._cols == COLS);
			assertTrue(tPListReplyDecoded._rows == ROWS);
			assertTrue(tPListReplyDecoded._cols == COLS);
			assertTrue(tPListReplyDecoded._launch == (i % 2 == 0));
		}
	}
}
