package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;

import org.msrg.publiy.utils.Writers;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class NodeCache implements INode {
	
	public final static int BIT_VECTOR_INIT_SIZE = 32;

	int _index = -1;
	int _realIndex = -1;
	
	protected final InetSocketAddress _localAddress;
	protected final INode _remoteNode;
	public final Path<NodeCache> _cachedFromPath;
	public final Path<INode> _fromPath;
	public final int _distance;
	private final HashMap<InetSocketAddress, Sequence> _reportedArrivedSequence;
	private final HashMap<InetSocketAddress, Sequence> _reportedDiscardedSequence;
	
	private Sequence _lastArrivedSequence;
	private Sequence _lastDiscardedSequence;
	private Sequence _safeSequence;
	private Object _safeSequenceUpdateLock = new Object();

	private final BitSet _closerBV;
	private BitSet _closerMaskedBV = null;
	private final BitSet _fartherBV;
	
	public void setAsCloser(int i) {
		_closerBV.set(i, true);
	}
	
	public void setMaskedClosers(BitSet mask) {
		_closerMaskedBV = (BitSet) _closerBV.clone();
		_closerMaskedBV.or(mask);
	}
	
	public void setAsFarther(int i) {
		_fartherBV.set(i, true);
	}
	
	public BitSet getMaskedClosers() {
		return (BitSet)_closerMaskedBV;
	}
	
	public BitSet getClosers() {
		return (BitSet)_closerBV;
	}
	
	public BitSet getFarthers() {
		return (BitSet) _fartherBV;
	}

	public void setIndex(int x) {
		_index = x;
	}
	
	public int getIndex() {
		return _index;
	}
	
	public void setRealIndex(int x) {
		_realIndex = x;
		if(_realIndex==0 && _index!=0)
			throw new IllegalStateException(this + ": "+ _realIndex);
	}
	
	public int getRealIndex() {
		return _realIndex;
	}
	
	NodeCache(OverlayManager overlayManager, INode remoteNode, int neighborhoodRadious) {
		Path<NodeCache> cachedFromPath = overlayManager.computeCachePathFrom(remoteNode.getAddress());
		_remoteNode = remoteNode;
		_localAddress = overlayManager.getLocalAddress();
		
		_closerBV = new BitSet(overlayManager.getNeighborhoodRadius() - 1);
		_fartherBV = new BitSet(BIT_VECTOR_INIT_SIZE);

		cachedFromPath.addNodeToBegining(this);
		_cachedFromPath = cachedFromPath;
		_distance = (overlayManager.getLocalNode().equals(remoteNode))?0:_cachedFromPath.getLength();
		
		_fromPath = Path.createOverlayNodesPath(_cachedFromPath, neighborhoodRadious);
		
		_reportedArrivedSequence = new HashMap<InetSocketAddress, Sequence>();
		_reportedDiscardedSequence = new HashMap<InetSocketAddress, Sequence>();
	}
	
	boolean updateReportedArrivedSequence(Sequence[] arrivedSequences) {
		boolean ret = false;
		
		synchronized (_reportedArrivedSequence) {
			for(int i=0 ; i<arrivedSequences.length ; i++)
				ret = ret | updateReportedArrivedSequence(arrivedSequences[i]);
		}
		
		return ret;
	}
	
	private boolean updateLastArrivedSequenceIfLocal(Sequence arrivedSequence) {
		synchronized (_safeSequenceUpdateLock) {
			if(! _localAddress.equals(arrivedSequence.getAddress()))
				return false;
	
			if(_lastArrivedSequence == null) {
				_lastArrivedSequence = arrivedSequence;
			}
			else if(arrivedSequence.succeeds(_lastArrivedSequence)) {
				_lastArrivedSequence = arrivedSequence;
			}
			else
				return false;
	
			boolean ret = true;
			
			if(_lastArrivedSequence == null)
				ret = false;
			else if(_safeSequence == null)
				_safeSequence = _lastArrivedSequence;
			else if(_lastArrivedSequence.succeeds(_safeSequence))
				_safeSequence = _lastArrivedSequence;
			else
				ret = false;
			
//			if(ret == true)
//				System.out.println("SafeSEQ Updated: '" + _safeSequence + "' " + this._remoteNode);
			
			return ret;
		}
	}
	
	private boolean updateReportedArrivedSequence(Sequence arrivedSequence) {
		boolean ret = updateLastArrivedSequenceIfLocal(arrivedSequence);
		
		Sequence oldArrivedSequence = _reportedArrivedSequence.get(arrivedSequence.getAddress());
		if(oldArrivedSequence == null) {
			_reportedArrivedSequence.put(arrivedSequence.getAddress(), arrivedSequence);
			return ret | isLocal();
		}
			
		if(arrivedSequence.succeeds(oldArrivedSequence)) {
			_reportedArrivedSequence.put(arrivedSequence.getAddress(), arrivedSequence);
			return ret | isLocal();
		}
		
		return ret;
	}
	
	private boolean updateLastDiscardedSequenceIfLocal(Sequence discardedSequence) {
		synchronized (_safeSequenceUpdateLock) {
			if(! _localAddress.equals(discardedSequence.getAddress()))
				return false;
	
			if(_lastDiscardedSequence == null) {
				_lastDiscardedSequence = discardedSequence;
			}
			else if(discardedSequence.succeeds(_lastDiscardedSequence)) {
				_lastDiscardedSequence = discardedSequence;
			}
			else
				return false;
	
			boolean ret = true;
			
			if(_lastDiscardedSequence == null)
				ret = false;
			else if(_safeSequence == null)
				_safeSequence = _lastDiscardedSequence;
			else if(_lastDiscardedSequence.succeeds(_safeSequence))
				_safeSequence = _lastDiscardedSequence;
			else
				ret = false;
			
//			if(ret == true)
//				System.out.println("SafeSEQ Updated: '" + _safeSequence + "' " + this._remoteNode);
			
			return ret;
		}
	}
	
	boolean updateReportedDiscardedSequence(Sequence[] discardedSequences) {
		boolean ret = false;
		
		synchronized (_reportedDiscardedSequence) {
			for(int i=0 ; i<discardedSequences.length ; i++)
				ret = ret | updateReportedDiscardedSequence(discardedSequences[i]);
		}
		
		return ret;
	}
	
	private boolean updateReportedDiscardedSequence(Sequence discardedSequence) {
		boolean ret = updateLastDiscardedSequenceIfLocal(discardedSequence);
		
		Sequence oldDiscardedSequence = _reportedDiscardedSequence.get(discardedSequence.getAddress());
		if(oldDiscardedSequence == null) {
			_reportedDiscardedSequence.put(discardedSequence.getAddress(), discardedSequence);
			return ret | isLocal();
		}
			
		if(discardedSequence.succeeds(oldDiscardedSequence)) {
			_reportedDiscardedSequence.put(discardedSequence.getAddress(), discardedSequence);
			return ret | isLocal();
		}

		return ret;
	}
	
	public Sequence getLastArrivedSequenceFromNode(INode fromNode) {
		return _reportedArrivedSequence.get(fromNode.getAddress());
	}
	
	public Sequence getLastDiscardedSequenceFromNode(INode fromNode) {
		return _reportedDiscardedSequence.get(fromNode.getAddress());
	}
	
	public Sequence getSafeSequence() {
		synchronized (_safeSequenceUpdateLock) {
			return _safeSequence;
		}
	}
	
	public boolean updatePathSequences() {
		if(isLocal())
			return false;
		
		Sequence aSafeSoFar=null, dSafeSoFar=null;
		boolean validASafeSoFar = true;
		NodeCache[] pathNodes = _cachedFromPath._nodes;
		for(int i=0 ; i<_cachedFromPath._length ; i++)
		{
			if(aSafeSoFar == null)
				aSafeSoFar = pathNodes[i]._lastArrivedSequence;
			else if(pathNodes[i]._lastArrivedSequence != null)
				if(aSafeSoFar.succeeds(pathNodes[i]._lastArrivedSequence))
					aSafeSoFar = pathNodes[i]._lastArrivedSequence;
			
			if(pathNodes[i]._lastArrivedSequence==null)
				validASafeSoFar = false;
					
			if(dSafeSoFar == null)
				dSafeSoFar = pathNodes[i]._lastDiscardedSequence;
			else if(pathNodes[i]._lastDiscardedSequence != null)
				if(pathNodes[i]._lastDiscardedSequence.succeeds(dSafeSoFar))
					dSafeSoFar = pathNodes[i]._lastDiscardedSequence;
		}
		
		Sequence initialSafeSequence = _safeSequence;
		synchronized (_safeSequenceUpdateLock) 
		{
			if(validASafeSoFar) {
				if(_safeSequence == null)
					_safeSequence = aSafeSoFar;
				else if(aSafeSoFar != null && aSafeSoFar.succeeds(_safeSequence))
					_safeSequence = aSafeSoFar;
			}
			
			if(_safeSequence == null)
				_safeSequence = dSafeSoFar;
			else if(dSafeSoFar != null && dSafeSoFar.succeeds(_safeSequence))
				_safeSequence = dSafeSoFar;
		}
		
//		System.out.println("SafeSEQ Updated: '" + _safeSequence + "' " + this._remoteNode);
		
		return initialSafeSequence != _safeSequence;
	}
	
	@Override
	public String toString() {
		return "NodeCache (" + _remoteNode.getAddress() + "@"+ _distance + "): " + _fromPath; 
	}

	@Override
	public InetSocketAddress getAddress() {
		return _remoteNode.getAddress();
	}

	@Override
	public INode getCloserNode() {
		return _remoteNode.getCloserNode();
	}

	@Override
	public INode[] getFartherNeighbors() {
		return _remoteNode.getFartherNeighbors();
	}

	@Override
	public boolean isLocal() {
		return _remoteNode.isLocal();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(this.getClass().isInstance(obj)) {
			NodeCache nodeObj = (NodeCache)obj;
			return _remoteNode.equals(nodeObj._remoteNode);
		}
		
		return _remoteNode.equals(obj);
	}
	
	@Override 
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	protected boolean updateSafeSequence() {
		boolean ret1 = true;
		if(_lastArrivedSequence == null)
			ret1 = false;
		else if(_safeSequence == null)
			_safeSequence = _lastArrivedSequence;
		else if(_lastArrivedSequence.succeeds(_safeSequence))
			_safeSequence = _lastArrivedSequence;
		else
			ret1 = false;

		boolean ret2 = true;
		if(_lastDiscardedSequence == null)
			ret2 = false;
		else if(_safeSequence == null)
			_safeSequence = _lastDiscardedSequence;
		else if(_lastDiscardedSequence.succeeds(_safeSequence))
			_safeSequence = _lastDiscardedSequence;
		else
			ret2 = false;
		
		boolean ret = ret1 | ret2;
//		if(ret == true)
//			System.out.println("SafeSEQ Updated: '" + _safeSequence + "' " + this._remoteNode);
		
		return ret;
		
	}

	@Override
	public int compareTo(INode o) {
		NodeCache oNodeCache = (NodeCache) o;
		
		if(_distance < oNodeCache._distance) 
			return -1;

		if(_distance > oNodeCache._distance)
			return 1;

		int addressCompare = _remoteNode.compareTo(oNodeCache._remoteNode);
//		if(addressCompare == 0)
//			throw new IllegalArgumentException("Comparing nodecache to itself: " + this + " vs. " + o);
		return addressCompare;
	}
	
	public String getArrivedDiscardedSummary() {
		String arr = Writers.write(_reportedArrivedSequence);
		String dsc = Writers.write(_reportedDiscardedSequence);
		
		return "Node(" + _remoteNode.getAddress().getPort() + ") " + _safeSequence + " - ARR: " + arr  + "\t DISC: " + dsc;
	}

	@Override
	public NodeTypes getNodeType() {
		return _remoteNode.getNodeType();
	}

	@Override
	public boolean remove(INode neighbor) {
		return _remoteNode.remove(neighbor);
	}

	@Override
	public String toStringRecursive(int i) {
		return _remoteNode.toStringRecursive(i);
	}

	@Override
	public void getSummary(Set<TRecovery_Join> trjSet, OverlayNode excludedNode, int maxDistance) {
		_remoteNode.getSummary(trjSet, excludedNode, maxDistance);
	}

	@Override
	public int getFartherCounts() {
		return _remoteNode.getFartherCounts();
	}

	@Override
	public int getNeighborCount() {
		return _remoteNode.getNeighborCount();
	}

	@Override
	public void setNodeType(NodeTypes nodeType) {
		_remoteNode.setNodeType(nodeType);		
	}

	@Override
	public boolean removeYourself() {
		return _remoteNode.removeYourself();
	}

	@Override
	public void addNeighbor(INode overlayNode) {
		_remoteNode.addNeighbor(overlayNode);		
	}

	@Override
	public INode addNeighbor(InetSocketAddress newNeighbor, NodeTypes nodeType) {
		return _remoteNode.addNeighbor(newNeighbor, nodeType);
	}

	@Override
	public INode[] getNeighbors() {
		return _remoteNode.getNeighbors();
	}

	@Override
	public int setFartherCounts() {
		return _remoteNode.setFartherCounts();
	}
}
