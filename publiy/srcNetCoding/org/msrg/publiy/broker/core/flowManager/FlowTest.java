package org.msrg.publiy.broker.core.flowManager;

import java.net.InetSocketAddress;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import junit.framework.TestCase;

public class FlowTest extends TestCase {

	protected final int _bandwidth = 4000;
	protected final int _standardPacketSize = 150;
	protected final int _maxSec = 100;
	protected final InetSocketAddress _localAddress = new InetSocketAddress("127.0.0.1", 1000);
	protected final InetSocketAddress _remoteAddress = new InetSocketAddress("127.0.0.1", 2000);
	protected IBrokerShadow _brokerShadow;
	
	@Override
	public void setUp() {
		SystemTime.setSystemTime(0);
		_brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _localAddress).setNC(true);
	}
	
	@Override
	public void tearDown() {
		SystemTime.resetTime();
	}
	
	public void testTrafficFlow() {
		IAllFlows flow = new AllFlows(_brokerShadow, _remoteAddress, _bandwidth);
		int j = (_bandwidth/_standardPacketSize);
		for(int i=0; i<j ; i++)
			assertTrue(flow.shouldSend(_standardPacketSize));
		assertFalse(flow.shouldSend(_standardPacketSize));
	}
	
	public void testAggregatedTrafficFlow() {
		testAggregatedTrafficFlow(0, _maxSec, _standardPacketSize, _bandwidth*100);
		testAggregatedTrafficFlow(_maxSec, _maxSec, _standardPacketSize, _bandwidth);
		testAggregatedTrafficFlow(_maxSec*2, _maxSec, _standardPacketSize/100, _bandwidth);
	}
	
	public void testAggregatedTrafficFlow(int startTime, int maxSec, int packetSize, int bandwidth) {
		IAllFlows flow = new AllFlows(_brokerShadow, _remoteAddress, bandwidth);
		final int[] throughputPerSecond = new int[maxSec];
		for(int timeMilli=startTime*1000 ; timeMilli<(startTime+maxSec)*1000 ; timeMilli++) {
			int tick = timeMilli/1000 - startTime;
			SystemTime.setSystemTime(timeMilli);
			if(throughputPerSecond[tick] + packetSize < bandwidth) {
				if(tick + startTime == timeMilli*1000 && tick>=1)
					assertTrue(throughputPerSecond[tick-1] == flow.getUsedBandwidth());

				throughputPerSecond[tick] += packetSize;
				assertTrue(flow.shouldSend(packetSize));
			} else {
				assertFalse(flow.shouldSend(packetSize));
				assertTrue(throughputPerSecond[tick] == flow.getUsedBandwidth());
			}
			
			assertTrue(flow.getUsedBandwidth() + flow.getRemainingBandwidth() == flow.getBandwidth());
		}		
		
		System.out.println("******** " +
				"Time: " + startTime + "-" + (startTime+maxSec) + "s, " +
				"Bandwidth: " + bandwidth + "B, " +
				"PacketSize: " + packetSize + "B");
		for(int i=0; i<throughputPerSecond.length; i++) {
			assertTrue(throughputPerSecond[i]<=bandwidth);
			assertTrue(1000*packetSize<bandwidth || throughputPerSecond[i]+packetSize > bandwidth);
			
			System.out.print((i==0?"":",") + throughputPerSecond[i]);
		}
		System.out.println("\nOK!\n");
	}
}
