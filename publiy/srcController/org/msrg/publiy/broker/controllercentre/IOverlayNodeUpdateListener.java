package org.msrg.publiy.broker.controllercentre;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.info.BrokerInfoTypes;

public interface IOverlayNodeUpdateListener {
	
	void overlayNodeUpdated(InetSocketAddress remote, BrokerInfoTypes updateType);

}
