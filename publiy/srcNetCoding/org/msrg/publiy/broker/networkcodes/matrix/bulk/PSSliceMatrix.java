package org.msrg.publiy.broker.networkcodes.matrix.bulk;

import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.raccoon.CodedCoefficients;
import org.msrg.raccoon.CodedPiece;

import org.msrg.publiy.networkcodes.PSCodedCoefficients;
import org.msrg.publiy.networkcodes.PSCodedPiece;
import org.msrg.publiy.utils.ByteBufferUtil;

import org.msrg.publiy.communication.core.packet.ISemiPacketable;

import org.msrg.raccoon.matrix.bulk.SliceMatrix;

public class PSSliceMatrix extends SliceMatrix implements ISemiPacketable {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = 4397953723109125386L;
	protected final Sequence _sourceSequene;
	
	protected PSSliceMatrix(Sequence sourceSequence, byte[] b) {
		super(b);
		
		_sourceSequene = sourceSequence;
	}

	public PSSliceMatrix(Sequence sourceSequence, ByteBuffer bb, int bbOffset) {
		this(sourceSequence, ByteBufferUtil.readBytesFromByteBuffer(bb, bbOffset+4, bb.getInt(bbOffset)));
	}
	
	public PSSliceMatrix(Sequence sourceSequence, int cols) {
		super(cols);
		
		_sourceSequene = sourceSequence;
	}

	@Override
	public int getContentSize() {
							// ColsOffset is not written!!
		return 4 +			// For size
				_b.length;	// For content
	}

	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer bb, int bbOffset) {
		int offset = bbOffset;
		bb.putInt(offset, _b.length);
		offset += 4;
		
		
		bb.position(offset);
		bb.put(_b);
		
		return getContentSize();
	}
	
	@Override
	public CodedPiece createEmptyCodedPiece(CodedCoefficients cc, SliceMatrix sm) {
		return new PSCodedPiece(_sourceSequene, (PSCodedCoefficients) cc, sm, false);
	}
}
