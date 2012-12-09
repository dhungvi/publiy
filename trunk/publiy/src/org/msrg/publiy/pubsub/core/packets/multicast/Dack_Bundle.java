package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class Dack_Bundle {

	public static final int I_REPORING_ADDRESS = 0;
	public static final int I_BUNDLE_SIZE = Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	public static final int I_SEQUENCES = I_BUNDLE_SIZE + 4;
	public static final int BUNDLE_BASE_SIZE = I_SEQUENCES;
	
	public final InetSocketAddress _reportingAddress;
	public final Sequence[] _sequences;
	
	public Dack_Bundle(InetSocketAddress reportingAddress, Sequence[] sequences) {
		_reportingAddress = reportingAddress;
		_sequences = sequences;
	}
	
	int loadBundleInBuffer(ByteBuffer buff, int initialOffset) {
		int size = getSizeInByteBuffer();
		byte[] bArray;
		
		bArray = Sequence.getAddressInByteArray(_reportingAddress);
		buff.position(initialOffset + I_REPORING_ADDRESS);
		buff.put(bArray);
		
		buff.position(initialOffset + I_BUNDLE_SIZE);
		buff.putInt(_sequences.length);
		
		for(int i=0 ; i<_sequences.length ; i++) {
			bArray = _sequences[i].getInByteArray();
			int offset = I_SEQUENCES + i * Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
			buff.position(initialOffset + offset);
			buff.put(bArray);
		}
		
		return size;
	}
	
	int getSizeInByteBuffer() {
		return BUNDLE_BASE_SIZE + _sequences.length * Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	}
	
	static Dack_Bundle getBundleFromBuffer(ByteBuffer bdy, int buffOffset) {
		if(bdy.capacity() < BUNDLE_BASE_SIZE)
			throw new IllegalArgumentException("ByteBuffer not of the minimum size: " + bdy.capacity());
			
		byte[] bArray;
		
		bdy.position(buffOffset + I_REPORING_ADDRESS);
		bArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		bdy.get(bArray);
		InetSocketAddress reportingAddress = Sequence.readAddressFromByteArray(bArray);
		
		int sequenceCount = bdy.getInt(buffOffset + I_BUNDLE_SIZE);
		
		int legalSize = BUNDLE_BASE_SIZE + sequenceCount*Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
		if(legalSize + buffOffset > bdy.capacity())
			throw new IllegalArgumentException("ByteBuffer size must be: " + legalSize + ", it is: " + bdy.capacity());
		
		byte[] seqBArray = new byte[Sequence.SEQ_IN_BYTES_ARRAY_SIZE];
		Sequence[] sequences = new Sequence[sequenceCount];
		for(int i=0 ; i<sequenceCount ; i++) {
			int offset = BUNDLE_BASE_SIZE + i*Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
			bdy.position(buffOffset + offset);
			bdy.get(seqBArray);
			sequences[i] = Sequence.readSequenceFromBytesArray(seqBArray);
		}
		
		return new Dack_Bundle(reportingAddress, sequences);
	}

	@Override
	public String toString() {
		String str = "DBundle (" + _sequences.length + "@" + _reportingAddress.getPort() + ")-[";
		for(int i=0 ; i<_sequences.length ; i++)
			str += _sequences[i] + ((i<_sequences.length-1)?"," : "");
		return str + "]";
	}
	
	public static Dack_Bundle makeSampleBundle(int seqSize, LocalSequencer localSequencer) {
		InetSocketAddress addr = new InetSocketAddress("127.0.0.2", 2000);
		
		Sequence[] sequences = new Sequence[seqSize];
		for(int i=0 ; i<seqSize ; i++)
			sequences[i] = localSequencer.getNext();
		
		Dack_Bundle dackBundle = new Dack_Bundle(addr, sequences);
		return dackBundle;
	}
	
	public static void main(String[] argv) {
		LocalSequencer localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 1000));
		
		Dack_Bundle dackBundle = makeSampleBundle(10, localSequencer);
		System.out.println(dackBundle); // OK
		
		ByteBuffer bdy = ByteBuffer.allocate(dackBundle.getSizeInByteBuffer());
		dackBundle.loadBundleInBuffer(bdy, 0);
		
		Dack_Bundle dackBundle2 = Dack_Bundle.getBundleFromBuffer(bdy, 0);
		System.out.println(dackBundle2); // OK
	}
}
