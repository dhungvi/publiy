package org.msrg.publiy.tests.subscription;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.overlaymanager.IWorkingOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.NodeCache;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.WorkingOverlayManager;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.multipath.WorkingSubscriptionManager;
import org.msrg.publiy.tests.commons.FileCommons;
import org.msrg.publiy.utils.FileUtils;
import junit.framework.TestCase;

public class WorkingSubscriptionManagerTest extends TestCase {
	
	private final static String TOP_FILENAME = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "tops" + FileUtils.separatorChar + "OverlayManagerTest_1.top";
	private final static String SUB_FILENAME = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "subs" + FileUtils.separatorChar + "CompareSubSet_0.sub";
	private final int SEED = 100;

	private Random _rand = new Random(SEED);
	private InetSocketAddress[] _allAddresses;
	private Subscription[] _allSubscriptions;
	protected final int _delta = 5;
	protected InetSocketAddress _localAddress = Broker.b2Addresses[10];
	protected final LocalSequencer _localSequencer = LocalSequencer.init(null, _localAddress);
	
	@Override
	protected void setUp() throws Exception {
		System.setProperty("Broker.DELTA", "" + _delta);

//		InetSocketAddress dummyAddress = new InetSocketAddress("localhost", 50000);
//		LocalSequencer.init(null, dummyAddress);
		
		_allAddresses = FileCommons.getAllInetAddressesFromLines(_localSequencer,TOP_FILENAME);
		_allSubscriptions = Subscription.readSubscriptionsFromFile(SUB_FILENAME);
	}

	@Override
	protected void tearDown() throws Exception { }
	
	public void testAll(){
		Set<InetSocketAddress> activeRemoteAddressesSet = new HashSet<InetSocketAddress>();
		activeRemoteAddressesSet.add(Broker.b2Addresses[20]);
		activeRemoteAddressesSet.add(Broker.b2Addresses[11]);
		activeRemoteAddressesSet.add(Broker.b2Addresses[3]);
		activeRemoteAddressesSet.add(Broker.b2Addresses[00]);
		
		testOne(activeRemoteAddressesSet);
	}
	
	public void testOne(Set<InetSocketAddress> activeRemoteAddressesSet){
		TRecovery_Join[] trjs = ConnectionManagerFactory.readRecoveryTopology(_localSequencer, TOP_FILENAME);
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _localAddress).setDelta(Broker.DELTA).setMP(true).setDelta(_delta);
		IOverlayManager overlayManager = new OverlayManager(brokerShadow);
		overlayManager.applyAllJoinSummary(trjs);
		overlayManager.initializeOverlayManager();
		
		System.out.println(Broker.DELTA + " " + overlayManager);
		
		IWorkingOverlayManager wOverlayManager = new WorkingOverlayManager(0, overlayManager, activeRemoteAddressesSet);
		System.out.println(Broker.DELTA + " " + wOverlayManager);
		
		NodeCache[]cNodes = wOverlayManager.initializeOverlayManager();
		assertTrue(activeRemoteAddressesSet.size() == cNodes.length); // local has been added to activeRemoteAddressesSet
		
		
		ISubscriptionManager masterSubManager = new SubscriptionManager(brokerShadow, "." + FileUtils.separatorChar + "h", true);
		for ( int i=0 ; i<_allSubscriptions.length ; i++ ){
			int randI = _rand.nextInt(_allAddresses.length);
			InetSocketAddress randFrom = _allAddresses[randI];
			TRecovery_Subscription trs = new TRecovery_Subscription(_allSubscriptions[i], null, randFrom);
			masterSubManager.applySummary(trs);
		}
		System.out.println(masterSubManager);
		
		WorkingSubscriptionManager workingSubManager = new WorkingSubscriptionManager(0, masterSubManager, wOverlayManager);
		System.out.println(workingSubManager);
	}
}
