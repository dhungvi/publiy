package org.msrg.publiy.communication.core.packet;

public interface IPacketListener {

	public void packetDropped(IPacketable packet);
	public void packetBeingSent(IPacketable packet);
	public void packetSent(IPacketable packet);
	
}
