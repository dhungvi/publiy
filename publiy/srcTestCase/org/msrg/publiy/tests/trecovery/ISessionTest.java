package org.msrg.publiy.tests.trecovery;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import junit.framework.TestCase;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueue;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManager;

import org.msrg.publiy.utils.FileUtils;

import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.communication.core.sessions.SessionTypes;

import org.msrg.publiy.broker.core.IConnectionManagerDebug;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerPS;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class ISessionTest extends TestCase {

	final static String TOP_FILENAME = "." + FileUtils.separatorChar + "misc" + FileUtils.separatorChar + "testdata" + FileUtils.separatorChar + "tops" + FileUtils.separatorChar + "OverlayManagerTest_1.top";
	
	protected final InetSocketAddress _localAddress = Broker.b2Addresses[10];
	protected IBrokerShadow _brokerShadow;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
		_brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _localAddress).setDelta(3);
	}
	
	@Override
	public void tearDown() { }
	
	public void testSessionFailure() throws IOException, InterruptedException {
		IOverlayManager overlayManager = new OverlayManager(_brokerShadow);
		ISubscriptionManager subscriptionManager = new SubscriptionManager(_brokerShadow, null, false);
		IMessageQueue mq = new MessageQueue(_brokerShadow, null, overlayManager, subscriptionManager, true);
		IConnectionManagerDebug connMan = new ConnectionManagerPS_ForTest(_brokerShadow, overlayManager, subscriptionManager, mq);
		
		TRecovery_Join[] trjs = ConnectionManagerFactory.readRecoveryTopology(_brokerShadow.getLocalSequencer(), TOP_FILENAME);
		overlayManager.applyAllJoinSummary(trjs);
		overlayManager.initializeOverlayManager();
		connMan.prepareToStart();
		connMan.startComponent();
		
		ISession[] defaultSessions = ISession.getDummyISessions(_brokerShadow, Broker.b2Addresses, SessionTypes.ST__PUBSUB_PEER_PUBSUB, SessionConnectionType.S_CON_T_ACTIVE);
		
		ISession[] sessions = {defaultSessions[11], defaultSessions[9], defaultSessions[1]};
		for ( int i=0 ; i<sessions.length ; i++ )
			connMan.registerSession(sessions[i]);
		
		{
			ISession[] allSessions = connMan.getAllSessions();
			assertEquals(sessions.length, allSessions.length);
		}
		
		connMan.failed(defaultSessions[1]);
		// Don't rush! Wait for processing to finish :)
		for(int i=0 ; i<100 ; i++)
			if(connMan.getConnectionEventsCount() == 0)
				break;
			else
				Thread.sleep(100);
		
		Thread.sleep(100);
		{
			ISession[] allSessions = connMan.getAllSessions();
			assertEquals(sessions.length + 2, allSessions.length);
			
			ISession session1 = connMan.getSessionPrivately(defaultSessions[1].getRemoteAddress());
			assertEquals(SessionConnectionType.S_CON_T_BEINGREPLACED, session1.getSessionConnectionType());

			ISession session4 = connMan.getSessionPrivately(new InetSocketAddress("127.0.0.1", 2004));
			assertEquals(SessionConnectionType.S_CON_T_REPLACING, session4.getSessionConnectionType());

			ISession session13 = connMan.getSessionPrivately(new InetSocketAddress("127.0.0.1", 2013));
			assertEquals(SessionConnectionType.S_CON_T_REPLACING, session13.getSessionConnectionType());

			ISession session9 = connMan.getSessionPrivately(defaultSessions[9].getRemoteAddress());
			assertEquals(SessionConnectionType.S_CON_T_ACTIVE, session9.getSessionConnectionType());

			ISession session11 = connMan.getSessionPrivately(defaultSessions[11].getRemoteAddress());
			assertEquals(SessionConnectionType.S_CON_T_ACTIVE, session11.getSessionConnectionType());
		}
	}
}


class ConnectionManagerPS_ForTest extends ConnectionManagerPS implements IConnectionManagerDebug {

	protected ConnectionManagerPS_ForTest(
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager, 
			IMessageQueue mq)
			throws IOException {
		super(brokerShadow, overlayManager, subscriptionManager, mq);
	}

	@Override
	public int getConnectionEventsCount() {
		synchronized(_connectionEvents) {
			return _connectionEvents.size();
		}
	}
	
	@Override
	public ISession[] getAllSessions() {
		return super.getAllSessions();
	}
	
	@Override
	public ISession getSessionPrivately(InetSocketAddress remote) {
		return super.getSessionPrivately(remote);
	}
}