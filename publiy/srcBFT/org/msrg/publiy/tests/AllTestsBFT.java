package org.msrg.publiy.tests;

import org.msrg.publiy.communication.core.packet.types.TBFTMessageManipulator_FilterPublicationTest;

import org.msrg.publiy.publishSubscribe.BFTPublicationTest;
import org.msrg.publiy.pubsub.core.messagequeue.BFTMessageQueueTest;
import org.msrg.publiy.pubsub.core.overlaymanager.BFTDackTest;
import org.msrg.publiy.pubsub.core.overlaymanager.BFTOverlayManagerTest;
import org.msrg.publiy.pubsub.core.overlaymanager.BFTOverlayNodeTest;
import org.msrg.publiy.pubsub.core.packets.multicast.BFTTMulticast_PublishTest;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT_HeartBeatTest;
import org.msrg.publiy.utils.FileUtilsTest;
import org.msrg.publiy.utils.security.dsa.DigestUtilsTest;
import org.msrg.publiy.broker.BrokerIdentityManagerTest;
import org.msrg.publiy.broker.core.connectionManager.BFTConnectionManagerTest;
import org.msrg.publiy.broker.core.sequence.SequencePairTest;
import org.msrg.publiy.broker.core.sequence.SequenceTest;
import org.msrg.publiy.broker.core.sequence.SimpleBFTDackIssuerProxyTest;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxyTest;
import org.msrg.publiy.broker.core.sequence.SimpleBFTVerfiableTest;
import org.msrg.publiy.broker.core.sequence.SlidingWindowBFTDackIssuerProxyTest;
import org.msrg.publiy.broker.security.keys.KeyManagerTest;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTestsBFT {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTestsBFT.class.getName());
		//$JUnit-BEGIN$
		
		suite.addTestSuite(SequenceTest.class);
		suite.addTestSuite(SequencePairTest.class);
		suite.addTestSuite(SimpleBFTIssuerProxyTest.class);
		suite.addTestSuite(SimpleBFTVerfiableTest.class);
		suite.addTestSuite(KeyManagerTest.class);
		suite.addTestSuite(BFTPublicationTest.class);
		suite.addTestSuite(BFTOverlayNodeTest.class);
		suite.addTestSuite(DigestUtilsTest.class);
		suite.addTestSuite(BFTTMulticast_PublishTest.class);
		suite.addTestSuite(BFTOverlayManagerTest.class);
		suite.addTestSuite(FileUtilsTest.class);
		suite.addTestSuite(BrokerIdentityManagerTest.class);
		suite.addTestSuite(TMulticast_Publish_BFT_HeartBeatTest.class);
		suite.addTestSuite(BFTDackTest.class);
		suite.addTestSuite(BFTConnectionManagerTest.class);
		suite.addTestSuite(SlidingWindowBFTDackIssuerProxyTest.class);
		
//		suite.addTestSuite(SimpleBFTDackVerifierProxyTest.class);
		suite.addTestSuite(SimpleBFTDackIssuerProxyTest.class);
		
		suite.addTestSuite(BFTMessageQueueTest.class);
		suite.addTestSuite(TBFTMessageManipulator_FilterPublicationTest.class);
		
		//$JUnit-END$
		return suite;
	}

}
