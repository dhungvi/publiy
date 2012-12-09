package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class TNetworkCoding_CodedPieceIdReqBreak extends TNetworkCoding_CodedPieceIdReq {
	
	public final byte _breakingSize;

	protected TNetworkCoding_CodedPieceIdReqBreak(
			CodedPieceIdReqTypes type, InetSocketAddress sender, Sequence sequence, int num) {
		super(type, sender, sequence);
		
		if(num > Byte.MAX_VALUE)
			throw new IllegalArgumentException("Num too big: " + num);
		
		_breakingSize = (byte) num;
	}
	
	public TNetworkCoding_CodedPieceIdReqBreak(
			InetSocketAddress sender, Sequence sequence, int breakingSize) {
		this(CodedPieceIdReqTypes.BREAK, sender, sequence, breakingSize);
	}

	public TNetworkCoding_CodedPieceIdReqBreak(ByteBuffer bb, int bbOffset) {
		super(bb, bbOffset);
		
		int offset = bbOffset + super.getContentSize();
		_breakingSize = bb.get(offset);
	}
	
	@Override
	public int getContentSize() {
		return super.getContentSize() + 1;
	}
	
	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer bb, int bbOffset) {
		int offset = bbOffset + super.putObjectInBuffer(localSequencer, bb, bbOffset);
		
		bb.put(offset, _breakingSize);
		offset += 1;
		
		return bbOffset - offset;
	}
	
	@Override
	public boolean equals(Object obj) {
		boolean superResult = super.equals(obj);
		if(!superResult)
			return false;
		
		TNetworkCoding_CodedPieceIdReqBreak tBreakObj = (TNetworkCoding_CodedPieceIdReqBreak) obj;
		return tBreakObj._breakingSize == _breakingSize;
	}
	
	@Override
	public String toString() {
		return super.toString() + "*" + _breakingSize;
	}
}
