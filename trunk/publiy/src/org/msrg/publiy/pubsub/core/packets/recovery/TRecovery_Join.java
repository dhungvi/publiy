package org.msrg.publiy.pubsub.core.packets.recovery;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.IHasBrokerInfo;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.JoinInfo;

public class TRecovery_Join extends TRecovery implements IHasBrokerInfo<JoinInfo> {
	
	public static final int TRECOVERY_JOIN_CONTENT_SIZE = 2 * Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE + 2;
	public static final int I_TRECOVERY_JOINING_NODE = TRECOVERY_BASE_SIZE + 0;
	public static final int I_TRECOVERY_JOINING_NODE_TYPE = I_TRECOVERY_JOINING_NODE + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	public static final int I_TRECOVERY_JOIN_POINT = I_TRECOVERY_JOINING_NODE_TYPE + 1;
	public static final int I_TRECOVERY_JOIN_POINT_TYPE = I_TRECOVERY_JOIN_POINT + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE; 
	
	private final InetSocketAddress _joiningNode;
	private final NodeTypes _joiningNodeType;
	private final InetSocketAddress _joinPoint;
	private final NodeTypes _joinPointType;
	
	public InetSocketAddress getJoiningNode(){
		return _joiningNode;
	}
	
	public InetSocketAddress getJoinPoint(){
		return _joinPoint;
	}
	
	public TRecovery_Join(Sequence sourceSequence, InetSocketAddress joiningNode, NodeTypes joiningNodeType, 
			InetSocketAddress joinPoint, NodeTypes joinPointType) {
		super(TRecoveryTypes.T_RECOVERY_JOIN, sourceSequence);
		_joiningNode = joiningNode;
		_joiningNodeType = joiningNodeType;
		_joinPoint = joinPoint;
		_joinPointType = joinPointType;
	}
	
	public NodeTypes getJoininingType(){
		return _joiningNodeType;
	}
	
	public NodeTypes getJoinPointType(){
		return _joinPointType;
	}
	
	public TRecovery_Join(ByteBuffer bdy){
		super(bdy);
		
		if ( getType() != TRecoveryTypes.T_RECOVERY_JOIN )
			throw new IllegalStateException ("Type mismatch: '" + getType() + "' !=TRecovery_Join.");

		byte[] bArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_TRECOVERY_JOINING_NODE);
		bdy.get(bArray);
		_joiningNode = Sequence.readAddressFromByteArray(bArray);
		
		bdy.position(I_TRECOVERY_JOIN_POINT);
		bdy.get(bArray);
		_joinPoint = Sequence.readAddressFromByteArray(bArray);
		
		byte typeByte = bdy.get(I_TRECOVERY_JOINING_NODE_TYPE);
		_joiningNodeType = NodeTypes.getNodeType(typeByte);
		
		typeByte = bdy.get(I_TRECOVERY_JOIN_POINT_TYPE);
		_joinPointType = NodeTypes.getNodeType(typeByte);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		super.putObjectInBuffer(localSequencer, buff);
		
		buff.position(I_CONTENT_SIZE);
		buff.put((byte)TRECOVERY_JOIN_CONTENT_SIZE);
		
		buff.position(I_TRECOVERY_JOINING_NODE);
		byte[] bArray = Sequence.getAddressInByteArray(_joiningNode);
		buff.put(bArray);
		
		buff.position(I_TRECOVERY_JOINING_NODE_TYPE);
		buff.put(_joiningNodeType.getCodedByte());
		
		buff.position(I_TRECOVERY_JOIN_POINT);
		bArray = Sequence.getAddressInByteArray(_joinPoint);
		buff.put(bArray);
		
