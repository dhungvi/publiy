package org.msrg.publiy.tests.roundbasedrouting;

import java.net.InetSocketAddress;
import java.util.HashSet;

import org.msrg.publiy.publishSubscribe.Publication;


public class PublicationDeliverySet extends HashSet<Publication> {
	
	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 1L;
	final InetSocketAddress _address;

	PublicationDeliverySet(InetSocketAddress address) {
		_address = address;
	}
	
	public boolean add(Publication publication) {
		return add(publication);
	}
}
