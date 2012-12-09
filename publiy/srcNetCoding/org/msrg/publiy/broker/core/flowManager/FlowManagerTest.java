package org.msrg.publiy.broker.core.flowManager;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.contentManager.SourcePSContent;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import junit.framework.TestCase;

public class FlowManagerTest extends TestCase {

	final static int _rows = 100;
	final static int _cols = 10000;
	final int _processedFlows = 10000;
	
	final int DEFAULT_PACKETSIZE = _cols + _rows + 10;
	static {
		assertTrue(
				"VM-ARGS: -DBroker.CODING=true -DNC.ROWS=" + _rows + " -DNC.COLS=" + _cols,
				Broker.CODING && Broker.ROWS == _rows && Broker.COLS == _cols);
		SystemTime.resetTime();
		BrokerInternalTimer.start();
	}
	
	IFlowManager _flowManager;
	
	final InetSocketAddress _localAddress = Sequence.getRandomAddress();
	final InetSocketAddress _remote1 = Sequence.getRandomAddress();
	final InetSocketAddress _remote2 = Sequence.getRandomAddress();
	final InetSocketAddress _remote3 = Sequence.getRandomAddress();

	final Sequence _contentSequence1 = Sequence.getRandomSequence();
	final Sequence _contentSequence2 = Sequence.getRandomSequence();
	final Sequence _contentSequence3 = Sequence.getRandomSequence();

	final Publication _publication = new Publication().addPredicate("heylo", 10123);
	
	final Content _content1 = SourcePSContent.createDefaultSourcePSContent(_contentSequence1, _publication, _rows, _cols);
	final Content _content2 = SourcePSContent.createDefaultSourcePSContent(_contentSequence2, _publication, _rows, _cols);
	final Content _content3 = SourcePSContent.createDefaultSourcePSContent(_contentSequence3, _publication, _rows, _cols);
	
	@Override
	public void setUp() {
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _localAddress).setNC(true).setNCCols(_cols).setNCRows(_rows);
		BrokerInternalTimer.inform("Default Packetsize: " + DEFAULT_PACKETSIZE);
		_flowManager = new FlowManager_ForTest(
				brokerShadow, 100, 2 * DEFAULT_PACKETSIZE, DEFAULT_PACKETSIZE);
		
		_flowManager.addFlow(FlowPriority.HIGH, _remote1, _content2);
		_flowManager.addFlow(FlowPriority.HIGH, _remote1, _content3);
		
		_flowManager.addFlow(FlowPriority.HIGH, _remote2, _content1);
		_flowManager.addFlow(FlowPriority.HIGH, _remote2, _content3);
		
