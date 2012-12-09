package org.msrg.publiy.utils;

import java.net.InetSocketAddress;

public class SortableNodeAddress extends InetSocketAddress implements Comparable<SortableNodeAddress> {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -8796085147604334847L;

	protected final double _count;
	
	protected SortableNodeAddress(InetSocketAddress remote, double count){
		super(remote.getAddress(), remote.getPort());
		
		_count = count;
	}
	
	@Override
	public int compareTo(SortableNodeAddress o) {
		if(super.equals(o))
			return 0;
		
		if((_count) > (o._count))
			return 1;
		
		else if((_count) < (o._count))
			return -1;
		
		int hash = hashCode();
		int oHash = o.hashCode();
		if(hash < oHash)
			return 1;
		else
			return -1;
	}
}