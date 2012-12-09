package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.publishSubscribe.Publication;

public class TNetworkCoding_CodedPieceId implements IPacketable {
	
	public final InetSocketAddress _sender;
	public final Sequence _sequence;
	public final Publication _publication;
	public final int _rows;
	public final int _cols;
	protected String _annotation = "";
//	public final IPacketListener _packetListener;
//	public final OverlayNode _destenationOverlayNode;

	public TNetworkCoding_CodedPieceId(
//			IPacketListener packetListener,
//			OverlayNode destenationOverlayNode,
			InetSocketAddress sender,
			Sequence sequence,
			Publication publication, int rows, int cols) {
		_sender = sender;
		_sequence = sequence;
		_publication = publication;
		_rows = rows;
		_cols = cols;
		
//		_packetListener = packetListener;
//		_destenationOverlayNode = destenationOverlayNode;
	}
	
	public TNetworkCoding_CodedPieceId(ByteBuffer bb, int bbOffset) {
		int offset = bbOffset;
		byte typeB = bb.get(offset);
		PacketableTypes type = PacketableTypes.getPacketableTypes(typeB);
		if(type != getObjectType())
			throw new IllegalArgumentException();
		offset += 1;

		_sender = Sequence.readAddressFromByteBuffer(bb, offset);
		offset += Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
		
		_sequence = Sequence.readSequenceFromByteBuff(bb, offset);
		offset += Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
		
		int pubStrLen = bb.getInt(offset);
		offset += 4;
		
		byte[] pubStrBArray = new byte[pubStrLen];
		bb.position(offset);
		bb.get(pubStrBArray);
		offset+=pubStrLen;
		
		String pubStr = new String(pubStrBArray);
		_publication = Publication.decode(pubStr);
		
		_rows = bb.getInt(offset);
		offset += 4;
		
		_cols = bb.getInt(offset);
		
//		_destenationOverlayNode = null;
//		_packetListener = null;
	}
	
	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TCODEDPIECE_ID;
	}

	@Override
	public int getContentSize() {
		return 1 +
				Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE +
				Sequence.SEQ_IN_BYTES_ARRAY_SIZE +
				4 + Publication.encode(_publication).length() +
				4 + 4;
	}

	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		putObjectInBuffer(localSequencer, buff, 0);
	}

	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer bb, int bbOffset) {
		int offset = bbOffset;
		
		bb.put(offset, getObjectType()._codedValue);
		offset += 1;
		
		byte[] senderAddrBArray = Sequence.getAddressInByteArray(_sender);
		bb.position(offset);
		bb.put(senderAddrBArray);
		offset += Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
		
		offset += _sequence.putObjectIntoByteBuffer(bb, offset);
		
		String pubStr = Publication.encode(_publication);
		byte[] pubStrBytes = pubStr.getBytes();
		bb.putInt(offset, pubStrBytes.length);
		offset += 4;
		
		bb.position(offset);
		bb.put(pubStrBytes);
		offset+=pubStrBytes.length;
		
		bb.putInt(offset, _rows);
		offset+=4;
		
		bb.putInt(offset, _cols);
		offset+=4;
		
		return offset - offset;
	}

	@Override
	public void annotate(String annotation) {
		_annotation += annotation;
	}

	@Override
	public String getAnnotations() {
		return _annotation;
	}

	@Override
	public Sequence getSourceSequence() {
		return _sequence;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!this.getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		TNetworkCoding_CodedPieceId tCodedPieceIdObj = (TNetworkCoding_CodedPieceId)obj;
		if(!tCodedPieceIdObj._sequence.equals(_sequence))
			return false;
		
		if(!_sender.equals(tCodedPieceIdObj._sender))
			return false;
		
		if(!_publication.equals(tCodedPieceIdObj._publication))
			return false;
		
		if(_cols != tCodedPieceIdObj._cols)
			return false;
		
		if(_rows != tCodedPieceIdObj._rows)
			return false;
		
		return true;
	}

	@Override
	public IPacketListener getPacketListener() {
//		return _packetListener;
		return null;
	}
	
	@Override
	public String toString() {
		return "CodedPieceId[SENDER:" + _sender.getPort() + "][" + _rows + "x" + _cols + "][" + _sequence + "]";
	}
}
