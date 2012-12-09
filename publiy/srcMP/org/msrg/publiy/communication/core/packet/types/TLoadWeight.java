package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

public class TLoadWeight extends THeartBeat {

	public final double _normalizedLoadWeight;
	public final int _currentLoad;
	
	TLoadWeight(InetSocketAddress inetSAddr, double normalizedLoadWeight) {
		this(inetSAddr, -1, normalizedLoadWeight);
	}
	
	public TLoadWeight(InetSocketAddress inetSAddr, int currentLoad, double normalizedLoadWeight) {
		super(inetSAddr, "");
		
		_normalizedLoadWeight = normalizedLoadWeight;
		_currentLoad = currentLoad;
	}
	
	public TLoadWeight(ByteBuffer bdy) {
		super(bdy);
		_currentLoad = bdy.getInt(super.getContentSize());
		_normalizedLoadWeight = bdy.getDouble(super.getContentSize() + 4);
	}
			
	@Override
	public int getContentSize() {
		return super.getContentSize() + 4 + 8;
	}

	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TLOCALLOADWEIGHT;
	}

	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buffer){
		super.putObjectInBuffer(localSequencer, buffer);
		
		buffer.putInt(super.getContentSize(), _currentLoad);
		buffer.putDouble(super.getContentSize() + 4, _normalizedLoadWeight);
	}
	
	@Override
	public String toString() {
		return "[" + _str + "_"+_inetSAddr + ": " + _currentLoad + " " + _normalizedLoadWeight + "]";
	}
	
	public static IPacketable getObjectFromBuffer(ByteBuffer bdy) {
		return new TLoadWeight(bdy);
	}

	public double getNormalizedLoadWeight() {
		return _normalizedLoadWeight;
	}
	
	public static void main(String[] argv) {
		LocalSequencer localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.2", 3000));

		TLoadWeight hb = new TLoadWeight(new InetSocketAddress("127.0.0.1", 2000), 0.01);
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, hb);
		IPacketable obj = PacketFactory.unwrapObject(null, raw);
		System.out.println(obj); //OK
		System.out.println(raw.getSize()); //OK
	}
	
	public InetSocketAddress getRemote() {
		return _inetSAddr;
	}
}
