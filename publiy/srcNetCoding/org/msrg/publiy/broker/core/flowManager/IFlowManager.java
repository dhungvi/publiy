package org.msrg.publiy.broker.core.flowManager;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;

import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.sequence.Sequence;

public interface IFlowManager {
	
	public int getActiveFlowsCount();
	
	public IFlow getNextOutgoingFlow(FlowSelectionPolicy flPolicy, int packetSize);
//	public void outgoingFlowEncodingFinished(IFlow flow);
	
	public IFlow addFlow(FlowPriority flowPriority, InetSocketAddress remote, Content content);
	public IFlow addFlow(
			FlowPriority flowPriority, InetSocketAddress remote, Content content, int piecesToSendBeforeBreak);
	public IFlow getFlow(InetSocketAddress remote, Sequence contentSequence);
	
	public void deactivate(InetSocketAddress remote);
	public void deactivate(InetSocketAddress remote, Sequence contentSequence);
	public void deactivate(Sequence contentSequence);
	
	public int getLocalIncomingBandwidth();
	public int getLocalIncomingRemainingBandwidth();
	public int getLocalIncomingUsedBandwidth();

	public int getLocalOutgoingBandwidth();
	public int getLocalOutgoingRemainingBandwidth();
	public int getLocalOutgoingUsedBandwidth();

	public int getBandwidth(InetSocketAddress remote);
	public int getRemainingBandwidth(InetSocketAddress remote);
	public int getUsedBandwidth(InetSocketAddress remote);
	
	public boolean loadConfiguredBandwidthFromFile(InetSocketAddress localAddress, String filename);
	public boolean loadConfiguredBandwidth(InetSocketAddress localAddress, String line);
	public void loadConfiguredBandwidth(Map<InetSocketAddress, Integer> configuredBandwidths);
	public void loadConfiguredBandwidth(InetSocketAddress remote, Integer configuredBandwidths);

	public boolean canAddNewFlows();

	public int getActiveFlowsCount(Sequence contentSequence);

	public boolean isContentLocallyPublished(Content content);
	public FlowPriority getFlowPriority(Content content, InetSocketAddress remote, int piecesToSendBeforeBreak);
	public FlowPriority getFlowPriority(Content content, InetSocketAddress remote);

	public boolean isContentLocallyPublished(Sequence contentSequence);
	public void addLaunchContent(Sequence sourceSequence, InetSocketAddress remote);
	public void addLaunchContent(Sequence contentSequence,Set<InetSocketAddress> remotes);
	
}
