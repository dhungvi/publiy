package org.msrg.publiy.pubsub.core.overlaymanager;

import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;


import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.NodeCache;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Depart;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.utils.log.casuallogger.exectime.ExecutionTimeType;
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeEntity;

public class WorkingOverlayManager extends OverlayManager implements IWorkingOverlayManager{

	private final IOverlayManager _masterOverlayManager;
	private Map<InetSocketAddress, InetSocketAddress> _mapsToMasterNode
	 		= new HashMap<InetSocketAddress, InetSocketAddress>();
	private Map<InetSocketAddress, InetSocketAddress> _mapsToRealNode
			= new HashMap<InetSocketAddress, InetSocketAddress>();
	private final Object _lock = new Object();
	private final NodeCache[] _nodesCacheArray;
	private final int _workingVersion;
	
	public WorkingOverlayManager(int workingVersion, IOverlayManager overlayManager, ISession[] activeSessions) {
		this(workingVersion, overlayManager, ISession.getISessionsAsInetSocketAddresses(activeSessions));
		
		for(int i=0 ; i<activeSessions.length ; i++)
		{
			InetSocketAddress remote = activeSessions[i].getRemoteAddress();
			if(activeSessions[i].isReal())
				_mapsToRealNode.put(remote, remote);
			else
			{
				Path<INode> pathFromRemote = getPathFrom(remote);
				InetSocketAddress[] pathFromRemoteAddresses = pathFromRemote.getAddresses();
				for(int j=1 ; j<pathFromRemoteAddresses.length ; j++)
				{
					ISession session = getSessionForRemote(activeSessions, pathFromRemoteAddresses[j]);
					if(session.isReal()){
						InetSocketAddress realRemote = session.getRemoteAddress();
						_mapsToRealNode.put(remote, realRemote);
						break;
					}
				}
			}
		}
		
		initializeRealIndeces();
		
		checkInitialize();
	}
	
	protected void checkInitialize() {
		for(int index =0 ; index <_nodesCacheArray.length ; index++) {
			NodeCache nodeCache = _nodesCacheArray[index];
			if(nodeCache == null)
				throw new NullPointerException();
			if(nodeCache.getFartherNeighbors()==null)
				throw new NullPointerException();
			if(nodeCache.getMaskedClosers()==null)
				throw new NullPointerException();
			if(nodeCache.getClosers()==null)
				throw new NullPointerException();
			if(nodeCache.getIndex()!=index)
				throw new IllegalStateException(nodeCache.getIndex() + " vs. " + index);
			if(nodeCache.getRealIndex()<0)
				throw new IllegalStateException(nodeCache.getRealIndex() + "");
		}
	}
	
	private ISession getSessionForRemote(ISession[] sessions, InetSocketAddress remote){
		for(int i=0 ; i<sessions.length ; i++)
			if(remote.equals(sessions[i].getRemoteAddress()))
				return sessions[i];
		
		throw new IllegalStateException("remote not found: " + remote + " in: " + Writers.write(sessions));
	}
	
	public void dumpOverlay(Writer ioWriter) throws IOException {
		super.dumpOverlay(ioWriter);
		
		ioWriter.write("Mappings(" + _workingVersion + "): ");
		for(Map.Entry<InetSocketAddress, InetSocketAddress> mapstoEntry : _mapsToMasterNode.entrySet())
			ioWriter.write(mapstoEntry.getKey() + "->" + mapstoEntry.getValue() + ",");
		ioWriter.write("\n");
		
		ioWriter.write("RealMappings(" + _workingVersion + "): ");
		for(Map.Entry<InetSocketAddress, InetSocketAddress> mapstoRealEntry : _mapsToRealNode.entrySet())
			ioWriter.write(mapstoRealEntry.getKey() + "->" + mapstoRealEntry.getValue() + ",");
		ioWriter.write("\n");
		
		ioWriter.write("#************** " + DATE_FORMAT.format(new Date()));
		ioWriter.write("\n");
	}
	
	public WorkingOverlayManager(int workingVersion, IOverlayManager overlayManager, Set<InetSocketAddress> activeRemoteAddressesSet) {
		super(overlayManager.getBrokerShadow());
		_masterOverlayManager = overlayManager;
		_workingVersion = workingVersion;
		activeRemoteAddressesSet.add(getLocalAddress());
		_mapsToMasterNode = new HashMap<InetSocketAddress, InetSocketAddress>();
		_mapsToRealNode = new HashMap<InetSocketAddress, InetSocketAddress>();
		
		List<InetSocketAddress> neighborsOrdered = overlayManager.getFartherNeighborsOrderedList(getLocalAddress());
		Iterator<InetSocketAddress> neighborsOrderedIt = neighborsOrdered.iterator();
		while ( neighborsOrderedIt.hasNext()){
			InetSocketAddress neighbor = neighborsOrderedIt.next();
			InetSocketAddress farthestActiveCloserNeighbor = getFarthestNodeInActiveSet(neighbor, overlayManager, activeRemoteAddressesSet);
			if(farthestActiveCloserNeighbor == null)
				farthestActiveCloserNeighbor = getLocalAddress();
			
			if(!activeRemoteAddressesSet.contains(neighbor) || neighbor.equals(getLocalAddress()))
			{
				_mapsToMasterNode.put(neighbor, farthestActiveCloserNeighbor);
				continue;
			}
			else
			{
				NodeTypes neighborType = overlayManager.getNodeType(neighbor);
				NodeTypes farthestActiveCloserNeighborType = overlayManager.getNodeType(farthestActiveCloserNeighbor);
				boolean added = super.addNode(neighbor, neighborType, farthestActiveCloserNeighbor, farthestActiveCloserNeighborType, false);
				_mapsToMasterNode.put(neighbor, neighbor);
				if(!added)
					throw new IllegalStateException("neighbor: " + neighbor + ", " + farthestActiveCloserNeighbor);				
			}
		}

		_nodesCacheArray = initializeOverlayManager();
		dumpOverlay();
	}
	
