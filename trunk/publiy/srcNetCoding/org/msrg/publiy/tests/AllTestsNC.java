package org.msrg.publiy.tests;

import org.msrg.raccoon.engine.CodingEngineTest;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerNCTest;
import org.msrg.publiy.broker.core.contentManager.ContentBreakPolicyTest;
import org.msrg.publiy.broker.core.contentManager.ContentManagerTest;
import org.msrg.publiy.broker.networkcoding.connectionManager.client.ConnectionManagerNC_ClientTest;
import org.msrg.publiy.networkcodes.PSCodedPieceTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTestsNC {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTestsNC.class.getName());
		//$JUnit-BEGIN$

		suite.addTestSuite(PSCodedPieceTest.class);
		suite.addTestSuite(ContentBreakPolicyTest.class);
		suite.addTestSuite(CodingEngineTest.class);
		suite.addTestSuite(ConnectionManagerNC_ClientTest.class);
		suite.addTestSuite(ContentManagerTest.class);
		suite.addTestSuite(ConnectionManagerNCTest.class);
		
		//$JUnit-END$
		return suite;
	}

}
