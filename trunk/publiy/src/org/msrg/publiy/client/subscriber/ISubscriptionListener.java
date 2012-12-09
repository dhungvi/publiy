package org.msrg.publiy.client.subscriber;

import java.util.Map;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.PublicationInfo;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;

public interface ISubscriptionListener extends ITMConfirmationListener {

	public void matchingPublicationDelivered(TMulticast_Publish tmp);
	public void matchingPublicationDelivered(Sequence sourceSequence, Publication publication);
	public PublicationInfo[] getReceivedPublications();
	public int getCount();
	public Map<String, Integer> getDeliveredPublicationCounterPerPublisher();
	public Map<String, Long> getLastPublicationDeliveryTimesPerPublisher();
	public Map<String, Publication> getLastPublicationDeliveredPerPublisher();
	
	public PublicationInfo getLastReceivedPublications();
	public long getLastPublicationReceiptTime();
	
}
