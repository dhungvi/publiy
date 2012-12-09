package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import junit.framework.TestCase;

public class TNetworkCoding_CodedPieceIdReqTest extends TestCase {

	InetSocketAddress 	_sender = Sequence.getRandomAddress();
	LocalSequencer _localSequencer = LocalSequencer.init(null, _sender);
	
	Sequence _sequence;
	TNetworkCoding_CodedPieceIdReq _tCodingIdReq;
	String _annotationString = "ANNOTATION_STRING";
	
	@Override
	public void setUp() {
		_sequence = Sequence.getRandomSequence();
		_tCodingIdReq =
			new TNetworkCoding_CodedPieceIdReqBreak(_sender, _sequence, 0);
		
		_tCodingIdReq.annotate(_annotationString);
	}
	
	public void testEncoding() {
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, _tCodingIdReq);
		
		TNetworkCoding_CodedPieceIdReq tCodingIdReq2 = 
			(TNetworkCoding_CodedPieceIdReq) PacketFactory.unwrapObject(null, raw, true);
		
		assertTrue(tCodingIdReq2.equals(_tCodingIdReq));
		assertTrue(_tCodingIdReq.equals(tCodingIdReq2));
		
		assertTrue(_annotationString.equals(_tCodingIdReq.getAnnotations()));
		assertTrue(_annotationString.equals(tCodingIdReq2.getAnnotations()));
	}
}
