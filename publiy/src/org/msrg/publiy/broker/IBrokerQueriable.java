package org.msrg.publiy.broker;


import org.msrg.publiy.broker.info.ExceptionInfo;
import org.msrg.publiy.broker.info.ISessionInfo;
import org.msrg.publiy.broker.info.JoinInfo;
import org.msrg.publiy.broker.info.PSSessionInfo;
import org.msrg.publiy.broker.info.StatisticalInfo;
import org.msrg.publiy.broker.info.SubscriptionInfo;
import org.msrg.publiy.component.ComponentStatus;

public interface IBrokerQueriable {

	public BrokerOpState getBrokerOpStatus();
	public ComponentStatus getBrokerComponentState();
	public StatisticalInfo[] getBrokerStatisticalInfos();
	public ExceptionInfo[] getExceptionInfos();
	
	public JoinInfo[] getTopologyLinks();
	public SubscriptionInfo[] getSubscriptionInfos();
	public ISessionInfo[] getSessions();
	public PSSessionInfo[] getPSSessions();	
}
