package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngine;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngineImp;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;


public class CoveringMockSubscriptionGenerator {

	static final boolean DEBUG = false;
	final static int SUB_GROUPS = 15;
	static final int SEED = 10;
	static final Random _rand = new Random(SEED);
	
	public static CoveringDescriptor makeRandomCoveringDescriptor(
			int levels, int totalSize, double coveringDensity, double coveringSkip) {
		CoveringDescriptor coveringDescriptor = new CoveringDescriptor();
		
		for(int i=0 ; i<levels ; i++ ) {
			int size = (int) (totalSize/levels);
			CoveringLevel cLevel = new CoveringLevel(i, size, coveringDensity, coveringSkip);
			coveringDescriptor.addCoveringLevel(cLevel);
		}
		
		return coveringDescriptor;
	}

	public static CoveringDescriptor makeCoveringDescriptor() {
		CoveringDescriptor coveringDescriptor = new CoveringDescriptor();
		CoveringLevel l1 = new CoveringLevel(0, 10, 0.1, 0);
		coveringDescriptor.addCoveringLevel(l1);
		
		CoveringLevel l2 = new CoveringLevel(1, 5, 0.2,  0.1);
		coveringDescriptor.addCoveringLevel(l2);
		
		CoveringLevel l3 = new CoveringLevel(2, 5, 1.0, 0);
		coveringDescriptor.addCoveringLevel(l3);
		
		return coveringDescriptor;
	}
	
	public static MockCoveringSubscription createNewMockCoveringSubscription(int level) {
		return new MockCoveringSubscription(level);
	}
	
	public static Set<MockCoveringSubscription> generate(CoveringDescriptor cDescriptor) {
		Set<MockCoveringSubscription> subSet = new HashSet<MockCoveringSubscription>();

		int count = 0;
		int unmultpliedCount = 0;
		Vector<MockCoveringSubscription> lowerSubscriptions = new Vector<MockCoveringSubscription>();
		for(CoveringLevel level : cDescriptor._coveringLevels) {
			Vector<MockCoveringSubscription> thisLevelSubscription = new Vector<MockCoveringSubscription>();
			for(int i=0 ; i<level._size ; i++) {
				MockCoveringSubscription newSubscription = createNewMockCoveringSubscription(level._level);
				
				thisLevelSubscription.add(newSubscription);
				count += newSubscription.count();
				unmultpliedCount++;
			}

			int lowerLevelCoveredIndex = 0;
			for(int i=0 ; lowerLevelCoveredIndex<lowerSubscriptions.size() && i<level._size ; i++) {
				int coversCount = (int) Math.ceil(level._densityThatCovers * Math.abs(_rand.nextGaussian()) * lowerSubscriptions.size());
				int skipsCount = 1 + (int) Math.floor(level._densityThatSkips * Math.abs(_rand.nextGaussian()) * lowerSubscriptions.size());
				lowerLevelCoveredIndex += skipsCount;
				
				int randIndex = _rand.nextInt(level._size);
				MockSubscription subscription = thisLevelSubscription.get(randIndex);
				if(subscription.coveredCount() != 0)
					continue;
				
				int lowerLevelLastCoveredIndex = lowerLevelCoveredIndex + coversCount;
				for( ; lowerLevelCoveredIndex<lowerLevelLastCoveredIndex && lowerLevelCoveredIndex<lowerSubscriptions.size() ; lowerLevelCoveredIndex++){
					MockSubscription lowerSubscription = lowerSubscriptions.get(lowerLevelCoveredIndex);
					subscription.addCoveredSubscription(lowerSubscription);
				}
			}
			
//			int lowerLevelCovered = (int) (level._densityThatCovers * lowerSubscriptions.size() + 0.5);
//			for(int i=0 ; i<lowerLevelCovered && i<level._size ; i++) {
//				MockSubscription subscription = thisLevelSubscription.get(i);
//				int randIndex = _rand.nextInt(lowerLevelCovered);
//				MockSubscription lowerSubscription = lowerSubscriptions.get(randIndex);
//				
//				subscription.addCoveredSubscription(lowerSubscription);
//			}
				
				
			lowerSubscriptions = thisLevelSubscription;
			for(MockCoveringSubscription mockSub : thisLevelSubscription) {
				if(MockMultipliedCoveringSubscription.class.isAssignableFrom(mockSub.getClass())) {
					for(MockCoveringSubscription individualSub : ((MockMultipliedCoveringSubscription)mockSub)._subs)
						subSet.add(individualSub);
				}

				subSet.add(mockSub);
			}
			if(DEBUG)
				System.out.println("GENERATED " + level + "\t" + thisLevelSubscription);
		}

		cDescriptor.setCount(count);
		cDescriptor.setUnmultipliedCount(unmultpliedCount);
		return subSet;
	}
	
