package org.msrg.publiy.communication.core.packet;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.utils.annotations.IAnnotatable;

import org.msrg.publiy.communication.core.packet.types.PacketableTypes;

public interface IRawPacket extends IAnnotatable {
	public ByteBuffer[] getBuffs();
	public ByteBuffer getHeader();
	public ByteBuffer getBody();
	public IRawPacket rewind();
	public IPacketListener getPacketListener();
	
	public PacketableTypes getType();
	
	public int getSize();
	
	public int getSession();
	public int getSeq();
	public int getLength();
	public short getEncoding();
	public InetSocketAddress getSender();
	public InetSocketAddress getReceiver();
	public RawPacketPriority getPacketPriority();
	
	public void setType(PacketableTypes type);
	public void setSession(int session);
	public void setSeq(int seq);
	public void setLength(int len);
	public void setEncoding(short enc);
	public void setSender(InetSocketAddress sndr);
	public void setReceiver(InetSocketAddress rcvr);
//	public void setPacketPriority(RawPacketPriority priority);
	
	public String getString(IBrokerShadow brokerShadow);
	public int getContentSize();
	public String getQuickString();
	
	public void lockChange();
	public IPacketable getObject();
	void setObject(IPacketable obj);
}
