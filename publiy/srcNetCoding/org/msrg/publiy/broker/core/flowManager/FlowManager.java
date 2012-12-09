package org.msrg.publiy.broker.core.flowManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class FlowManager implements IFlowManager {
	
	protected static final int MIN_CRITICAL_LEFTOVER_BLOCKS = 2;
	static final int DEFAULT_PIECES_TO_SEND_FOR_LAUNCH_CONTENT = Broker.ROWS * 2;
	static final int DEFAULT_PIECES_TO_SEND_FOR_NONLAUNCH_CONTENT = Broker.ROWS * 500;
	static final int DEFAULT_PIECES_TO_SEND_FOR_FLOWS = Broker.ROWS;
	
	protected final List<IAllFlows> _allFlowsList =
		new LinkedList<IAllFlows>();
	
	protected Map<Sequence, Set<InetSocketAddress>> _launchContents =
		new HashMap<Sequence, Set<InetSocketAddress>>();

	protected final Map<InetSocketAddress, IAllFlows> _allFlowsMap =
		new HashMap<InetSocketAddress, IAllFlows>();
	
	protected final Map<Sequence, Integer> _contentServingCounters =
		new HashMap<Sequence, Integer>();
	
	protected final Random _rand =
		new Random(SystemTime.currentTimeMillis());
	
	protected final IBrokerShadow _brokerShadow;
	protected final InetSocketAddress _localAddress;
	protected final IAllFlows _aggregatedIntputFlowsBW;
	protected final IAllFlows _aggregatedOutputFlowsBW;
	protected int _concurrentEncodings = 0;
//	protected final int MAX_CONCURRENT_ENCODING;
	
	protected final Map<InetSocketAddress, Integer> _configuredBandwidths;
	protected final int DEFAULT_FLOW_BW;
	protected final int MAX_OUTPUT_BW;
	protected final int MAX_INPUT_BW;
	
	public FlowManager(IBrokerShadow brokerShadow, int maxInputBandwidth, int maxOutputBandwidth, int defaultFlowBandwidth) {
//		MAX_CONCURRENT_ENCODING = maxConcurrentEncodings;
		DEFAULT_FLOW_BW = defaultFlowBandwidth;
		MAX_OUTPUT_BW = maxOutputBandwidth;
		MAX_INPUT_BW = maxInputBandwidth;
		
		_brokerShadow = brokerShadow;
		_localAddress = _brokerShadow.getLocalAddress();
		_aggregatedIntputFlowsBW = createFlow(_localAddress, MAX_INPUT_BW);
		_aggregatedOutputFlowsBW = createFlow(_localAddress, MAX_OUTPUT_BW);
		
		_configuredBandwidths = new HashMap<InetSocketAddress, Integer>();
	}
	
	@Override
	public int getBandwidth(InetSocketAddress remote) {
		Integer bw = _configuredBandwidths.get(remote);
		if(bw == null)
			return DEFAULT_FLOW_BW;
		else
			return bw.intValue();
	}
	
	public void loadConfiguredBandwidth(InetSocketAddress remote, Integer configuredBandwidth) {
		IAllFlows flow = _allFlowsMap.get(remote);
		if(flow!=null)
			flow.setBandwidth(configuredBandwidth);
		
		_configuredBandwidths.put(remote, configuredBandwidth);
	}
	
	@Override
	public void loadConfiguredBandwidth(Map<InetSocketAddress, Integer> configuredBandwidths) {
		if(_allFlowsMap.size() != 0)
			throw new IllegalStateException();
	}
	
	public IFlow addFlow(FlowPriority flowPriority, InetSocketAddress remote, Content content) {
		return addFlow(flowPriority, remote, content, DEFAULT_PIECES_TO_SEND_FOR_FLOWS);
	}
	
	@Override
	public IFlow addFlow(
			FlowPriority flowPriority, InetSocketAddress remote, Content content, int piecesToSendBeforeBreak) {
		IAllFlows allFlow = _allFlowsMap.get(remote);
		
		if(allFlow == null || !allFlow.isActive()) {
			if(allFlow != null) {
				_allFlowsMap.remove(remote);
				_allFlowsList.remove(allFlow);
			}
			
//			if(piecesToSendBeforeBreak == 0)
//				return null;
			
			int bandwidth = getBandwidth(remote);
			if(remote.equals(_localAddress))
				throw new IllegalArgumentException("remote: " + remote + " vs. " + _localAddress);
			
			allFlow = createFlow(remote, bandwidth);
			addAllFlowPrivately(allFlow);
		}
		
//		if(piecesToSendBeforeBreak <= 0) {
//			IFlow flow = allFlow.getFlow(content);
//			flow.setPiecesToSendBeforeBreak(piecesToSendBeforeBreak);
//			if(flow != null)
//				flow.deactivate();
//			return null;
//		}
		
		IFlow flow = allFlow.addFlow(flowPriority, content, piecesToSendBeforeBreak);
		return flow;
	}

	protected void addAllFlowPrivately(IAllFlows allFlow) {
		_allFlowsMap.put(allFlow.getRemote(), allFlow);
		_allFlowsList.add(allFlow);
	}
	
	protected final IAllFlows createFlow(InetSocketAddress remote, int bandwidth) {
		return new AllFlows(_brokerShadow, remote, bandwidth);
	}
	
	@Override
	public int getRemainingBandwidth(InetSocketAddress remote) {
		IAllFlows allFlow = _allFlowsMap.get(remote);
		
		if(allFlow == null)
			return getBandwidth(remote);
		
		return allFlow.getRemainingBandwidth();
	}

	@Override
	public int getUsedBandwidth(InetSocketAddress remote) {
		return getBandwidth(remote) - getRemainingBandwidth(remote);
	}

	protected final Pattern _pattern = Pattern.compile("[\\W]*/?([\\w\\.]*):(\\d*)" + "[\\W]*/?([\\w\\.]*):(\\d*)" + "[\\W]*(\\d*)[\\W]*");
	@Override
	public boolean loadConfiguredBandwidth(InetSocketAddress localAddress, String line) {
		line = line.trim();
		if(line.startsWith("#"))
			return true;
		
		Matcher matcher = _pattern.matcher(line);
		if(!matcher.matches())
			return false;
		
		if(matcher.groupCount() != 5)
			return false;
		
		String ipStr1 = matcher.group(1);
		String portStr1 = matcher.group(2);
		String ipStr2 = matcher.group(3);
		String portStr2 = matcher.group(4);
		String bwStr = matcher.group(5);
		
		
		InetSocketAddress addr1 = new InetSocketAddress(ipStr1, new Integer(portStr1).intValue());
		if(!addr1.equals(localAddress))
			return true;
		
		InetSocketAddress addr2 = new InetSocketAddress(ipStr2, new Integer(portStr2).intValue());
		Integer bw = new Integer(bwStr);
		
		loadConfiguredBandwidth(addr2, bw);
		return true;
	}
	
	@Override
	public boolean loadConfiguredBandwidthFromFile(InetSocketAddress localAddress,
			String filename) {
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(filename));
			
			while(true) {
				String line = bReader.readLine();
				if(line == null)
					break;
				
				loadConfiguredBandwidth(localAddress, line);
			}
			
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public IFlow getFlow(InetSocketAddress remote, Sequence contentSequence) {
		return getFlowPrivately(remote, contentSequence);
	}
	
	protected IFlow getFlowPrivately(InetSocketAddress remote, Sequence contentSequence) {
		IAllFlows allFlow = _allFlowsMap.get(remote);
		if(allFlow == null)
			return null;
		
		return allFlow.getFlow(contentSequence);
	}

	@Override
	public void deactivate(InetSocketAddress remote) {
		IAllFlows allFlow = _allFlowsMap.get(remote);
		if(allFlow == null)
			return;
		
		allFlow.deactivate();
	}

	@Override
	public void deactivate(InetSocketAddress remote, Sequence contentSequence) {
		IAllFlows allFlow = _allFlowsMap.get(remote);
		if(allFlow == null)
			return;
		
		allFlow.deactivate(contentSequence);
	}

	@Override
	public void deactivate(Sequence contentSequence) {
		throw new UnsupportedOperationException();
	}

//	@Override
//	public final void outgoingFlowEncodingFinished(IFlow flow) {
//		_concurrentEncodings--;
//		if(_concurrentEncodings == MAX_CONCURRENT_ENCODING-1)
//			BrokerInternalTimer.inform("Lowered from max flows limit: " + _concurrentEncodings + " vs. " + MAX_CONCURRENT_ENCODING + ": " + _localAddress.getPort());
//		
//		if(_concurrentEncodings >= MAX_CONCURRENT_ENCODING)
//			throw new IllegalStateException(_concurrentEncodings + " vs. " + MAX_CONCURRENT_ENCODING);
//		else if(_concurrentEncodings<0)
//			throw new IllegalStateException(_concurrentEncodings + " vs. " + MAX_CONCURRENT_ENCODING);
//		
//		if(flow != null)
//			((Flow)flow).pieceSent();
//	}
	
	@Override
	public IFlow getNextOutgoingFlow(FlowSelectionPolicy flPolicy, int packetSize) {
//		if(_concurrentEncodings >= MAX_CONCURRENT_ENCODING) {
//			BrokerInternalTimer.inform("Reached max flows limit: " + _concurrentEncodings + " vs. " + MAX_CONCURRENT_ENCODING + ": " + _localAddress.getPort());
//			return null;
//		}
		
		IFlow flow = getNextOutgoingFlowPrivately(flPolicy, packetSize);
		if(flow != null)
			_concurrentEncodings++;
		
		return flow;
	}
	
	protected IFlow getNextOutgoingFlowPrivately(FlowSelectionPolicy flPolicy, int packetSize) {
		int remainingBandwidth = _aggregatedOutputFlowsBW.getRemainingBandwidth();
		if(remainingBandwidth < packetSize) {
			if(!Broker.RELEASE || Broker.DEBUG)
				BrokerInternalTimer.inform("No more output BW left: " + remainingBandwidth + "<" + packetSize);
			return null;
		}
		
		IFlow flow = null;
		
		switch(flPolicy) {
		case FL_FAIR_ROUNDROBIN_ROUNDROBIN:
			for(FlowPriority flowPriority : FlowPriority.values()) {
				flow = pickNextFlowRoundrobinRoundrobin(flowPriority, packetSize);
				if(flow != null)
					break;
			}
			break;
		
		default:
			throw new UnsupportedOperationException("Not supported: " + flPolicy);
		}
		
		if(flow != null)
			if(!_aggregatedOutputFlowsBW.shouldSend(packetSize))
				throw new IllegalStateException();
		
		return flow;
	}

	protected IFlow pickNextFlowRoundrobinRoundrobin(FlowPriority flowPriority, int packetSize) {
		if(_allFlowsList.size() != _allFlowsMap.size())
			throw new IllegalStateException("Size mismatch: " + _allFlowsList.size() + " vs. " + _allFlowsMap.size());
		
		if(_allFlowsList.size() == 0)
			return null;
		
		for(int i=0 ; i<_allFlowsList.size() ; i++) {
			IAllFlows allFlow = _allFlowsList.remove(0);
			if(!allFlow.isActive()) {
				_allFlowsMap.remove(allFlow.getRemote());
				continue;
			}
			
			_allFlowsList.add(allFlow);
			IFlow flow = allFlow.pickNextFlowRoundrobin(flowPriority, packetSize);
			if(flow != null) {
				adjustFlowPriority(flow);
				return flow;
			}
		}

		return null;
	}

	@Override
	public int getLocalIncomingBandwidth() {
		return _aggregatedIntputFlowsBW.getBandwidth();
	}

	@Override
	public int getLocalIncomingRemainingBandwidth() {
		return _aggregatedIntputFlowsBW.getRemainingBandwidth();
	}

	@Override
	public int getLocalIncomingUsedBandwidth() {
		return _aggregatedIntputFlowsBW.getUsedBandwidth();
	}

	@Override
	public int getLocalOutgoingBandwidth() {
		return _aggregatedOutputFlowsBW.getBandwidth();
	}

	@Override
	public int getLocalOutgoingRemainingBandwidth() {
		return _aggregatedOutputFlowsBW.getRemainingBandwidth();
	}

	@Override
	public int getLocalOutgoingUsedBandwidth() {
		return _aggregatedOutputFlowsBW.getUsedBandwidth();
	}
	
	@Override
	public String toString() {
		return "FlowM[" + _localAddress.getPort() + "]";
	}

	@Override
	public int getActiveFlowsCount() {
		int activeFlowsCount = 0;
		for(IAllFlows allFlows : _allFlowsList)
			activeFlowsCount += allFlows.getActiveFlowsCount();
		return activeFlowsCount;
	}

	@Override
	public boolean canAddNewFlows() {
		if(Broker.MAX_ACTIVE_FLOWS < 0)
			return true;
		else
			return getActiveFlowsCount() < Broker.MAX_ACTIVE_FLOWS;
	}

	@Override
	public int getActiveFlowsCount(Sequence contentSequence) {
		int activeFlowsCount = 0;
		for(IAllFlows allFlows : _allFlowsList)
			if(allFlows.hasActiveFlows(contentSequence))
				activeFlowsCount++;
		return activeFlowsCount;
	}

	@Override
	public boolean isContentLocallyPublished(Content content) {
		return isContentLocallyPublished(content.getSourceSequence());
	}
	
	@Override
	public boolean isContentLocallyPublished(Sequence contentSequence) {
		return _localAddress.equals(contentSequence._address);
	}

	@Override
	public void addLaunchContent(
			Sequence sourceSequence, Set<InetSocketAddress> remotes) {
		for(InetSocketAddress remote : remotes)
			addLaunchContent(sourceSequence, remote);
	}
	
	@Override
	public void addLaunchContent(
			Sequence sourceSequence, InetSocketAddress remote) {
		Set<InetSocketAddress> remotes = _launchContents.get(sourceSequence);
		if(remotes == null) {
			remotes = new HashSet<InetSocketAddress>();
			_launchContents.put(sourceSequence, remotes);
		}
		
		remotes.add(remote);
	}
	
	@Override
	public FlowPriority getFlowPriority(
			Content content, InetSocketAddress remote,
			int piecesToSendBeforeBreak) {
		if(piecesToSendBeforeBreak < MIN_CRITICAL_LEFTOVER_BLOCKS)
			if(_rand.nextInt(10) == 0)
				return FlowPriority.ULTIMATE;
		
		return getFlowPriority(content, remote);
	}
	
	@Override
	public FlowPriority getFlowPriority(
			Content content, InetSocketAddress remote) {
		Sequence sourceSequence = content.getSourceSequence();
		if(isContentLocallyPublished(sourceSequence))
			return FlowPriority.ULTIMATE;

		Set<InetSocketAddress> remotes = _launchContents.get(sourceSequence);
		if(remotes != null && remotes.contains(remote))
				return FlowPriority.ULTIMATE;
		
		return FlowPriority.HIGH;
	}
	
	protected void adjustFlowPriority(IFlow flow) {
		InetSocketAddress remote = flow.getRemoteAddress();
		Sequence sourceSequence = flow.getContentSequence();
		Set<InetSocketAddress> remotes =
			_launchContents.get(sourceSequence);
		if(remotes != null) {
			if(remotes.contains(remote))
				adjustFlowPriorityForLaunchContent(flow, sourceSequence);
		} else {
			adjustFlowPriorityForNonLaunchContent(flow, sourceSequence);
		}
	}
	
	protected void adjustFlowPriorityForNonLaunchContent(
			IFlow flow, Sequence contentSequence) {
		Integer served = _contentServingCounters.get(contentSequence);
		if(served == null)
			served = new Integer(0);
		
		_contentServingCounters.put(contentSequence, ++served);
		
		int bucketSize = DEFAULT_PIECES_TO_SEND_FOR_NONLAUNCH_CONTENT;
		switch(flow.getFlowPriority())
		{
		case ULTIMATE:
			break;
			
		case HIGH:
			if(served / bucketSize > 0)
				flow.degradeFlowPriority();
			break;
			
		case MEDIUM:
			if(served / bucketSize > 1)
				flow.degradeFlowPriority();
			break;
			
		case LOW:
			break;
		}
	}
	
	protected void adjustFlowPriorityForLaunchContent(
			IFlow flow, Sequence contentSequence) {
//		if(true)
//			return;
		Integer served = _contentServingCounters.get(contentSequence);
		if(served == null)
			served = 0;
		
		_contentServingCounters.put(contentSequence, ++served);
		
		if(!flow.isActive()) {
			IAllFlows allFlows = _allFlowsMap.get(flow.getRemoteAddress());
			if(allFlows != null)
				allFlows.changeFlowPriorityPrivately(flow, FlowPriority.LOW);
			return;
		}

		int bucketSize = DEFAULT_PIECES_TO_SEND_FOR_LAUNCH_CONTENT;
		switch(flow.getFlowPriority())
		{
		case ULTIMATE:
			if(served / bucketSize > 0)
				flow.degradeFlowPriority();
			break;

		case HIGH:
			if(served / bucketSize > 1)
				flow.degradeFlowPriority();
			break;
			
		case MEDIUM:
			if(served / bucketSize > 2)
				flow.degradeFlowPriority();
			break;
			
		case LOW:
			break;
		}
	}
}
