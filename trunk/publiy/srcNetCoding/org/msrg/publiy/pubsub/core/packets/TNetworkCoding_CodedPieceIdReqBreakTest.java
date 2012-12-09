package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import junit.framework.TestCase;

public class TNetworkCoding_CodedPieceIdReqBreakTest extends TestCase {

	Sequence _sequence = Sequence.getRandomSequence();
	InetSocketAddress _sender = Sequence .getRandomAddress();
	LocalSequencer _localSeqencer = LocalSequencer.init(null, _sender);
	
	public void testEncodingDecoding() {
		for(int breakingSize=0 ; breakingSize<Byte.MAX_VALUE ; breakingSize++) {
			TNetworkCoding_CodedPieceIdReqBreak tBreak =
				new TNetworkCoding_CodedPieceIdReqBreak(_sender, _sequence, breakingSize);
			
			IRawPacket raw = PacketFactory.wrapObject(_localSeqencer, tBreak);
			IPacketable packet = PacketFactory.unwrapObject(null, raw);
			
			assertTrue(packet.equals(tBreak));
			assertTrue(tBreak.equals(packet));
		}
		
		System.out.println("OK!");
	}
}
