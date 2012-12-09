package org.msrg.publiy.broker.core.plistManager;

import java.net.InetSocketAddress;


import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.client.subscriber.ISubscriptionListener;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqBreak;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReq;

public interface IPListManager extends ISubscriptionListener {

	public PList addPList(
			Publication pub, Sequence contentSequence, int rows, int cols,
			InetSocketAddress sourceAddress, InetSocketAddress homeBroker);
	public PList getPList(Sequence contentSequence);
	
	public void handlePListClientRequest(TNetworkCoding_PListReq pListReq);
	public void handlePListBreak(TNetworkCoding_CodedPieceIdReqBreak tCodedPieceBreak);
	public void handlePListBrokerReply(Publication pub);
	public void handlePListBrokerRequest(Publication pub);

}
