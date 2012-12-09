package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;
import java.util.Set;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;


public interface INode extends Comparable<INode> {

	public boolean isLocal();
	public INode[] getFartherNeighbors();
	public InetSocketAddress getAddress();
	public INode getCloserNode();
	
	public boolean equals(Object obj);
	public int hashCode();
	public NodeTypes getNodeType();
	public boolean remove(INode neighbor);
	public String toStringRecursive(int i);
	public void getSummary(Set<TRecovery_Join> trjSet, OverlayNode excludedNode, int maxDistance);
	public int getFartherCounts();
	public int getNeighborCount();
	public void setNodeType(NodeTypes nodeType);
	public boolean removeYourself();
	public void addNeighbor(INode overlayNode);
	public INode addNeighbor(InetSocketAddress newNeighbor, NodeTypes nodeType);
	public INode[] getNeighbors();
	public int setFartherCounts();
	
}
