package org.msrg.publiy.broker.core.flowManager;

import java.net.InetSocketAddress;



import org.msrg.publiy.networkcodes.PSCodedPiece;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.contentManager.ReceivedPSContent;
import org.msrg.publiy.broker.core.contentManager.SourcePSContent;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import junit.framework.TestCase;

public class FlowManagerTest3 extends TestCase {

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
	final IBrokerShadow _brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _localAddress).setNC(true).setNCCols(_cols).setNCRows(_rows);
	
	final Sequence _contentSequence0 = new Sequence(_localAddress, 0, 1);
	final Sequence _contentSequence1 = new Sequence(_remote1, 0, 1);
	final Sequence _contentSequence2 = new Sequence(_remote2, 0, 1);
	final Sequence _contentSequence3 = new Sequence(_remote3, 0, 1);

	final Publication _publication = new Publication().addPredicate("heylo", 10123);
	
	final Content _content1_org = SourcePSContent.createDefaultSourcePSContent(_contentSequence1, _publication, _rows, _cols);
	final Content _content2_org = SourcePSContent.createDefaultSourcePSContent(_contentSequence2, _publication, _rows, _cols);
	
	final Content _content1 = ReceivedPSContent.createReceivedPSContent(null, _contentSequence1, _publication, _rows, _cols);
	final Content _content2 = ReceivedPSContent.createReceivedPSContent(null, _contentSequence2, _publication, _rows, _cols);
	
	IFlow _flow_remote2_content1; //, _flow_remote3_content1;
//	IFlow _flow_remote3_content2, _flow_remote1_content2;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.inform("Default Packetsize: " + DEFAULT_PACKETSIZE);
		_flowManager = new FlowManager_ForTest3(_brokerShadow, 100, 2 * DEFAULT_PACKETSIZE, DEFAULT_PACKETSIZE);

		_flow_remote2_content1 =
			_flowManager.addFlow(_flowManager.getFlowPriority(_content1, _remote2), _remote2, _content1);
//		_flow_remote3_content1 =
//			_flowManager.addFlow(_flowManager.getFlowPriority(_content1), _remote3, _content1);

//		_flow_remote1_content2 =
//			_flowManager.addFlow(_flowManager.getFlowPriority(_content2), _remote1, _content2);
//		_flow_remote3_content2 =
//			_flowManager.addFlow(_flowManager.getFlowPriority(_content2), _remote3, _content2);
	}
	
	public void printInfo() {
		BrokerInternalTimer.inform(
				_flow_remote2_content1.getPiecesSent() + ", "// +
//				_flow_remote3_content1.getPiecesSent() + ", "+
//				_flow_remote1_content2.getPiecesSent() + ", " +
//				_flow_remote3_content2.getPiecesSent()
				);
	}
	
	public void testPickNextFlowRoundrobinRoundrobin() {
		assertTrue(_content1_org.getAvailableCodedPieceCount() == _content1_org.getRows());
		assertTrue(_content2_org.getAvailableCodedPieceCount() == _content2_org.getRows());
		
		assertTrue(_content1.getAvailableCodedPieceCount() == 0);
		assertTrue(_content2.getAvailableCodedPieceCount() == 0);
		_flow_remote2_content1.setPiecesToSendBeforeBreak(-1);
		for(int i=0 ; i<_rows * 4 ; i++)
		{
			IFlow flow =
				_flowManager.getNextOutgoingFlow(
						FlowSelectionPolicy.FL_FAIR_ROUNDROBIN_ROUNDROBIN, 0);
			assertTrue("i=" + i, flow == _flow_remote2_content1);
			if(i<_rows)
				assertFalse("i=" + i, flow.canEncodeAndSend(true));
			else
				assertTrue("i=" + i, flow.canEncodeAndSend(true));
			
			flow =
				_flowManager.getNextOutgoingFlow(
						FlowSelectionPolicy.FL_FAIR_ROUNDROBIN_ROUNDROBIN, 0);
			assertTrue("i=" + i, flow == _flow_remote2_content1);
			if(i<_rows)
				assertFalse("i=" + i, flow.canEncodeAndSend(true));
			else
				assertTrue("i=" + i, flow.canEncodeAndSend(true));
			if(i<_rows)
				assertTrue(_content1.getMainSeedCodedPieceCount() == i);
			else
				assertTrue(_content1.getMainSeedCodedPieceCount() == -1);
			PSCodedPiece psCodedPiece1 = _content1_org.code();
			assertTrue(psCodedPiece1._fromMainSeed);
			_content1.addCodedPiece(_remote1, psCodedPiece1);
			
			assertTrue("i=" + i, flow.canEncodeAndSend(true));
			if(i==_rows-1)
				assertTrue("i=" + i, _content1.decode());
			
			printInfo();
		}
		
		BrokerInternalTimer.inform("END.");
	}
	
	@Override
	public void tearDown() {
		SystemTime.resetTime();
	}
}

class FlowManager_ForTest3 extends FlowManager {

	public FlowManager_ForTest3(IBrokerShadow brokerShadow,
			int maxInputBandwidth, int maxOutputBandwidth, int defaultFlowBandwidth) {
		super(brokerShadow, maxInputBandwidth, maxOutputBandwidth, defaultFlowBandwidth);
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
