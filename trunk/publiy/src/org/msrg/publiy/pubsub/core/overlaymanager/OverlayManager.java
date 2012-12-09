package org.msrg.publiy.pubsub.core.overlaymanager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.JoinInfo;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ITimestamp;
import org.msrg.publiy.pubsub.core.packets.multicast.Dack_Bundle;
import org.msrg.publiy.pubsub.core.packets.multicast.TDack;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Depart;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;

public class OverlayManager implements IOverlayManager, ILoggerSource {
	protected String _dumpFileName;
	protected ILocalOverlayNode _localNode; 
	protected final IBrokerShadow _brokerShadow;
	protected final LocalSequencer _localSequencer;
	protected final InetSocketAddress _localAddress;
	protected final int _neighborhoodRadius;
	
	protected Map<InetSocketAddress, INode> _nodes;
	private Map<InetSocketAddress, NodeCache> _nodesCache;
	protected final StatisticsLogger _statLogger;
	
	public ILocalOverlayNode getLocalNode() {
//		InetSocketAddress localAddress = LocalSequencer.getLocalSequencer().getLocalAddress();
//		return getLocalNode(localAddress);
		return _localNode;
	}
	
	public ILocalOverlayNode getLocalNode(InetSocketAddress localAddress) {
		if(_localNode == null) {
			if(localAddress==null)
				localAddress = _localSequencer.getLocalAddress();
			_localNode = createLocalOverlayNode(_localSequencer, localAddress);
			insertIntoNodes(_localNode);
			getNodeCache(localAddress);
		}
		return _localNode;
	}
	
	protected ILocalOverlayNode createLocalOverlayNode(LocalSequencer localSequencer, InetSocketAddress localAddress) {
		return new LocalOverlayNode(localSequencer, localAddress);
	}
	
	private INode insertIntoNodes(INode node) {
		INode prevOverlayNode = _nodes.put(node.getAddress(), node);
		if(prevOverlayNode != null) {
			return prevOverlayNode;
		} else {
			if(_statLogger != null)
				_statLogger.topologyMapSizeUpdated(_nodes.size());
			
			return null;
		}
	}
	
	@Override
	public NodeCache[] initializeOverlayManager() {
		NodeCache[] nodes = getAllCachedNodes();
		for(int i=0 ; i<nodes.length ; i++) {
			nodes[i].setIndex(i);
			
			if(!Broker.RELEASE || Broker.DEBUG)
				if(i>0 && nodes[i]._distance < nodes[i-1]._distance)
					throw new IllegalStateException(nodes[i] + " vs. " + nodes[i-1]);
		}
		
		
		for(int i=0 ; i<nodes.length ; i++) {
			Path<NodeCache> path = nodes[i]._cachedFromPath;
			for(int j=0 ; j<path.getLength() ; j++)
			{
				NodeCache closer = path.get(j);
				nodes[i].setAsCloser(closer.getIndex());
				closer.setAsFarther(nodes[i].getIndex());
			}
		}
		
		for(int i=1 ; i<nodes.length && nodes[i]._distance == 1 ; i++) {
			NodeCache neighbor = nodes[i];
			BitSet farthers = neighbor.getFarthers();
			
			BitSet mask = new BitSet(nodes.length);
			mask.set(0, nodes.length);
			mask.andNot(farthers);
			
			for(int j = farthers.nextSetBit(0); j >= 0; j = farthers.nextSetBit(j+1)) {
				nodes[j].setMaskedClosers(mask);
			}
		}
		
		BitSet localMaskedClosers = new BitSet(nodes.length);
		localMaskedClosers.set(0, nodes.length-1);
		nodes[0].setMaskedClosers(localMaskedClosers);
		return nodes;
	}
	
	protected Map<InetSocketAddress, INode> createNodesMap() {
		return new HashMap<InetSocketAddress, INode>();
	}
	
	public OverlayManager(IBrokerShadow brokerShadow) {
		_brokerShadow = brokerShadow;
		_localSequencer = _brokerShadow.getLocalSequencer();
		_localAddress = _localSequencer.getLocalAddress();
		_neighborhoodRadius = _brokerShadow.getNeighborhoodRadius();
		_statLogger = _brokerShadow.getStatisticsLogger();
		InetSocketAddress localAddress = (_brokerShadow==null ? null : _brokerShadow.getLocalAddress());
		_nodes = createNodesMap();
		_nodesCache = new HashMap<InetSocketAddress, NodeCache>();
		ILocalOverlayNode local = getLocalNode(localAddress);
		local.getFartherCounts();
		_dumpFileName = getDumpFilename(_brokerShadow);
	}
	
