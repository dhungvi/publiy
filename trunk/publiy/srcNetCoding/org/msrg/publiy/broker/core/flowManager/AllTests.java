package org.msrg.publiy.broker.core.flowManager;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for broker.core.flowManager");
		//$JUnit-BEGIN$
		suite.addTestSuite(FlowTest.class);
		suite.addTestSuite(AllFlowsTest.class);
		suite.addTestSuite(FlowManagerTest.class);
		//$JUnit-END$
		return suite;
	}

}
