package org.msrg.publiy.pubsub.core.subscriptionmanager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.publishSubscribe.Subscription;


import junit.framework.TestCase;

public class SubscriptionEntryTest extends TestCase {

	protected BrokerIdentityManager _idManager;
	protected InetSocketAddress _srcIp = new InetSocketAddress(2000);
	protected String _srcId = "p0";
	protected String _subEntryStr;
	
	public void setUp() {
		_idManager = new BrokerIdentityManager(_srcIp, 1);
		_idManager.loadId(_srcId + "\t" + _srcIp);
		_subEntryStr = _srcId + ":0:4802       " +
								_srcId + "      " +
								"(       ATTR1   =       3 " +
								",     ATTR0   <       3 " +
								",     ATTR0   >       1 , )";
	}
	
	public void testDecoding() {
		SubscriptionEntry subEntry = SubscriptionEntry.decode(_subEntryStr, _idManager);
		System.out.println(subEntry);
		
		Subscription sub = subEntry.getSubscription();
		assertTrue(sub._predicates.size() == 3);
		assertTrue(sub.hasPredicate("ATTR1", '=', 3));
		assertTrue(sub.hasPredicate("ATTR0", '<', 3));
		assertTrue(sub.hasPredicate("ATTR0", '>', 1));
	}
}
