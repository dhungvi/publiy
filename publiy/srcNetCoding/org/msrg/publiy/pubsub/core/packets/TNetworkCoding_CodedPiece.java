package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.networkcodes.PSCodedCoefficients;
import org.msrg.publiy.networkcodes.PSCodedPiece;

import org.msrg.raccoon.matrix.bulk.BulkMatrix;
import org.msrg.raccoon.matrix.bulk.SliceMatrix;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class TNetworkCoding_CodedPiece extends PSCodedPiece implements IPacketable {

	public final InetSocketAddress _sender;
	String _annotations = "";
	public final IPacketListener _packetListener;
	
	public TNetworkCoding_CodedPiece(
			InetSocketAddress sender,
			IPacketListener packetListener,
			Sequence sourceSeq,
			PSCodedCoefficients cc, SliceMatrix sm,
			boolean fromMainSeed) {
		super(sourceSeq, cc, sm, fromMainSeed);
		
		_sender = sender;
		_packetListener = packetListener;
	}

	public TNetworkCoding_CodedPiece(
			InetSocketAddress sender,
			IPacketListener packetListener,
			Sequence sourceSeq,
			PSCodedCoefficients cc, BulkMatrix bm, boolean fromMainSeed) {
		super(sourceSeq, cc, bm, fromMainSeed);
		
		_sender = sender;
		_packetListener = packetListener;
	}

	public TNetworkCoding_CodedPiece(ByteBuffer bb, int bbOffset) {
		super(bb, bbOffset+1+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);

		int offset = bbOffset;
		byte typeB = bb.get(offset);
		PacketableTypes type = PacketableTypes.getPacketableTypes(typeB);
		if(type != getObjectType())
			throw new IllegalArgumentException();
		offset += 1;
		
		_sender = Sequence.readAddressFromByteBuffer(bb, offset);
		offset += Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
		
		_packetListener = null;
	}

	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TCODEDPIECE;
	}

	@Override
	public void annotate(String annotation) {
		_annotations += annotation;
	}

	@Override
	public String getAnnotations() {
		return _annotations;
	}

	@Override
	public Sequence getSourceSequence() {
		return _sourceSeq;
	}

	@Override
	public IPacketListener getPacketListener() {
		return _packetListener;
	}

	@Override
	public int getContentSize() {
		return 1 + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE + super.getContentSize();
	}
	
	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer bb, int bbOffset) {
		int offset = bbOffset;
		bb.put(offset, getObjectType()._codedValue);
		offset += 1;
			
		byte[] senderByteArray = Sequence.getAddressInByteArray(_sender);
		bb.position(offset);
		bb.put(senderByteArray);
		offset += Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
		
		return (offset - bbOffset) + super.putObjectInBuffer(localSequencer, bb, offset);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		TNetworkCoding_CodedPiece tCodedPiece = (TNetworkCoding_CodedPiece) obj;
		
		if(!_sender.equals(tCodedPiece._sender))
			return false;

		return super.equals(tCodedPiece);
	}
}
