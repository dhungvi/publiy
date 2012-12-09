package org.msrg.publiy.communication.core.niobinding;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BIOBindingImpRegressionTests {

	static final int RUNS_COUNT = 10;
	
	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for communication.core.niobinding");
		//$JUnit-BEGIN$
		
		for(int i=0 ; i<RUNS_COUNT ; i++) {
			suite.addTestSuite(UDPConInfoNonListeningTest.class);
			suite.addTestSuite(TCPConInfoNonListeningTest.class);
		}
		
		//$JUnit-END$
		return suite;
	}

}
