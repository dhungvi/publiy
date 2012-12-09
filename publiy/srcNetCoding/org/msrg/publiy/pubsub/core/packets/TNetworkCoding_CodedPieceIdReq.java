package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public abstract class TNetworkCoding_CodedPieceIdReq implements IPacketable {
	
	public final InetSocketAddress _sender;
	public final Sequence _sequence;
	public final CodedPieceIdReqTypes _reqType;
	protected String _annotation = "";
	
	TNetworkCoding_CodedPieceIdReq(CodedPieceIdReqTypes reqType, InetSocketAddress sender, Sequence sequence) {
		_sequence = sequence;
		_sender = sender;
		_reqType = reqType;
	}
	
	public TNetworkCoding_CodedPieceIdReq(ByteBuffer bb, int bbOffset) {
		int offset = bbOffset;
		byte typeB = bb.get(bbOffset);
		PacketableTypes type = PacketableTypes.getPacketableTypes(typeB);
		if(type != getObjectType())
			throw new IllegalArgumentException();
		offset += 1;
		
		
		byte reqTypeB = bb.get(offset);
		_reqType = CodedPieceIdReqTypes.getCodedPieceIdReqTypes(reqTypeB);
		offset += 1;
		
		_sender = Sequence.readAddressFromByteBuffer(bb, offset);
		offset += Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
		
		_sequence = Sequence.readSequenceFromByteBuff(bb, offset);
		offset += Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	}
	
	@Override
	public final PacketableTypes getObjectType() {
		return PacketableTypes.TCODEDPIECE_ID_REQ;
	}
	
	@Override
	public IPacketListener getPacketListener() {
		return null;
	}
	
	@Override
	public int getContentSize() {
		return 1 + 1 + Sequence.SEQ_IN_BYTES_ARRAY_SIZE + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
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

		bb.put(offset, _reqType._byteValue);
		offset += 1;
		
		byte[] senderByteArray = Sequence.getAddressInByteArray(_sender);
		bb.position(offset);
		bb.put(senderByteArray);
		offset += Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
		
		offset += _sequence.putObjectIntoByteBuffer(bb, offset);
		
		return offset - bbOffset;
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
		
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		TNetworkCoding_CodedPieceIdReq tCodedIdReq =
			(TNetworkCoding_CodedPieceIdReq) obj;
		
		if(!_sender.equals(tCodedIdReq._sender))
			return false;
		
		if(!tCodedIdReq._sequence.equals(_sequence))
			return false;

		if(_reqType != tCodedIdReq._reqType)
			return false;
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return _sequence.hashCode();
	}

	public CodedPieceIdReqTypes getRequestType() {
		return _reqType;
	}
	
	public static IPacketable getObjectFromBuffer(ByteBuffer bb, int bbOffset) {
		int offset = bbOffset;
		byte typeB = bb.get(bbOffset);
		PacketableTypes type = PacketableTypes.getPacketableTypes(typeB);
		if(type != PacketableTypes.TCODEDPIECE_ID_REQ)
			throw new IllegalArgumentException();
		offset += 1;
		
		byte reqTypeB = bb.get(offset);
		CodedPieceIdReqTypes reqType = CodedPieceIdReqTypes.getCodedPieceIdReqTypes(reqTypeB);
		switch(reqType) {
		case BREAK:
			return new TNetworkCoding_CodedPieceIdReqBreak(bb, bbOffset);
			
		case SEND_ID:
			return new TNetworkCoding_CodedPieceIdReqSendId(bb, bbOffset);
			
		case SEND_CONTENT:
			return new TNetworkCoding_CodedPieceIdReqSendContentSendId(bb, bbOffset);
			
		case SEND_CONTENT_ID:
			return new TNetworkCoding_CodedPieceIdReqSendContent(bb, bbOffset);
			
		case BREAK_SEND_ID:
			return new TNetworkCoding_CodedPieceIdReqBreakSendId(bb, bbOffset);
			
		case PLIST_REPLY:
			return new TNetworkCoding_PListReply(bb, bbOffset);

		case PLIST_REQ:
			return new TNetworkCoding_PListReq(bb, bbOffset);
			
		case BREAK_DECLINE:
			return new TNetworkCoding_CodedPieceIdReqBreakDeclined(bb, bbOffset);
			
		default:
			throw new UnsupportedOperationException("Unknown type: " + reqType);
		}
	}
	
	@Override
	public String toString() {
		return "CodedPieceIdReq[" + _reqType + "]"
				+ _sequence.toStringVeryVeryShort()
				+ ", FROM:" + _sender.getPort();
	}
}
