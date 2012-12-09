package org.msrg.publiy.broker.info;

import java.net.InetSocketAddress;

import org.msrg.publiy.publishSubscribe.Publication;


public class PublicationInfo extends IBrokerInfo {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 620853133724719243L;
	private final Publication _publication;
	private final InetSocketAddress _from;
	
	public PublicationInfo(Publication publication, InetSocketAddress from){
		super(BrokerInfoTypes.BROKER_INFO_PUBLICATIONS);
		_publication = publication;
		_from = from;
	}
	
	public InetSocketAddress getFrom(){
		return _from;
	}
	
	public Publication getPublication(){
		return _publication;
	}

	@Override
	protected String toStringPrivately() {
		return "F: " + _from + "::" + _publication;
	}
	
}
