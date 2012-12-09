package org.msrg.publiy.utils;

import java.nio.ByteBuffer;

public class ByteBufferUtil {

	public static byte[] readBytesFromByteBuffer(ByteBuffer bb, int bbOffset, int bbReadLength) {
		byte[] bytes = new byte[bbReadLength];
		bb.position(bbOffset);
		bb.get(bytes);

		return bytes;
	}
}
