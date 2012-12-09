package org.msrg.publiy.pubsub.core.packets.networkcoding.multicast;

import java.net.InetSocketAddress;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class TMulticast_Publish_NC extends TMulticast_Publish_MP {

	protected final NetCodedPublication _bPub;
	
	protected TMulticast_Publish_NC(NetCodedPublication bPub, Publication publication, InetSocketAddress from, 
			byte pathLenght, PubForwardingStrategy forwardingStrategy, LocalSequencer localSequencer) {
		super(getStaticType(), publication, from, 0, (byte)0, forwardingStrategy, localSequencer.getNext());
		
		_bPub = bPub;
	}

	public NetCodedPublication getBulkPublication() {
		return _bPub;
	}
	
	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_PUBLICATION_NC;
	}
	
	@Override
	public TMulticastTypes getType() {
		return TMulticastTypes.T_MULTICAST_PUBLICATION_NC;
	}
}
