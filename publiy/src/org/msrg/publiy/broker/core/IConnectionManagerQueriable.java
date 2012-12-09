package org.msrg.publiy.broker.core;

import org.msrg.publiy.broker.info.ISessionInfo;
import org.msrg.publiy.broker.info.JoinInfo;
import org.msrg.publiy.broker.info.PSSessionInfo;
import org.msrg.publiy.broker.info.SubscriptionInfo;

public interface IConnectionManagerQueriable {
	
	public JoinInfo[] getTopologyLinks();
	public SubscriptionInfo[] getSubscriptionInfos();
	public ISessionInfo[] getSessionInfos();
	public ISessionInfo[] getActiveSessions();
	public ISessionInfo[] getInactiveSessions();
	public PSSessionInfo[] getPSSessions();	
	
}
