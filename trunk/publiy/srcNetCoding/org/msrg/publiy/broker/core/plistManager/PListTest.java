package org.msrg.publiy.broker.core.plistManager;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.msrg.publiy.utils.SortableNodeAddressSet;

import junit.framework.TestCase;

public class PListTest extends TestCase {
	
	InetSocketAddress _requester;
	protected Set<InetSocketAddress> _localClients =
		new HashSet<InetSocketAddress>();
	protected Map<InetSocketAddress, Integer> _localClientCounter =
		new HashMap<InetSocketAddress, Integer>();
	protected final int _addrCount = 10;
	
	public void setUp() {
		_requester = new InetSocketAddress("127.0.0.1", 1000);
		_localClients.add(_requester);
		for(int i=1 ; i<_addrCount ; i++) {
			InetSocketAddress clientAddr = new InetSocketAddress("127.0.0.1", 1000 + i);
			_localClients.add(clientAddr);
			_localClientCounter.put(clientAddr, 0);
		}
	}
	
//	protected static Set<InetSocketAddress> getLocalClients(
//			int count, InetSocketAddress requester,
//			Set<InetSocketAddress> localClients, boolean update);
	
	public void testGetLocalClients() {
		SortableNodeAddressSet sortableNodesSet = new SortableNodeAddressSet();
		int repeat = 10;
		for(int i=0 ; i<_addrCount * repeat ; i++) {
			Set<InetSocketAddress> clients =
				PList.getLocalClients(sortableNodesSet, 1, _requester, _localClients);
			for(InetSocketAddress client : clients) {
				Integer oldVal = _localClientCounter.get(client);
				_localClientCounter.put(client, oldVal+1);
			}
		}
		
		for(Entry<InetSocketAddress, Integer> entry : _localClientCounter.entrySet()) {
			assertTrue("Address: " + entry.getKey(),
					entry.getValue().intValue() - repeat <= 2);
		}
	}

}
