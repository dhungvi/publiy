package org.msrg.publiy.profiling;

import java.util.Random;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;


public class GroupBasedPublicationSubscriptionGenerator extends AbstractPublicationSubscriptionGenerator {

	public final int _SUBGROUPS_MATCHING_A_PUB;
	public final int _SUBS_IN_A_SUBGROUP_MATCHING_A_PUB;
	public final double _PERCENTILE;
	
	GroupBasedPublicationSubscriptionGenerator(int pubsCount, // int subsCount,
			int pubsGroupsCount, int subsGroupsCount, int subgroupsMatchingAPub, int subsInASubgroupMatchingAPub,
			int percentile) {
		super(
				pubsCount, 
				((pubsCount * pubsGroupsCount * subgroupsMatchingAPub * subsInASubgroupMatchingAPub) / subsGroupsCount),
				pubsGroupsCount,
				subsGroupsCount);
		_SUBGROUPS_MATCHING_A_PUB = subgroupsMatchingAPub;
		_SUBS_IN_A_SUBGROUP_MATCHING_A_PUB = subsInASubgroupMatchingAPub;
		_PERCENTILE = percentile;
	}
	
	@Override
	public void generate() {
		int subGroupIndex = 0;
		int pubsCounter = 0;
		
		for(int i=0 ; i<_PUBS_GROUPS_COUNT ; i++) {
			for(int j=0 ; j<_PUBS_COUNT ; j++) {
				Publication pub = new Publication();
				pubsCounter++;
				pub.addPredicate(_CLASS_ATTR, pubsCounter);
				
				_PUBLICATIONS[i][j] = pub;
				
				SimplePredicate classPred = SimplePredicate.buildSimplePredicate(_CLASS_ATTR, '=', pubsCounter);
				
				for(int k=0 ; k<_SUBGROUPS_MATCHING_A_PUB ; k++) {
					for(int l=0 ; l<_SUBS_IN_A_SUBGROUP_MATCHING_A_PUB ; l++) {
						if(_SUBS_GENERATE_COUNTER[subGroupIndex] < _SUBS_COUNT) {
							float rand = _rand.nextFloat();
							if(rand > (_PERCENTILE / 100)) {
								System.out.println(rand + "%");
								continue;
							}
							
							Subscription newSubscription = new Subscription();
							newSubscription.addPredicate(classPred);
							
							_SUBSCRIPTIONS[subGroupIndex][_SUBS_GENERATE_COUNTER[subGroupIndex]] = newSubscription;
							_SUBS_GENERATE_COUNTER[subGroupIndex]++;
						}
						
					}
					subGroupIndex = (subGroupIndex + 1) % _SUBS_GROUPS_COUNT;
				}
			}
		}
	}
	
	public static GroupBasedPublicationSubscriptionGenerator runGeneratorOne(int pubsCount, int pubsGroups, int subsGroups) {
		// Class-specific parameters
		int SUBGROUPS_MATCHING_A_PUB = 1;
		int SUBS_IN_A_SUBGROUP_MATCHING_A_PUB = subsGroups;
		
		GroupBasedPublicationSubscriptionGenerator pubSubGenerator = 
			new GroupBasedPublicationSubscriptionGenerator(
					pubsCount,  
					pubsGroups, subsGroups, 
					SUBGROUPS_MATCHING_A_PUB, SUBS_IN_A_SUBGROUP_MATCHING_A_PUB,
					100);
		
		pubSubGenerator.generate();
		return pubSubGenerator;
	}
	
	Random _rand = new Random(100);
	public static GroupBasedPublicationSubscriptionGenerator runGeneratorAll(
			int pubsCount, int pubsGroups, int subsGroups, int percentile) {
		// Class-specific parameters
		int SUBGROUPS_MATCHING_A_PUB = subsGroups;
		int SUBS_IN_A_SUBGROUP_MATCHING_A_PUB = 1;
		
		GroupBasedPublicationSubscriptionGenerator pubSubGenerator = 
			new GroupBasedPublicationSubscriptionGenerator(
					pubsCount,  
					pubsGroups, subsGroups, 
					SUBGROUPS_MATCHING_A_PUB, SUBS_IN_A_SUBGROUP_MATCHING_A_PUB,
					percentile);
		
		pubSubGenerator.generate();
		return pubSubGenerator;
	}
	
	public static void main(String[] argv) {
		int pubsCount = 27;
		int pubsGroups = 27;
		int subsGroups = 26;
		int percentile = 1;
		final String outputDir = "c:\\runs\\x\\f10\\data" + pubsCount + "." + percentile + "\\";
		
		GroupBasedPublicationSubscriptionGenerator pubSubGenerator = runGeneratorOne(pubsCount, pubsGroups, subsGroups);
//		GroupBasedPublicationSubscriptionGenerator pubSubGenerator = 
//			runGeneratorAll(pubsCount, pubsGroups, subsGroups, percentile);
		
//		pubSubGenerator.printAll();
//		pubSubGenerator.printSummary();
//		String summary2 = pubSubGenerator.printSummary2();
//		System.out.println(summary2);
		
		pubSubGenerator.dumpSubscriptionsInFile(outputDir + "SubFile-fout10-8-27", ".sub");
		pubSubGenerator.dumpPubicationsInFile(outputDir + "PubFile-fout10-8-27", ".pub");
//		pubSubGenerator.dumpSubscriptionsInFile(outputDir + "SubFile-(" + pubSubGenerator._SUBGROUPS_MATCHING_A_PUB + "-" + pubSubGenerator._SUBS_IN_A_SUBGROUP_MATCHING_A_PUB + ")", ".sub");
//		pubSubGenerator.dumpPubicationsInFile(outputDir + "PubFile-(" + pubSubGenerator._SUBGROUPS_MATCHING_A_PUB + "-" + pubSubGenerator._SUBS_IN_A_SUBGROUP_MATCHING_A_PUB + ")", ".pub");
	}
}
