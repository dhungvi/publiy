package org.msrg.publiy.communication.core.packet;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;

import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.packet.types.PacketableTypes;

public class RawPacket implements IRawPacket{
	public static final short DEFALT_ENCODING = 1;
	public static final byte TYPE_UNKNOWN = -1;
	public static final int SESSION_UNKNOWN = -1;
	public static final int SEQ_UNKNOWN = -1;
	
	public static final int IHEADER_TYPE = 0;
	public static final int IHEADER_CONTENT_SIZE = IHEADER_TYPE + 1;
	public static final int IHEADER_ANNOTATION_SIZE = IHEADER_CONTENT_SIZE + Integer.SIZE/8;
	public static final int IHEADER_SESSION = IHEADER_ANNOTATION_SIZE + 2;
	public static final int IHEADER_SEQ = IHEADER_SESSION + 4;
	public static final int IHEADER_SRC_IP = IHEADER_SEQ + 4;
	public static final int IHEADER_DST_IP = IHEADER_SRC_IP + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	public static final int IHEADER_MSG_LENGTH = IHEADER_DST_IP + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	public static final int IHEADER_MSG_ENCODING = IHEADER_MSG_LENGTH + 4;
	
	public static final int HEADER_SIZE = IHEADER_MSG_ENCODING + 2;
	
	protected final IPacketListener _packetListener;
	private final ByteBuffer[] _buffs;
	private IPacketable _obj;
	private boolean _lockChange = false;
	
	RawPacket(IPacketable obj, ByteBuffer hdr, ByteBuffer bdy, PacketableTypes type, int contentSize, int annotationSize){
		this(obj, hdr, bdy);
		setType(type);
		setContentSize(contentSize);
		setAnnotationSize(annotationSize);
		setBodyLength();
	}
	
	private void setBodyLength(){
		int len = _buffs[1].capacity();
		setLength(len);
	}
	
	@Override
	public ByteBuffer[] getBuffs(){
		return _buffs;
	}
	
	@Override
	public IRawPacket rewind(){
		_buffs[0].rewind();
		_buffs[1].rewind();
		return this;
	}
	
	@Override
	public ByteBuffer getHeader(){
		return _buffs[0];
	}
	
	@Override
	public ByteBuffer getBody(){
		return _buffs[1];
	}
	
	@Override
	public PacketableTypes getType(){
		if ( _buffs[0] == null )
			throw new IllegalStateException("_buffs[0] (header) is null");
		
		byte bType = _buffs[0].get(IHEADER_TYPE);
		PacketableTypes packetType = PacketableTypes.getPacketableTypes(bType);
		return packetType;
	}
	
	@Override
	public int getSession(){
		if ( _buffs[0] == null )
			return -1;
		int session = _buffs[0].getInt(IHEADER_SESSION);
		return session;
	}

	@Override
	public int getSeq(){
		if ( _buffs[0] == null )
			return -1;
		int seq = _buffs[0].getInt(IHEADER_SEQ);
		return seq;
	}
	
	@Override
	public int getLength(){
		if ( _buffs[0] == null )
			return -1;
		int len = _buffs[0].getInt(IHEADER_MSG_LENGTH);
		return len;
	}
	
	@Override
	public short getEncoding(){
		if ( _buffs[0] == null )
			return -1;
		short enc = _buffs[0].getShort(IHEADER_MSG_ENCODING);
		return enc;		
	}
	
	@Override
	public InetSocketAddress getSender(){
		if ( _buffs[0] == null )
			return null;
		
//		byte[] b_addr = new byte[4];
//		b_addr[0] = _buffs[0].get(IHEADER_SRC_IP+0);
//		b_addr[1] = _buffs[0].get(IHEADER_SRC_IP+1);
//		b_addr[2] = _buffs[0].get(IHEADER_SRC_IP+2);
//		b_addr[3] = _buffs[0].get(IHEADER_SRC_IP+3);
//		short port = _buffs[0].getShort(IHEADER_SRC_PORT);
//		InetAddress addr;
//		try{
//			addr = InetAddress.getByAddress(b_addr);
//		}catch(Exception x){addr = null;}
//		InetSocketAddress inetAddr = new InetSocketAddress(addr, port);
		byte[] bArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		_buffs[0].position(IHEADER_SRC_IP);
		_buffs[0].get(bArray);
		InetSocketAddress inetAddr = Sequence.readAddressFromByteArray(bArray);

		return inetAddr;
	}
	
