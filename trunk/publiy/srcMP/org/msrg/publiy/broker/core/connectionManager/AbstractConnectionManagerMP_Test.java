package org.msrg.publiy.broker.core.connectionManager;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.msrg.publiy.communication.core.packet.types.TPingReply;
import org.msrg.publiy.communication.core.sessions.ISessionMP;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.SessionTypes;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueue;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManager;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.log.casuallogger.RTTLogger;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class AbstractConnectionManagerMP_Test extends TestCase {

	protected final int delta = 3;
	protected int port = 1000;
	protected InetSocketAddress localAddress;
	protected InetSocketAddress remotes[] = new InetSocketAddress[delta+1];
	
	@Override
	public void setUp() {
		SystemTime.resetTime();
		BrokerInternalTimer.start();
		localAddress = new InetSocketAddress(port++);
		LocalSequencer.init(null, localAddress);
		remotes[0] = localAddress;
		for(int i=1 ; i<delta+1 ; i++)
			remotes[i] = new InetSocketAddress(port++);
	}

	public void test() throws IOException {
		BrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(delta).setMP(true);
		LocalSequencer localSequencer = brokerShadow.getLocalSequencer();
		IOverlayManager overlayManager = new OverlayManager(brokerShadow);
		for(int i=1 ; i<delta+1 ; i++)
			overlayManager.applySummary(
					new TRecovery_Join(localSequencer.getNext(), remotes[i], NodeTypes.NODE_BROKER, remotes[i-1], NodeTypes.NODE_BROKER));
		
		ISubscriptionManager subscriptionManager = new SubscriptionManager(brokerShadow, null, false);
		IMessageQueue messageQueue = new MessageQueue(brokerShadow, null, overlayManager, subscriptionManager, true);
		AbstractConnectionManagerMP_ForTest conManMP =
			new AbstractConnectionManagerMP_ForTest(
					"ConManMP-test", ConnectionManagerTypes.CONNECTION_MANAGER_MULTIPATH,
					null, brokerShadow, overlayManager, subscriptionManager, messageQueue);
		
		RTTLogger_ForTest rttLogger = new RTTLogger_ForTest(brokerShadow);
		rttLogger.initializeFile();
		ISessionMP sessions[] = new ISessionMP[delta+1];
		for(int i=1 ; i<delta+1 ; i++) {
			sessions[i] = (ISessionMP) ISessionManager.createBaseSession(brokerShadow, SessionTypes.ST_PUBSUB_CONNECTED);
			sessions[i].setRemoteCC(remotes[i]);
		}
		
		for(int i=0 ; i<10 ; i++) {
			SystemTime.setSystemTime(Broker.TPING_SEND_INTERVAL * i);
			rttLogger.updatefromTPingReply(remotes[1], new TPingReply(0, 0, 150, 150, null));
			rttLogger.updatefromTPingReply(remotes[2], new TPingReply(0, 0, 100, 100, null));
			rttLogger.updatefromTPingReply(remotes[3], new TPingReply(0, 0, 50, 50, null));
			rttLogger.runMe();
			
			double[] nfactors = 
				printfactors(conManMP, delta, rttLogger, overlayManager, sessions);
			
			print(nfactors, false);
			System.out.print("\t" + rttLogger);
			rttLogger.clearWriter();
		}
	}

	private void print(double[] nfactors, boolean endline) {
		for(int i=0 ; i<nfactors.length ; i++)
			System.out.print(i!=nfactors.length-1 ? (nfactors[i] + ",") : (nfactors[i] + (endline ? "\n" : "")));
	}

	private double[] printfactors(AbstractConnectionManagerMP_ForTest conManMP,
			int delta, RTTLogger rttLogger, IOverlayManager overlayManager,
			ISessionMP[] sessions) {
		double[] ret = new double[delta];
		double normalizationBase =
			conManMP.getAverageNormalizationBase(rttLogger, sessions);
		for(int i=1 ; i<delta+1 ; i++) {
			double nfactor =
				conManMP.getSessionNormalizationFactor(normalizationBase,
						rttLogger, overlayManager, sessions[i]);
			ret[i-1] = nfactor;
		}
		return ret;
	}
}

class AbstractConnectionManagerMP_ForTest extends AbstractConnectionManagerMP {

	public static final int BW_OUT = 100;
	static {
		System.setProperty("Broker.BW_OUT", "" + BW_OUT);
	}
	
	protected AbstractConnectionManagerMP_ForTest(String connectionManagerName,
			ConnectionManagerTypes type,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue) throws IOException {
		super(connectionManagerName, type, broker, brokerShadow, overlayManager, subscriptionManager, messageQueue, createToplogy(), null);
	}

	protected static TRecovery_Join[] createToplogy() {
		return null;
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