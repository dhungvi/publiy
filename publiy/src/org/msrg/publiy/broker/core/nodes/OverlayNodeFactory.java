package org.msrg.publiy.broker.core.nodes;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.msrg.publiy.utils.PropertyGrabber;
import org.msrg.publiy.utils.Utility;

public class OverlayNodeFactory {
	
	protected static OverlayNodeFactory _factory = new OverlayNodeFactory();
	
	public static synchronized void createFactory() {
		if(_factory == null)
			_factory = new OverlayNodeFactory();
	}
	
	public static synchronized OverlayNodeFactory getInstance() {
		return _factory;
	}
	
	protected OverlayNodeFactory() {}

	private Set<OverlayNodeId> _allNodeIds = new TreeSet<OverlayNodeId>();
	private Map<String, OverlayNodeId> _allNodesMap = new HashMap<String, OverlayNodeId>();
	private Map<InetSocketAddress, OverlayNodeId> _addressNodesMap = new HashMap<InetSocketAddress, OverlayNodeId>();

	public OverlayNodeId getNodeId(InetSocketAddress remote) {
		return _addressNodesMap.get(remote);
	}
	
	public boolean renameNodeId(String oldIdentifier, String newNodeName){
		if ( identifierExists(newNodeName) != null )
			return false;
		
		OverlayNodeId nodeId = identifierExists(oldIdentifier);
		if ( nodeId == null )
			return false;
		
		_allNodeIds.remove(nodeId);
		nodeId._identifier = newNodeName;
		nodeId._nodeProperties.setProperty(PropertyGrabber.PROPERTY_NODE_NAME, newNodeName);
		_allNodeIds.add(nodeId);

		Iterator<OverlayNodeId> allNodesIt = _allNodeIds.iterator();
		while(allNodesIt.hasNext()){
			OverlayNodeId oneNodeId = allNodesIt.next();
			oneNodeId.changeJoinPointName(oldIdentifier, newNodeName);
		}
		return true;
	}
	
	public OverlayNodeId identifierExists(String identifier){
		OverlayNodeId nodeId = _allNodesMap.get(identifier);
		return nodeId;
	}
	
	public OverlayNodeId getNodeId(String identifier, Properties nodeProperties){
		OverlayNodeId nodeId = getNodeId(identifier);
		String nodeIdStr = nodeProperties.getProperty(PropertyGrabber.PROPERTY_NODE_NAME);
		if ( !nodeIdStr.equalsIgnoreCase(nodeId._identifier) )
			throw new IllegalStateException("Identifiers do not match: " + nodeId._identifier + " " + nodeIdStr);
		
		nodeId._nodeProperties = (nodeProperties==null?new Properties():nodeProperties);
		String nodeAddressStr = nodeId._nodeProperties.getProperty(PropertyGrabber.PROPERTY_NODE_ADDRESS);
		updateNodeAddress(nodeId, Utility.stringToInetSocketAddress(nodeAddressStr));

		return nodeId;
	}
	
	public OverlayNodeId getNodeId(String identifier) {
		identifier = identifier.trim();
		if ( identifier == null || identifier.equalsIgnoreCase("") || identifier.equalsIgnoreCase("null") )
			return null;
		
		OverlayNodeId nodeId = identifierExists(identifier);
		if ( nodeId != null )
			return nodeId;
		
		nodeId = createNewOverlayNodeId(identifier);
		_allNodeIds.add(nodeId);
		_allNodesMap.put(identifier, nodeId);
		return nodeId;
	}
	
	public Set<OverlayNodeId> getAllNodeIds(){
		return _allNodeIds;
	}
	
	public void clearAll(){
		_allNodeIds.clear();
		_allNodesMap.clear();
	}
	
	public void updateNodeAddress(OverlayNodeId nodeId, InetSocketAddress address) {
		nodeId._adderss = address;
		_addressNodesMap.put(address, nodeId);
	}
	
	public void setNodeAddress(OverlayNodeId nodeId, InetSocketAddress address){
		nodeId._nodeProperties.setProperty(PropertyGrabber.PROPERTY_NODE_ADDRESS, address.toString().substring(1));
		updateNodeAddress(nodeId, address);
	}
	
	protected OverlayNodeId createNewOverlayNodeId(String id) {
		return new OverlayNodeId(id);
	}
}