	@Override
	public InetSocketAddress getReceiver(){
		if ( _buffs[0] == null )
			return null;
		
		byte[] bArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		_buffs[0].position(IHEADER_DST_IP);
		_buffs[0].get(bArray);
		InetSocketAddress inetAddr = Sequence.readAddressFromByteArray(bArray);

		return inetAddr;		
	}

	@Override
	public void setEncoding(short enc) {
		if ( _lockChange )
			throw new IllegalStateException("RawPacket is locked for changes.");

		_buffs[0].putShort(IHEADER_MSG_ENCODING, enc);		
	}

	@Override
	public void setLength(int len) {
		if ( _lockChange )
			throw new IllegalStateException("RawPacket is locked for changes.");
		
		if ( len >= Integer.MAX_VALUE )
			throw new IllegalArgumentException ("RawPacket::RawPacket(.) object encoding is too long.");
		
		_buffs[0].putInt(IHEADER_MSG_LENGTH, len);		
	}

	@Override
	public void setReceiver(InetSocketAddress rcvr) throws IllegalArgumentException {
		if ( _lockChange )
			throw new IllegalStateException("RawPacket is locked for changes.");
		
		if ( rcvr == null )
			throw new IllegalArgumentException("RawPacket::setReceiver() receiver InetSocketAddress cannot be null.");
		
		byte[] bArray = Sequence.getAddressInByteArray(rcvr);
		_buffs[0].position(IHEADER_DST_IP);
		_buffs[0].put(bArray);
	}

	@Override
	public void setSender(InetSocketAddress sndr) {
		if ( _lockChange )
			throw new IllegalStateException("RawPacket is locked for changes.");
		
		if ( sndr == null )
			throw new IllegalArgumentException("RawPacket::setReceiver() sndr InetSocketAddress cannot be null.");

		byte[] bArray = Sequence.getAddressInByteArray(sndr);
		_buffs[0].position(IHEADER_SRC_IP);
		_buffs[0].put(bArray);
	}

	@Override
	public void setSeq(int seq) {
		if ( _lockChange )
			throw new IllegalStateException("RawPacket is locked for changes.");
		
		_buffs[0].putInt(IHEADER_SEQ, seq);
	}

	@Override
	public void setSession(int session) {
		if ( _lockChange )
			throw new IllegalStateException("RawPacket is locked for changes.");
		
		_buffs[0].putInt(IHEADER_SESSION, session);		
	}

	@Override
	public void setType(PacketableTypes type) {
		if ( _lockChange )
			throw new IllegalStateException("RawPacket is locked for changes.");
		
		byte bType = type.getCodedValue();
		_buffs[0].put(IHEADER_TYPE, bType);
	}

	@Override
	public RawPacketPriority getPacketPriority() {
		PacketableTypes type = getType();
		
		switch(type){
		case TCODEDPIECE_ID_REQ:
		case THEARTBEAT:
		case TSESSIONINITIATION:
			return RawPacketPriority.High;
			
		case TRECOVERY:
			return RawPacketPriority.Low;
		
		default:
			return RawPacketPriority.Normal;
		}
	}
	
	@Override
	public String toString(){
		throw new UnsupportedOperationException("This is a very expensive operation; if you really want the String representation use: getString()");
	}
	
	@Override
	public String getString(IBrokerShadow brokerShadow) {
		return PacketFactory.unwrapObject(brokerShadow, this).toString() + "[RAW]";
	}
	
