package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.TestCase;

public class SequenceTest extends TestCase {
	
	protected final int REGRESSION_COUNT = 10000;

	@Override
	public void setUp() { }
	
	public void test() {
		InetSocketAddress address = Sequence.getRandomAddress();
		
		int offset = 1;
		byte[] bArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+1];
		Sequence.putAddressInByteArray(address, bArray, offset);
		InetSocketAddress ad1 = Sequence.readAddressFromByteArray(bArray, offset);
		assertTrue(ad1.equals(address));
	}
	
	public void testAddressInOutByteBuffer() {
		for ( int i=0 ; i<65536 ; i+=10 )
		{
			InetSocketAddress address = new InetSocketAddress("142.1.147.179", i);
			
			byte[] bArray = Sequence.getAddressInByteArray(address);
			InetSocketAddress ad1 = Sequence.readAddressFromByteArray(bArray);
			assertTrue(ad1.equals(address));
			
			
			ByteBuffer bb = ByteBuffer.allocate(2 * Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
			bb.position(Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
			bb.put(bArray);

			InetSocketAddress ad2 = Sequence.readAddressFromByteBuffer(bb, Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
			assertTrue(ad2.equals(address));
		}
	}
	
	public void testSequenceInOutByteBuffer() {
		Random rand = new Random();
		for ( int i=0 ; i<65536 ; i++ )
		{
			long epoch = rand.nextLong();
			int order = rand.nextInt();
			InetSocketAddress address = new InetSocketAddress("142.1.147.179", i);
			Sequence seq = new Sequence(address, epoch, order);
			
			int randOffset = rand.nextInt(256);
			ByteBuffer bb = ByteBuffer.allocate(randOffset + Sequence.SEQ_IN_BYTES_ARRAY_SIZE);
			
			seq.putObjectIntoByteBuffer(bb, randOffset);
			Sequence seq1 = Sequence.readSequenceFromByteBuff(bb, randOffset);
			assertEquals(seq, seq1);
			
			byte[] sArray = seq.getInByteArray();
			Sequence seq2 = Sequence.readSequenceFromBytesArray(sArray);
			assertEquals(seq, seq2);
			
			bb = ByteBuffer.allocate(2 * Sequence.SEQ_IN_BYTES_ARRAY_SIZE);
			bb.position(Sequence.SEQ_IN_BYTES_ARRAY_SIZE);
			bb.put(sArray);
			
			Sequence seq3 = Sequence.readSequenceFromByteBuff(bb, Sequence.SEQ_IN_BYTES_ARRAY_SIZE);
			assertEquals(seq, seq3);
		}
	}
	
	public void testAddressParsing() {
		for(int i=0 ; i<REGRESSION_COUNT ; i++) {
			InetSocketAddress address = Sequence.getRandomAddress();
			{
				String addressStrValue = address.toString();
				InetSocketAddress addressFromString = Sequence.readAddressFromString(addressStrValue);
				assertEquals(address, addressFromString);
			}
			
			{
				String addressStrValue = "" + address;
				InetSocketAddress addressFromString = Sequence.readAddressFromString(addressStrValue);
				assertEquals(address, addressFromString);
			}
		}
	}

	public void testSequenceParsing() {
		for(int i=0 ; i<REGRESSION_COUNT ; i++) {
			Sequence sequence = Sequence.getRandomSequence();
			{
				String sequenceStrValue = sequence.toStringLong();
				Sequence sequenceFromString = Sequence.readSequenceFromString(sequenceStrValue);
				assertTrue(sequence.equals(sequenceFromString));
			}
			
			{
				String sequenceStrValue = "" + sequence.toStringLong();
				Sequence sequenceFromString = Sequence.readSequenceFromString(sequenceStrValue);
				assertTrue(sequence.equals(sequenceFromString));
			}
		}
	}
	
	public void testInt() {
		byte[] data = new byte[Integer.SIZE/8];
		for(int i=0 ; i<REGRESSION_COUNT ; i++) {
			assertEquals(Integer.SIZE/8, Sequence.putIntInByteArray(i, data, 0));
			assertEquals(i, Sequence.readIntFromByteArray(data, 0));
		}
	}

	public void testLong() {
		byte[] data = new byte[Long.SIZE/8];
		for(long l=0 ; l<REGRESSION_COUNT ; l++) {
			assertEquals(Long.SIZE/8, Sequence.putLongInByteArray(l, data, 0));
			assertEquals(l, Sequence.readLongFromByteArray(data, 0));
		}
	}
}
