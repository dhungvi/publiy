package org.msrg.publiy.broker.core;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_UnSubscribe;

public interface IConnectionManagerPS extends IConnectionManager {

//	public void publish(TMulticast_Publish tmp);
//	public void subscribe(TMulticast_Subscribe tms);
	public void unsubscribe(TMulticast_UnSubscribe tmus);
	
}