	protected String getDumpFilename(IBrokerShadow brokerShadow) {
		return brokerShadow==null ? null : brokerShadow.getRecoveryFileName();
	}
	
	protected boolean addNode(InetSocketAddress joiningAddress, NodeTypes joiningType, InetSocketAddress joinPointAddress, NodeTypes joinpoingType) {
		return addNode(joiningAddress, joiningType, joinPointAddress, joinpoingType, true);
	}
	
	protected boolean addNode(InetSocketAddress joiningAddress, NodeTypes joiningType, InetSocketAddress joinPointAddress, NodeTypes joinpoingType, boolean dumpOverlay) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, super.toString() + "OVERLAY Adding new node: '" + joiningAddress + "[" + joiningType + "]" +  joinPointAddress + "[" + joinpoingType + "] _ " + _nodes.containsKey(joiningAddress));
		synchronized(_nodes) {
			if(_nodes.containsKey(joiningAddress)) {
				return false;
			}
			
			INode joinPointNode = _nodes.get(joinPointAddress);
			if(joinPointNode == null) {
				dumpOverlayNow();
				throw new IllegalStateException("OverlayManager::addNeighbor - ERROR, '" + joinPointAddress + "' is not a valid joinPoint in the overlay.");
			}
			
			if(joinPointNode.getNodeType() != joinpoingType) {
//				throw new IllegalArgumentException("Node: " + joinPointNode + ": " + joinPointNode.getNodeType() + " vs. " + joinpoingType);
				joinPointNode.setNodeType(joinpoingType);
			}
			
			if(!_localNode.getAddress().equals(joinPointAddress)) // || _localNode.getAddress().equals(joiningAddress)) {
			{
				Path<INode> joinPointPath = getPathFrom(joinPointAddress);
				if(joinPointPath.getLength() >= _brokerShadow.getNeighborhoodRadius()) 
					throw new IllegalArgumentException("Join point too far: " + joiningAddress + "_" + joinPointPath);
			}
			
			INode joiningNode = getNewOverlayNode(joiningAddress, joiningType, joinPointNode);
			INode prevNode = insertIntoNodes(joiningNode);
			_localNode.getFartherCounts();
			getNodeCache(joiningAddress);
			
			if(prevNode != null)// & prevNode != joiningNode)
				throw new IllegalStateException();
			
			if(dumpOverlay)
				dumpOverlay();
		}
			
		return true;
	}
	
	private void dumpOverlayNow() {
		BrokerInternalTimer.inform("DUPMPING OVERLAY: ");
		BrokerInternalTimer.inform("\tALL NODES: " + _nodes);
		BrokerInternalTimer.inform("\t: " + this);
	}

	protected INode getNewOverlayNode(InetSocketAddress joiningAddress, NodeTypes nodeType, INode joinPointNode) {
		return new OverlayNode(_localSequencer, joiningAddress, nodeType, joinPointNode);
	}
	
	private boolean removeNode(InetSocketAddress nodeAddr, InetSocketAddress joinPointAddress) {
		INode node = null;
		synchronized(_nodes) {
			node = _nodes.remove(nodeAddr);	
		}
		if(node == null)
			return false;
		
		if(!node.getCloserNode().getAddress().equals(joinPointAddress))
			throw new IllegalStateException("OverlayManager::removeNode, ERROR - '" + nodeAddr + "' is connected to '" + node.getCloserNode() + "' and cannot depart from '" + joinPointAddress + "'");
		
		return node.removeYourself();
	}

	@Override
	public void handleMessage(TMulticast_Join tmj) {
		InetSocketAddress joiningAddress = tmj.getJoiningNode();
		InetSocketAddress joinPointAddress = tmj.getJoinPoint();

		NodeTypes joiningNodeType = tmj.getJoininingType();
		NodeTypes joinPointType = tmj.getJoinPointType();
		
		addNode(joiningAddress, joiningNodeType, joinPointAddress, joinPointType);
	}
	
	@Override
	public void handleMessage(TMulticast_Depart tmd) {
		InetSocketAddress nodeAddress = tmd.getJoiningNode();
		InetSocketAddress joinPointAddress = tmd.getJoinPoint();
		
		removeNode(nodeAddress, joinPointAddress);
	}

	@Override
	public void updateFromTDack(TDack tDack) {
		updateArrivedSequences(tDack._arrivedDackBundles);
		updateDiscardedSequences(tDack._discardedDackBundles);
	}
	
	@Override
	public String getAllArrivedDiscardedSummary() {
		String str = "{";
		for(Iterator<NodeCache> cacheIt = _nodesCache.values().iterator(); cacheIt.hasNext() ;) {
			NodeCache cache = cacheIt.next();
			str += cache.getArrivedDiscardedSummary();
		}
		
		return str + "}";
	}

	private void updateDiscardedSequences(Dack_Bundle[] discardedBundles) {
		for(int i=0 ; i<discardedBundles.length ; i++) {
			Dack_Bundle discardedDackBundle = discardedBundles[i];
			
			InetSocketAddress remoteAddress = discardedDackBundle._reportingAddress;
			Sequence[] discardedSequences = discardedDackBundle._sequences;
			
			NodeCache nodeCache = getNodeCache(remoteAddress);
			if(nodeCache != null)
				nodeCache.updateReportedDiscardedSequence(discardedSequences);
			else
				LoggerFactory.getLogger().warn(this, "Updating discarded Sequences failed. Node '" + remoteAddress + "' not part of topology map.");
		}
	}
	
	private void updateArrivedSequences(Dack_Bundle[] arrivedBundles) {
		for(int i=0 ; i<arrivedBundles.length ; i++) {
			Dack_Bundle arrivedDackBundle = arrivedBundles[i];
			
			InetSocketAddress remoteAddress = arrivedDackBundle._reportingAddress;
			Sequence[] arrivedSequences = arrivedDackBundle._sequences;
			
			NodeCache nodeCache = getNodeCache(remoteAddress);
			if(nodeCache != null)
				nodeCache.updateReportedArrivedSequence(arrivedSequences);
			else
				LoggerFactory.getLogger().warn(this, "Updating arrived Sequences failed. Node '" + remoteAddress + "' not part of topology map.");
		}
	}
	
	
	Path<NodeCache> computeCachePathFrom(InetSocketAddress fromAddr) {
		Path<NodeCache> cachePath = Path.createNodesCachePath(_neighborhoodRadius);
		Path<INode> path = getPathFrom_Privately(fromAddr);
		
		for(int i=1 ; i<path._length ; i++) {
			NodeCache nodeCache = getNodeCache(path._nodes[i].getAddress());
			cachePath.addNode(nodeCache);
		}
		
		return cachePath;
	}
	
	private Path<INode> getPathFrom_Privately(InetSocketAddress fromAddr) {
		INode fromNode = _nodes.get(fromAddr);	
		if(fromNode == null)
			return null;
		
		Path<INode> p = Path.createOverlayNodesPath(_neighborhoodRadius);
		INode node = fromNode;
		
		while (!node.equals(_localNode)) {
			p.addNode(node);
			node = node.getCloserNode();
		}
		
		return p;
	}

	@Override
	public String toString() {
		String str = "OverlayManager::";
		str += getLocalNode().toStringRecursive(0);
		return str;
	}
	
	@Override
	public void applySummary(TRecovery_Join trj) {
		LoggerFactory.getLogger().debug(this, "Applying Summary: " + trj);
		InetSocketAddress addr1 = trj.getJoiningNode();
		InetSocketAddress addr2 = trj.getJoinPoint();
		NodeTypes type1 = trj.getJoininingType();
		NodeTypes type2 = trj.getJoinPointType();
		
		if(!addNode(addr1, type1, addr2, type2))
			if(!addNode(addr2, type2, addr1, type1))
				LoggerFactory.getLogger().warn(this, "Did not add '" + addr1 + "' '" + addr2);
	}

	@Override
	public TRecovery_Join[] getSummaryFor(InetSocketAddress remote) {
		synchronized(_nodes) {
			Path<INode> pathFromRemote = getPathFrom(remote);
			if(pathFromRemote == null)
				return null;
			
			INode firstNode = pathFromRemote.getLast();
			int radios = _neighborhoodRadius - ((remote.equals(_localNode.getAddress()))?0:pathFromRemote.getLength());
			List<INode> fartherNodes = getBehindNodes(_localNode, firstNode, radios);
			TRecovery_Join[] trjs = new TRecovery_Join[fartherNodes.size()-1];
			
			int i=0;
			Iterator<INode> fartherNodesIt = fartherNodes.iterator();
			while (fartherNodesIt.hasNext()) {
				INode farther = fartherNodesIt.next();
				if(farther != _localNode) {
					INode closerNode = farther.getCloserNode();
					TRecovery_Join trj = new TRecovery_Join(_localSequencer.getNext(), farther.getAddress(), farther.getNodeType(), closerNode.getAddress(), closerNode.getNodeType());
					trjs[i++] = trj;
				}
			}

			return trjs;
			
//			int distanceRemote;
//			if(_localNode.getAddress().equals(remote))
//				distanceRemote = 0;
//			else
//				distanceRemote = pathFromRemote.getLength();
//
//			int overlaySize = _nodes.size() - 1;
//			TRecovery_Join[] summary = new TRecovery_Join[overlaySize];
//			int index = 0;
//			for(int i=1 ; i<_localNode._neighborCount ; i++)
//				index = _localNode._neighbors[i].getSummary(summary, index);
//			
//			Set<TRecovery_Join> trjsSet = new HashSet<TRecovery_Join>();
//			for(int i=0 ; i<summary.length ; i++)
//			{
//				InetSocketAddress joiningNodeAddress = summary[i].getJoiningNode();
//				Path<INode> pathFromJoiningNode = getPathFrom(joiningNodeAddress);
//				if(pathFromRemote.passes(joiningNodeAddress) || pathFromJoiningNode.passes(remote)) {
//					trjsSet.add(summary[i]);
//					continue;
//				}
//				int distanceJoiningNode = pathFromJoiningNode.getLength();
//				if(distanceJoiningNode +  distanceRemote <= Broker.DELTA + 1)
//					trjsSet.add(summary[i]);
//			}
//
//			return trjsSet.toArray(_tmpTrjArray);
		}
	}
	
	protected final DateFormat DATE_FORMAT = new SimpleDateFormat("mm:ss");
	@Override
	public final boolean dumpOverlay() {
		if(_dumpFileName == null)
			return true;
		
		if(Broker.RELEASE && _brokerShadow.isMP())
			return true;
		
		try {
			FileWriter fwriter = new FileWriter(_dumpFileName, true);
			dumpOverlay(fwriter);
			fwriter.close();
		}catch (IOException iox) {
			return false;
		}
		return true;
	}

	public void dumpOverlay(Writer ioWriter) throws IOException {
		TRecovery_Join [] trjs = getSummaryFor(_localNode.getAddress());
		for(int i=0 ; i<trjs.length ; i++)
			ioWriter.write(trjs[i].getJoinPointType().getCodedChar() + trjs[i].getJoinPoint().toString() + " " + 
							trjs[i].getJoininingType().getCodedChar() + trjs[i].getJoiningNode() + "\n");
		
		ioWriter.write("#************** " + DATE_FORMAT.format(new Date()) + "\n");
	}
	
	@Override
	public void joinNode(InetSocketAddress joinPointAddress) {
		addNode(joinPointAddress, NodeTypes.NODE_BROKER, _localNode.getAddress(), LocalSequencer.getNodeTypeGeneric());
	}

	@Override
	public InetSocketAddress[] getNeighbors(InetSocketAddress remote) {
		synchronized(_nodes) {
			INode remoteNode = _nodes.get(remote);
			if(remoteNode == null)
				return null;
			InetSocketAddress[] retAddrs = new InetSocketAddress[remoteNode.getNeighborCount()];
			for(int i=0 ; i<remoteNode.getNeighborCount() ; i++)
				retAddrs[i] = remoteNode.getNeighbors()[i].getAddress();
			return retAddrs;
		}
	}

	@Override
	public void applyAllJoinSummary(TRecovery_Join[] trjs) {
		synchronized(_nodes) {
			Map<InetSocketAddress, List<TRecovery_Join>> mapping = new HashMap<InetSocketAddress, List<TRecovery_Join>>();
			
			for(int i=0 ; i<trjs.length ; i++) {
				if(trjs[i] == null)
					continue;
				InetSocketAddress joinPointAddress = trjs[i].getJoinPoint();
				InetSocketAddress joiningAddress = trjs[i].getJoiningNode();
				
				List<TRecovery_Join> list = mapping.get(joinPointAddress);
				if(list == null) {
					list = new LinkedList<TRecovery_Join>();
					mapping.put(joinPointAddress, list);
				}
				list.add(trjs[i]);
				
				List<TRecovery_Join> list2 = mapping.get(joiningAddress);
				if(list2 == null) {
					list2 = new LinkedList<TRecovery_Join>();
					mapping.put(joiningAddress, list2);
				}
				list2.add(trjs[i]);
			}
			applyJoinSummaryRecursively(_localNode, mapping);
			dumpOverlay();
		}
	}
	
	private void applyJoinSummaryRecursively(INode node, Map<InetSocketAddress, List<TRecovery_Join>> trjs) {
		InetSocketAddress addr = node.getAddress();
		List<TRecovery_Join> set = trjs.get(addr);
		if(set==null)
		{
			int initialNeighborsSize = node.getNeighborCount();
			for(int i=1 ; i<initialNeighborsSize ; i++)
			{
				assert node.getNeighborCount() == initialNeighborsSize;
				applyJoinSummaryRecursively(node.getNeighbors()[i], trjs);
			}
			return;
		}
		
		Iterator<TRecovery_Join> it = set.iterator();
		while (it.hasNext()) {
			TRecovery_Join tr = it.next();
			it.remove();
			InetSocketAddress addr1 = tr.getJoiningNode();
			NodeTypes type1 = tr.getJoininingType();
			InetSocketAddress addr2 = tr.getJoinPoint();
			NodeTypes type2 = tr.getJoinPointType();
			
			if(addr1.equals(addr)) {
				INode newNode = node.addNeighbor(addr2, type2);
				INode prevNode = insertIntoNodes(newNode);
				_localNode.getFartherCounts();
				if(prevNode!=null & prevNode != newNode)
					throw new IllegalStateException();
				
				trjs.get(addr2).remove(tr);
			}
			else{
				INode newNode = node.addNeighbor(addr1, type1);
				insertIntoNodes(newNode);
				_localNode.getFartherCounts();
				trjs.get(addr1).remove(tr);
			}
		}
		for(int i=1 ; i<node.getNeighborCount() ; i++)
			applyJoinSummaryRecursively(node.getNeighbors()[i], trjs);
		
		getNodeCache(node.getAddress());
	}
	
	@Override
	public List<InetSocketAddress> getFartherNeighborsOrderedList(InetSocketAddress remote) {
		List<InetSocketAddress> fartherNeighborsList = new LinkedList<InetSocketAddress>();
		synchronized(_nodes) {
			getFartherNeighborsOrderedCollectionPrivately(remote, fartherNeighborsList);
		}
		return fartherNeighborsList;
	}
	
	@Override
	public Set<InetSocketAddress> getFartherNeighbors(InetSocketAddress remote) {
		Set<InetSocketAddress> fartherNeighborsOrderedSet = new HashSet<InetSocketAddress>();
		synchronized(_nodes) {
			getFartherNeighborsOrderedCollectionPrivately(remote, fartherNeighborsOrderedSet);
		}
		return fartherNeighborsOrderedSet;
	}
	
	private Collection<InetSocketAddress> getFartherNeighborsOrderedCollectionPrivately(InetSocketAddress remote, Collection<InetSocketAddress> neighborsOrderedCollection) {
		INode node = _nodes.get(remote);
		if(node != null)
			retrieveFartherNeighbors(node, neighborsOrderedCollection);
		
		return neighborsOrderedCollection;	
	}
	
	private void retrieveFartherNeighbors(INode node, Collection<InetSocketAddress> neighborList) {
		neighborList.add(node.getAddress());
		for(int i=1 ; i<node.getNeighborCount() ; i++)
			retrieveFartherNeighbors(node.getNeighbors()[i], neighborList);	
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return _localNode.getAddress();
	}

	@Override
	public JoinInfo[] getTopologyLinks() {
		TRecovery_Join [] trjs = getSummaryFor(_localNode.getAddress());
		JoinInfo[] joinInfos = new JoinInfo[trjs.length];
		
		for(int i=0 ; i<joinInfos.length ; i++)
			joinInfos[i] = trjs[i].getInfo();
		
		return joinInfos;
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_OVERLAY_MANAGER;
	}

	public Map<InetSocketAddress, INode> getNodeCaches() {
		return _nodes;
	}
	
	@Override
	public NodeCache getNodeCache(InetSocketAddress remoteAddress) {
		INode remoteNode = _nodes.get(remoteAddress);
		if(remoteNode == null)
			return null;
		
		NodeCache nodeCache = _nodesCache.get(remoteAddress);
		if(nodeCache == null) {
			if(!_nodes.containsKey(remoteNode))
				throw new IllegalStateException("Node is not part of topology map: " + remoteNode);
				
			nodeCache = createNodeCache(remoteNode);
			_nodesCache.put(remoteAddress, nodeCache);
		}
		
		return nodeCache;
	}
	
	protected NodeCache createNodeCache(INode node) {
		return new NodeCache(this, node, _neighborhoodRadius);
	}
	
	protected NodeCache getNodeCacheNoCreate(INode node) {
		return _nodesCache.get(node);
	}
	
	protected NodeCache getNodeCacheNoCreate(InetSocketAddress remote) {
		return _nodesCache.get(remote);
	}
	
	@Override
	public void updateAllNodesCache(ITimestamp arrivedTimestamp, ITimestamp discardedTimestamp) {
		synchronized(_nodes)
		{
			updateFromLocalTimestamps(arrivedTimestamp, discardedTimestamp);
			
			Set<Entry<InetSocketAddress, INode>> nodeEntries = _nodes.entrySet();
			Iterator<Entry<InetSocketAddress, INode>> nodeEntriesIt = nodeEntries.iterator();
			while(nodeEntriesIt.hasNext())
			{
				Entry<InetSocketAddress, INode> nodeEntry = nodeEntriesIt.next();
				INode node = nodeEntry.getValue();
				updateNodeCache(node);
			}
		}
	}
	
	private void updateFromLocalTimestamps(ITimestamp arrivedTimestamp, ITimestamp discardedTimestamp) {
		Set<Sequence> arrivedSequencesSet = new HashSet<Sequence>();
		Set<Sequence> discardedSequencesSet = new HashSet<Sequence>();

		NodeCache localNodeCache = getNodeCache(_localNode.getAddress());
		List<INode> nodesWithinRange = getBehindNodes(_localNode, _localNode, _neighborhoodRadius);
		for(INode nodeWithinRange : nodesWithinRange) {
			if(arrivedTimestamp != null) {
				Sequence arrivedSequence = arrivedTimestamp.getLastReceivedSequence(nodeWithinRange.getAddress());
				if(arrivedSequence!=null)
					arrivedSequencesSet.add(arrivedSequence);
			}
			
			if(discardedTimestamp != null) {
				Sequence discardedSequence = discardedTimestamp.getLastReceivedSequence(nodeWithinRange.getAddress());
				if(discardedSequence!=null)
					discardedSequencesSet.add(discardedSequence);
			}
		}
			
		localNodeCache.updateReportedArrivedSequence(arrivedSequencesSet.toArray(new Sequence[0]));
		localNodeCache.updateReportedDiscardedSequence(discardedSequencesSet.toArray(new Sequence[0]));
		//TODO: keep an eye on below.
//		localNodeCache.updateSafeSequence();
	}

	private NodeCache updateNodeCache(INode node) {
		NodeCache nodeCache = getNodeCache(node.getAddress());
		if(nodeCache == null)
			return null;
		
		nodeCache.updatePathSequences();
		
		return nodeCache;
	}

	@Override
	public int nodesDistnacesThroughLocal(INode node1, INode node2) {
		NodeCache nodeCache1 = getNodeCache(node1.getAddress());
		NodeCache nodeCache2 = getNodeCache(node2.getAddress());
		
		if(nodeCache1 == null || nodeCache2 == null)
			return -1;
		
		Path<NodeCache> path1 = nodeCache1._cachedFromPath;
		Path<NodeCache> path2 = nodeCache2._cachedFromPath;
		
		if(path1.intersect(path2))
			return -1;
		else
			return nodeCache1._distance + nodeCache2._distance;
	}

	@Override
	public Path<INode> getPathFrom(InetSocketAddress fromAddr, boolean cached) {
		if(cached == true)
		{
			NodeCache nodeCache = getNodeCache(fromAddr);
			if(nodeCache != null)
				return nodeCache._fromPath;
			
			else
				return getPathFrom_Privately(fromAddr);
		}
		else
			return getPathFrom_Privately(fromAddr);
	}
	
	@Override
	public Path<INode> getPathFrom(InetSocketAddress fromAddr) {
		return getPathFrom(fromAddr, true);
	}
	
	private List<INode> getBehindNodes(INode remoteNode, 
										INode excludedNodeDirection, int radios) {
		
		List<INode> nodesCollection = new LinkedList<INode>();
		
		getFartherNodesOtherThan(nodesCollection, remoteNode, excludedNodeDirection, radios);
		
		return nodesCollection;
	}
	
	private void getFartherNodesOtherThan(Collection<INode> nodesCollection, INode node, 
											INode excludedNode, int radios) {
		
		if(radios == 0) {
			if(!node.equals(excludedNode))
				nodesCollection.add(node);
			return;
		}
		
		if(radios < 0)
			throw new IllegalStateException("Radios is negetive: " + radios);

		nodesCollection.add(node);
		
		for(int i=node.isLocal()?1:0 ; i<node.getNeighborCount() ; i++)
		{
			if(node.getNeighbors()[i].equals(excludedNode))
				continue;
			
			getFartherNodesOtherThan(nodesCollection, node.getNeighbors()[i], node, radios - 1);
		}
		
	}
	
	@Override
	public Dack_Bundle[][] prepareOutgoingArrivedSequences(InetSocketAddress remoteAddress) {
		NodeCache remoteCachedNode = getNodeCache(remoteAddress);
		if(remoteCachedNode == null)
			return null;
		
		if(remoteCachedNode._distance > _neighborhoodRadius)
			throw new IllegalStateException("Distance too large: " + remoteCachedNode._distance);
		
		int behindDistance = _neighborhoodRadius - remoteCachedNode._distance;
		int beyondDistance = behindDistance;
		
		Set<Dack_Bundle> retArrivedBundlesSet = new HashSet<Dack_Bundle>();
		Set<Dack_Bundle> retDiscardBundlesSet = new HashSet<Dack_Bundle>();
		
		Path<INode> pathFromRemote = getPathFrom(remoteAddress);
		
		List<INode> remoteBehindDeltaNeighborsSet = getBehindNodes(_localNode, pathFromRemote.getLast(), behindDistance);
		List<INode> remoteBeyondDeltaNeighborsSet = getBehindNodes(remoteCachedNode._remoteNode, remoteCachedNode._remoteNode.getCloserNode(), beyondDistance);
		
		Iterator<INode> remoteBehindDeltaNeighborsIt = remoteBehindDeltaNeighborsSet.iterator();
		while (remoteBehindDeltaNeighborsIt.hasNext())
		{
			INode behindNode = remoteBehindDeltaNeighborsIt.next();
			InetSocketAddress behindNodeAddress = behindNode.getAddress();
			NodeCache behindNodeCache = getNodeCache(behindNodeAddress);
		
			Set<Sequence> behindToBeyondArrivedSequenceSet = new HashSet<Sequence>();
			Set<Sequence> behindToBeyondDiscardedSequenceSet = new HashSet<Sequence>();
			Iterator<INode> remoteBeyondDeltaNeighborsIt = remoteBeyondDeltaNeighborsSet.iterator();
			while (remoteBeyondDeltaNeighborsIt.hasNext())
			{
				INode beyondNode = remoteBeyondDeltaNeighborsIt.next();
				NodeCache beyondNodeCache = getNodeCache(beyondNode.getAddress());
				int distance = behindNodeCache._distance + beyondNodeCache._distance;
				if(distance <= _neighborhoodRadius) {
					Sequence reportedLastArrived = behindNodeCache.getLastArrivedSequenceFromNode(beyondNode);
					Sequence reportedLastDiscarded = behindNodeCache.getLastDiscardedSequenceFromNode(beyondNode);
					
					if(reportedLastArrived != null)
						behindToBeyondArrivedSequenceSet.add(reportedLastArrived);
					if(reportedLastDiscarded != null)
						behindToBeyondDiscardedSequenceSet.add(reportedLastDiscarded);
				}
			}
			
			Sequence[] arrivedSequences = behindToBeyondArrivedSequenceSet.toArray(new Sequence[0]);
			Sequence[] discardedSequences = behindToBeyondDiscardedSequenceSet.toArray(new Sequence[0]);
			         
			Dack_Bundle arrivedDackBundle = new Dack_Bundle(behindNodeAddress, arrivedSequences);
			Dack_Bundle discardedDackBundle = new Dack_Bundle(behindNodeAddress, discardedSequences);
			
			retArrivedBundlesSet.add(arrivedDackBundle);
			retDiscardBundlesSet.add(discardedDackBundle);
		}
		
		
		Dack_Bundle[][] allBundles = new Dack_Bundle[2][];
		allBundles[0] = retArrivedBundlesSet.toArray(new Dack_Bundle[0]);
		allBundles[1] = retDiscardBundlesSet.toArray(new Dack_Bundle[0]);
		
		return allBundles;
	}
	
	@Override
	public boolean safeToDiscard(InetSocketAddress recipientAddress, Sequence messageLocalGeneratedSequence) {
		if(!messageLocalGeneratedSequence.getAddress().equals(_localNode.getAddress()))
			throw new IllegalArgumentException("This is not a local sequence: " + messageLocalGeneratedSequence);
		
		NodeCache recipientCache = getNodeCache(recipientAddress);
		
		Sequence safeSequence = recipientCache.getSafeSequence();
		if(safeSequence == null)
			return false;
		else if(safeSequence.succeedsOrEquals(messageLocalGeneratedSequence))
			return true;
		else
			return false;
	}

	@Override
	public NodeCache[] getAllCachedNodes() {
		TreeSet<NodeCache> sortedSet = new TreeSet<NodeCache>(_nodesCache.values());
		NodeCache[] nodes = sortedSet.toArray(new NodeCache[0]);
		
		return nodes;
	}
	
	public String getDumpFilename() {
		return _dumpFileName;
	}

	@Override
	public boolean containsRemote(InetSocketAddress remote) {
		synchronized(_nodes) {
			return _nodes.containsKey(remote);
		}
	}

	@Override
	public InetSocketAddress[] getNonClientNeighbors(InetSocketAddress remote) {
		synchronized(_nodes) {
			INode remoteNode = _nodes.get(remote);
			if(remoteNode == null)
				return null;
			int brokerCounter = 0;
			for( int i=0 ; i<remoteNode.getNeighborCount() ; i++) {
				boolean isClient = _nodes.get(remoteNode.getNeighbors()[i].getAddress()).getNodeType().isClient();
				if(!isClient)
					brokerCounter++;
			}
			
			InetSocketAddress[] retAddrs = new InetSocketAddress[brokerCounter];
			for( int i=0 ; i<retAddrs.length ; i++) {
				boolean isClient = _nodes.get(remoteNode.getNeighbors()[i].getAddress()).getNodeType().isClient();
				if(!isClient)
					retAddrs[i] = remoteNode.getNeighbors()[i].getAddress();
			}
			
			return retAddrs;
		}
	}

	@Override
	public NodeTypes getNodeType(InetSocketAddress remote) {
		synchronized(_nodes) {
			INode node = _nodes.get(remote);
			if(node == null)
				throw new IllegalStateException(remote.toString() + " is not present in: " + this);
			
			return node.getNodeType();
		}
	}
	
	@Override
	public Set<InetSocketAddress> getShortSummary() {
		return new HashSet<InetSocketAddress>(_nodes.keySet());
	}

	@Override
	public void applySummary(String lines) {
		StringTokenizer sTokenizer = new StringTokenizer(lines, "\n");
		int size = sTokenizer.countTokens();
		TRecovery_Join[] trjs = new TRecovery_Join[size];
		for(int i=0 ; i<size; i++) {
			trjs[i] = TRecovery_Join.getTRecoveryObject(_localSequencer, (String)sTokenizer.nextElement());
		}
		
		applyAllJoinSummary(trjs);
	}
	

	@Override
	public InetSocketAddress isClientOf(InetSocketAddress clientAddress) {
		BrokerIdentityManager idManager = _brokerShadow.getBrokerIdentityManager();
		return idManager.getJoinpointAddress(clientAddress.toString());
	}

	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}

	@Override
	public int getNeighborhoodSize() {
		return _nodes.size();
	}

	@Override
	public LocalSequencer getLocalSequencer() {
		return _localSequencer;
	}

	@Override
	public int getNeighborhoodRadius() {
		return _neighborhoodRadius;
	}
	
	@Override
	public InetSocketAddress getNewFromForMorphedMessage(InetSocketAddress remote, InetSocketAddress oldFrom) {
		LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_SUB_MANAGER, "getNewFromForMorphedMessage(" + remote + ", " + oldFrom + ")");
		if(remote.equals(_localAddress))
			return oldFrom;
		
		Path<INode> pathFromFrom = getPathFrom(oldFrom);
		Path<INode> pathFromRemote = getPathFrom(remote);
		
		if(pathFromRemote == null)
			throw new IllegalStateException("Cannot morph subscription for remote '" + remote + "' since it is not part of the topology.");

		if(pathFromFrom == null)
			throw new IllegalStateException("Cannot morph subscription for form FROM '" + oldFrom + "' since it is not part of the topology.");

		if(pathFromRemote.intersect(pathFromFrom))
			return null;

		int remoteDistance = pathFromRemote.getLength();
		int fromDistance = pathFromFrom.getLength();

		if(remoteDistance + fromDistance <= _brokerShadow.getNeighborhoodRadius())
			return oldFrom;

		InetSocketAddress [] fromAddresses = pathFromFrom.getReverseAddresses();
		int remainingDistance = _brokerShadow.getNeighborhoodRadius() - (remoteDistance);
		if(remainingDistance == 0)
			return _localAddress;

		return fromAddresses[remainingDistance-1];
	}
}
