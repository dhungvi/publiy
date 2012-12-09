package org.msrg.publiy.networkcodes;

import java.nio.ByteBuffer;

import org.msrg.raccoon.CodedPiece;
import org.msrg.raccoon.matrix.bulk.BulkMatrix;
import org.msrg.raccoon.matrix.bulk.SliceMatrix;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSliceMatrix;

import org.msrg.publiy.communication.core.packet.ISemiPacketable;

public class PSCodedPiece extends CodedPiece implements ISemiPacketable {

	public final Sequence _sourceSeq;
	public boolean _fromMainSeed;
	
	public PSCodedPiece(ByteBuffer bb, int bbOffset) {
		this(bb, bbOffset,
				new PSCodedCoefficients(bb, bbOffset + Sequence.SEQ_IN_BYTES_ARRAY_SIZE+1));
	}
	
	private PSCodedPiece(ByteBuffer bb, int bbOffset, PSCodedCoefficients cc) {
		super(cc,
				new PSSliceMatrix(
						Sequence.readSequenceFromByteBuff(bb, bbOffset),
						bb,
						bbOffset+Sequence.SEQ_IN_BYTES_ARRAY_SIZE+1+cc.getSizeInByteBuffer())
		);
		
		_sourceSeq = Sequence.readSequenceFromByteBuff(bb, bbOffset);
		_fromMainSeed = bb.get(bbOffset+Sequence.SEQ_IN_BYTES_ARRAY_SIZE)==0?false:true;
	}
	
	public PSCodedPiece(Sequence sourceSeq, PSCodedCoefficients cc, BulkMatrix bm, boolean fromMainSeed) {
		super(cc, bm);
		
		_sourceSeq = sourceSeq;
		_fromMainSeed = fromMainSeed;// = cc.getColumnSize() == Broker.ROWS;
	}
	
	public PSCodedPiece(Sequence sourceSeq, PSCodedCoefficients cc, SliceMatrix sm, boolean fromMainSeed) {
		super(cc, sm);
		
		_sourceSeq = sourceSeq;
		_fromMainSeed = fromMainSeed;// = cc.getColumnSize() == Broker.ROWS;
	}
	
	public Sequence getSourceSequence() {
		return _sourceSeq;
	}

	public static PSCodedPiece makeCodedPiece(Sequence sourceSeq, BulkMatrix bm, boolean fromMainSeed) {
		PSCodedCoefficients cc = new PSCodedCoefficients(bm._rows);
		return new PSCodedPiece(sourceSeq, cc, bm, fromMainSeed);
	}
	
	@Override
	public int getContentSize() {
		return Sequence.SEQ_IN_BYTES_ARRAY_SIZE + 1 +
				_cc.getSizeInByteBuffer() +
				4 +
				_codedContent.getSizeInByteBuffer();
	}

	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		putObjectInBuffer(localSequencer, buff, 0);
	}
	
	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer bb, int bbOffset) {
		int offset = bbOffset;
		
		_sourceSeq.putObjectIntoByteBuffer(bb, offset);
		offset += Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
		
		bb.put(offset, (byte)(_fromMainSeed?1:0));
		offset += 1;
		
		offset += ((PSCodedCoefficients)_cc).putObjectInBuffer(localSequencer, bb, offset);
		offset += ((PSSliceMatrix)_codedContent).putObjectInBuffer(localSequencer, bb, offset);
		
		return (offset - bbOffset);
	}

	
	public final int getCols() {
		return _codedContent._cols;
	}
	
	public final int getRows() {
		return _cc.getLength();
	}
	
	public PSCodedCoefficients getCodedCoefficients() {
		return (PSCodedCoefficients) _cc;
	}
	
	@Override
	public String toString() {
		return _sourceSeq.toStringVeryVeryShort() + ":" + super.toString();
	}
	
	public void setFromMainSeed() {
		_fromMainSeed = true;
	}
}
