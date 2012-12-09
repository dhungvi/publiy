package org.msrg.publiy.broker.core.connectionManager;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReply;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.utils.SortableNodeAddress;
import org.msrg.publiy.utils.SortableNodeAddressSet;

import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.contentManager.servingPolicy.ContentServingPolicy;
import org.msrg.publiy.broker.core.flowManager.FlowSelectionPolicy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;

public interface IConnectionManagerNC {
	
	public LocalSequencer getLocalSequencer();
	public InetSocketAddress getLocalAddress();
	public InetSocketAddress getUDPLocalAddress();
	public IBrokerShadow getBrokerShadow();
	public FlowSelectionPolicy getFlowSectionPolicy();
	public ContentServingPolicy getContentServingPolicy();
	public int getDefaultFlowPacketSize();
	
	public boolean serveContent(Content content);
	
	public boolean isServingContent(Content content);
	public void applyContentServingPolicy(Content content);
	public void sendCodedPieceIdReqBreak(
			Sequence sourceSequence, InetSocketAddress remote, int breakingSize);
	public void sendCodedPieceIdReqBreakDeclined(
			Sequence sourceSequence, InetSocketAddress requester, int outstandingPieces);
	public void sendCodedPieceId(Content content, InetSocketAddress remote);
	
	public Set<InetSocketAddress> getMatchingSet(
			Publication publication, boolean localSubscribers);

	
	public void publishContent(PSSourceCodedBatch psSourceCodedBatch);
	public void addContentFlows(Content content);
	public void sendPListReplyPublication(Publication plistReplyPublication);
	
	public void sendPListReply(TNetworkCoding_PListReply pList, InetSocketAddress requester);
	public void sendTMPListRequest(TMulticast_Publish_MP tmp);
	
	public SortableNodeAddressSet getSortableLaunchNodesSet();
	public SortableNodeAddressSet getSortableNonLaunchNodesSet();
	public SortedSet<SortableNodeAddress> convertToSortableLaunchNodeAddress(Set<InetSocketAddress> remotes);
	public SortedSet<SortableNodeAddress> convertToSortableNonLaunchNodeAddress(Set<InetSocketAddress> remotes);
	public void incrementLaunchSortableNodeAddress(Collection<InetSocketAddress> plistClients, double val);
	public void incrementNonLaunchSortableNodeAddress(Collection<InetSocketAddress> plistClients, double val);
}
