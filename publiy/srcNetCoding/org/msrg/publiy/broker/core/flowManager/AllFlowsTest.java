package org.msrg.publiy.broker.core.flowManager;

import java.net.InetSocketAddress;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import junit.framework.TestCase;

public class AllFlowsTest extends TestCase {
	
	IFlowManager _flowManager;
	IAllFlows _allFlow;
	InetSocketAddress _localAddress;
	InetSocketAddress _remote;
	final int _packetSize = 1100;
	final int _bandwidth = 5 * _packetSize;

	String _line;
	String _commentLine = "\t#\t";
	
	@Override
	public void setUp() {
		SystemTime.resetTime();
		BrokerInternalTimer.start();
		_localAddress = Sequence.getRandomAddress();
		_remote = Sequence.getRandomAddress();
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _localAddress).setNC(true);
		_allFlow = new AllFlows(brokerShadow, _remote, _bandwidth);
		_flowManager = new FlowManager(brokerShadow, 100, 100, 100);
	}

	@Override
	public void tearDown() {
		SystemTime.resetTime();
	}
	
	public void testBandwidth() {
		runTestFlows(_packetSize, 50, 10000);
	}

	protected void runTestFlows(int packetSize, int timeProgressMillis, int testDurationMillis) {
		int totalSent = 0;
		for(int timeMillis=0 ; timeMillis<testDurationMillis ; timeMillis+=timeProgressMillis) {
			SystemTime.setSystemTime(timeMillis);
			
			int remainingBWbefore = _allFlow.getRemainingBandwidth();
			boolean shouldSend = _allFlow.shouldSend(packetSize);
//			int remainingBWafter = _allFlow.getRemainingBandwidth();
			BrokerInternalTimer.inform(
					String.format("%05d", timeMillis) + " [" + shouldSend + "]: " + remainingBWbefore);
			
			if(shouldSend)
				totalSent++;
		}
		
		BrokerInternalTimer.inform("Total sent: " + totalSent);
		BrokerInternalTimer.inform("Average sent: " + ((packetSize*totalSent)/(testDurationMillis/1000)));
	}
	
	public void testLoadConfiguredBandwidth() {
		for(int i=0 ; i<10000 ; i++) {
			_line = "\t" + _localAddress.toString() + " " + "\t" + _remote + " " + i + "\t";
			_flowManager.loadConfiguredBandwidth(_localAddress, _line);
			_flowManager.loadConfiguredBandwidth(_localAddress, _commentLine);
			assertTrue(_flowManager.getBandwidth(_remote) == i);
			

			_line = "\t" + _remote.toString() + " " + "\t" + _localAddress + " " + i*2;
			_flowManager.loadConfiguredBandwidth(_localAddress, _line);
			assertTrue(_flowManager.getBandwidth(_remote) == i);
		}
		
		System.out.println("OK!");
	}
}