	public static Publication[] generatePubsForSubscriptions(
			int pubsCount, Collection<CoveringSubscription> subs){
		MatchingEngine me = new MatchingEngineImp();
		Publication[] pubs = generatePubsForSubscriptions(me, pubsCount, subs);
		
		return pubs;
	}
	
	public static Publication[] generatePubsForSubscriptions(
			MatchingEngine me, int pubsCount, Collection<CoveringSubscription> subs){
		PublicationCoveringComputer pubGenerator = new PublicationCoveringComputer();
		Publication[] pubs = pubGenerator.generate(me, pubsCount, subs);
		return pubs;
	}
	
	protected static int testPublicationMatchesCovering(MatchingEngine me, Publication pub, CoveringSubscription sub, Map<Integer, CoveringSubscription> subsMap) {
		int testedCount = 0;
		for(MockCoveringSubscription mockCoveringSub : sub._coveringSubscriptions) {
			CoveringSubscription coveringSub = subsMap.get(mockCoveringSub._subscriptionId);
			if(!me.match(coveringSub._subscription, pub))
				throw new IllegalStateException("Publication:\t" + pub + "\nMatches:\t" + sub + "\nNot Matches:\t" + coveringSub);
			else
				testedCount++;
		}
		
		return testedCount;
	}
	
	public static void analyzePubsForSubscriptions(MatchingEngine me, Publication[] pubs, Map<Integer, CoveringSubscription> subsMap){
		int totalTestedCount = 0;
		int totalMatchingCount = 0;
		int linebreaker = 0;
		Collection<CoveringSubscription> subs = subsMap.values();
		for(int i=0 ; i<pubs.length ;i++) {
			Publication pub = pubs[i];
			if(DEBUG)
				System.out.print("Pub#" + i + "\t");
			
			int matchingCount = 0;
			StringWriter writer = (DEBUG?new StringWriter():null);
			for(CoveringSubscription sub : subs)
			{
				if(me.match(sub._subscription, pub)) {
					if(DEBUG)
						writer.append((matchingCount==0?"":",") + sub);
					matchingCount++;
					totalMatchingCount++;
					
					totalTestedCount += testPublicationMatchesCovering(me, pub, sub, subsMap);
				}
			}
			
			if(DEBUG)
//				System.out.println(matchingCount + "\t" + writer.toString() + (linebreaker++ % 100 == 0? "\n" + (linebreaker/100) + "-" : ""));
//			else
				System.out.print(matchingCount + "," + (linebreaker++ % 100 == 0? "\n" + (linebreaker/100) + "-" : ""));
		}
		
		System.out.println();
		System.out.println("TotalTestedCoveringPublicationMatching: " + totalTestedCount);
		System.out.println("TotalPublicationMatching: " + totalMatchingCount);
	}
	
	public static void main(String []argv) {
//		CoveringDescriptor coveringDescriptor = makeCoveringDescriptor();
//		CoveringDescriptor coveringDescriptor = makeRandomCoveringDescriptor(10, 30000, 0.95);
		CoveringDescriptor coveringDescriptor =
			CoveringLevelNormalMultiplier.makeRandomMultpliedCoveringDescriptor
//			(10, 400, 0.1, 0.01, 1, 0);
			(200, 40000, 0.1, 0.01, 1, 0);
		Set<MockCoveringSubscription> subSet = generate(coveringDescriptor);
		
		System.out.println("Total(" + subSet.size() + " v. " + coveringDescriptor.getCount() + " v. " + coveringDescriptor.getUnmultipliedCount() + ") " + (DEBUG?subSet:""));
		
		Map<Integer, CoveringSubscription> materializedSubSet = materializeCoveringSubscriptions(subSet);
		CoveringMockSubscriptionGeneratorTest.checkMaterializedSubs(materializedSubSet);
		writeOutMaterializedSubscriptions(materializedSubSet);
		
		for(int i=1; i<=SUB_GROUPS ; i++) {
			Set<MockCoveringSubscription>[] coveringSubSets = analyzeCovering(i, subSet);
			analyzeMaterializedSubscriptions(i, coveringSubSets, materializedSubSet);
			System.out.println(analyzePredicateCount(coveringSubSets, materializedSubSet));
		}
		
		System.out.println("TotalPred#(" + countPredicates(materializedSubSet.values()) + ")");
		
		MatchingEngine me = new MatchingEngineImp();
		Publication[] pubs = generatePubsForSubscriptions(me, 10000, materializedSubSet.values());
		analyzePubsForSubscriptions(me, pubs, materializedSubSet);
		CoveringMockSubscriptionGeneratorTest.checkPubsForSubscriptions(pubs, materializedSubSet);
	}