	protected void initializeRealIndeces() {
		for(NodeCache nodeCache : _nodesCacheArray) {
			int realIndex = -2;
			InetSocketAddress remote = nodeCache.getAddress();
			InetSocketAddress realRemote = _mapsToRealNode.get(remote);
			if(realRemote != null) {
				NodeCache realNodeCache = getNodeCacheNoCreate(realRemote);
				realIndex = realNodeCache.getIndex();
			}
			nodeCache.setRealIndex(realIndex);
		}
		_nodesCacheArray[0].setRealIndex(0);
	}
	
	@Override
	public void handleMessage(TMulticast_Join tmj){
		throw new UnsupportedOperationException(tmj.toStringTooLong());
	}
	
	@Override
	public void handleMessage(TMulticast_Depart tmd){
		throw new UnsupportedOperationException(tmd.toStringTooLong());
	}
	
	private InetSocketAddress getFarthestNodeInActiveSet(InetSocketAddress neighbor, IOverlayManager overlayManager, Set<InetSocketAddress> activeRemoteAddressesSet){
		Path<INode> masterNeighborPath = overlayManager.getPathFrom(neighbor);
		InetSocketAddress farthestActiveCloserNeighbor = null;
		int pLen = masterNeighborPath.getLength();
		for(int i=1 ; i<pLen ; i++){
			InetSocketAddress tmp = masterNeighborPath.get(i).getAddress();
			if(!activeRemoteAddressesSet.contains(tmp))
				continue;
			farthestActiveCloserNeighbor = tmp;
			break;
		}
		return farthestActiveCloserNeighbor;
	}
	
	public String toString(){
		String str1 = "W-" + super.toString();
		String str2 = Writers.write(_mapsToMasterNode);
		return str1 + "\n\t" + str2;
	}

	@Override
	public InetSocketAddress mapsToWokringNodeAddress(InetSocketAddress masterNodeAddress) {
		synchronized(_lock){
			return _mapsToMasterNode.get(masterNodeAddress);
		}
	}
	
	@Override
	public InetSocketAddress mapsToRealWokringNodeAddress(InetSocketAddress masterNodeAddress) {
		synchronized(_lock){
			return _mapsToRealNode.get(masterNodeAddress);
		}
	}
	
	// Returns null, if matching set had changed (i.e., a matching remote is not found in nodesCache.
	@Override
	public WorkingRemoteSet computeSoftLinksAddresses(InOutBWEnforcer bwEnforcer, PubForwardingStrategy forwardingStrategy,
					WorkingRemoteSet workingSet){
		if(!workingSet.isValid(this))
			return null;
		
		if(!workingSet.hasMatchingSet() || workingSet.hasRemoteSet())
			return null;
		
		IExecutionTimeEntity genericExecutionTimeEntity =
				_brokerShadow.getExecutionTypeEntity(ExecutionTimeType.EXEC_TIME_PATH_COMPUTATION);
		if(genericExecutionTimeEntity != null)
			genericExecutionTimeEntity.executionStarted();

		Set<InetSocketAddress> matchingSet = workingSet.getMatchingRemotes();
		synchronized(_lock)
		{
			BitSet matchingBV = null;
			try{
				matchingBV = convertToBVSet(matchingSet);
			}catch(Exception x) {
				System.err.println("ERROR: " + matchingSet);
				System.err.println("ERROR: " + getNodeCaches());
				throw new IllegalStateException(x);
			}
			
			BitSet realBV;
			switch(forwardingStrategy){
			case PUB_FORWARDING_STRATEGY_0:
				if(genericExecutionTimeEntity != null)
					genericExecutionTimeEntity.discard();
				throw new UnsupportedOperationException(forwardingStrategy + " not supported.");
				
			case PUB_FORWARDING_STRATEGY_1:
				realBV = getSoftLinks_Strategy1(matchingBV);
				break;
				
			case PUB_FORWARDING_STRATEGY_2:
				realBV = getSoftLinks_Strategy2(matchingBV);
				break;
				
			case PUB_FORWARDING_STRATEGY_3:
				realBV = getSoftLinks_Strategy3(matchingBV);
				break;

			case PUB_FORWARDING_STRATEGY_4:
				realBV = getSoftLinks_Strategy4(bwEnforcer, matchingBV);
				break;
				
			default:
				if(genericExecutionTimeEntity != null)
					genericExecutionTimeEntity.discard();
				throw new UnsupportedOperationException(forwardingStrategy + " is not supported.. " + this);
			}
			matchingBV = null;
			
			Set<InetSocketAddress> realSet = convertToSet(realBV, false);
			workingSet.setRealRemotes(realSet);
			
			if(genericExecutionTimeEntity != null)
				genericExecutionTimeEntity.executionEnded(true, false);
			return workingSet;
		}
	}
	
