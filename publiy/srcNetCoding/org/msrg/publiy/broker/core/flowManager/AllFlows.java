package org.msrg.publiy.broker.core.flowManager;

import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.pubsub.multipath.loadweights.ProfilerBucket;
import org.msrg.publiy.utils.ElementTotal;
import org.msrg.publiy.utils.LimittedSizeList;
import org.msrg.publiy.utils.SystemTime;

public class AllFlows implements IAllFlows {
	
	private static final int _profilerCacheSize = 20;
	private static final int _bucketRefreshRate = 50;
	
	protected int _bandwidth;
	protected int _normalizedBandwidth;
	
	protected FlowState _flowState;
	protected final InetSocketAddress _localAddress;
	protected final InetSocketAddress _remote;
	protected final LimittedSizeList<ProfilerBucket> _bucketList;
//	protected final List<IFlow> _flowsList = new LinkedList<IFlow>();
	protected final Map<FlowPriority, List<IFlow>> _flowsPriorityLists;
	protected final Map<Sequence, IFlow> _flowsMap = new HashMap<Sequence, IFlow>();
	protected final IBrokerShadow _brokerShadow;
	
	AllFlows(IBrokerShadow brokerShadow, InetSocketAddress remote, int bandwidth) {
		_brokerShadow = brokerShadow;
		_localAddress = _brokerShadow.getLocalAddress();
		_remote = remote;
		_bucketList = new LimittedSizeList<ProfilerBucket>(new ProfilerBucket[_profilerCacheSize]);
		_flowsPriorityLists = new HashMap<FlowPriority, List<IFlow>>();
		for(FlowPriority flowPriority : FlowPriority.values())
			_flowsPriorityLists.put(flowPriority, new LinkedList<IFlow>());
		
		setBandwidth(bandwidth);
		setFlowState(FlowState.ACTIVE);
	}
	
	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}
	
	protected void setFlowState(FlowState flowState) {
		_flowState = flowState;
	}
	
	@Override
	public int getBandwidth() {
		return _bandwidth;
	}

	@Override
	public InetSocketAddress getRemote() {
		return _remote;
	}

	protected final ProfilerBucket createNewBucket() {
		return new ProfilerBucket(1);
	}
	
	@Override
	public boolean shouldSend(int packetSize) {
		ProfilerBucket pBucket = getValidBucket();
		
		double usedBandwidth = getTotalTraffic();
		if(usedBandwidth + packetSize > _normalizedBandwidth)
			return false;
		
		pBucket.addToBucket(0, packetSize);
		return true;
	}
	
	protected double getTotalTraffic() {
		ElementTotal<LimittedSizeList<ProfilerBucket>> total = _bucketList.getElementsTotal(0);
		if ( total == null )
			return -1;
		
		return total._total;
	}
	
	protected ProfilerBucket getValidBucket() {
		ProfilerBucket pBucket = _bucketList.getLastElement();
		if(pBucket == null) {
			pBucket = createNewBucket();
			_bucketList.append(pBucket);
		} else {
			int refreshedBucketsCount =
				(int)(SystemTime.currentTimeMillis()/_bucketRefreshRate)
				-
				(int)(pBucket.getCreationTime()/_bucketRefreshRate);
			
			for (int i=0 ; i<refreshedBucketsCount && i<_bucketList.getSize() ; i++) {
				pBucket.invalidate();
				pBucket = createNewBucket();
				_bucketList.append(pBucket);
			}
		}
		
		return pBucket;
	}

	@Override
	public int getRemainingBandwidth() {
		return _bandwidth - getUsedBandwidth();
	}

	@Override
	public int getUsedBandwidth() {
		getValidBucket();
		
		ElementTotal<LimittedSizeList<ProfilerBucket>> elementTotal =
			_bucketList.getElementsTotal(0);
		
		if(elementTotal == null)
			return -1;
		else
			return (int) elementTotal._total;
	}

	@Override
	public void setBandwidth(Integer bandwidth) {
		_bandwidth = bandwidth;
		_normalizedBandwidth =
			_bandwidth * ((_profilerCacheSize * _bucketRefreshRate)/1000);
	}

	@Override
	public Collection<IFlow> getFlows() {
		return _flowsMap.values();
	}

	@Override
	public FlowState getFlowState() {
		return _flowState;
	}

	@Override
	public boolean isActive() {
		return _flowState == FlowState.ACTIVE;
	}
	

	@Override
	public void deactivate(Sequence contentSequence) {
		IFlow flow = _flowsMap.remove(contentSequence);
		if(flow == null)
			return;
		
		flow.deactivate();
	}
	
	@Override
	public void deactivate() {
		_flowState = FlowState.INACTIVE;
		for(IFlow flow : _flowsMap.values())
			flow.deactivate();
		
		_flowsPriorityLists.clear();
		_flowsMap.clear();
	}

	@Override
	public IFlow addFlow(
			FlowPriority flowPriority, Content content, int piecesToSendBeforeBreak) {
		if(!isActive())
			throw new IllegalStateException();
		
		if(flowPriority == null)
			throw new IllegalArgumentException();
		
		Sequence contentSequence = content.getSourceSequence();
		Flow flow = (Flow) _flowsMap.get(contentSequence);
		if(flow == null) {
			if(piecesToSendBeforeBreak == 0)
				return null;
			
			flow = new Flow(content, this);
			_flowsMap.put(contentSequence, flow);
		} else if (!flow.isActive()) {
			_flowsPriorityLists.get(flow.getFlowPriority()).remove(flow);
			
			flow = new Flow(content, this);
			_flowsMap.put(contentSequence, flow);
		}
			
		changeFlowPriorityPrivately(flow, flowPriority);
		flow.setPiecesToSendBeforeBreak(piecesToSendBeforeBreak);
		if(piecesToSendBeforeBreak <= 0)
			flow.deactivate();
		
		return flow;
	}

