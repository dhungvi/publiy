package org.msrg.publiy.broker.core.flowManager;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;


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

public class FlowManagerTest2 extends TestCase {

	final static int _rows = 2;
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
	
	final InetSocketAddress _localAddress = new InetSocketAddress(1000);
	final InetSocketAddress _remote1 = new InetSocketAddress(1001);
	final InetSocketAddress _remote2 = new InetSocketAddress(1002);
	final InetSocketAddress _remote3 = new InetSocketAddress(1003);

	final Sequence _contentSequence0 = new Sequence(_localAddress, 0, 1);
	final Sequence _contentSequence1 = new Sequence(_remote1, 0, 1);
	final Sequence _contentSequence2 = new Sequence(_remote2, 0, 1);
	final Sequence _contentSequence3 = new Sequence(_remote3, 0, 1);

	final Publication _publication = new Publication().addPredicate("heylo", 10123);
	
	final Content _content0 = SourcePSContent.createDefaultSourcePSContent(_contentSequence0, _publication, _rows, _cols);
	final Content _content1 = SourcePSContent.createDefaultSourcePSContent(_contentSequence1, _publication, _rows, _cols);
	final Content _content2 = SourcePSContent.createDefaultSourcePSContent(_contentSequence2, _publication, _rows, _cols);
	final Content _content3 = SourcePSContent.createDefaultSourcePSContent(_contentSequence3, _publication, _rows, _cols);
	
