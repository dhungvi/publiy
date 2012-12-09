package org.msrg.publiy.broker;

import java.net.InetSocketAddress;


import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.component.IComponent;

public interface IBroker extends IBrokerShadow, IComponent, IBrokerControllable { //extends IPublisher, ISubscriber {

	public BrokerOpState getBrokerOpState();
	
	public void recoveryComplete();
	public void joinComplete();
	public void departComplete();
	
	public void registerBrokerOpStateListener(IBrokerOpStateListener brokerOpStateListener);
	public void deregisterBrokerOpStateListener(IBrokerOpStateListener brokerOpStateListener);
	
	public IConnectionManager getConnectionManager();
	
	public long getAffinityMask();
	public double getCpuAverage();
	public InOutBWEnforcer getBWEnforcer();
	public InetSocketAddress getClientsJoiningBrokerAddress();

	public IBrokerShadow getBrokerShadow();
	
}
