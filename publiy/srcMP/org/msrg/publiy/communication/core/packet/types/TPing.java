package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

public class TPing extends THeartBeat {

	public static final int IPING_TIME = THeartBeat.THEARTBEAT_BASE_LEN;
	public static final int THEARTBEAT_BASE_LEN = IPING_TIME + 8;
	
	final protected long _time;
	
	public TPing(InetSocketAddress inetSAddr) {
		super(inetSAddr, "TPing");
		_time = SystemTime.currentTimeMillis();
	}
	
	protected TPing(InetSocketAddress inetSAddr, String str, long time){
		super(inetSAddr, str);
		_time = time;
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buffer){
		super.putObjectInBuffer(localSequencer, buffer);
		
		buffer.putLong(super.getContentSize(), _time);
	}

	public PacketableTypes getObjectType() {
		return PacketableTypes.TPING;
	}
	
	@Override
	public int getContentSize() {
		return super.getContentSize() + 8;
	}

	public long getTime(){
		return _time;
	}
	
	public static IPacketable getObjectFromBuffer(ByteBuffer bdy) {
		THeartBeat heartbeat = new THeartBeat(bdy);
		long time = bdy.getLong(heartbeat.getContentSize());
		
		return new TPing(heartbeat.getInetSocketAddress(), heartbeat.getString(), time);
	}
	
	public static void main(String[] argv) {
		LocalSequencer localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 20000));
		TPing tp = new TPing(new InetSocketAddress("127.0.0.1", 2000));
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, tp);
		IPacketable obj = PacketFactory.unwrapObject(null, raw);
		System.out.println(obj); //OK
	}
}
