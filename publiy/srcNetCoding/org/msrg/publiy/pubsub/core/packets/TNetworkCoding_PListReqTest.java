package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.publishSubscribe.Publication;


import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import junit.framework.TestCase;

public class TNetworkCoding_PListReqTest extends TestCase {

	InetSocketAddress _sender = Sequence.getRandomAddress();
	LocalSequencer _senderLocalSequence = LocalSequencer.init(null, _sender);
	
	static final int ROWS = 100;
	static final int COLS = 10000;
	
	public void testEncodeDecode() {
		for(int i=0 ; i<1000 ; i++) {
			boolean fromSource = i%2==0;
			Sequence contentSequence = Sequence.getRandomSequence();
			Publication publication = new Publication().addPredicate("ATTR1", 123132).addStringPredicate("ATTRSTR", "HI");
			TNetworkCoding_PListReq tPListReq =
				new TNetworkCoding_PListReq(
						_sender, contentSequence,
						ROWS, COLS,
						publication, fromSource);
			
			IRawPacket raw = PacketFactory.wrapObject(_senderLocalSequence, tPListReq);
			IPacketable decodedPacket = PacketFactory.unwrapObject(null, raw, true);
			TNetworkCoding_PListReq tPListReqDecoded = (TNetworkCoding_PListReq) decodedPacket;
			assertTrue(tPListReq != decodedPacket);
			assertTrue(decodedPacket.equals(tPListReq));
			assertTrue(tPListReq.equals(decodedPacket));
			assertTrue(tPListReq._rows == ROWS);
			assertTrue(tPListReq._cols == COLS);
			assertTrue(tPListReqDecoded._rows == ROWS);
			assertTrue(tPListReqDecoded._cols == COLS);

		}
	}
}
