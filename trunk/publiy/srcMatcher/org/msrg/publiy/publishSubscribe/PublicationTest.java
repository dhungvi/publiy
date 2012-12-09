package org.msrg.publiy.publishSubscribe;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import junit.framework.TestCase;

public class PublicationTest extends TestCase {

	@Override
	public void setUp() {
		BrokerInternalTimer.start(false);
	}
	
	public void testEndcodeDecodeIntegerPredicates() {
		Publication publication =
			new Publication().addPredicate("salam", 10000);;
		String str = Publication.encode(publication);

		Publication publication2 = Publication.decode(str);
		assertTrue(publication.equals(publication2));
		
		BrokerInternalTimer.inform("OK!");
	}
	
	public void testEndcodeDecodeStringPredicates() {
		Publication publication =
			new Publication().addStringPredicate("salam", "Hi");
		String str = Publication.encode(publication);

		Publication publication2 = Publication.decode(str);
		assertTrue(publication.equals(publication2));
		
		BrokerInternalTimer.inform("OK!");
	}
	
	public void testEndcodeDecodeIntegerStringPredicates() {
		Publication publication =
			new Publication().addStringPredicate("salam", "Hi").addPredicate("salaam", 123);
		String str = Publication.encode(publication);

		Publication publication2 = Publication.decode(str);
		assertTrue(publication.equals(publication2));
		
		BrokerInternalTimer.inform("OK!");
	}
}
