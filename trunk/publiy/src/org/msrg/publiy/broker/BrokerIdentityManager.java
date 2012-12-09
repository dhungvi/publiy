package org.msrg.publiy.broker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.broker.core.nodes.OverlayNodeFactory;
import org.msrg.publiy.broker.core.nodes.OverlayNodeId;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class BrokerIdentityManager {

	private OverlayNodeId _localNodeId;
	private final InetSocketAddress _localAddress;
	private final Map<String, OverlayNodeId> _idMap =
		new HashMap<String, OverlayNodeId>();
	private final Map<OverlayNodeId, OverlayNodeId> _idJoinpointMap =
		new HashMap<OverlayNodeId, OverlayNodeId>();
	private final Map<String, InetSocketAddress> _addrMap =
		new HashMap<String, InetSocketAddress>();
	private final int _delta;
	
	public BrokerIdentityManager(InetSocketAddress localAddress, int delta) {
		_localAddress = localAddress;
		_delta = delta;
	}

	public int getDelta() {
		return _delta;
	}
	
	public boolean loadIdFile(String idFile) throws IOException {
		boolean result = true;
		FileReader fReader = new FileReader(idFile);
		BufferedReader reader = new BufferedReader(fReader);
		String line = null;
		while((line = reader.readLine())!=null && result) {
			result &= loadId(line);
		}
		
		reader.close();
		return result;
	}
	
	public boolean loadIdContent(String[] lines) {
		for(int i=0 ; i<lines.length ; i++)
			if(!loadId(lines[i]))
				return false;
		
		return true;
	}
	
	public boolean loadId(String nodeIdStr, InetSocketAddress remoteAddr, String joinPointName) {
		return loadId(nodeIdStr, remoteAddr.toString(), joinPointName);
	}

	public boolean loadId(String idName, String addrStr, String idJoinpointName) {
		boolean ret = true;
		if(addrStr.charAt(0) == '/')
			addrStr = addrStr.substring(1);
		
		InetSocketAddress addr = Sequence.readAddressFromString(addrStr);
		if(addr == null)
			return false;

		OverlayNodeFactory nodeFactory = OverlayNodeFactory.getInstance();
		OverlayNodeId id = nodeFactory.getNodeId(idName);
		if(id == null)
			return false;
		nodeFactory.setNodeAddress(id, addr);
		
		OverlayNodeId idJoinpoint =
			idJoinpointName==null?null:nodeFactory.getNodeId(idJoinpointName);
		
		synchronized (_idMap) {
			_idMap.put(addrStr, id);
			_idMap.put(addrStr, id);
			_addrMap.put(idName, addr);
			_idMap.put(addrStr, id);
			_idMap.put(idName, id);
			
			if(idJoinpoint != null)
				_idJoinpointMap.put(id, idJoinpoint);
		}
		
		if(addr.equals(_localAddress))
			setLocalNodeId(id);
		
		return ret;
	}
	
	public boolean loadId(String line) {
		String[] lineParts = line.split("\\s+");
		if(lineParts.length != 2 && lineParts.length != 3)
			return false;
		
		String idName = lineParts[0];
		String addrStr = lineParts[1];
		String idJoinpointName = lineParts.length==3?lineParts[2]:null;
		return loadId(idName, addrStr, idJoinpointName);
	}
	
	private void setLocalNodeId(OverlayNodeId id) {
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_BROKER, "@" + BrokerInternalTimer.read() + ": Setting LocalOverlayNodeId: " + id);
		_localNodeId = id;
	}
	
	public boolean isLocal(OverlayNodeId nodeId) {
		return (nodeId == _localNodeId);
	}
	
	public InetSocketAddress getJoinpointAddress(String idStr) {
		synchronized (_idMap) {
			OverlayNodeId id = _idMap.get(idStr);
			if(id == null)
				return null;
			
			OverlayNodeId joinpointId = _idJoinpointMap.get(id);
			if(joinpointId == null)
				return null;
			
			return joinpointId.getNodeAddress();
		}
	}

	public OverlayNodeId getBrokerId(InetSocketAddress address) {
		return  address == null ? null : getBrokerId(address.toString());
	}
	
	public OverlayNodeId getBrokerId(String idStr) {
		synchronized (_idMap) {
			OverlayNodeId id = _idMap.get(idStr);
			if(id == null && idStr.length() > 1 && idStr.charAt(0) == '/')
				id = _idMap.get(idStr.substring(1));
			
			return id;
		}
	}
	
	@Override
	public String toString() {
		String str = "[";
		boolean first = true;
		synchronized (_idMap)
		{
			for(Entry<String, OverlayNodeId> entry : _idMap.entrySet()) {
				str += ((first?"":",") + entry.getKey() + "->" + entry.getValue());
				first = false;
			}
		}
		
		return str + "]";
	}

	public InetSocketAddress getJoinpointAddress(InetSocketAddress from) {
		return getJoinpointAddress(from.toString());
	}
	
	public InetSocketAddress getNodeAddress(String idname) {
		return _addrMap.get(idname);
	}

	public boolean match(String id, InetSocketAddress address) {
		if(id == null || address == null)
			return true;
		
		InetSocketAddress registeredAddr = _addrMap.get(id);
		if(registeredAddr == null)
			return false;
		
		return registeredAddr.equals(address);
	}
}