//	@Override
//	public IFlow addFlow(FlowPriority flowPriority, Content content) {
//		return addFlow(flowPriority, content, -1);
//	}
	
	protected final void changeFlowPriority(Flow flow, FlowPriority flowPriority) {
		changeFlowPriorityPrivately(flow, flowPriority);
	}
	
	@Override
	public final void changeFlowPriorityPrivately(IFlow flow, FlowPriority flowPriority) {
		FlowPriority oldFlowPriority = flow.getFlowPriority();
		if(oldFlowPriority != null)
			if(!_flowsPriorityLists.get(oldFlowPriority).remove(flow))
				{} //throw new IllegalStateException();

		((Flow)flow).setFlowPriority(flowPriority);
		List<IFlow> priorityFlows = _flowsPriorityLists.get(flowPriority);
		if(priorityFlows == null)
			throw new NullPointerException("" + flowPriority + "\n" + _flowsPriorityLists);
		
		priorityFlows.add(flow);
	}

	@Override
	public IFlow getFlow(Sequence contentSequence) {
		return _flowsMap.get(contentSequence);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		AllFlows allFlowsObj = (AllFlows) obj;
		return allFlowsObj._remote.equals(_remote);
	}
	
	@Override
	public String toString() {
		return toString(new StringWriter()).toString();
	}
	
	public StringWriter toString(StringWriter writer) {
		writer.append("AllFlows[" + _flowState + "_" + _remote.getPort() + "]{");
		boolean first = true;
		for(List<IFlow> priorityFlows : _flowsPriorityLists.values()) {
			for(IFlow flow : priorityFlows) {
				if(first)
					writer.append(flow.toString());
				else
					writer.append("," + flow);
				first = false;
			}
			writer.append("}");
		}
		
		return writer;
	}

	@Override
	public IFlow pickNextFlowRoundrobin(FlowPriority flowPriority, int packetSize) {
		if(!isActive())
			return null;
		
		List<IFlow> priorityFlowsList = _flowsPriorityLists.get(flowPriority);
		
		if(getSizeAllPrioritiesPrivately() == 0) {
			deactivate();
			return null;
		}
		
		if (priorityFlowsList.size() == 0)
			return null;
		
		if(!shouldSend(packetSize))
			return null;
		
		for(int i=0 ; i<priorityFlowsList.size() ; i++) {
			IFlow flow = priorityFlowsList.remove(0);
			if(flow.isActive()) {
				priorityFlowsList.add(flow);
				if(flow.canEncodeAndSend(true))
					return flow;
			}
		}
		
		return null;
	}
	
	protected int getSizeAllPrioritiesPrivately() {
		int size = 0;
		for(List<IFlow> flowPriorities : _flowsPriorityLists.values())
			size += flowPriorities.size();
		
		return size;
	}

	@Override
	public IFlow getFlow(Content content) {
		Sequence contentSequence = content.getSourceSequence();
		return getFlow(contentSequence);
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return _localAddress;
	}

	@Override
	public int getActiveFlowsCount() {
		if(_flowState != FlowState.ACTIVE)
			return 0;
		
		int activeFlowsCount = 0;
		for(List<IFlow> flowPriorities : _flowsPriorityLists.values())
			for(IFlow flow : flowPriorities)
				if(flow.isActive())
					if(flow.canSendLimitless())
							activeFlowsCount++;
		
		return activeFlowsCount;
	}

	@Override
	public boolean hasActiveFlows(Sequence contentSequence) {
		return _flowsMap.containsKey(contentSequence);
	}
}
