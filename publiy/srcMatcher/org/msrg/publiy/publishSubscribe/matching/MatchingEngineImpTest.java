package org.msrg.publiy.publishSubscribe.matching;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.publishSubscribe.Advertisement;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;

import junit.framework.TestCase;

public class MatchingEngineImpTest extends TestCase {

	MatchingEngine _me;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start(false);
		_me = new MatchingEngineImp();
	}
	
	public void testAdvMatching(){
		Advertisement adv = new Advertisement();

		SimplePredicate sp1 = SimplePredicate.buildSimplePredicate("ATT1", '>', 1);
		SimplePredicate sp2 = SimplePredicate.buildSimplePredicate("ATT2", '>', 0);
		adv.addPredicate(sp1); adv.addPredicate(sp2);
		
//		SimplePredicate ssp1 = SimplePredicate.buildSimplePredicate("ATT1", '=', 1);
//		SimplePredicate ssp2 = SimplePredicate.buildSimplePredicate("ATT2", '=', 1);
//		sub.addPredicate(ssp1); pub.addPredicate(ssp2);
		
		{
			Publication pub1 = new Publication().addPredicate("ATT1", 1).addPredicate("ATT2", 2);
			assertFalse(_me.match(adv, pub1));
		}
		
		{
			Publication pub2 = new Publication().addPredicate("ATT1", 2);
			assertTrue(_me.match(adv, pub2));
		}

		adv.addStringPredicate(SimpleStringPredicate.buildSimpleStringPredicate("ATT1", '~', "HI"));
		
		{
			Publication pub3 = new Publication().addPredicate("ATT1", 2);
			assertTrue(_me.match(adv, pub3));
		}
		
		{
			Publication pub4 = new Publication().addStringPredicate("ATT1", "HI").addPredicate("ATT1", 2);
			assertTrue(_me.match(adv, pub4));
		}

		{
			Publication pub4 = new Publication().addStringPredicate("ATT1", "Bye").addPredicate("ATT1", 2);
			assertFalse(_me.match(adv, pub4));
		}

		BrokerInternalTimer.inform("OK!");
	}
	
	public void testSubMatching(){
		Advertisement adv = new Advertisement();
		SimplePredicate sp1 = SimplePredicate.buildSimplePredicate("ATT1", '=', 1);
		SimplePredicate sp2 = SimplePredicate.buildSimplePredicate("ATT2", '>', 0);
		adv.addPredicate(sp1); adv.addPredicate(sp2);

		Subscription sub = new Subscription();		
		SimplePredicate ssp1 = SimplePredicate.buildSimplePredicate("ATT1", '=', 1);
		SimplePredicate ssp2 = SimplePredicate.buildSimplePredicate("ATT2", '=', 1);
		sub.addPredicate(ssp1); sub.addPredicate(ssp2);
		
		assertTrue(_me.match(adv, sub));
		BrokerInternalTimer.inform("OK!");
	}
	
	public void testPubMatching() {
		Subscription sub = new Subscription();
		SimplePredicate sp1 = SimplePredicate.buildSimplePredicate("ATT1", '%', 3);
		SimplePredicate sp2 = SimplePredicate.buildSimplePredicate("ATT1", '%', 5);
		sub.addPredicate(sp1); sub.addPredicate(sp2);
		
		for ( int i=1 ; i<100 ; i++ ) {
			Publication pub = new Publication();
			pub.addPredicate("ATT1", i);// pub.addPredicate("ATT2", 2);
			if(i%3 == 0 && i%5 == 0)
				assertTrue(_me.match(sub, pub));
			else
				assertFalse(_me.match(sub, pub));
		}
		
		BrokerInternalTimer.inform("OK!");
	}
	
	public void testPubStringMatching() {
		Subscription sub = new Subscription();
		SimplePredicate sp1 = SimplePredicate.buildSimplePredicate("ATT1", '%', 3);
		SimplePredicate sp2 = SimplePredicate.buildSimplePredicate("ATT1", '%', 5);
		SimpleStringPredicate ssp1 = SimpleStringPredicate.buildSimpleStringPredicate("ATT1", '~', "HI");
		sub.addPredicate(sp1); sub.addPredicate(sp2); sub.addStringPredicate(ssp1);

		for ( int i=1 ; i<100 ; i++ ) {
			Publication pub = new Publication();
			pub.addPredicate("ATT1", i);// pub.addPredicate("ATT2", 2);
			assertFalse(_me.match(sub, pub));
		}
		
		for ( int i=1 ; i<100 ; i++ ) {
			Publication pub =
				new Publication().addPredicate("ATT1", i).addStringPredicate("ATT1", "HI").addStringPredicate("ATT2", "Byte");
			if(i%3 == 0 && i%5 == 0)
				assertTrue(_me.match(sub, pub));
			else
				assertFalse(_me.match(sub, pub));

		}
		
		BrokerInternalTimer.inform("OK!");
	}
	
	public void testMultipleAttributes() {
		String pubStr =
			"u       71151  u       187664         u       268373         u       205734         u       290398         u       108614";
		String matchingSubStr =
			"        u       =       187664 ,";
		String nonmatchingSubStr =
			"        u       =       322759 ,";
		
		Publication pub = Publication.decode(pubStr);
		Subscription matchingSub = Subscription.decode(matchingSubStr);
		Subscription nonmatchingSub = Subscription.decode(nonmatchingSubStr);

		assertTrue(_me.match(matchingSub, pub));
		assertFalse(_me.match(nonmatchingSub, pub));
		
		return;
	}
}
