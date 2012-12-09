package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.communication.core.packet.types.TPingReply;

public class RTTLogger_Test extends TestCase {
	
	@Override
	public void setUp() { }
	
	@Override
	public void tearDown() { }
	
	public void test() throws IOException {
		SystemTime.resetTime();
		BrokerInternalTimer.start();
		final InetSocketAddress localAddress = new InetSocketAddress(1000);
		final int delta = 4;
		BrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(delta);
		RTTLogger_ForTest rttLogger = new RTTLogger_ForTest(brokerShadow);
		rttLogger.initializeFile();
		
		for(int i = 1 ; i<11 ; i++) {
			SystemTime.setSystemTime(i * Broker.TPING_SEND_INTERVAL);

			long pTime = SystemTime.currentTimeMillis();
			double cpu = 0;
			double inputPubRate = i;
			double outputPubRate = 100 * i;
			
			TPingReply update = new TPingReply(pTime, cpu, inputPubRate, outputPubRate, null);
			rttLogger.updatefromTPingReply(localAddress, update);
		
			rttLogger.runMe();
			System.out.print(cpu + "\t" + inputPubRate + "\t" + outputPubRate + ":\t" + rttLogger);
			rttLogger.clearWriter();
		}
	}
}

class RTTLogger_ForTest extends RTTLogger {
	
	public RTTLogger_ForTest(BrokerShadow brokerShadow) {
		super(brokerShadow);
	}
	
	@Override
	public void initializeFile() {
		_logFileWriter = new StringWriter();
		_initialized = true;
	}
	
	@Override
	public String toString() {
		return _logFileWriter.toString();
	}
	
	public void clearWriter() {
		((StringWriter)_logFileWriter).getBuffer().setLength(0);
	}
	
	@Override
	public void runMe() throws IOException {
		super.runMe();
	}
}
