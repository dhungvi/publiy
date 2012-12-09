package org.msrg.publiy.broker.core.flowManager;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.sequence.Sequence;

public interface IAllFlows {

	public int getActiveFlowsCount();
//	public IFlow addFlow(FlowPriority flowPriority, Content content);
	public IFlow addFlow(FlowPriority flowPriority, Content content, int piecesToSendBeforeBreak);
	public IFlow getFlow(Content content);
	public IFlow getFlow(Sequence contentSequence);
	
	public IFlow pickNextFlowRoundrobin(FlowPriority flowPriority, int packetSize);
	
	public InetSocketAddress getRemote();
	public InetSocketAddress getLocalAddress();
	
	public FlowState getFlowState();
	public boolean isActive();
	public void deactivate();
	public void deactivate(Sequence contentSequence);
	
	public void setBandwidth(Integer configuredBandwidth);
	public int getBandwidth();
	public int getRemainingBandwidth();
	public int getUsedBandwidth();
	
	public boolean shouldSend(int packetSize);

	public Collection<IFlow> getFlows();
	public boolean hasActiveFlows(Sequence contentSequence);
	public void changeFlowPriorityPrivately(IFlow flow, FlowPriority flowPriority);
	public IBrokerShadow getBrokerShadow();
}
