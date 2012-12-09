package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class TMulticast_Depart extends TMulticast {
	
	public static final int TMULTICAST_DEPART_CONTENT_SIZE = 2 * Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	public static final int TMULTICAST_DEPART_SUB_CONTENT_SIZE = TMULTICAST_DEPART_CONTENT_SIZE + 2;
	
	private InetSocketAddress _joiningNode;
	private InetSocketAddress _joinPoint;
	
	public InetSocketAddress getJoiningNode() {
		return _joiningNode;
	}
	
	public InetSocketAddress getJoinPoint() {
		return _joinPoint;
	}
	
	public TMulticast_Depart(InetSocketAddress joiningNode, InetSocketAddress joinPoint, LocalSequencer localSequncer) {
		this(joiningNode, joinPoint, localSequncer.getNext());
	}
	
	public TMulticast_Depart(InetSocketAddress joiningNode, InetSocketAddress joinPoint, Sequence sourceSequence) {
		super(TMulticastTypes.T_MULTICAST_DEPART, sourceSequence);
		_joiningNode = joiningNode;
		_joinPoint = joinPoint;
	}
	
	public TMulticast_Depart(ByteBuffer bdy, int contentSize, int subContentSize, String annotations) {
		super(bdy, contentSize, annotations);
		
		if(subContentSize != getTMSpecificContentSize())
			throw new IllegalArgumentException("SubContentSize is wrong: " + subContentSize + " != " + getTMSpecificContentSize());
		
		if(getType() != TMulticastTypes.T_MULTICAST_DEPART)
			throw new IllegalStateException ("Type mismatch: '" + getType() + "' !=TMulticast_Join.");

		byte[] bArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_SUB_CONTENT);
		bdy.get(bArray);
		_joiningNode = Sequence.readAddressFromByteArray(bArray);
		
		bdy.position(I_SUB_CONTENT + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
		bdy.get(bArray);
		_joinPoint = Sequence.readAddressFromByteArray(bArray);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		putObjectInBuffer(localSequencer, buff, true);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, boolean addLocalSeq) {
		super.putObjectInBuffer(localSequencer, buff, addLocalSeq);
		
		buff.position(I_SUB_CONTENT_SIZE);
		buff.putShort((short)(TMULTICAST_DEPART_CONTENT_SIZE));
		
		buff.position(I_SUB_CONTENT);
		buff.put(Sequence.getAddressInByteArray(_joiningNode));
		
		buff.position(I_SUB_CONTENT + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
		buff.put(Sequence.getAddressInByteArray(_joinPoint));
	}

	@Override
	public String toString() {
		String str = "TMulticast_Join ("+_joiningNode +"=>" + _joinPoint +") @ " + _sourceSequence + "[";
		for(int i=0 ; i<_sequencesVector.length ; i++)
			str += _sequencesVector[i] + ",";
		return str + "]";

	}
	
	public static void main(String [] argv) {
//		InetSocketAddress localAddr = new InetSocketAddress("127.0.0.1", 1000);
//		InetSocketAddress remoteAddr = new InetSocketAddress("127.0.0.1", 2000);
//		LocalSequencer.init(null, localAddr);
//		TMulticast_Depart tm1 = new TMulticast_Depart(localAddr, remoteAddr);
//		
//		TMulticast_Depart tm2 = new TMulticast_Depart(tm1.putObjectInBuffer());
//		TMulticast_Depart tm3 = new TMulticast_Depart(tm2.putObjectInBuffer());
//		TMulticast_Depart tm4 = new TMulticast_Depart(tm3.putObjectInBuffer());
//		TMulticast_Depart tm5 = new TMulticast_Depart(tm4.putObjectInBuffer());
//		
//		System.out.println(tm1 + "\n" + tm2 + "\n" + tm3 + "\n" + tm4 + "\n" + tm5); //OK
	}
	
	@Override
	public int getTMSpecificContentSize() {
		return TMULTICAST_DEPART_SUB_CONTENT_SIZE;
	}
	
	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_DEPART;
	}
	
	@Override
	public TMulticastTypes getType() {
		return TMulticastTypes.T_MULTICAST_DEPART;
	}
}
