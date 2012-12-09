package org.msrg.publiy.publishSubscribe;

import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;

import junit.framework.TestCase;

public class SubscriptionTest extends TestCase {

	private Subscription _subscription;
	private String _encodedStr;
	private String _toStringStr;
	private String _decodeStr = "ATTR9=19,ATTR8<19,ATTR8>17";
	
	@Override
	public void setUp() {
		_subscription = new Subscription();
		SimplePredicate sp1 = SimplePredicate.buildSimplePredicate("saalaaam", '=', 10000);
		_subscription.addPredicate(sp1);
		
		SimplePredicate sp2 = SimplePredicate.buildSimplePredicate("count", '%', 3);
		_subscription.addPredicate(sp2);
		
		_encodedStr = Subscription.encode(_subscription);
		_toStringStr = _subscription.toString();
	}
	
	public void testEncode() {
		assertTrue(_encodedStr != null);
		assertTrue(_encodedStr.contains("	saalaaam	=	10000 , 	count	%	3 , "));
		System.out.println("TEST_OK! " + _encodedStr);
	}

	public void testDecode() {
		Subscription decodedSubscription = Subscription.decode(_decodeStr);
		assertTrue(decodedSubscription != null);
		assertTrue(decodedSubscription._predicates.size() == 3);
		for(SimplePredicate sp : decodedSubscription._predicates) {
			if(sp._attribute.equals("ATTR9")) {
				assertTrue(sp._operator == '=');
				assertTrue(sp._intValue == 19);
			} else if(sp._attribute.equals("ATTR8")) {
				if(sp._operator == '<')
					assertTrue(sp._intValue == 19);
				else if(sp._operator == '>')
					assertTrue(sp._intValue == 17);
			} else {
				fail("Invalid attribute: " + sp);
			}
		}

		Subscription encodedStrDecodedSubscription =
			Subscription.decode(_encodedStr);
		assertTrue(encodedStrDecodedSubscription.equals(_subscription));
		System.out.println("TEST_OK! \t" + encodedStrDecodedSubscription);
		
		
		Subscription toStringStrDecodedSubscription = Subscription.decode(_toStringStr);
		assertTrue(toStringStrDecodedSubscription.equals(_subscription));
		System.out.println("TEST_OK! " + toStringStrDecodedSubscription);
	}
}
