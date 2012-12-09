package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import org.msrg.publiy.publishSubscribe.Subscription;

import junit.framework.TestCase;

public class SubscrptionCoveringComputerTest extends TestCase {

	protected String[] _coveredSubStrs = new String[3];
	protected String[] _coveringSubStrs = new String[3];
	protected int i=0;
	
	@Override
	public void setUp() {
		_coveredSubStrs[i] = "Sub([ATTR11=569, ATTR10<520, ATTR10>518, ATTR13=685, ATTR12=606, ATTR15=772, ATTR14=724, ATTR17=881, ATTR16=826, ATTR19=979, ATTR18=906])";
		_coveringSubStrs[i++] = "Sub([ATTR12=606, ATTR11<571, ATTR11>562, ATTR13=685, ATTR15=772, ATTR14=724, ATTR17=881, ATTR16=826, ATTR19=979, ATTR18=906])";
		
		_coveredSubStrs[i] = "Sub([ATTR10=519, ATTR11=569, ATTR13=685, ATTR12=606, ATTR15=772, ATTR14=724, ATTR17=881, ATTR16=826, ATTR19=979, ATTR18=906])";
		_coveringSubStrs[i++] = "Sub([ATTR11=569, ATTR10<520, ATTR10>518, ATTR13=685, ATTR12=606, ATTR15=772, ATTR14=724, ATTR17=881, ATTR16=826, ATTR19=979, ATTR18=906])";
		
		_coveredSubStrs[i] = "Sub([ATTR11=569, ATTR10<520, ATTR10>518, ATTR13=685, ATTR12=606, ATTR15=772, ATTR14=724, ATTR17=881, ATTR16=826, ATTR19=979, ATTR18=906])";
		_coveringSubStrs[i++] = "Sub([ATTR10=519, ATTR11=569, ATTR13=685, ATTR12=606, ATTR15=772, ATTR14=724, ATTR17=881, ATTR16=826, ATTR19=979, ATTR18=906])";
	}
	
	public void testCoveringComputation() {
		for(int k=1 ; k<i ; k++) {
			Subscription coveringSub = Subscription.decode(_coveringSubStrs[k]);
			Subscription coveredSub = Subscription.decode(_coveredSubStrs[k]);
			
			assertTrue(coveredSub.equalsExactly(SubscrptionCoveringComputer.computeCovering(coveredSub, coveredSub)));
			assertTrue(coveringSub.equalsExactly(SubscrptionCoveringComputer.computeCovering(coveringSub, coveringSub)));
			
			assertTrue(coveringSub.equalsExactly(SubscrptionCoveringComputer.computeCovering(coveringSub, coveredSub)));
		}
	}
}
