package org.msrg.publiy.tests;

import org.msrg.publiy.broker.core.connectionManager.AbstractConnectionManagerMP_Test;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueueNodeMP_unittest;
import org.msrg.publiy.pubsub.core.multipath.WorkingManagerBundles_UnitTest;
import org.msrg.publiy.tests.topology.WorkingOverlayManagerTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTestsMP {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTestsMP.class.getName());
		//$JUnit-BEGIN$
		
		suite.addTestSuite(AbstractConnectionManagerMP_Test.class);
		suite.addTestSuite(WorkingOverlayManagerTest.class);
		suite.addTestSuite(MessageQueueNodeMP_unittest.class);
		suite.addTestSuite(WorkingManagerBundles_UnitTest.class);
		//$JUnit-END$
		return suite;
	}

}
