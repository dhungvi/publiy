package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.publishSubscribe.Publication;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import junit.framework.TestCase;

public class TNetworkCoding_CodedPieceIdTest extends TestCase {

	final int _rows = 500;
	final int _cols = 300;
	
	TNetworkCoding_CodedPieceId _tCodedPieceId;
	InetSocketAddress _sender;
	Sequence _sequence;
	Publication _publication;
	String _annotationString = "ANNOTATION_STRING";
	LocalSequencer _localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 3030));
	
	@Override
	public void setUp() {
		_sender = Sequence.getRandomAddress();
		_sequence = Sequence.getRandomSequence();
		_publication = new Publication().addPredicate("hi", 12121);
		_tCodedPieceId = new TNetworkCoding_CodedPieceId(
				_sender,
				_sequence, _publication, _rows, _cols);
		
		_tCodedPieceId.annotate(_annotationString);
	}
	
	public void testEncodingDecoding() {
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, _tCodedPieceId);
		
		TNetworkCoding_CodedPieceId tCodedPieceId2 =
			(TNetworkCoding_CodedPieceId) PacketFactory.unwrapObject(null, raw, false);
		assertTrue(tCodedPieceId2.equals(_tCodedPieceId));
		assertTrue(_tCodedPieceId.equals(tCodedPieceId2));
		
		assertTrue(_annotationString.equals(tCodedPieceId2.getAnnotations()));
	}
}
