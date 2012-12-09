package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSBulkMatrix;
import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.networkcodes.PSCodedCoefficients;

import junit.framework.TestCase;

public class TNetworkCoding_CodedPieceTest extends TestCase {

	final int _cols = 10;
	final int _rows = 20;
	InetSocketAddress _sender;
	Sequence _sequence;
	PSBulkMatrix _bm;
	PSCodedCoefficients _cc;
	TNetworkCoding_CodedPiece _tCodedPiece;
	String _annotationString = "ANNOTATION_STRING";
	
	private boolean _packetDroppedListerReceivedCallback = false;
	private boolean _packetSentListerReceivedCallback = false;
	private boolean _packetBeingSentListerReceivedCallback = false;
	protected LocalSequencer _localSequencer;
	@Override
	public void setUp() {
		_sender = Sequence.getRandomAddress();
		_localSequencer = LocalSequencer.init(null, _sender);
		
		_sequence = _localSequencer.getNext();
		_cc = new PSCodedCoefficients(_rows);
		_bm = PSBulkMatrix.createBulkMatixIncrementalData(_sequence, _rows, _cols);
		
		_tCodedPiece = new TNetworkCoding_CodedPiece(
				_sender, new SimplePacketListener(this), _sequence, _cc, _bm, true);
		
		_tCodedPiece.annotate(_annotationString);
	}
	
	void packetSentNotificationReceived(IPacketable packet) {
		assertTrue(packet == _tCodedPiece);
		assertFalse(_packetSentListerReceivedCallback);
		_packetSentListerReceivedCallback = true;
	}
	
	void packetBeingSentNotificationReceived(IPacketable packet) {
		assertTrue(packet == _tCodedPiece);
		assertFalse(_packetBeingSentListerReceivedCallback);
		_packetBeingSentListerReceivedCallback = true;
	}

	void packetDroppedNotificationReceived(IPacketable packet) {
		assertTrue(packet == _tCodedPiece);
		assertFalse(_packetDroppedListerReceivedCallback);
		_packetDroppedListerReceivedCallback = true;
	}
	
	public void testEncodingDecoding() {
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, _tCodedPiece);
		
		TNetworkCoding_CodedPiece tCodedPiece2 =
			(TNetworkCoding_CodedPiece) PacketFactory.unwrapObject(null, raw, false);
		assertTrue(tCodedPiece2.equals(_tCodedPiece));
		assertTrue(_tCodedPiece.equals(tCodedPiece2));
		
		assertTrue(_annotationString.equals(tCodedPiece2.getAnnotations()));
	}
	
	public void testPacketListener() {
		IPacketListener packetListener = _tCodedPiece.getPacketListener();
		assertFalse(packetListener == null);
		
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, _tCodedPiece);
		
		packetListener.packetDropped(raw.getObject());
		assertTrue(_packetDroppedListerReceivedCallback);
		assertFalse(_packetBeingSentListerReceivedCallback);
		assertFalse(_packetSentListerReceivedCallback);
		
		packetListener.packetBeingSent(raw.getObject());
		assertTrue(_packetDroppedListerReceivedCallback);
		assertTrue(_packetBeingSentListerReceivedCallback);
		assertFalse(_packetSentListerReceivedCallback);

		packetListener.packetSent(raw.getObject());
		assertTrue(_packetDroppedListerReceivedCallback);
		assertTrue(_packetBeingSentListerReceivedCallback);
		assertTrue(_packetSentListerReceivedCallback);
	}
}

class SimplePacketListener implements IPacketListener {
	
	TNetworkCoding_CodedPieceTest _tester;
	
	SimplePacketListener(TNetworkCoding_CodedPieceTest tester) {
		_tester = tester;
	}
	
	@Override
	public void packetDropped(IPacketable packet) {
		_tester.packetDroppedNotificationReceived(packet);
	}

	@Override
	public void packetSent(IPacketable packet) {
		_tester.packetSentNotificationReceived(packet);
	}

	@Override
	public void packetBeingSent(IPacketable packet) {
		_tester.packetBeingSentNotificationReceived(packet);		
	}
}
