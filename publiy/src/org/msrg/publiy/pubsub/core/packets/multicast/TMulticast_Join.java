package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.node.NodeTypes;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class TMulticast_Join extends TMulticast {
	
	public static final int TMULTICAST_JOIN_SUB_CONTENT_SIZE = 2 * Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE + 2;
	public static final int I_JOINING_NODE = TMULTICAST_BASE_SIZE;
	public static final int I_JOINING_NODE_TYPE = I_JOINING_NODE + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	public static final int I_JOIN_POINT_NODE = I_JOINING_NODE_TYPE + 1;
	public static final int I_JOIN_POINT_NODE_TYPE = I_JOIN_POINT_NODE + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	
	private final InetSocketAddress _joiningNode;
	private final NodeTypes _joiningNodeType;
	private final InetSocketAddress _joinPoint;
	private final NodeTypes _joinPointType;
	
	public TMulticast_Join getShiftedClone(int shift, Sequence sequence) {
		TMulticast_Join clone = new TMulticast_Join(this._joiningNode, this._joiningNodeType, this._joinPoint, this._joinPointType, this._sourceSequence);
		TMulticast.duplicate(this, clone);
		clone.shiftSequenceVector(shift);
		
		if (sequence==null)
			throw new NullPointerException("Shifting '" + shift + "': " + this.toStringTooLong());

		clone._sequencesVector[shift-1] = sequence;
		return clone;
	}
	
	public InetSocketAddress getJoiningNode() {
		return _joiningNode;
	}
	
	public InetSocketAddress getJoinPoint() {
		return _joinPoint;
	}
	
	public TMulticast_Join(InetSocketAddress joiningNode, NodeTypes joiningNodeType, InetSocketAddress joinPoint, NodeTypes joinPointType, LocalSequencer localSequencer) {
		this(joiningNode, joiningNodeType, joinPoint, joinPointType, localSequencer.getNext());
	}
	
	public TMulticast_Join(InetSocketAddress joiningNode, NodeTypes joiningNodeType, InetSocketAddress joinPoint, NodeTypes joinPointType, Sequence sourceSequence) {
		super(TMulticastTypes.T_MULTICAST_JOIN, sourceSequence);
		_joiningNode = joiningNode;
		_joiningNodeType = joiningNodeType;
		_joinPoint = joinPoint;
		_joinPointType = joinPointType;
	}
	
	public TMulticast_Join(ByteBuffer bdy, int contentSize, int subContentSize, String annotations) {
		super(bdy, contentSize, annotations);
		
		if ( subContentSize != getTMSpecificContentSize() )
			throw new IllegalArgumentException("SubContentSize is wrong: " + subContentSize + " != " + getTMSpecificContentSize() );
		
		if ( getType() != TMulticastTypes.T_MULTICAST_JOIN )
			throw new IllegalStateException ("Type mismatch: '" + getType() + "' !=TMulticast_Join.");

		byte[] bArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_JOINING_NODE);// I_SUB_CONTENT);
		bdy.get(bArray);
		_joiningNode = Sequence.readAddressFromByteArray(bArray);
		
		byte typeByte = bdy.get(I_JOINING_NODE_TYPE);
		_joiningNodeType = NodeTypes.getNodeType(typeByte);
		
		bdy.position(I_JOIN_POINT_NODE);// I_SUB_CONTENT + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
		bdy.get(bArray);
		_joinPoint = Sequence.readAddressFromByteArray(bArray);
		
		typeByte = bdy.get(I_JOIN_POINT_NODE_TYPE);
		_joinPointType = NodeTypes.getNodeType(typeByte);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		putObjectInBuffer(localSequencer, buff, true);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, boolean addLocalSeq) {
		super.putObjectInBuffer(localSequencer, buff, addLocalSeq);
		
		buff.position(I_JOINING_NODE);
		buff.put(Sequence.getAddressInByteArray(_joiningNode));
		
		buff.position(I_JOINING_NODE_TYPE);
		buff.put(_joiningNodeType.getCodedByte());
		
		buff.position(I_JOIN_POINT_NODE);
		buff.put(Sequence.getAddressInByteArray(_joinPoint));
		
		buff.position(I_JOIN_POINT_NODE_TYPE);
		buff.put(_joinPointType.getCodedByte());
	}

	@Override
	public String toString() {
		String str = "TMulticast_Join ("+_joiningNode.getPort() + _joiningNodeType.getCodedChar() +
					"=>" + _joinPoint.getPort() + _joinPointType.getCodedChar() + ") @ " + _sourceSequence + "[";
//		for ( int i=0 ; i<Broker.DELTA+1 ; i++ )
//			str += _sequences[i] + ",";
		if ( _isGuided == false )
			return str + "]";
		else 
			return "G_" + str + "]{{" + _guidedInfo + "}}";
	}
	
	public static void main(String [] argv) {
		InetSocketAddress localAddr = new InetSocketAddress("127.0.0.1", 1000);
		InetSocketAddress remoteAddr = new InetSocketAddress("127.0.0.1", 2000);
		LocalSequencer localSequencer = LocalSequencer.init(null, localAddr);
		TMulticast_Join tm1 = new TMulticast_Join(localAddr, NodeTypes.NODE_BROKER, remoteAddr, NodeTypes.NODE_BROKER, localSequencer);
		tm1 = tm1.getShiftedClone(1, localSequencer.getNext());
//		tm1.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress6, 800));
		tm1.annotate("AnnotationString");
		tm1.annotate("Second annotation!");
		
		IRawPacket raw1 = PacketFactory.wrapObject(localSequencer, tm1);
		TMulticast_Join tm2 = (TMulticast_Join) PacketFactory.unwrapObject(null, raw1);
		String annotations = tm2.getAnnotations();
		System.out.println(annotations); // OK

//		tm2.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress6, 800));
		IRawPacket raw2 = PacketFactory.wrapObject(localSequencer, tm2);
		TMulticast_Join tm3 = (TMulticast_Join) PacketFactory.unwrapObject(null, raw2);

//		tm3.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress6, 800));
		IRawPacket raw3 = PacketFactory.wrapObject(localSequencer, tm3);
		TMulticast_Join tm4 = (TMulticast_Join) PacketFactory.unwrapObject(null, raw3);
		
//		tm4.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress6, 800));
		IRawPacket raw4 = PacketFactory.wrapObject(localSequencer, tm4);
		TMulticast_Join tm5 = (TMulticast_Join) PacketFactory.unwrapObject(null, raw4);
		
		annotations = tm5.getAnnotations();
		System.out.println(annotations); // OK
		
		System.out.println(tm1 + "\n" + tm2 + "\n" + tm3 + "\n" + tm4 + "\n" + tm5);
		System.out.println("->" + raw1.getQuickString());
	}
	
	@Override
	public int getTMSpecificContentSize() {
		return TMULTICAST_JOIN_SUB_CONTENT_SIZE;
	}
	
	public NodeTypes getJoininingType() {
		return _joiningNodeType;
	}
	
	public NodeTypes getJoinPointType() {
		return _joinPointType;
	}
	
	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_JOIN;
	}
	
	@Override
	public TMulticastTypes getType() {
		return TMulticastTypes.T_MULTICAST_JOIN;
	}
}