		buff.position(I_TRECOVERY_JOIN_POINT_TYPE);
		buff.put(_joinPointType.getCodedByte());
	}

	@Override
	public String toString(){
		String str = _subType.toString() + "("+_joiningNode.getPort() + _joiningNodeType.getCodedChar() + 
				"=>" + _joinPoint.getPort() + _joinPointType.getCodedChar() +") @ " + _sourceSequence;
		return str;

	}
	
	@Override
	public int hashCode(){
		throw new UnsupportedOperationException(this.toStringShort());
	}
	
	@Override
	public boolean equals(Object obj){
		return obj == this;
	}
	
	@Override
	public IRawPacket morph(ISession session, IOverlayManager overlayManager){
		InetSocketAddress remote = session.getRemoteAddress();
		Path<INode> pathFromJoinPoint1 = overlayManager.getPathFrom(_joinPoint);
		Path<INode> pathFromJoinPoint2 = overlayManager.getPathFrom(_joiningNode);
		
		if ( remote.equals(overlayManager.getLocalAddress()) )
		{
			assert pathFromJoinPoint1 != null;
			assert pathFromJoinPoint2 != null;
			return PacketFactory.wrapObject(overlayManager.getLocalSequencer(), this);
		}
		
		Path<INode> pathFromRemote = overlayManager.getPathFrom(remote);
		INode last1 = pathFromJoinPoint1.getLast();
		INode last2 = pathFromJoinPoint2.getLast();
		INode lastR = pathFromRemote.getLast();
		
		if ( last1.equals(lastR) || last2.equals(lastR) )
			return null;
		else
			return PacketFactory.wrapObject(overlayManager.getLocalSequencer(), this);
		
//		if ( remote.equals(_joiningNode) || remote.equals(_joinPoint) )
//			return PacketFactory.wrapObject(this);
//		
//		if ( pathFromJoinPoint1 == null )
//			throw new IllegalStateException("Joinpoint '" + _joiningNode + "' must have already been in the overlay.");
//		if ( pathFromJoinPoint2 == null )
//			throw new IllegalStateException("Joinpoint '" + _joiningNode + "' must have already been in the overlay.");
//
//		if ( pathFromRemote == null )
//			throw new IllegalStateException("Remote '" + remote + "' must have already been in the overlay.");
//		
//		if ( pathFromJoinPoint1.passes(remote) )
//			return PacketFactory.wrapObject(this);
//		
//		if ( pathFromRemote.passes(_joinPoint))
//			return PacketFactory.wrapObject(this);
//		
//		int remoteDistance = pathFromRemote.getLength();
//		int joinPoint1Distance = pathFromJoinPoint1.getLength();
//		int joinPoint2Distance = pathFromJoinPoint2.getLength();
//		
//		if ( (remoteDistance + joinPoint1Distance <= Broker.DELTA + 1) && 
//				(remoteDistance + joinPoint2Distance <= Broker.DELTA + 1) )
//			return PacketFactory.wrapObject(this);
//		
//		return null;
	}
	
	@Override
	public TRecoveryTypes getStaticType(){
		return TRecoveryTypes.T_RECOVERY_JOIN;
	}
	
	public static TRecovery_Join getTRecoveryObject(LocalSequencer localSequencer, String str){
		str = str.trim();
		if ( str.charAt(0) == '#' )
			return null;
		
		int end1 = str.indexOf(" ");
		String str1 = str.substring(0, end1);
		String str2 = str.substring(end1+1, str.length());
		
		char typeChar1 = str1.charAt(0);
		String ip1 = str1.substring(2, str1.indexOf(":"));
		String p1 = str1.substring(str1.indexOf(":")+1, str1.length());
		char typeChar2 = str1.charAt(0);
		String ip2 = str2.substring(2, str2.indexOf(":"));
		String p2 = str2.substring(str2.indexOf(":")+1, str2.length());
		
		Integer port1 = new Integer(p1);
		Integer port2 = new Integer(p2);
		
		InetSocketAddress addr1 = new InetSocketAddress(ip1, port1);
		InetSocketAddress addr2 = new InetSocketAddress(ip2, port2);
		
		NodeTypes joiningNodeType = NodeTypes.getNodeType(typeChar1);
		NodeTypes joinPointType = NodeTypes.getNodeType(typeChar2);
		TRecovery_Join trj = new TRecovery_Join(localSequencer.getNext(), addr2, joinPointType, addr1, joiningNodeType);
		return trj;
	}
	
	public static void main(String [] argv) {
//		System.out.println(getTRecoveryObject("#**************")); //OK
//		String str = "/10.0.0.1:2000 /192.168.237.26:2003";
//		TRecovery_Join trj = getTRecoveryObject(str);
		
//		InetSocketAddress addr1 = new InetSocketAddress("127.0.0.1", 1000);
//		InetSocketAddress addr2 = new InetSocketAddress("127.0.0.1", 2000);
//		LocalSequencer.init(null, addr1);
//		TRecovery_Join trj = getTRecoveryObject("" + addr1 + " " + addr2);
//		System.out.println(trj);
//		InetSocketAddress localAddr = new InetSocketAddress("127.0.0.1", 1000);
//		InetSocketAddress remoteAddr = new InetSocketAddress("127.0.0.1", 2000);
//		LocalSequencer.init(null, localAddr);
//		TRecovery_Join tm1 = new TRecovery_Join(localAddr, remoteAddr);
//		
//		TRecovery_Join tm2 = new TRecovery_Join(tm1.putObjectInBuffer());
//		TRecovery_Join tm3 = new TRecovery_Join(tm2.putObjectInBuffer());
//		TRecovery_Join tm4 = new TRecovery_Join(tm3.putObjectInBuffer());
//		TRecovery_Join tm5 = new TRecovery_Join(tm4.putObjectInBuffer());
//		
//		System.out.println(tm1 + "\n" + tm2 + "\n" + tm3 + "\n" + tm4 + "\n" + tm5); //OK
	}

	@Override
	public JoinInfo getInfo() {
		JoinInfo joinInfo = new JoinInfo(_joiningNode, _joiningNodeType, _joinPoint, _joinPointType);
		return joinInfo;
	}
	
	@Override
	protected int getTRSpecificContentSize() {
		return TRECOVERY_JOIN_CONTENT_SIZE;
	}

	public String toStringShort(){
		return "TRJ[" + _joiningNode.getPort() + "=>" + _joinPoint.getPort() + "]";
	}
}