	public static String analyzePredicateCount(
			Set<MockCoveringSubscription>[] coveringSubSets, Map<Integer, CoveringSubscription> materializedSubSet) {
		Set<CoveringSubscription> subs = new HashSet<CoveringSubscription>();
		for(Set<MockCoveringSubscription> coveringSubGroup : coveringSubSets)
		{
			for(MockCoveringSubscription mockCoveringSub : coveringSubGroup) {
				CoveringSubscription coveringSub;
				if(CoveringSubscription.class.isAssignableFrom(mockCoveringSub.getClass()))
					coveringSub = (CoveringSubscription)mockCoveringSub;
				else
					coveringSub = materializedSubSet.get(mockCoveringSub._subscriptionId);
				subs.add(coveringSub);
			}
		}
		
		return "Pred#: " + countPredicates(subs);
	}

	public static int countPredicates(Collection<CoveringSubscription> subs) {
		int  totalPredicatesCount = 0;
		for(CoveringSubscription sub : subs)
			totalPredicatesCount += sub._subscription._predicates.size();
		
		return totalPredicatesCount;
	}
	
	static void writeOutMaterializedSubscriptions(Map<Integer, CoveringSubscription> materializedSubSet) {
		if(!DEBUG)
			return;
		
		for(CoveringSubscription coveringSub : materializedSubSet.values())
			System.out.println("*\t" + coveringSub);
	}

	private static void analyzeMaterializedSubscriptions(
			int group, Set<MockCoveringSubscription>[] coveringSubSets, Map<Integer, CoveringSubscription> materializedSubSet) {
		if(!DEBUG)
			return;
		
		System.out.println("\tGroup " + group);
		for(int j=0 ; j<coveringSubSets.length ; j++) {
			Set<MockCoveringSubscription> mockCoveringSubs = coveringSubSets[j];
			for(MockCoveringSubscription mockCoveringSub : mockCoveringSubs) {
				CoveringSubscription coveringSub = materializedSubSet.get(mockCoveringSub._subscriptionId);
				if(coveringSub != null)
					System.out.println("\t\t" + coveringSub);
			}
		}		
	}

	private static Map<String, AttributeBound> getAttributeBounds(Set<Subscription> subscriptions) {
		Map<String, Set<SimplePredicate>> predicatesMap = new HashMap<String, Set<SimplePredicate>>();
		for(Subscription subscription : subscriptions) {
			for(SimplePredicate sp : subscription._predicates) {
				String attribute = sp._attribute;
				
				Set<SimplePredicate> predicatesSet = predicatesMap.get(attribute);
				if(predicatesSet == null) {
					predicatesSet = new HashSet<SimplePredicate>();
					predicatesMap.put(attribute, predicatesSet);
				}
				
				predicatesSet.add(sp);
			}
		}

		Map<String, AttributeBound> attributesBoundsMap = new HashMap<String, AttributeBound>();
		for(Set<SimplePredicate> predicates : predicatesMap.values()) {
			AttributeBound attrBound = AttributeBound.createAttributeBound(predicates);
			attributesBoundsMap.put(attrBound._attribute, attrBound);
		}
		return attributesBoundsMap;
	}
	
