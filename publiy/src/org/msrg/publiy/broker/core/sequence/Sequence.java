package org.msrg.publiy.broker.core.sequence;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.msrg.publiy.broker.BrokerIdentityManager;


public class Sequence implements Serializable {
	
	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = -6495029810387590629L;
	public transient static final int INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE = 8;
	public transient static final int SEQ_IN_BYTES_ARRAY_SIZE = 20;
	public transient static final byte [] NULL_SEQ_BYTES = new byte[SEQ_IN_BYTES_ARRAY_SIZE];
	static {
		for(int i=0 ; i<NULL_SEQ_BYTES.length ; i++)
			NULL_SEQ_BYTES[i] = (byte)0;
	}

	public final int _order;
	public final long _epoch;
	public final InetSocketAddress _address;
	
	public static Sequence getPsuedoSequence(InetSocketAddress addr, int order) {
		return new Sequence(addr, 0, order);
	}
	
	public Sequence(InetSocketAddress addr, long epoch, int order) {
		_order = order;
		_epoch = epoch;
		_address = addr;
	}

	public boolean succeedsOrEquals(Sequence seq2) {
		if ( !this._address.equals(seq2._address) )
			throw new IllegalArgumentException("Sequence::succeeds(.) - ERROR, sequences do not correspond to the same inetAddress.");
		
		if ( this._epoch > seq2._epoch )
			return true;
		
		else if ( this._epoch < seq2._epoch )
			return false;
		
		if ( this._order >= seq2._order )
			return true;
		
		return false;
	}
	
	public boolean succeeds(Sequence seq2) {
		if ( !this._address.equals(seq2._address) )
			throw new IllegalArgumentException("Sequence::succeeds(.) - ERROR, sequences do not correspond to the same inetAddress.");
		if ( this._epoch > seq2._epoch )
			return true;
		else if ( this._epoch < seq2._epoch )
			return false;
		if ( this._order > seq2._order )
			return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		return hashCodeExact();
	}
	
	public int hashCodeExact() {
		return _address.hashCode() + _order;
	}
	
	public boolean equals(Object obj) {
		return equalsExact(obj);
	}
	
	public boolean equalsExact(Object obj) {
		if ( obj == null )
			return false;
		if ( obj.getClass().isInstance(this) ) {
			Sequence seqObj = (Sequence) obj;
			if ( !this._address.equals(seqObj._address) )
				return false;
		
			return (this._epoch == seqObj._epoch) && (this._order == seqObj._order);
		}
		else
			return obj.equals(this);
	}
	
	public InetSocketAddress getAddress() {
		return _address;
	}
	
	public long getEpoch() {
		return _epoch;
	}
	
	public int getOrder() {
		return _order;
	}
	
	@Override
	public String toString() {
		return toStringVeryShort();
	}

	public String toString(BrokerIdentityManager idMan) {
		if(idMan == null)
			return toString();
		
		return "Seq[" + idMan.getBrokerId(_address) + "_" + _order + "]";
	}
	
	public String toStringLong() {
		return toStringClean(_address) + "_" + _epoch + "_" + _order;
	}

	public String toStringShort() {
		return "Seq[" + _address.getPort() + ":" + "_" + (_epoch%10000) + "_" + _order + "]";
	}
	
	public String toStringVeryVeryShort() {
		return _address.getPort() + "_" + _order;
	}
	
	public String toStringVeryShort() {
		return "Seq[" + _address.getPort() + "_" + _order + "]";
	}
	
	public static Sequence readSequenceFromByteBuff(ByteBuffer bb, int bbOffset)throws IllegalArgumentException{
		int offset = bbOffset;
		
		InetSocketAddress inetAddr = readAddressFromByteBuffer(bb, bbOffset);
		offset += INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
		
		long epoch = bb.getLong(offset);
		offset += 8;
		
		int order = bb.getInt(offset);
		
		Sequence seq = new Sequence(inetAddr, epoch, order);
		if(order == 0 && epoch == 0) {
			byte[] bytes = inetAddr.getAddress().getAddress();
			for(byte b : bytes)
				if(b != 0)
					return seq;
			return null;
		}
		return seq;
	}
	
