package org.msrg.publiy.broker.core.connectionManager;

import java.net.InetSocketAddress;

public class UnSortableNodeAddress extends InetSocketAddress {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -4031579402914820758L;
	protected int _count;

	public UnSortableNodeAddress(UnSortableNodeAddress remote){
		super(remote.getAddress(), remote.getPort());
		
		_count = remote._count + 1;
	}
	
	public UnSortableNodeAddress(InetSocketAddress remote) {
		super(remote.getAddress(), remote.getPort());
		
		_count = 0;
	}
	
	protected UnSortableNodeAddress(String hostname, int port) {
		super(hostname, port);
		
		_count = 0;
	}
	
	public boolean addToCountAndCompare(int count) {
		return ++_count < count;
	}
}