	public static Map<Integer, CoveringSubscription> materializeCoveringSubscriptions(Set<MockCoveringSubscription> subsUnordered) {
		Map<Integer, CoveringSubscription> subsMap = new HashMap<Integer, CoveringSubscription>();
		
		MockCoveringSubscription[] sortedSubs = new TreeSet<MockCoveringSubscription>(subsUnordered).toArray(new MockCoveringSubscription[0]);
		int pointer = 0;
		for(int w=0 ; w<sortedSubs.length ; w++)
		{
			MockCoveringSubscription mockSub = sortedSubs[w];
			Subscription sub = new Subscription();
			String attributeLevel = "ATTR" + mockSub._level; 
			pointer++;
			sub.addPredicate(SimplePredicate.buildSimplePredicate(attributeLevel, '=', pointer));
					
			CoveringSubscription coveringSub = new CoveringSubscription(mockSub);
			coveringSub.materialize(null, sub);
			if(MockMultipliedCoveringSubscription.class.isAssignableFrom(mockSub.getClass())) {
				MockMultipliedCoveringSubscription multipliedMockSub = ((MockMultipliedCoveringSubscription)mockSub);
				for(MockCoveringSubscription mockIndividualSub : multipliedMockSub._subs)
					subsMap.put(new Integer(mockIndividualSub._subscriptionId), coveringSub);
			}
			
			subsMap.put(new Integer(mockSub._subscriptionId), coveringSub);
			
			Set<Subscription> coveredSubs = new HashSet<Subscription>();
			for(MockCoveringSubscription mockCoveredSub : mockSub._coveredSubscriptions) {
				if (mockCoveredSub._level < mockSub._level)
					coveredSubs.add(subsMap.get(mockCoveredSub._subscriptionId)._subscription);
			}
			
			Map<String, AttributeBound> attrBoundMap = getAttributeBounds(coveredSubs);
			for(AttributeBound attrBound : attrBoundMap.values()) {
				String attribute = attrBound._attribute;
				if(!attribute.equals("ATTR" + (mockSub._level - 1)))
					continue;
				
				int lower = attrBound.getLower();
				int upper = attrBound.getUpper();
				
				SimplePredicate spUpper = SimplePredicate.buildSimplePredicate(attribute, '<', upper);
				sub.addPredicate(spUpper);
				
				SimplePredicate spLower = SimplePredicate.buildSimplePredicate(attribute, '>', lower);
				sub.addPredicate(spLower);
			}
		}
		
		for(int w=sortedSubs.length-1 ; w>=0 ; w--) {
			MockCoveringSubscription mockSub = sortedSubs[w];
			int level = mockSub._level;
			int upperLevel = level + 1;
			Map<String, AttributeBound> upperAttributeBounds = new HashMap<String, AttributeBound>();
			
			if(mockSub._coveringSubscriptions.isEmpty())
				continue;
			
			for(MockCoveringSubscription coveringSubscription : mockSub._coveringSubscriptions) {
				if(coveringSubscription._level == level)
					continue;
				Subscription coveringSub = subsMap.get(coveringSubscription._subscriptionId)._subscription;
				for(SimplePredicate sp : coveringSub._predicates) {
					if(sp._attribute.equals("ATTR" + level))
						continue;
					
					AttributeBound upperAttributeBound = upperAttributeBounds.get(sp._attribute);
					if(upperAttributeBound == null){
						upperAttributeBound = new AttributeBound(sp._attribute);
						upperAttributeBounds.put(sp._attribute, upperAttributeBound);
					}
//					if(sp._attribute.equals(upperAttribute))
						upperAttributeBound.updateAttributeBound(sp);
				}
			}
			
			CoveringSubscription coveringSub = subsMap.get(mockSub._subscriptionId);
			for(AttributeBound upperAttributeBound : upperAttributeBounds.values())
				for(SimplePredicate sp : upperAttributeBound.getPredicates())
					coveringSub._subscription.addPredicate(sp);
		}
		
		return subsMap;
	}

	private static Set<MockCoveringSubscription>[] analyzeCovering(int groupsCount, Set<MockCoveringSubscription> subSet) {
		int total = 0;
		for(MockCoveringSubscription sub : subSet)
			total += sub.count();
		
		Set<MockCoveringSubscription>[] coveringSubSets =
			CoveringSubscriptionComputer.divideAndGetCoveringSubscriptions(groupsCount, subSet);
		
		double totalCoveringSize = 0;
		for(Set<MockCoveringSubscription> coveringSubSet : coveringSubSets) {
			int coveringSize = coveringSubSet.size();
			totalCoveringSize += coveringSize;
			
			if(DEBUG)
				System.out.println("CoveringSet(" + coveringSize + "): " + coveringSubSet);
		}
		
		System.out.println("TotalCoveringSize(g=" + groupsCount + "): " + totalCoveringSize + " (" + (100*totalCoveringSize/total) + "%)");
		
		return coveringSubSets;
	}
}