		_flowManager.addFlow(FlowPriority.HIGH, _remote3, _content1);
		_flowManager.addFlow(FlowPriority.HIGH, _remote3, _content2);
	}
	
	@Override
	public void tearDown() {
		SystemTime.resetTime();
	}
	
	public void testFlows() {
		int packetSize = DEFAULT_PACKETSIZE;
		int timeProgressMillis = 100;
		int totalDurationMillis = 10 * 1000;
		List<IFlow> roundrobinFlows = runTestFlows(packetSize, timeProgressMillis, totalDurationMillis);
		checkrondrobinFlows(roundrobinFlows);
		
		
		{
			IAllFlows allFlows1 = ((FlowManager_ForTest)_flowManager).getAllFlows(_remote1);
			checkAllFlows(allFlows1, FlowPriority.ULTIMATE, 0);
			checkAllFlows(allFlows1, FlowPriority.HIGH, 2);
			checkAllFlows(allFlows1, FlowPriority.MEDIUM, 0);
			checkAllFlows(allFlows1, FlowPriority.LOW, 0);
			IAllFlows allFlows2 = ((FlowManager_ForTest)_flowManager).getAllFlows(_remote2);
			checkAllFlows(allFlows2, FlowPriority.ULTIMATE, 0);
			checkAllFlows(allFlows2, FlowPriority.HIGH, 2);
			checkAllFlows(allFlows2, FlowPriority.MEDIUM, 0);
			checkAllFlows(allFlows2, FlowPriority.LOW, 0);
			IAllFlows allFlows3 = ((FlowManager_ForTest)_flowManager).getAllFlows(_remote3);
			checkAllFlows(allFlows3, FlowPriority.ULTIMATE, 0);
			checkAllFlows(allFlows3, FlowPriority.HIGH, 2);
			checkAllFlows(allFlows3, FlowPriority.MEDIUM, 0);
			checkAllFlows(allFlows3, FlowPriority.LOW, 0);
			
			IFlow flow1_2 = _flowManager.addFlow(FlowPriority.LOW, _remote1, _content2);
			checkAllFlows(allFlows1, FlowPriority.ULTIMATE, 0);
			checkAllFlows(allFlows1, FlowPriority.HIGH, 1);
			checkAllFlows(allFlows1, FlowPriority.MEDIUM, 0);
			checkAllFlows(allFlows1, FlowPriority.LOW, 1);
			assertTrue(_remote1.equals(flow1_2.getRemoteAddress()));
			assertTrue(_content2.getSourceSequence().equals(flow1_2.getContentSequence()));
			assertTrue(flow1_2.getFlowPriority() == FlowPriority.LOW);
			
			
			IAllFlows newAllFlows1 = ((FlowManager_ForTest)_flowManager).getAllFlows(_remote1);
			assertTrue(newAllFlows1 == allFlows1);

			checkAllFlows(allFlows1, FlowPriority.ULTIMATE, 0);
			checkAllFlows(allFlows1, FlowPriority.HIGH, 1);
			checkAllFlows(allFlows1, FlowPriority.MEDIUM, 0);
			checkAllFlows(allFlows1, FlowPriority.LOW, 1);
			IFlow flow1_1 = _flowManager.addFlow(FlowPriority.ULTIMATE, _remote1, _content1);
			assertTrue(flow1_1.isActive());
			assertTrue(flow1_1.getFlowPriority() == FlowPriority.ULTIMATE);
			checkAllFlows(allFlows1, FlowPriority.ULTIMATE, 1);
			checkAllFlows(allFlows1, FlowPriority.HIGH, 1);
			checkAllFlows(allFlows1, FlowPriority.MEDIUM, 0);
			checkAllFlows(allFlows1, FlowPriority.LOW, 1);
			
			
			flow1_1 = _flowManager.addFlow(FlowPriority.MEDIUM, _remote1, _content1);
			assertTrue(flow1_1.isActive());
			assertTrue(flow1_1.getFlowPriority() == FlowPriority.MEDIUM);
			checkAllFlows(allFlows1, FlowPriority.ULTIMATE, 0);
			checkAllFlows(allFlows1, FlowPriority.HIGH, 1);
			checkAllFlows(allFlows1, FlowPriority.MEDIUM, 1);
			checkAllFlows(allFlows1, FlowPriority.LOW, 1);
			
			
			IFlow flow1_3 = _flowManager.getFlow(_remote1, _content3.getSourceSequence());
			assertTrue(flow1_3.getFlowPriority() == FlowPriority.HIGH);
			assertTrue(_remote1.equals(flow1_3.getRemoteAddress()));
			assertTrue(_content3.getSourceSequence().equals(flow1_3.getContentSequence()));
			
			checkAllFlows(allFlows1, FlowPriority.ULTIMATE, 0);
			checkAllFlows(allFlows1, FlowPriority.HIGH, 1);
			checkAllFlows(allFlows1, FlowPriority.MEDIUM, 1);
			checkAllFlows(allFlows1, FlowPriority.LOW, 1);
		}
		
		{
			IFlow flow3_2 = _flowManager.addFlow(FlowPriority.LOW, _remote3, _content2);
			assertTrue(flow3_2.getFlowPriority() == FlowPriority.LOW);
			IFlow flow1_2 = _flowManager.getFlow(_remote1, _content2.getSourceSequence());
			assertTrue(flow1_2.getFlowPriority() == FlowPriority.LOW);
			IFlow flow1_3 = _flowManager.getFlow(_remote1, _content3.getSourceSequence());
			assertTrue(flow1_3.getFlowPriority() == FlowPriority.HIGH);
			IFlow flow2_3 = _flowManager.getFlow(_remote2, _content2.getSourceSequence());
			assertNull(flow2_3);
			IFlow flow3_3 = _flowManager.getFlow(_remote3, _content3.getSourceSequence());
			assertNull(flow3_3);
			
			IAllFlows allFlows1 = ((FlowManager_ForTest)_flowManager).getAllFlows(_remote1);
			checkAllFlows(allFlows1, FlowPriority.ULTIMATE, 0);
			checkAllFlows(allFlows1, FlowPriority.HIGH, 1);
			checkAllFlows(allFlows1, FlowPriority.MEDIUM, 1);
			checkAllFlows(allFlows1, FlowPriority.LOW, 1);

			IAllFlows allFlows2 = ((FlowManager_ForTest)_flowManager).getAllFlows(_remote2);
			checkAllFlows(allFlows2, FlowPriority.ULTIMATE, 0);
			checkAllFlows(allFlows2, FlowPriority.HIGH, 2);
			checkAllFlows(allFlows2, FlowPriority.MEDIUM, 0);
			checkAllFlows(allFlows2, FlowPriority.LOW, 0);

			IAllFlows allFlows3 = ((FlowManager_ForTest)_flowManager).getAllFlows(_remote3);
			checkAllFlows(allFlows3, FlowPriority.ULTIMATE, 0);
			checkAllFlows(allFlows3, FlowPriority.HIGH, 1);
			checkAllFlows(allFlows3, FlowPriority.MEDIUM, 0);
			checkAllFlows(allFlows3, FlowPriority.LOW, 1);
		}
	}
	
	protected void checkAllFlows(IAllFlows allFlows, FlowPriority flowPriority, int size) {
		List<IFlow> flowList = ((AllFlows)allFlows)._flowsPriorityLists.get(flowPriority);
		
		int countedSize = 0;
		for(IFlow flow : flowList) {
			assertTrue(flow.getFlowPriority() == flowPriority);
			if(flow.isActive())
				countedSize++;
		}
		
		assertTrue(countedSize == size);
	}
	
	protected List<IFlow> runTestFlows(int packetSize, int timeProgressMillis, int totalDurationMillis) {
		List<IFlow> roundrobinFlows = new LinkedList<IFlow>();
		for(int timeMillis=0 ; timeMillis<totalDurationMillis ; timeMillis+=timeProgressMillis) {
			SystemTime.setSystemTime(timeMillis);
			IFlow flow =
				_flowManager.getNextOutgoingFlow(
						FlowSelectionPolicy.FL_FAIR_ROUNDROBIN_ROUNDROBIN, packetSize);
			
			if(flow != null)
				roundrobinFlows.add(flow);
		}
		return roundrobinFlows;
	}

	protected void checkrondrobinFlows(List<IFlow> roundrobinFlows) {
		Map<Content, Integer> contents = new HashMap<Content, Integer>();
		Map<InetSocketAddress, Integer> remotes = new HashMap<InetSocketAddress, Integer>();
		
		for(IFlow flow : roundrobinFlows) {
			Content content = flow.getContent();
			Integer contentCount = contents.get(content);
			if(contentCount == null)
				contentCount = 1;
			else
				contentCount = contentCount + 1;
			contents.put(content, contentCount);
			
			
			InetSocketAddress remote = flow.getRemoteAddress();
			Integer remoteCount = remotes.get(remote);
			if(remoteCount == null)
				remoteCount = 1;
			else
				remoteCount = remoteCount + 1;
			remotes.put(remote, remoteCount);
		}
		
		{
			assertTrue(contents.size() == 3);
			int maxContentCount = -1, minContentCount = -1;
			for(int contnetCount : contents.values())
				if(maxContentCount == -1) {
					maxContentCount = contnetCount;
					minContentCount = contnetCount;
				} else if (contnetCount > maxContentCount)
					maxContentCount = contnetCount;
				else if (contnetCount < minContentCount)
					minContentCount = contnetCount;
			assertTrue(maxContentCount - minContentCount <= 1);
		}

		{
			assertTrue(remotes.size() == 3);
			int maxRemotesCount = -1, minRemotesCount = -1;
			for(int remoteCount : remotes.values())
				if(maxRemotesCount == -1) {
					maxRemotesCount = remoteCount;
					minRemotesCount = remoteCount;
				} else if (remoteCount > maxRemotesCount)
					maxRemotesCount = remoteCount;
				else if (remoteCount < minRemotesCount)
					minRemotesCount = remoteCount;
			assertTrue(maxRemotesCount - minRemotesCount <= 1);
		}

		{
			BrokerInternalTimer.inform("Total flows out: " + roundrobinFlows.size());
			_flowManager.getBandwidth(_localAddress);
		}
	}
}

class FlowManager_ForTest extends FlowManager {

	public FlowManager_ForTest(IBrokerShadow brokerShadow,
			int maxInputBandwidth, int maxOutputBandwidth, int defaultFlowBandwidth) {
		super(brokerShadow, maxInputBandwidth,
				maxOutputBandwidth, defaultFlowBandwidth);
	}
	
	public IAllFlows getAllFlows(InetSocketAddress remote) {
		return _allFlowsMap.get(remote);
	}
}
