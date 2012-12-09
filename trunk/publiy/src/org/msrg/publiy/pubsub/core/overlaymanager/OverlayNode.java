package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;
import java.util.Set;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class OverlayNode implements INode {
	
	public static final int MAX_NODE_DEGREE = 20;

	protected final LocalSequencer _localSequencer;
	protected Sequence _creationSequence;
	protected InetSocketAddress _address;
	protected INode [] _neighbors;
	protected int _neighborCount = 0;
	protected int _fartherCounts = 0;
	protected NodeTypes _nodeType;
	
	protected OverlayNodeState _overlayNodeState = OverlayNodeState.O_N_S_UNKNOWN;

	@Override
	public int setFartherCounts() {
		int fartherSum = 0;
		for(int i=1 ; i<_neighborCount ; i++)
			fartherSum += _neighbors[i].setFartherCounts();
		
		_fartherCounts = fartherSum + 1;
		return _fartherCounts;
	}
	
	@Override
	public int getFartherCounts() {
		return _fartherCounts;
	}
	
	@Override
	public int getNeighborCount() {
		return _neighborCount;
	}
	
	protected void setOverlayNodeState(OverlayNodeState overlayNodeState) {
		_overlayNodeState = overlayNodeState;
	}
	
	protected OverlayNodeState getOverlayNodeState() {
		return _overlayNodeState;
	}
	
	protected OverlayNode(LocalSequencer localSequencer, InetSocketAddress address, NodeTypes nodeType) {
		_localSequencer = localSequencer;
		_creationSequence = _localSequencer.getNext();
		_address = address;
		_neighbors = new OverlayNode[MAX_NODE_DEGREE];
		_nodeType = nodeType;
	}
	
	public OverlayNode(LocalSequencer localSequencer, InetSocketAddress address, NodeTypes nodeType, INode closerNode) {
		this(localSequencer, address, nodeType);
		_neighbors[0] = closerNode;
		_neighborCount = 1;
		closerNode.addNeighbor(this);
	}
	
	@Override
	public NodeTypes getNodeType() {
		return _nodeType;
	}

	@Override
	public INode getCloserNode() {
		return _neighbors[0];
	}
	
	@Override
	public INode[] getNeighbors() {
		return _neighbors;
	}
	
	@Override
	public void getSummary(Set<TRecovery_Join> trjSet, OverlayNode excludedNode, int maxDistance) {
		if(maxDistance < 0)
			return;
		
		INode closerNode = getCloserNode();
		trjSet.add(new TRecovery_Join(_localSequencer.getNext(), this._address, this._nodeType, closerNode.getAddress(), closerNode.getNodeType()));
		
		if(maxDistance == 0)
			return;
		
		for(int i=0 ; i<_neighborCount ; i++)
			if(_neighbors[i] != excludedNode) 
				_neighbors[i].getSummary(trjSet, this, maxDistance-1);
	}
	
	@Override
	public InetSocketAddress getAddress() {
		return _address;
	} 
	
	@Override
	public void addNeighbor(INode farther) {
		try{
			for(int i=0 ; i<_neighborCount ; i++)
				if(_neighbors[i].equals(farther))
					return;
			
			_neighbors[_neighborCount++] = farther;
		}catch(ArrayIndexOutOfBoundsException aibx) {
			LoggerFactory.getLogger().infoX(LoggingSource.LOG_SRC_OVERLAY_MANAGER, aibx, "ArrayIndexOutOfBoundsException::: node:" + this + "\t to: " + farther);
		}
	}
	
	@Override
	public INode addNeighbor(InetSocketAddress newNeighbor, NodeTypes nodeType) {
		for(int i=0 ; i<_neighborCount ; i++)
			if(newNeighbor.equals(_neighbors[i].getAddress()))
				return _neighbors[i];
		OverlayNode newNode = createNewOverlayNode(newNeighbor, nodeType, this);
//		addNeighbor(newNode);
		return newNode;
	}
	
	protected OverlayNode createNewOverlayNode(InetSocketAddress remote, NodeTypes nodeType, INode neighbor) {
		return new OverlayNode(_localSequencer, remote, nodeType, neighbor);
	}
	
	public boolean removeYourself() {
		if(_neighborCount!=1)
			return false;
		
		return _neighbors[0].remove(this);
	}
	
	@Override
	public INode[] getFartherNeighbors() {
		int fartherNeighborsCount = _neighborCount - 1;
		INode[] fartherNeighbors = new OverlayNode[fartherNeighborsCount];
		for(int i=0 ; i<_neighborCount-1 ; i++)
			fartherNeighbors[i] = _neighbors[i+1];
		return fartherNeighbors;
	}

	@Override
	public boolean remove(INode neighbor) {
		int index = -1;

		for(int i=0 ; i<_neighborCount ; i++)
			if(_neighbors[i].equals(neighbor)) {
				index = i;
				break;
			}
		
		if(index==-1)
			return false;
		
		if(index == 0 && !isLocal()) //LocalOverlayNode.class.isInstance(this))
			throw new IllegalStateException("A non-LocalOverlayNode's closer neighbor cannot be removed.");

		_neighborCount--;

		for(int j=index ; j<_neighborCount ; j++)
			_neighbors[j] = _neighbors[j+1];
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return _address.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(InetSocketAddress.class.isInstance(obj)) {
			InetSocketAddress objAddress = (InetSocketAddress) obj;
			return this._address.equals(objAddress);
		}
			
		if(OverlayNode.class.isInstance(obj) || LocalOverlayNode.class.isInstance(obj)) {
			OverlayNode onObj = (OverlayNode) obj;
			return onObj._address.equals(this._address);
		}

		return false;
	}
	
	@Override
	public String toStringRecursive(int level) {
		String str = "";
		String str0 = "";
		
		for(int i=0 ; i<level ; i++)
			str0 += "\t";	
		
		for(int i=1 ; i<_neighborCount ; i++)
			str += "\n" + str0 + "(" + _address + "->" + _neighbors[i].getAddress() + ") " + _neighbors[i].toStringRecursive(level+1);
		
		return str;
	}
	
	@Override
	public String toString() {
		return "ONode[" + Writers.write(_address) +"]";
	}
	
	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public int compareTo(INode o) {
		byte[] ipBytes = _address.getAddress().getAddress();
		byte[] ipOBytes = o.getAddress().getAddress().getAddress();
		
		for(int i=0 ; i<ipBytes.length ; i++)
			if(ipBytes[i] > ipOBytes[i])
				return 1;
			else if(ipBytes[i] < ipOBytes[i])
				return -1;
		
		int port = _address.getPort();
		int oPort = o.getAddress().getPort();
		if(port > oPort)
			return 1;
		else if(port <oPort)
			return -1;
		else
			return 0;
	}

	@Override
	public void setNodeType(NodeTypes nodeType) {
		_nodeType = nodeType;
	}
}

