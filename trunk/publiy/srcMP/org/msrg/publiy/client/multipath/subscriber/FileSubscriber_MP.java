package org.msrg.publiy.client.multipath.subscriber;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.client.subscriber.SimpleFileSubscriber;

public class FileSubscriber_MP extends SimpleFileSubscriber {

	private BundledPublication_MPDelivery _pub_mp_delivery =
		new BundledPublication_MPDelivery(_localAddress);
	
	public FileSubscriber_MP(InetSocketAddress localAddress,
			BrokerOpState opState, InetSocketAddress joinPointAddress,
			PubForwardingStrategy brokerForwardingStrategy, String filename,
			int delay, Properties arguments) throws IOException {
		super(localAddress, opState, joinPointAddress, brokerForwardingStrategy,
				filename, delay, arguments);
	}
	
	public FileSubscriber_MP(InetSocketAddress localAddress,
			BrokerOpState opState, InetSocketAddress joinPointAddress,
			PubForwardingStrategy brokerForwardingStrategy, Properties props)
			throws IOException {
		super(localAddress, opState, joinPointAddress, brokerForwardingStrategy, props);
	}

	@Override
	public void stopComponent() {
		_pub_mp_delivery.flushAllRemaining();
		super.stopComponent();
	}
	
	@Override
	public void matchingPublicationDelivered(TMulticast_Publish tmp) {
		super.matchingPublicationDelivered(tmp);
		
		if(!Broker.RELEASE && tmp.getType() == TMulticastTypes.T_MULTICAST_PUBLICATION_MP) {
			if(getPublicationForwardingStrategy() == PubForwardingStrategy.PUB_FORWARDING_STRATEGY_0)
				_pub_mp_delivery.publicationDelivered((TMulticast_Publish_MP)tmp);
		}
	}
	
	@Override
	public NodeTypes getNodeType() {
		return NodeTypes.NODE_MP_SUBSCRIBER;
	}
}