	private void setContentSize(int contentSize){
		if ( _lockChange )
			throw new IllegalStateException("RawPacket is locked for changes.");
		
		if ( contentSize >= Integer.MAX_VALUE )
			throw new IllegalArgumentException ("Content size is too large: " + contentSize);
		
		_buffs[0].putInt(IHEADER_CONTENT_SIZE, contentSize);
	}
	
	private void setAnnotationSize(int annotationSize){
		if ( _lockChange )
			throw new IllegalStateException("RawPacket is locked for changes.");
		
		if ( annotationSize >= Short.MAX_VALUE )
			throw new IllegalArgumentException ("Annotation size is too large: " + annotationSize);
		
		_buffs[0].putShort(IHEADER_ANNOTATION_SIZE, (short)annotationSize);
		setBodyLength();
	}
	
	private int getAnnotationLocation(){
		return getContentSize();
	}
	
	public short getAnnotationSize(){
		return _buffs[0].getShort(IHEADER_ANNOTATION_SIZE);
	}

	@Override
	public int getContentSize(){
		return _buffs[0].getInt(IHEADER_CONTENT_SIZE);
	}

	@Override
	public String getAnnotations() {
		int annotationLocation = getAnnotationLocation();
		short annotationSize = getAnnotationSize();
		
		byte[] bArray = new byte[annotationSize];
		_buffs[1].position(annotationLocation);
		_buffs[1].get(bArray);
		String annotations = new String(bArray);
		
		return annotations;
	}

	@Override
	public final void annotate(String annotation) {
		if ( _lockChange )
			throw new IllegalStateException("RawPacket is locked for changes.");
		
		byte[] bArray = annotation.getBytes();
		ByteBuffer newBdy = ByteBuffer.allocate(_buffs[1].capacity() + bArray.length);
		_buffs[1].rewind();
		newBdy.put(_buffs[1]);
		newBdy.put(bArray);
		_buffs[1] = newBdy;

		int oldAnnotationSize = getAnnotationSize();
		int newAnnotationSize = oldAnnotationSize + bArray.length;
		setAnnotationSize(newAnnotationSize);
	}

	public static IRawPacket reconstruct(ByteBuffer hdrBuff, ByteBuffer bdyBuff) {
		return new RawPacket(null, hdrBuff, bdyBuff);
	}

	private RawPacket(IPacketable obj, ByteBuffer hdrBuff, ByteBuffer bdyBuff) {
		_buffs = new ByteBuffer[2];
		if ( (hdrBuff == null) )
			throw new IllegalArgumentException("RawPacket::hdr cannot be null.");
		if ( (bdyBuff == null) )
			throw new IllegalArgumentException("RawPacket::bdy cannot be null.");
		if ( hdrBuff.capacity() != HEADER_SIZE )
			throw new IllegalArgumentException("RawPacket::hdr size mismatch.");
		_buffs[0] = hdrBuff;
		_buffs[1] = bdyBuff;
		
		setObject(obj);
		if(obj == null)
			_packetListener = null;
		else
			_packetListener = obj.getPacketListener();
	}

	@Override
	public String getQuickString() {
		if ( _obj != null )
			return _obj.toString();
		else
			return "RAW::NO_QUICK_STRING";
	}

	@Override
	public IPacketable getObject() {
		return _obj;
	}

	@Override
	public void setObject(IPacketable obj) {
		_obj = obj;
	}

	@Override
	public Sequence getSourceSequence() {
		if ( getType() == PacketableTypes.TMULTICAST )
			return TMulticast.getSourceSequenceFromRaw(this);
		else
			return null;
	}

	@Override
	public void lockChange() {
		if ( _lockChange )
			throw new IllegalStateException("RawPacket is already locked for changes.");
		
		rewind();
		_lockChange = true;
	}

	@Override
	public int getSize() {
		return _buffs[0].capacity() + _buffs[1].capacity();
	}

	@Override
	public IPacketListener getPacketListener() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
