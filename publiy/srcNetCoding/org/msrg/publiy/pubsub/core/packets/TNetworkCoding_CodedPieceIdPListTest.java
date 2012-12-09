package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.util.Set;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import junit.framework.TestCase;

public class TNetworkCoding_CodedPieceIdPListTest extends TestCase {
	
	static final InetSocketAddress REPLYINGBRROKER = Sequence.getRandomAddress();
	
	InetSocketAddress _sender = new InetSocketAddress("127.0.0.1", 4012);
	LocalSequencer _senderLocalSequence = LocalSequencer.init(null, _sender);
	
	final int _remotesSize = 0;
	final int _rows = 100;
	final int _cols = 10000;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
	}
	
	public void testEncodeDecode() {
		for(int i=0 ; i<10000 ; i++) {
			final Sequence _contentSequence = Sequence.getRandomSequence();
			final Set<InetSocketAddress> _remotes = Sequence.getRandomAddressesSet(_remotesSize);
			TNetworkCoding_PListReply _tCodedPiecePList =
				new TNetworkCoding_PListReply(
						i % 2 == 0,
						_sender, _contentSequence, _rows, _cols, _remotes, null);
			IRawPacket raw = PacketFactory.wrapObject(_senderLocalSequence, _tCodedPiecePList);
			IPacketable tCodedPiecePList2 = PacketFactory.unwrapObject(null, raw, true);
			
			assertTrue(tCodedPiecePList2.equals(_tCodedPiecePList));
			assertTrue(_tCodedPiecePList.equals(tCodedPiecePList2));
		}
		
		BrokerInternalTimer.inform("OK!");
	}
}
