package org.msrg.publiy.communication.core.packet;

import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public interface ISemiPacketable {

	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff);
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer bb, int bbOffset);
	public String toString();
	
	public int getContentSize();
}
