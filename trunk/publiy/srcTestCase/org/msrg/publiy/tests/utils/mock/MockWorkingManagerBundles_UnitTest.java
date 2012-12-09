package org.msrg.publiy.tests.utils.mock;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.PubForwardingStrategy;


import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.multipath.WorkingManagersBundle;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.WorkingRemoteSet;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManager;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import junit.framework.TestCase;

public class MockWorkingManagerBundles_UnitTest extends TestCase {
	
	protected WorkingManagersBundle _workingManagerBundle;
	protected IOverlayManager _masterOverlayManager;
	protected ISubscriptionManager _masterSubscriptionManager;
	protected final InetSocketAddress _localAddress = new InetSocketAddress("127.0.0.1", 2003);
	protected final String _topString =
		"S/127.0.0.1:2003 B/127.0.0.1:2000" + "\n" +
		"B/127.0.0.1:2000 B/127.0.0.1:2009" + "\n" +
		"B/127.0.0.1:2009 B/127.0.0.1:2010" + "\n" +
		"B/127.0.0.1:2010 B/127.0.0.1:2019" + "\n" +
		"B/127.0.0.1:2010 B/127.0.0.1:2011" + "\n" +
		"B/127.0.0.1:2010 B/127.0.0.1:2001" + "\n" +
		"B/127.0.0.1:2009 B/127.0.0.1:2018" + "\n" +
		"B/127.0.0.1:2018 P/127.0.0.1:2015" + "\n" +
		"B/127.0.0.1:2018 S/127.0.0.1:2006" + "\n" +
		"B/127.0.0.1:2000 P/127.0.0.1:2012";
	protected String _subString =
		"127.0.0.1:2000 word    =       1 ,     count   %       5," + "\n" +
		"127.0.0.1:2012 word    =       1 ,     count   %       5," + "\n" +
		"127.0.0.1:2009 word    =       1 ,     count   %       5," + "\n" +
		"127.0.0.1:2001 word    =       1 ,     count   %       5," + "\n" +
		"127.0.0.1:2011 word    =       1 ,     count   %       5," + "\n" +
		"127.0.0.1:2010 word    =       1 ,     count   %       5," + "\n" +
		"127.0.0.1:2018 word    =       1 ,     count   %       5,";
	protected TRecovery_Subscription[] _trs;
	
	protected WorkingRemoteSet _initialMatchingSet;
	protected WorkingRemoteSet _strategizedMatchingSet;
	protected final String _matchingAddresses =
		"127.0.0.1:2011" + "\n" +
		"127.0.0.1:2012" + "\n" +
		"127.0.0.1:2015";
	
	protected String _actSessionsStr = "127.0.0.1:2000";
	protected String _softSessionsStr = "127.0.0.1:2001,127.0.0.1:2015," +
		"127.0.0.1:2018,127.0.0.1:2000," +
		"127.0.0.1:2019,127.0.0.1:2006," +
		"127.0.0.1:2009,127.0.0.1:2010," +
		"127.0.0.1:2012";
	protected InetSocketAddress _localAddress1 = new InetSocketAddress("127.0.0.1", 2003);
	protected InetSocketAddress _localAddress2 = new InetSocketAddress("127.0.0.1", 2010);
	protected String _candSessionsStr = "127.0.0.1:2011";
	protected Set<ISession> _sessions = new HashSet<ISession>();
		
	public void setUp(){
		LoggerFactory.modifyLogger(null, LoggingSource.values(),
				null, LoggingSource.values(),
				null, LoggingSource.values(),
				null, LoggingSource.values(),
				null, LoggingSource.values());
		
	}

	protected void writeResults(Writer ioWriter) throws IOException {
		ioWriter.write("Master Overlay:\n");
		((OverlayManager)_masterOverlayManager).dumpOverlay(ioWriter);
	
		for(ISession session : _sessions)
			ioWriter.write(session.toString());
		
		((OverlayManager)_workingManagerBundle._workingOverlayManager).dumpOverlay(ioWriter);
		
		ioWriter.write("Working Subcsription manager:\n");
		((SubscriptionManager)_workingManagerBundle._workingSubscriptionManager).dumpSubscriptions(ioWriter);
	}
	
	public void tearDown() {
		Writer ioWriter = new StringWriter();

		try {
			writeResults(ioWriter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(ioWriter.toString());
	}
	
	public void testWorkingBundles() {
		_strategizedMatchingSet = _workingManagerBundle._workingOverlayManager.computeSoftLinksAddresses(null, PubForwardingStrategy.PUB_FORWARDING_STRATEGY_3, _initialMatchingSet);
		assert(true);
	}
	
	public static WorkingManagersBundle createWorkingManagerBundles(
			int delta,
			InetSocketAddress localAddress,
			String activeSessionsStr, String softSessionsStr, String candidateSessionsStr,
			String topLines, String subscriptionLines) {

		LocalSequencer localSequencer = LocalSequencer.init(null, localAddress);
		IOverlayManager masterOverlayManager = new OverlayManager(null);
		masterOverlayManager.applySummary(topLines);
		
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(delta).setMP(true);

		Set<ISession> sessions = new HashSet<ISession>();
		sessions.addAll(ISessionManager.getSessionObjectsSet(brokerShadow, activeSessionsStr, SessionConnectionType.S_CON_T_ACTIVE));
		sessions.addAll(ISessionManager.getSessionObjectsSet(brokerShadow, softSessionsStr, SessionConnectionType.S_CON_T_SOFT));
		sessions.addAll(ISessionManager.getSessionObjectsSet(brokerShadow, candidateSessionsStr, SessionConnectionType.S_CON_T_CANDIDATE));

		ISubscriptionManager masterSubscriptionManager = new SubscriptionManager(brokerShadow, null, true);
		TRecovery_Subscription[] trs = TRecovery_Subscription.createTRecoverySubscriptions(subscriptionLines, localSequencer, true);
		masterSubscriptionManager.applyAllSubscriptionSummary(trs);

		WorkingManagersBundle workingManagerBundle = WorkingManagersBundle.createWorkingManagersBundle(masterOverlayManager, sessions.toArray(new ISession[0]), masterSubscriptionManager);
		
		return workingManagerBundle;
	}
}
