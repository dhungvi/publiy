package org.msrg.publiy.pubsub.core;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.NodeCache;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.multicast.Dack_Bundle;
import org.msrg.publiy.pubsub.core.packets.multicast.TDack;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Depart;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;

public interface IOverlayManager extends IOverlayManagerQueriable {
	
	public NodeCache[] getAllCachedNodes();

	public void joinNode(InetSocketAddress joinPointAddress);
	public void handleMessage(TMulticast_Join tmj);
	public void handleMessage(TMulticast_Depart tmd);
	void updateFromTDack(TDack tDack);
	
	Path<INode> getPathFrom(InetSocketAddress fromAddr);
	Path<INode> getPathFrom(InetSocketAddress fromAddr, boolean cached);
	NodeCache getNodeCache(InetSocketAddress remoteAddress);
	
	public Set<InetSocketAddress> getShortSummary();
	public TRecovery_Join [] getSummaryFor(InetSocketAddress remote);
	void applySummary(TRecovery_Join trj);
	void applySummary(String lines);
	public boolean dumpOverlay();

	public InetSocketAddress[] getNeighbors(InetSocketAddress remote);
	public InetSocketAddress[] getNonClientNeighbors(InetSocketAddress remote);
	public void applyAllJoinSummary(TRecovery_Join[] trjs);
	Set<InetSocketAddress> getFartherNeighbors(InetSocketAddress remote);
	List<InetSocketAddress> getFartherNeighborsOrderedList(InetSocketAddress remote);
	
	public LocalSequencer getLocalSequencer();
	public InetSocketAddress getLocalAddress();
	public int nodesDistnacesThroughLocal(INode node1, INode node2);
	
	public void updateAllNodesCache(ITimestamp arrivedTimestamp, ITimestamp discardedTimestamp);
	public boolean safeToDiscard(InetSocketAddress recipientAddress, Sequence messageLocalGeneratedSequence);
	public Dack_Bundle[][] prepareOutgoingArrivedSequences(InetSocketAddress remoteAddress);

	NodeCache[] initializeOverlayManager();

	public String getDumpFilename();

	String getAllArrivedDiscardedSummary();
	NodeTypes getNodeType(InetSocketAddress remote);

	public InetSocketAddress isClientOf(InetSocketAddress clientAddress);
	public IBrokerShadow getBrokerShadow();

	public int getNeighborhoodSize();
	public int getNeighborhoodRadius();

	public InetSocketAddress getNewFromForMorphedMessage(InetSocketAddress remote, InetSocketAddress oldFrom);

}
