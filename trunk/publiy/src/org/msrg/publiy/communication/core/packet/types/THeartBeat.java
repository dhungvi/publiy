package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UnknownFormatConversionException;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

public class THeartBeat implements IPacketable {
	public static final int ITHEARTBEAT_ADDRESS = 0; // ITHEARTBEAT_TIME + 8;
	public static final int ITHEARTBEAT_STRING_LENGTH = ITHEARTBEAT_ADDRESS + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	public static final int ITHEARTBEAT_STRING = ITHEARTBEAT_STRING_LENGTH + 2;
	public static final int THEARTBEAT_BASE_LEN = ITHEARTBEAT_STRING;
	
	protected final InetSocketAddress _inetSAddr;
	protected final String _str;
	
	protected THeartBeat(InetSocketAddress inetSAddr, String str) throws IllegalArgumentException {
		_str = str;
//		if ( inetSAddr == null )
//			throw new IllegalArgumentException("THeartBeat::THeartBeat(.) cannot accept null InetSocketAddress.");
		_inetSAddr = inetSAddr;
	}
	
	public THeartBeat(InetSocketAddress inetSAddr){
		this(inetSAddr, "Heartbeat");
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buffer){
		byte[] bAddress = Sequence.getAddressInByteArray(_inetSAddr);
		buffer.position(ITHEARTBEAT_ADDRESS);
		buffer.put(bAddress);
		
		byte[] strBytes = _str.getBytes();
		buffer.position(ITHEARTBEAT_STRING_LENGTH);
		buffer.putShort((short)strBytes.length);
		
		buffer.position(ITHEARTBEAT_STRING);
		buffer.put(strBytes);
	}

	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.THEARTBEAT;
	}
		
	public String toString(){
		return "[THeartBeat: ("+_inetSAddr + "): " + _str + "]";
	}
	
	public String getString(){
		return _str;
	}
	
	public InetSocketAddress getInetSocketAddress(){
		return _inetSAddr;
	}
	
	@Override
	public int getContentSize() {
		int str_len = _str.length();		
		int len = THEARTBEAT_BASE_LEN + str_len /* bytes for _str */;
		return len;
	}

	@Override
	public void annotate(String annotation) {
		return;
	}

	@Override
	public String getAnnotations() {
		return "";
	}

	@Override
	public Sequence getSourceSequence() {
		return null;
	}

	public static IPacketable getObjectFromBuffer(ByteBuffer bdy) {
		return new THeartBeat(bdy);
	}
	
	protected THeartBeat(ByteBuffer bdy) {
		int len = bdy.capacity();
		if ( len < THeartBeat.THEARTBEAT_BASE_LEN )
			throw new UnknownFormatConversionException("PacketableTypes::getObjectFromBuffer(.): body does not have minimum length of 'THeartBeat'.");

		byte[] addressBArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		bdy.position(THeartBeat.ITHEARTBEAT_ADDRESS);
		bdy.get(addressBArray);
		InetSocketAddress sAddr = Sequence.readAddressFromByteArray(addressBArray);
		
		int strLen = bdy.getShort(ITHEARTBEAT_STRING_LENGTH);
		byte[] strBytes = new byte[strLen];
		bdy.position(ITHEARTBEAT_STRING);
		bdy.get(strBytes);
		String str = new String(strBytes);
		
		_inetSAddr = sAddr;
		_str = str;
	}
	
	public static void main(String[] argv) {
		LocalSequencer localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 3000));
		THeartBeat hb = new THeartBeat(new InetSocketAddress("127.0.0.1", 2000));
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, hb);
		IPacketable obj = PacketFactory.unwrapObject(null, raw);
		System.out.println(obj); //OK
	}
	
	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, int bbOffset) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public IPacketListener getPacketListener() {
		return null;
	}
}
