package org.msrg.publiy.utils;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class SortableNodeAddressSet {
	
	private final Map<InetSocketAddress, SortableNodeAddress> _nodeRecords =
		new HashMap<InetSocketAddress, SortableNodeAddress>();
	
	public SortableNodeAddressSet() { }
	
	public SortableNodeAddress incrementSortableNodeAddress(
			InetSocketAddress remote, double val) {
		synchronized(_nodeRecords) {
			SortableNodeAddress existingRemote = _nodeRecords.get(remote);
			double oCount = 0;
			if(existingRemote != null)
				oCount = existingRemote._count;
			
			SortableNodeAddress newRemote =
				new SortableNodeAddress(remote, oCount + val);
			_nodeRecords.put(remote, newRemote);
			
			return existingRemote;
		}
	}
	
	public SortableNodeAddress getSortableNodeAddress(
			InetSocketAddress remote) {
		synchronized(_nodeRecords) {
			SortableNodeAddress existingRemote = _nodeRecords.get(remote);
			if(existingRemote == null) {
				existingRemote = new SortableNodeAddress(remote, 0);
				_nodeRecords.put(remote, existingRemote);
			}
			
			return existingRemote;
		}
	}

	public void incrementSortableNodeAddress(
			Collection<InetSocketAddress> remotes, double val) {
		if(remotes == null)
			return;
		
		for(InetSocketAddress remote : remotes)
			incrementSortableNodeAddress(remote, val);
	}
	
	public SortedSet<SortableNodeAddress> convertToSortableSet(
			Collection<InetSocketAddress> nodes) {
		SortedSet<SortableNodeAddress> sortableSet =
			new TreeSet<SortableNodeAddress>();
		for(InetSocketAddress node : nodes)
			sortableSet.add(getSortableNodeAddress(node));
		
		if(nodes.size() != sortableSet.size())
			throw new IllegalStateException();
		
		return sortableSet;
	}
}