	protected BitSet convertToBVSet(Set<InetSocketAddress> set) {
		BitSet bv = getEmptyBitSet();
		for(InetSocketAddress remote : set){
			NodeCache nc = getNodeCacheNoCreate(remote);
			if(nc != null)
				bv.set(nc.getIndex());
		}

		return bv;
	}
	
	protected Set<InetSocketAddress> convertToSet(BitSet bvSet, boolean includeZero) {
		Set<InetSocketAddress> set = new HashSet<InetSocketAddress>();
		int minIndex = includeZero?0:1;
		for(int i = bvSet.nextSetBit(minIndex); i >= minIndex; i = bvSet.nextSetBit(i+1))
			set.add(_nodesCacheArray[i].getAddress());
		
		return set;
	}
	
	private BitSet getSoftLinks_Strategy1(BitSet matching){
		BitSet ret = getEmptyBitSet();
		for(int i=0 ; i<_nodesCacheArray.length ; i++){
			if(matching.get(i))
				ret.set(i);
		}
		
		return ret;
	}
	
	protected BitSet getEmptyBitSet() {
		return new BitSet(_nodesCacheArray.length);
	}

	private BitSet getSoftLinks_Strategy2(BitSet matching){
		BitSet closers = null;
		
		for(int i = matching.nextSetBit(1); i > 0; i = matching.nextSetBit(i+1)) 
		{
			if(closers == null) {
				BitSet maskedBV = _nodesCacheArray[i].getMaskedClosers();
				closers = (BitSet)maskedBV.clone();
			} else {
				closers.and(_nodesCacheArray[i].getMaskedClosers());
			}
		}

		BitSet ret = getEmptyBitSet();
		if(closers == null){
			return ret;
		}
		
		for(int i=closers.length()-1 ; 
				i>=0 && !matching.isEmpty() ; i--)
		{
			BitSet farthers = _nodesCacheArray[i].getFarthers();
			if(closers.get(i) && matching.intersects(farthers))
			{
				matching.andNot(farthers);
				ret.set(i);
			}
		}
		
		return mapToReal(ret, true);
	}
	
	private BitSet mapToReal(BitSet bv, boolean clearFarther) {
		BitSet ret = getEmptyBitSet();
		BitSet clearingFartherBV = (clearFarther?getEmptyBitSet():null);
		for(int i = bv.nextSetBit(0); i >= 0; i = bv.nextSetBit(i+1)) {
			int realIndex = _nodesCacheArray[i].getRealIndex();
			ret.set(realIndex);
			NodeCache realNodeCache = _nodesCacheArray[realIndex];
			if(clearFarther) {
				BitSet farther = realNodeCache.getFarthers();
				if(clearingFartherBV.get(realIndex)) {
					clearingFartherBV.or(farther);
				} else {
					clearingFartherBV.or(farther);
					clearingFartherBV.clear(realIndex);
				}
			}
		}
		
		if(clearFarther)
			ret.andNot(clearingFartherBV);
		return ret;
	}
	
	private BitSet getSoftLinks_Strategy3(BitSet matching) {
		BitSet ret = getEmptyBitSet();
		for(int i = matching.nextSetBit(1); i > 0; i = matching.nextSetBit(i+1))
		{
			if(matching.get(i)){
				BitSet farthers = (BitSet) _nodesCacheArray[i].getFarthers();
				matching.andNot(farthers);
				ret.set(i);
			}
		}
		
		return mapToReal(ret, true);
	}

	private BitSet getSoftLinks_Strategy4(InOutBWEnforcer bwEnforcer, BitSet matching){
		double usedThreshold = _brokerShadow.getUsedBWThreshold();
		if(bwEnforcer.pastUsedThreshold(usedThreshold)) {
//			if(_RAND.nextDouble() <= usedThreshold)
			{
				return getSoftLinks_Strategy2(matching);
			}
		}
		return getSoftLinks_Strategy3(matching);
	}

	@Override
	public int getWorkingVersion() {
		return _workingVersion;
	}

	@Override
	public IOverlayManager getMasterOverlayManager() {
		return _masterOverlayManager;
	}

	@Override
	public boolean isReal(InetSocketAddress remote) {
		return _mapsToRealNode.get(remote).equals(remote);
	}
	
	@Override
	protected String getDumpFilename(IBrokerShadow brokerShadow) {
		return brokerShadow==null ? null : brokerShadow.getRecoveryFileName() + "w";
	}
	
	@Override
	public InetSocketAddress isClientOf(InetSocketAddress clientAddress) {
		return _masterOverlayManager.isClientOf(clientAddress);
	}
}
