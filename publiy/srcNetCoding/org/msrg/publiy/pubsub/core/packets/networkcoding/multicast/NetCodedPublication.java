package org.msrg.publiy.pubsub.core.packets.networkcoding.multicast;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.raccoon.matrix.bulk.BulkMatrix1D;

public class NetCodedPublication extends BulkMatrix1D {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = -7963898943998396118L;
	private final Publication _publication;
	
	public NetCodedPublication(Publication publication, int cols) {
		super(cols);
		
		_publication = publication;
	}

	public Publication getPublication() {
		return _publication;
	}

}
