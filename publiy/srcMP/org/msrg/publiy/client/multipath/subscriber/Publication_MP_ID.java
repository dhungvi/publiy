package org.msrg.publiy.client.multipath.subscriber;

import java.net.InetSocketAddress;
import java.util.List;


import org.msrg.publiy.client.publisher.SimpleFilePublisher;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.utils.Writers;

public class Publication_MP_ID {

	private final InetSocketAddress _sourceAddress;
	private final int _pubid;
	
	Publication_MP_ID(TMulticast_Publish_MP tmp){
		_sourceAddress = tmp.getSourceAddress();
		Publication publication = tmp.getPublication();
		List<Integer> idList =
			publication.getValue(SimpleFilePublisher.getGeneralCounterAttributeName());
		if(idList == null || idList.isEmpty())
			_pubid = -1;
		else
			_pubid = idList.get(0);
	}
	
	@Override
	public String toString(){
		return "PUB_ID[" + Writers.write(_sourceAddress) + "_" + _pubid + "]";
	}
	
	@Override
	public int hashCode(){
		return _sourceAddress.hashCode() + _pubid;
	}
	
	@Override
	public boolean equals(Object o){
		if ( Publication_MP_ID.class.isInstance(o) )
		{
			if ( ((Publication_MP_ID)o)._sourceAddress.equals(_sourceAddress) &&
					((Publication_MP_ID)o)._pubid==_pubid
				)
				return true;
			else 
				return false;
		}
		else
			throw new UnsupportedOperationException(o + " is not supported. ");
	}
	
}