	IFlow _flow_remote1_content0, _flow_remote2_content0, _flow_remote3_content0;
	IFlow _flow_remote1_content2, _flow_remote1_content3;
	IFlow _flow_remote2_content1, _flow_remote2_content3;
	IFlow _flow_remote3_content1, _flow_remote3_content2;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.inform("Default Packetsize: " + DEFAULT_PACKETSIZE);
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _localAddress).setNC(true).setNCCols(_cols).setNCRows(_rows);
		_flowManager = new FlowManager_ForTest2(brokerShadow, 100, 2 * DEFAULT_PACKETSIZE, DEFAULT_PACKETSIZE);

		_flow_remote1_content0 =
			_flowManager.addFlow(_flowManager.getFlowPriority(_content0, _remote1), _remote1, _content0);
		_flow_remote2_content0 =
			_flowManager.addFlow(_flowManager.getFlowPriority(_content0, _remote2), _remote2, _content0);
		_flow_remote3_content0 =
			_flowManager.addFlow(_flowManager.getFlowPriority(_content0, _remote3), _remote3, _content0);
		
		_flow_remote1_content2 =
			_flowManager.addFlow(_flowManager.getFlowPriority(_content2, _remote1), _remote1, _content2);
		_flow_remote1_content3 =
			_flowManager.addFlow(_flowManager.getFlowPriority(_content3, _remote1), _remote1, _content3);
		
		_flow_remote2_content1 =
			_flowManager.addFlow(_flowManager.getFlowPriority(_content1, _remote2), _remote2, _content1);
		_flow_remote2_content3 =
			_flowManager.addFlow(_flowManager.getFlowPriority(_content3, _remote2), _remote2, _content3);
		
		_flow_remote3_content1 =
			_flowManager.addFlow(_flowManager.getFlowPriority(_content1, _remote3), _remote3, _content1);
		_flow_remote3_content2 =
			_flowManager.addFlow(_flowManager.getFlowPriority(_content2, _remote3), _remote3, _content2);
	}
	
	public void testPickNextFlowRoundrobinRoundrobin() {
		assertTrue(_flow_remote1_content0.getFlowPriority() == FlowPriority.ULTIMATE);
		assertTrue(_flow_remote2_content0.getFlowPriority() == FlowPriority.ULTIMATE);
		assertTrue(_flow_remote3_content0.getFlowPriority() == FlowPriority.ULTIMATE);
		assertTrue(_flow_remote1_content2.getFlowPriority() == FlowPriority.HIGH);
		assertTrue(_flow_remote1_content3.getFlowPriority() == FlowPriority.HIGH);
		assertTrue(_flow_remote2_content1.getFlowPriority() == FlowPriority.HIGH);
		assertTrue(_flow_remote2_content3.getFlowPriority() == FlowPriority.HIGH);
		assertTrue(_flow_remote3_content1.getFlowPriority() == FlowPriority.HIGH);
		assertTrue(_flow_remote3_content2.getFlowPriority() == FlowPriority.HIGH);
		
		Set<IFlow> notUltimate = new HashSet<IFlow>();
		int maxUltimate = FlowManager.DEFAULT_PIECES_TO_SEND_FOR_LAUNCH_CONTENT;
		for(int i=0 ; i<maxUltimate+2 ; i++)
		{
			IFlow flow =
				((FlowManager_ForTest2)_flowManager).
				getNextOutgoingFlowPrivately(FlowSelectionPolicy.FL_FAIR_ROUNDROBIN_ROUNDROBIN, 0);
			assertTrue("i=" + i + ", flow=" + flow,
					flow == _flow_remote1_content0 ||
					flow == _flow_remote2_content0 ||
					flow == _flow_remote3_content0);
			
			if(flow.getFlowPriority() != FlowPriority.ULTIMATE)
				assertTrue("i=" + i + ", flow=" + flow, notUltimate.add(flow));
		}
		
		Set<IFlow> notHigh = new HashSet<IFlow>();
		int maxHigh = FlowManager.DEFAULT_PIECES_TO_SEND_FOR_LAUNCH_CONTENT + 3 * FlowManager.DEFAULT_PIECES_TO_SEND_FOR_NONLAUNCH_CONTENT;
		for(int i=0 ; i<maxHigh+2+1 ; i++) {
			IFlow flow =
				((FlowManager_ForTest2)_flowManager).getNextOutgoingFlowPrivately(
						FlowSelectionPolicy.FL_FAIR_ROUNDROBIN_ROUNDROBIN, 0);

			if(flow.getFlowPriority() != FlowPriority.HIGH)
				assertTrue("i=" + i + ", flow=" + flow, notHigh.add(flow));
		}
		
		Set<IFlow> notMed = new HashSet<IFlow>();
		int maxMed = FlowManager.DEFAULT_PIECES_TO_SEND_FOR_LAUNCH_CONTENT + 3 * FlowManager.DEFAULT_PIECES_TO_SEND_FOR_NONLAUNCH_CONTENT;
		for(int i=0 ; i<maxMed ; i++) {
			IFlow flow =
				((FlowManager_ForTest2)_flowManager).getNextOutgoingFlowPrivately(
						FlowSelectionPolicy.FL_FAIR_ROUNDROBIN_ROUNDROBIN, 0);

			if(flow.getFlowPriority() != FlowPriority.MEDIUM)
				assertTrue("i=" + i + ", flow=" + flow, notMed.add(flow));
		}

		int lowMed = 100 * (FlowManager.DEFAULT_PIECES_TO_SEND_FOR_LAUNCH_CONTENT + 3 * FlowManager.DEFAULT_PIECES_TO_SEND_FOR_NONLAUNCH_CONTENT);
		for(int i=0 ; i<lowMed ; i++) {
			IFlow flow =
				((FlowManager_ForTest2)_flowManager).getNextOutgoingFlowPrivately(
						FlowSelectionPolicy.FL_FAIR_ROUNDROBIN_ROUNDROBIN, 0);

			if(flow.getFlowPriority() != FlowPriority.LOW)
				assertTrue("i=" + i + ", flow=" + flow, notMed.add(flow));
		}

		BrokerInternalTimer.inform("END.");
	}
	
	@Override
	public void tearDown() {
		SystemTime.resetTime();
	}
}

class FlowManager_ForTest2 extends FlowManager {

	public FlowManager_ForTest2(IBrokerShadow brokerShadow,
			int maxInputBandwidth, int maxOutputBandwidth, int defaultFlowBandwidth) {
		super(brokerShadow, maxInputBandwidth,
				maxOutputBandwidth, defaultFlowBandwidth);
	}
	
	public IAllFlows getAllFlows(InetSocketAddress remote) {
		return _allFlowsMap.get(remote);
	}
	
	@Override
	public void adjustFlowPriority(IFlow flow) {
		FlowPriority oldFlowPriority = flow.getFlowPriority();
		super.adjustFlowPriority(flow);
		FlowPriority newFlowPriority = flow.getFlowPriority();
		
		if(oldFlowPriority != newFlowPriority)
			BrokerInternalTimer.inform("Flow priority changed: " + flow);
	}
	
	@Override
	protected IFlow getNextOutgoingFlowPrivately(FlowSelectionPolicy flPolicy, int packetSize) {
		IFlow flow = super.getNextOutgoingFlowPrivately(flPolicy, packetSize);
		printStat();
		return flow;
	}
	
	protected void printStat() {
		BrokerInternalTimer.inform(_contentServingCounters.toString());
	}
}
