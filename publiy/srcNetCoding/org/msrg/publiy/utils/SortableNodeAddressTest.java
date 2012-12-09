package org.msrg.publiy.utils;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.msrg.publiy.broker.core.sequence.Sequence;

import junit.framework.TestCase;

public class SortableNodeAddressTest extends TestCase {
	
	SortableNodeAddressSet _sortableNodeSet;
	
	@Override
	public void setUp() {
		_sortableNodeSet = new SortableNodeAddressSet();
	}
	
	@Override
	public void tearDown() {
		_sortableNodeSet = null;
	}
	
	public void testEquality() {
		InetSocketAddress address = Sequence.getRandomAddress();
		SortableNodeAddress sortedAddress =
			_sortableNodeSet.getSortableNodeAddress(address);
		assertTrue(address.equals(sortedAddress));
		assertTrue(sortedAddress.equals(address));
	}
	
	public void testSorting() {
		InetSocketAddress remote1 = new InetSocketAddress(1001);
		InetSocketAddress remote2 = new InetSocketAddress(1002);
		
		_sortableNodeSet.incrementSortableNodeAddress(remote1, 1);
		
		Set<InetSocketAddress> remotes = new HashSet<InetSocketAddress>();
		remotes.add(remote1); remotes.add(remote2);

		for(int i=0; i<1000; i++) {
			SortedSet<SortableNodeAddress> sortedRemotes =
				_sortableNodeSet.convertToSortableSet(remotes);
			assertTrue(remote2.equals(sortedRemotes.first()));
		}
		
		_sortableNodeSet.incrementSortableNodeAddress(remote2, 2);
		for(int i=0; i<1000; i++) {
			SortedSet<SortableNodeAddress> sortedRemotes =
				_sortableNodeSet.convertToSortableSet(remotes);
			assertTrue(remote1.equals(sortedRemotes.first()));
		}
	}
}
