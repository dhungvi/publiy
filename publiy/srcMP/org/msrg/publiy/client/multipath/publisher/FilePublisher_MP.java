package org.msrg.publiy.client.multipath.publisher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastAnnotatorFactory;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.utils.annotations.AnnotationEvent;
import org.msrg.publiy.utils.log.LoggerFactory;

import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.client.publisher.SimpleFilePublisher;

public class FilePublisher_MP extends SimpleFilePublisher {

	public FilePublisher_MP(InetSocketAddress localAddress,
			BrokerOpState opState, InetSocketAddress joinPointAddress,
			PubForwardingStrategy brokerForwardingStrategy, Properties props)
			throws IOException {
		super(localAddress, opState, joinPointAddress, brokerForwardingStrategy, props);
	}

	public FilePublisher_MP(InetSocketAddress localAddress,
			BrokerOpState opState, InetSocketAddress joinPointAddress,
			PubForwardingStrategy brokerForwardingStrategy, String filename,
			int delay, Properties arguments) throws IOException {
		super(localAddress, opState, joinPointAddress, brokerForwardingStrategy, filename, delay, arguments);
	}

	@Override
	public TMulticast_Publish publish(Publication publication, ITMConfirmationListener tmConfirmationListener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TMulticast_Publish[] publish(Publication[] publications, ITMConfirmationListener tmConfirmationListener) {
		if(publications == null || publications.length == 0)
			return null;
		
		int bundleSize = TMulticast_Publish_MP.getBundleSize();
		
		TMulticast_Publish[] tmps = new TMulticast_Publish[publications.length * bundleSize];
		for(int i=0 ; i<publications.length ; i++) {
			if(publications[i] == null)
				continue;
			
			TMulticast_Publish_MP[] bundle;
			if(PUB_FORWARDING_STRATEGY==PubForwardingStrategy.PUB_FORWARDING_STRATEGY_0)
				bundle = TMulticast_Publish_MP.getBundledTMulticast_Publish_MPs(publications[i], _localAddress, _localSequencer);
			else {
				bundle = new TMulticast_Publish_MP[1];
				bundle[0] = (TMulticast_Publish_MP) createTMulticastPublication(publications[i]);
			}
			for(int j=0 ; j<bundle.length ; j++) {
				TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(_localSequencer, bundle[j], AnnotationEvent.ANNOTATION_EVENT_PUBLICATION_GENERATION, "SimpleFilePublisher #" + (_count-1));
				tmps[i*bundleSize + j] = bundle[j];
				_totalPublished++;
				if(DEBUG)
					LoggerFactory.getLogger().debug(this, "########## Publishing: " + bundle[j] + " ##########");
			}
		}
		
		_connectionManager.sendTMulticast(tmps, tmConfirmationListener);
		_pubGenerationLogger.publicationGenerated(tmps);
		return tmps;
	}
	
	@Override
	protected TMulticast_Publish createTMulticastPublication(Publication publication) {
		return new TMulticast_Publish_MP(publication, _localAddress, 0, (byte) 0, _brokerShadow.getPublicationForwardingStrategy(), _localSequencer.getNext());
	}
	
	@Override
	protected int getDelay() {
		return super.getDelay();
	}
	
	@Override
	protected void prepareReadPublication(Publication pub) {
		super.prepareReadPublication(pub);
		int port = _localAddress.getPort();
		pub.addPredicate("NODEID", port);
	}

	@Override
	public NodeTypes getNodeType() {
		return NodeTypes.NODE_MP_PUBLISHER;
	}
}
