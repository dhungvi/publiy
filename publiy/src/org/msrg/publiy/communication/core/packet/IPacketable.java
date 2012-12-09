package org.msrg.publiy.communication.core.packet;

import org.msrg.publiy.communication.core.packet.types.PacketableTypes;

import org.msrg.publiy.utils.annotations.IAnnotatable;

public interface IPacketable extends ISemiPacketable, IAnnotatable {

	public PacketableTypes getObjectType();
	public IPacketListener getPacketListener();
	
}