	public static Sequence readSequenceFromBytesArray(byte[] bArray)throws IllegalArgumentException{
		if ( bArray == null )
			throw new IllegalArgumentException("Bytes is null.");
		if ( bArray.length != SEQ_IN_BYTES_ARRAY_SIZE )
			throw new IllegalArgumentException("Sequence representation in byte[] is 18b long.");
		boolean isNull = true;
		for ( int i=0 ; i<SEQ_IN_BYTES_ARRAY_SIZE && isNull; i++ )
			if ( bArray[i] != 0 )
				isNull = false;
		if ( isNull )
			return null;
		byte[] bAddressArray = new byte[INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		for ( int i=0 ; i<bAddressArray.length ; i++ )
			bAddressArray[i] = bArray[i];
		InetSocketAddress inetAddr = readAddressFromByteArray(bAddressArray);
		
		long epoch = ((long)(bArray[8] & 0xFF) << 8*7);
			 epoch+= ((long)(bArray[9] & 0xFF) << 8*6);
			 epoch+= ((long)(bArray[10] & 0xFF) << 8*5); 
			 epoch+= ((long)(bArray[11] & 0xFF) << 8*4);
			 epoch+= ((long)(bArray[12]& 0xFF) << 8*3);
			 epoch+= ((long)(bArray[13]& 0xFF) << 8*2);
			 epoch+= ((long)(bArray[14]& 0xFF) << 8*1); 
			 epoch+= ((long)(bArray[15]& 0xFF) << 8*0);
		int order = (bArray[16] << 8*3) & 0xFF000000 ;
			order+= (bArray[17] << 8*2) & 0xFF0000 ;
			order+= (bArray[18] << 8*1) & 0xFF00 ;
			order+= (bArray[19] << 8*0) & 0xFF;
		
		return new Sequence(inetAddr, epoch, order);
	}
	
	public int getContentSize() {
		return SEQ_IN_BYTES_ARRAY_SIZE;
	}
	
	public int putObjectIntoByteBuffer(ByteBuffer bb, int offset) {
		byte[] bytes = getInByteArray();
		bb.position(offset);
		bb.put(bytes);
		
		return bytes.length;
	}
	
	public byte[] getInByteArray() {
		byte[] bArray = new byte[SEQ_IN_BYTES_ARRAY_SIZE];

		byte[] bAddressArray = getAddressInByteArray(_address);
		for ( int i=0 ; i<bAddressArray.length ; i++ )
			bArray[i] = bAddressArray[i];
		
		bArray[8] = (byte)((_epoch>>8*7) & 0xFF);
		bArray[9] = (byte)((_epoch>>8*6) & 0xFF);
		bArray[10] = (byte)((_epoch>>8*5) & 0xFF);
		bArray[11] = (byte)((_epoch>>8*4) & 0xFF);
		bArray[12]= (byte)((_epoch>>8*3) & 0xFF);
		bArray[13]= (byte)((_epoch>>8*2) & 0xFF);
		bArray[14]= (byte)((_epoch>>8*1) & 0xFF);
		bArray[15]= (byte)((_epoch>>8*0) & 0xFF);
		
		bArray[16]= (byte)((_order>>8*3) & 0xFF);
		bArray[17]= (byte)((_order>>8*2) & 0xFF);
		bArray[18]= (byte)((_order>>8*1) & 0xFF);
		bArray[19]= (byte)((_order>>8*0) & 0xFF);
		
		return bArray;
	}
	
	public static byte[] getAddressInByteArray(InetSocketAddress address) {
		byte[] bArray = new byte[INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		putAddressInByteArray(address, bArray, 0);
		return bArray;
	}
	
	public static int putAddressInByteArray(InetSocketAddress address, byte[] bArray, int offset) {
		if(bArray.length < offset + INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE - 1)
			throw new IllegalArgumentException();
		
		int i = offset;
		byte[] ip = address.getAddress().getAddress();
		bArray[i++] = ip[0];
		bArray[i++] = ip[1];
		bArray[i++] = ip[2];
		bArray[i++] = ip[3];
		
		int port = address.getPort();
		bArray[i++] = (byte)((port>>24) & 0xFF);
		bArray[i++] = (byte)((port>>16) & 0xFF);
		bArray[i++] = (byte)((port>>8) & 0xFF);
		bArray[i++] = (byte)((port>>0) & 0xFF);
		
		return i;
	}
	
	public static InetSocketAddress readAddressFromByteBuffer(ByteBuffer bb, int bbOffset) {
		byte[] byteAddress = new byte[8];
		bb.position(bbOffset);
		bb.get(byteAddress);
		
		return readAddressFromByteArray(byteAddress);
	}
	
	public static InetSocketAddress readAddressFromByteArray(byte[] bytes) {
		return readAddressFromByteArray(bytes, 0);
	}
	
	public static InetSocketAddress readAddressFromByteArray(byte[] bytes, int offset) {
		if ( bytes.length < INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE + offset - 1)
			throw new IllegalArgumentException("Array length mismatch: " + bytes.length);
		String ipStr = "" + (((int)bytes[0+offset])&0xFF) + "." + (((int)bytes[1+offset])&0xFF) + "." + (((int)bytes[2+offset])&0xFF) + "." + (((int)bytes[3+offset])&0xFF);
		int port = (bytes[4+offset]<<24) & 0xFF000000;
			port += (bytes[5+offset]<<16) & 0xFF0000;
			port += (bytes[6+offset]<<8) & 0xFF00;
			port += (bytes[7+offset]<<0) & 0xFF;
		return new InetSocketAddress(ipStr, port);
	}

	public static InetSocketAddress getRandomLocalAddress() {
		Random rand = new Random();
		return new InetSocketAddress("127.0.0.1", Math.abs(rand.nextInt()) % Short.MAX_VALUE);
	}
	
	public static InetSocketAddress[] getRandomAddresses(int count) {
		Random rand = new Random();
		InetSocketAddress[] addresses = new InetSocketAddress[count];
		for(int i=0 ; i<addresses.length ; i++)
			addresses[i] = getRandomAddress(rand);
		
		return addresses;
	}

	public static Set<InetSocketAddress> getRandomAddressesSet(int count) {
		Random rand = new Random();
		Set<InetSocketAddress> addresses = new HashSet<InetSocketAddress>();
		for(int i=0 ; i<count ; i++)
			addresses.add(getRandomAddress(rand));
		
		return addresses;
	}

	public static Set<InetSocketAddress> getIncrementalLocalAddressesSet(int minPort, int maxPort, int steps) {
		InetSocketAddress[] addresses =
			getIncrementalLocalAddresses(minPort, maxPort, steps);
		
		Set<InetSocketAddress> addressesSet = new HashSet<InetSocketAddress>();
		for(InetSocketAddress address : addresses)
			addressesSet.add(address);
		
		return addressesSet;
	}
	
	public static InetSocketAddress[] getIncrementalLocalAddresses(int minPort, int maxPort, int steps) {
		InetSocketAddress[] addresses = new InetSocketAddress[(maxPort - minPort)/steps + 1];
		for(int i=0 ; i<addresses.length ; i++)
			addresses[i] = new InetSocketAddress("127.0.0.1", i*steps+minPort);
		
		return addresses;
	}
	
	public static InetSocketAddress getRandomAddress() {
		Random rand = new Random();
		return getRandomAddress(rand);
	}
	
	public static InetSocketAddress getRandomAddress(Random rand) {
		return new InetSocketAddress("" +
				Math.abs(rand.nextInt(Byte.MAX_VALUE)) + "." +
				Math.abs(rand.nextInt(Byte.MAX_VALUE)) + "." +
				Math.abs(rand.nextInt(Byte.MAX_VALUE)) + "." +
				Math.abs(rand.nextInt(Byte.MAX_VALUE)),
				Math.abs(rand.nextInt()) % Short.MAX_VALUE);
	}
	
	public static Sequence getRandomSequence() {
		Random rand = new Random();
		return new Sequence(getRandomAddress(rand), rand.nextLong(), rand.nextInt());
	}

	public static Sequence getRandomSequence(InetSocketAddress address) {
		Random rand = new Random();
		return new Sequence(address, rand.nextLong(), rand.nextInt());
	}
	
	static Pattern _INETSOCKET_ADDRESS_PATTERN =
//		Pattern.compile("/?\\W*([0-9]*)\\.([0-9]*)\\.([0-9]*)\\.([0-9]*)\\:([0-9]*)\\W*");
		Pattern.compile("/?\\W*([^:]*)\\:([0-9]*)\\W*");
	public static InetSocketAddress readAddressFromString(String addressStrValue) {
		if(addressStrValue == null)
			return null;
		synchronized(_INETSOCKET_ADDRESS_PATTERN) {
			Matcher matcher = _INETSOCKET_ADDRESS_PATTERN.matcher(addressStrValue);
			if(!matcher.matches())
				return null;
			
			if(matcher.groupCount() != 2)
				return null;
			
			InetSocketAddress address =
				new InetSocketAddress(
						matcher.group(1), new Integer(matcher.group(2)));
			
			return address;
		}
	}

	static Pattern _SEQUENCE_PATTERN =
		Pattern.compile(_INETSOCKET_ADDRESS_PATTERN +
				"_(\\-?[0-9]*)_(\\-?[0-9]*)\\W*");
	public static Sequence readSequenceFromString(String sequenceStrValue) {
		if(sequenceStrValue == null)
			return null;
		synchronized(_SEQUENCE_PATTERN) {
			Matcher matcher = _SEQUENCE_PATTERN.matcher(sequenceStrValue);
			if(!matcher.matches())
				return null;
			
			if(matcher.groupCount() != 4)
				return null;
			
			InetSocketAddress address =
				new InetSocketAddress(
						matcher.group(1), new Integer(matcher.group(2)));
			
			String epochStr = matcher.group(3);
			String orderStr = matcher.group(4);
			return new Sequence(address, new Long(epochStr).longValue(), new Integer(orderStr).intValue());
		}
	}
	
	public static String toStringClean(InetSocketAddress address) {
		String addressStr = address.getAddress().toString();
		int lastIndexOfSlash = addressStr.lastIndexOf('/');
		if(lastIndexOfSlash >= 0)
			addressStr = addressStr.substring(lastIndexOfSlash + 1);
		return addressStr + ":" + address.getPort();
	}

	public static int putLongInByteArray(long l, byte[] data, int offset) {
		for(int i=0 ; i<Long.SIZE/8 ; i++)
			data[offset+i] = (byte) ((l >> (Long.SIZE/8 - 1 - i)*8));
		return offset += Long.SIZE/8;
	}

	public static long readLongFromByteArray(byte[] data, int offset) {
		long l = 0;
		for(int i=0 ; i<Long.SIZE/8 ; i++) {
			l <<= 8;
			l ^= (long) (data[offset+i] & 0xFF);
		}
		return l;
	}
	public static int putIntInByteArray(int v, byte[] data, int offset) {
		for(int i=0 ; i<Integer.SIZE/8 ; i++)
			data[offset+i] = (byte) ((v >> (Integer.SIZE/8 - 1 - i)*8));
		return offset += Integer.SIZE/8;
	}

	public static int readIntFromByteArray(byte[] data, int offset) {
		int v = 0;
		for(int i=0 ; i<Integer.SIZE/8 ; i++) {
			v <<= 8;
			v ^= (int) (data[offset+i] & 0xFF);
		}
		return v;
	}

	public static byte[] subarray(byte[] data, int offset, byte size) {
		byte[] ret = new byte[size];
		for(int i=0 ; i<size ; i++)
			ret[i] = data[offset+i];
		return ret;
	}
}
