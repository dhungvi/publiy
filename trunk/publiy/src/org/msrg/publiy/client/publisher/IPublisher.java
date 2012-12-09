package org.msrg.publiy.client.publisher;

import org.msrg.publiy.broker.info.PublicationInfo;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;

public interface IPublisher extends ITMConfirmationListener {

	public TMulticast_Publish publish(Publication publication, ITMConfirmationListener TMConfirmationListener);
	public TMulticast_Publish[] publish(Publication[] publications, ITMConfirmationListener tmConfirmationListener);
	public PublicationInfo[] getUnconfirmedPublications();
	
	public boolean play();
	public boolean pause();
	public int getPublicationBatchSize();
	
}
