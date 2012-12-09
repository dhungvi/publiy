package org.msrg.publiy.pubsub.core;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.info.JoinInfo;

public interface IOverlayManagerQueriable {

	public JoinInfo[] getTopologyLinks();
	public boolean containsRemote(InetSocketAddress remote);
	
}
