package org.msrg.publiy.pubsub.core.packets;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for pubsub.core.packets");
		//$JUnit-BEGIN$
		suite.addTestSuite(TNetworkCoding_CodedPieceTest.class);
		suite.addTestSuite(TNetworkCoding_CodedPieceIdTest.class);
		suite.addTestSuite(TNetworkCoding_CodedPieceIdReqTest.class);
		//$JUnit-END$
		return suite;
	}

}
