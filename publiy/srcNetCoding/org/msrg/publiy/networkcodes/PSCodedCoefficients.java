package org.msrg.publiy.networkcodes;

import java.nio.ByteBuffer;

import org.msrg.raccoon.CodedCoefficients;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.utils.ByteBufferUtil;

import org.msrg.publiy.communication.core.packet.ISemiPacketable;

public class PSCodedCoefficients extends CodedCoefficients implements ISemiPacketable {
	
	public PSCodedCoefficients(byte[] coefficients) {
		super(coefficients);
	}

	public PSCodedCoefficients(int length) {
		super(length);
	}

	public PSCodedCoefficients(Byte[] coefficients) {
		super(coefficients);
	}

	public PSCodedCoefficients(ByteBuffer bb, int beginOffset) {
		super(ByteBufferUtil.readBytesFromByteBuffer(bb, beginOffset + 4, bb.getInt(beginOffset)));
	}
	
	@Override
	public int getContentSize() {
		return 4 + 				// For size;
				getLength();	// For coded coefficients
	}

	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		putObjectInBuffer(localSequencer, buff, 0);
	}

	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer bb, int bbOffset) {
		int size = getLength();
		
		int offset = bbOffset;
		bb.putInt(offset, size);
		
		offset += 4;
		bb.position(offset);
		
		for(int i=0 ; i<size; i++)
			bb.put(i+offset, _b[0][i]);
		
		return 4 + size;
	}
	
}
