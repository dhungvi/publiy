package org.msrg.publiy.profiling;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;


public class PercentileBasedPublicationSubscriptionGenerator extends AbstractPublicationSubscriptionGenerator {

	final int _PUBS_TO_SUBS_MATCHING_PERCENTAGE;
	
	PercentileBasedPublicationSubscriptionGenerator(int pubsCount,
			int subsCount, int pubsGroupsCount, int subsGroupsCount,
			int pubSubMatchingPercentage) {
		super(pubsCount, subsCount, pubsGroupsCount, subsGroupsCount);
		
		_PUBS_TO_SUBS_MATCHING_PERCENTAGE = pubSubMatchingPercentage;
	}

	@Override
	public void generate() {
		int pubsCounter = 0;
		
		for(int i=0 ; i<_PUBS_GROUPS_COUNT ; i++) {
			for(int j=0 ; j<_PUBS_COUNT ; j++) {
				Publication pub = new Publication();
				pub.addPredicate(_CLASS_ATTR, _CLASS_VAL);
				pubsCounter++;
				pub.addPredicate(_PUB_COUNTER_ATTR, pubsCounter);;
				
				_PUBLICATIONS[i][j] = pub;
				
				SimplePredicate pubCounterPred =
						SimplePredicate.buildSimplePredicate(_PUB_COUNTER_ATTR, '=', pubsCounter);
				Subscription sub = new Subscription();
				sub.addPredicate(_PRED_CLASS);
				sub.addPredicate(pubCounterPred);
				
				for(int k=0 ; k<_PUBS_TO_SUBS_MATCHING_PERCENTAGE ; k++) {
					int subGroup = _rand.nextInt(_SUBS_GROUPS_COUNT);
					while ( subGroup <_SUBS_GROUPS_COUNT && _SUBS_GENERATE_COUNTER[subGroup] >= _SUBS_COUNT)
						subGroup++;
					if(subGroup == _SUBS_GROUPS_COUNT) {
						subGroup = 0;
						while ( subGroup <_SUBS_GROUPS_COUNT && _SUBS_GENERATE_COUNTER[subGroup] >= _SUBS_COUNT)
							subGroup++;
					}
					if(subGroup == _SUBS_GROUPS_COUNT) {
						generateRestAsRepeatedPublications();
						return;
					}
					
					_SUBSCRIPTIONS[subGroup][_SUBS_GENERATE_COUNTER[subGroup]] = sub;
					_SUBS_GENERATE_COUNTER[subGroup]++;
				}
			}
		}
		
		generateRestAsNullSubscriptions();
	}
	
	public static void main(String[] argv) {
		// General parameters
		int PUB_COUNT = 10;
		int PUB_GROUPS = 10;
		int SUBS_COUNT = 10;
		int SUBS_GROUPS = 10;
		final String outputDir = "c:\\temp\\data\\";
		
		// Class-specific parameters
		int MATCHING_PERCENTAGE = 100;
		
		AbstractPublicationSubscriptionGenerator pubSubGenerator = 
			new PercentileBasedPublicationSubscriptionGenerator(
					PUB_COUNT, SUBS_COUNT, PUB_GROUPS, SUBS_GROUPS, MATCHING_PERCENTAGE);
		
		pubSubGenerator.generate();
		pubSubGenerator.printAll();
//		pubSubGenerator.printPublicationMatching();
//		pubSubGenerator.printSubscriptionMatching();
		pubSubGenerator.printSummary();
		String summary2 = pubSubGenerator.printSummary2();
		System.out.println(summary2);
		
		pubSubGenerator.dumpSubscriptionsInFile(
				outputDir + "SubFile-(" + MATCHING_PERCENTAGE + ")", ".sub");
		pubSubGenerator.dumpPubicationsInFile(
				outputDir + "PubFile-(" + MATCHING_PERCENTAGE + ")", ".pub");
	}
}
