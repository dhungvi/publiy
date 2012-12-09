package org.msrg.publiy.pubsub.core.packets.multicast;

import java.nio.ByteBuffer;

public abstract class TMulticast_UnSubscribe extends TMulticast {

	protected TMulticast_UnSubscribe(ByteBuffer bdy, int contentSize, String annotations) {
		super(bdy, contentSize, annotations);
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	@Override
	public int getTMSpecificContentSize(){
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_UNKNOWN;
	}
	
	@Override
	public TMulticastTypes getType() {
		return TMulticastTypes.T_MULTICAST_UNKNOWN;
	}
}
