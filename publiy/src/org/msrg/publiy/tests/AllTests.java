package org.msrg.publiy.tests;

import org.msrg.publiy.communication.core.niobinding.TCPConInfoNonListeningTest;
import org.msrg.publiy.communication.core.niobinding.UDPConInfoNonListeningTest;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiationTest;

import org.msrg.publiy.broker.core.sequence.SequenceTest;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueueTest;
import org.msrg.publiy.pubsub.core.messagequeue.TimestampTest;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManagerTest;
import org.msrg.publiy.pubsub.core.overlaymanager.PathTest;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_SubscribeTest;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecoveryTest;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManagerTest;
import org.msrg.publiy.pubsub.multipath.loadweights.LoadWeightRepository_UnitTest;
import org.msrg.publiy.tests.trecovery.ISessionTest;
import org.msrg.publiy.tests.trecovery.TRecoveryJoinTest;
import org.msrg.publiy.tests.trecovery.TRecoveryJoinTester;
import org.msrg.publiy.tests.trecovery.TRecoverySubscriptionTest;
import org.msrg.publiy.tests.trecovery.TRecoverySubscriptionTester;
import org.msrg.publiy.utils.LocalAddressGrabberTest;
import org.msrg.publiy.utils.log.casuallogger.RTTLogger_Test;
import org.msrg.publiy.utils.log.casuallogger.exectime.ExecutionTime_unittest;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//$JUnit-BEGIN$

		suite.addTestSuite(TSessionInitiationTest.class);
		suite.addTestSuite(LocalAddressGrabberTest.class);
		suite.addTestSuite(ExecutionTime_unittest.class);
		suite.addTestSuite(MessageQueueTest.class);
		suite.addTestSuite(TRecoveryTest.class);
		suite.addTestSuite(OverlayManagerTest.class);
		suite.addTestSuite(RTTLogger_Test.class);
		suite.addTestSuite(TRecoveryJoinTester.class);
		suite.addTestSuite(TRecoveryJoinTest.class);
		suite.addTestSuite(TRecoverySubscriptionTest.class);
		suite.addTestSuite(SequenceTest.class);
		suite.addTestSuite(TimestampTest.class);
		suite.addTestSuite(PathTest.class);
		suite.addTestSuite(TMulticast_SubscribeTest.class);
		suite.addTestSuite(TRecoverySubscriptionTester.class);
		suite.addTestSuite(SubscriptionManagerTest.class);
		suite.addTestSuite(TRecoveryTest.class);
		suite.addTestSuite(ISessionTest.class);
		suite.addTestSuite(LoadWeightRepository_UnitTest.class);

		suite.addTestSuite(TCPConInfoNonListeningTest.class);
		suite.addTestSuite(UDPConInfoNonListeningTest.class);

//		suite.addTestSuite(TestL.class);
//		suite.addTestSuite(TestNL.class);
		
		//$JUnit-END$
		return suite;
	}

}
