package org.msrg.publiy.profiling;

import java.util.Random;


import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngine;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngineImp;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;
import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.PropertyGrabber;

public class SubscriptionProfiling {
	
	private static final int SUBS_COUNT = 100;
	private static final int PUBS_COUNT = 1000;
	
	private static final double CLASS_VALUE_RANGE = 10;
	private static final double COUNT_S_VALUE_RANGE = 150;
	private static final double COUNT_P_VALUE_RANGE = 10000;
	
	private static final int SUB_SEED = 1000;
	private static final int PUB_SEED = 2000;
	
	public static int matchingBoth(	Publication[] pubs, int a_p, int b_p, 
									Subscription[] subs1, int a_s1, int b_s1, 
									Subscription[] subs2, int a_s2, int b_s2) {
		int matchingboth = 0;
		MatchingEngine me = new MatchingEngineImp();
		
		for(int i=a_p ; i<b_p ; i++) {
			boolean match1 = false;
			for(int j=a_s1 ; j<b_s1 && match1==false ; j++)
				if(me.match(subs1[j], pubs[i]))
					match1 = true;
			
			for(int j=a_s2 ; j<b_s2 && match1 ; j++) {
				if(me.match(subs2[j], pubs[i])) {
					matchingboth++;
					break;
				}
			}
		}
		return matchingboth;
	}
	
	public static int matchingFirstNotSecond(ProfilingData profData, int a_p, int b_p, 
																	int a_s1, int b_s1, 
																	int a_s2, int b_s2) {
		int matchingFirstNotSecond = 0;
		MatchingEngine me = new MatchingEngineImp();

		for(int i=a_p ; i<b_p ; i++) {
			boolean match1 = false;
			for(int j=a_s1 ; j<b_s1 && match1==false ; j++)
			if(me.match(profData._subs[j], profData._pubs[i]))
				match1 = true;
	
			boolean match2 = false;
			for(int j=a_s2 ; j<b_s2 && match1 ; j++)
				if(me.match(profData._subs[j], profData._pubs[i])) {
					match2 = true;
					break;
				}
			
			if(match1 && !match2)
				matchingFirstNotSecond++;
		}
		return matchingFirstNotSecond;
	}
	
	public static Subscription[] generateRandomSubscriptions(int subscriptionSize) {
		Random randGenerator = new Random(SUB_SEED);
		
		Subscription[] allSubs = new Subscription[subscriptionSize];
		
		for(int i=0 ; i<subscriptionSize ; i++) {
			Subscription newSub = new Subscription();
			
			int vClass = (int)(CLASS_VALUE_RANGE * randGenerator.nextDouble());
			int vCount = (int)(COUNT_S_VALUE_RANGE * randGenerator.nextDouble());
			if(vCount == 0)
				vCount = 1;
			
			SimplePredicate predClass = SimplePredicate.buildSimplePredicate("Class", '=', vClass);
			SimplePredicate predCount = SimplePredicate.buildSimplePredicate("A-Count", '%', vCount);
			
			newSub.addPredicate(predClass);
			newSub.addPredicate(predCount);
			
			allSubs[i] = newSub;
		}
		
		return allSubs;
	}
	
	public static Publication[] generateRandomPublications(int publicationSize) {
		Random randGenerator = new Random(PUB_SEED);
		Publication[] allPubs = new Publication[publicationSize];
		for(int i=0 ; i<publicationSize ; i++) {
			Publication newPublication = new Publication();

			int vClass = (int)(CLASS_VALUE_RANGE * randGenerator.nextDouble());
			int vCount = (int)(COUNT_P_VALUE_RANGE * randGenerator.nextDouble());
			
			newPublication.addPredicate("Class", vClass);
			newPublication.addPredicate("A-Count", vCount);
			
			allPubs[i] = newPublication;
		}
		
		return allPubs;
	}
	
	public static ProfilingData profilingTiming(ProfilingData profData, int a_p, int b_p, int a_s, int b_s) {
		if(profData._pubs==null) {
			Publication[] allPubs = generateRandomPublications(profData._pubCount);
			profData._pubs = allPubs;
		}
		profData._publicationMatching = new int[profData._pubs.length];
		
		if(profData._subs==null) {
			Subscription[] allSubs = generateRandomSubscriptions(profData._subCount);
			profData._subs = allSubs;
		}
		profData._subscriptionMatching = new int[profData._subs.length];
		
		MatchingEngine me = new MatchingEngineImp();
		
		boolean skipRest = profData._skipMatching;
		long total=0;
		long t1=0, t2=0;
		int uniqueMatchCount=0, matchCount=0, nonMatchCount=0;
		
		for(int i=a_p ; i<b_p ; i++) {
			t1 = System.nanoTime();
			boolean anyMatch = false;
			for(int j=a_s ; j<b_s ; j++) {
				boolean b = me.match(profData._subs[j], profData._pubs[i]);
				if(b) {
					profData._subscriptionMatching[j]++;
					profData._publicationMatching[i]++;
					
					matchCount++;
					if(!anyMatch) {
						uniqueMatchCount++;
						anyMatch = true;
					}
					if(skipRest)
						break;
				}
				
//				System.out.print(b?"+":"-");
			}
			if(!anyMatch)
				nonMatchCount++;

			t2 = System.nanoTime();
			total += (t2 - t1);
		}
		
		profData._totalMatchingTime = total;
		profData._averageMatchingTime = (long)(total/profData._pubs.length);
		profData._uniqueMatchCount = uniqueMatchCount;
		profData._matchCount = matchCount;
		profData._nonMatchCount = nonMatchCount;
		return profData;
	}
	
	public static void main2(String[] argv) {
		int subscriptionSize = SUBS_COUNT;
		int publicationSize = PUBS_COUNT;
		
		ProfilingData profDataSkip = new ProfilingData(subscriptionSize, publicationSize, true);
		profilingTiming(profDataSkip, 0, subscriptionSize, 0, publicationSize);
		System.out.println(profDataSkip);
//		profDataSkip.printAllSubscriptionMatching();
//		profDataSkip.printAllPublicationMatching();
		
		System.out.println();

		ProfilingData profDataNSkip = new ProfilingData(subscriptionSize, publicationSize, false);
		profilingTiming(profDataNSkip, 0, subscriptionSize, 0, publicationSize);
		System.out.println(profDataNSkip);
//		profDataNSkip.printAllSubscriptionMatching();
//		profDataNSkip.printAllPublicationMatching();
		profDataNSkip.printBuckettedPublicationMatching(5, 10);
		profDataNSkip.printBuckettedSubscriptionMatching(5, 10);
		
		profDataNSkip.printSubscriptionBunchesReceivedPublications(50);
		
		profDataSkip.dumpSubscriptionsInFile("." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "generatedSubSet-1", PropertyGrabber.DEFAULT_PUBLICATIONS_FILE_EXTENTION, 50);
		profDataSkip.dumpPublicationsInFile("." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "generatedPubSet-1", PropertyGrabber.DEFAULT_PUBLICATIONS_FILE_EXTENTION, 1000);
	}

	public static void runNetwork2() {
		int subscriptionSize = SUBS_COUNT;
		int publicationSize = PUBS_COUNT;
		int pubsGroup = 2;
		int subsGroup = 2;

		ProfilingData profDataPS = new ProfilingData(subscriptionSize*subsGroup, publicationSize*pubsGroup, true);
		
		for(int i=0 ; i<pubsGroup ; i++) {
			for(int j=0 ; j<subsGroup ; j++) {
				profilingTiming(profDataPS, i*publicationSize, (i+1)*publicationSize, j*subscriptionSize, (j+1)*subscriptionSize);		
				System.out.println(profDataPS.toStringShort("P"+(i+1)+"-S"+(j+1)));
				}
		}

		profilingTiming(profDataPS, 0, PUBS_COUNT*pubsGroup, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P12"+"-S12"));
		
		int matchFirstNotOthers = matchingFirstNotSecond(profDataPS, 0, publicationSize,
																	0, subscriptionSize,
																	subscriptionSize, subscriptionSize*2);
		System.out.println("P1-(S1 & !S2): " + matchFirstNotOthers);
		
		matchFirstNotOthers = matchingFirstNotSecond(profDataPS, publicationSize, publicationSize*2,
																subscriptionSize, subscriptionSize*2,
																0, subscriptionSize);
		System.out.println("P2-(S2 & !S1): " + matchFirstNotOthers);
	}
	
	public static void runNetwork3() {
		int subscriptionSize = SUBS_COUNT;
		int publicationSize = PUBS_COUNT;
		int pubsGroup = 3;
		int subsGroup = 3;
		
		ProfilingData profDataPS = new ProfilingData(subscriptionSize*subsGroup, publicationSize*pubsGroup, true);
		
		for(int i=0 ; i<pubsGroup ; i++) {
			for(int j=0 ; j<subsGroup ; j++) {
				profilingTiming(profDataPS, i*publicationSize, (i+1)*publicationSize, j*subscriptionSize, (j+1)*subscriptionSize);		
				System.out.println(profDataPS.toStringShort("P"+(i+1)+"-S"+(j+1)) + "\t");
			}
		}
		
		profilingTiming(profDataPS, 0, PUBS_COUNT*1, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P1"+"-S123") + "\t"  + profDataPS.matchedAtleastOncePublications(0, pubsGroup*publicationSize));
		
		profilingTiming(profDataPS, PUBS_COUNT*1, PUBS_COUNT*2, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P2"+"-S123") + "\t"  + profDataPS.matchedAtleastOncePublications(0, pubsGroup*publicationSize));
		
		profilingTiming(profDataPS, PUBS_COUNT*2, PUBS_COUNT*pubsGroup, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P3"+"-S123") + "\t"  + profDataPS.matchedAtleastOncePublications(0, pubsGroup*publicationSize));
		
		profilingTiming(profDataPS, 0, PUBS_COUNT*pubsGroup, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P123"+"-S123") + "\t"  + profDataPS.matchedAtleastOncePublications(0, pubsGroup*publicationSize));
		
		for(int i=0 ; i<pubsGroup ; i++)
			for(int j=0 ; j<subsGroup ; j++) {
				int matchingOnly = profDataPS.matchingOnly(i*publicationSize, (i+1)*publicationSize, 
										j*subscriptionSize, (j+1)*subscriptionSize, 
										0, subscriptionSize*subsGroup);
				System.out.println("P"+(i+1)+"-only-S"+(1+j)+": " + matchingOnly);
			}
		
		
//		int matchFirstNotOthers = matchingFirstNotSecond(profDataPS, 0, publicationSize,
//																	0, subscriptionSize,
//																	subscriptionSize, subscriptionSize*3);
//		System.out.println("P1-(S1 & !S2 & !S3): " + matchFirstNotOthers);
//		
//		matchFirstNotOthers = matchingFirstNotSecond(profDataPS, publicationSize*2, publicationSize*3,
//																subscriptionSize*2, subscriptionSize*3,
//																0, subscriptionSize*2);
//		System.out.println("P3-(S3 & !S1 & !S2): " + matchFirstNotOthers);
//		profDataPS.dumpSubscriptionsInFile(filename, a_s, b_s);

//		profDataPS.dumpPublicationsInFile("." + FileUtils.separatorChar + "data/ComparePubSet", SimpleFilePublisher.DEFAULT_PUBLICATIONS_FILE_EXTENTION, publicationSize);
//		profDataPS.dumpSubscriptionsInFile("." + FileUtils.separatorChar + "data/CompareSubSet", SimpleFileSubscriber.DEFAULT_PUBLICATIONS_FILE_EXTENTION, subscriptionSize);
	}

	public static void runNetwork6() {
		int subscriptionSize = SUBS_COUNT;
		int publicationSize = PUBS_COUNT;
		int pubsGroup = 6;
		int subsGroup = 6;
		
		ProfilingData profDataPS = new ProfilingData(subscriptionSize*subsGroup, publicationSize*pubsGroup, true);
		
		for(int i=0 ; i<pubsGroup ; i++) {
			for(int j=0 ; j<subsGroup ; j++) {
				profilingTiming(profDataPS, i*publicationSize, (i+1)*publicationSize, j*subscriptionSize, (j+1)*subscriptionSize);		
				System.out.println(profDataPS.toStringShort("P"+(i+1)+"-S"+(j+1)) + "\t");
			}
		}
		
		profilingTiming(profDataPS, 0, PUBS_COUNT*1, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P1"+"-S123456") + "\t"  + profDataPS.matchedAtleastOncePublications(0, pubsGroup*publicationSize));
		
		profilingTiming(profDataPS, PUBS_COUNT*1, PUBS_COUNT*2, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P2"+"-S123456") + "\t"  + profDataPS.matchedAtleastOncePublications(0, pubsGroup*publicationSize));
		
		profilingTiming(profDataPS, PUBS_COUNT*2, PUBS_COUNT*3, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P3"+"-S123456") + "\t"  + profDataPS.matchedAtleastOncePublications(0, pubsGroup*publicationSize));
		
		profilingTiming(profDataPS, PUBS_COUNT*3, PUBS_COUNT*4, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P3"+"-S123456") + "\t"  + profDataPS.matchedAtleastOncePublications(0, pubsGroup*publicationSize));

		profilingTiming(profDataPS, PUBS_COUNT*4, PUBS_COUNT*5, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P3"+"-S123456") + "\t"  + profDataPS.matchedAtleastOncePublications(0, pubsGroup*publicationSize));

		profilingTiming(profDataPS, PUBS_COUNT*5, PUBS_COUNT*pubsGroup, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P3"+"-S123456") + "\t"  + profDataPS.matchedAtleastOncePublications(0, pubsGroup*publicationSize));

		//////////////////////
		profilingTiming(profDataPS, 0, PUBS_COUNT*pubsGroup, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P123456"+"-S123456") + "\t"  + profDataPS.matchedAtleastOncePublications(0, pubsGroup*publicationSize));
		
		for(int i=0 ; i<pubsGroup ; i++) {
			for(int j=0 ; j<subsGroup ; j++) {
				int matchingOnly = profDataPS.matchingOnly(i*publicationSize, (i+1)*publicationSize, 
										j*subscriptionSize, (j+1)*subscriptionSize, 
										0, subscriptionSize*subsGroup);
				System.out.println("P"+(i+1)+"-only-S"+(1+j)+": " + matchingOnly);
			}
		}
		
//		profDataPS.dumpPublicationsInFile("." + FileUtils.separatorChar + "data/Compare3PubSet", SimpleFilePublisher.DEFAULT_PUBLICATIONS_FILE_EXTENTION, publicationSize);
//		profDataPS.dumpSubscriptionsInFile("." + FileUtils.separatorChar + "data/Compare3SubSet", SimpleFileSubscriber.DEFAULT_PUBLICATIONS_FILE_EXTENTION, subscriptionSize);
	}

	public static void runNetwork4() {
		int subscriptionSize = SUBS_COUNT;
		int publicationSize = PUBS_COUNT;
		int pubsGroup = 4;
		int subsGroup = 4;
		
		ProfilingData profDataPS = new ProfilingData(subscriptionSize*subsGroup, publicationSize*pubsGroup, true);
		
		for(int i=0 ; i<pubsGroup ; i++) {
			for(int j=0 ; j<subsGroup ; j++) {
				profilingTiming(profDataPS, i*publicationSize, (i+1)*publicationSize, j*subscriptionSize, (j+1)*subscriptionSize);		
				System.out.println(profDataPS.toStringShort("P"+(i+1)+"-S"+(j+1)));
				}
		}
		
		profilingTiming(profDataPS, 0, PUBS_COUNT*pubsGroup, 0, SUBS_COUNT*subsGroup);
		System.out.println(profDataPS.toStringShort("P123"+"-S123"));
		
		int matchFirstNotOthers = matchingFirstNotSecond(profDataPS, 0, publicationSize,
																	0, subscriptionSize,
																	subscriptionSize, subscriptionSize*3);
		System.out.println("P1-(S1 & !S2 & !S3): " + matchFirstNotOthers);
		
		matchFirstNotOthers = matchingFirstNotSecond(profDataPS, publicationSize*2, publicationSize*3,
																subscriptionSize*2, subscriptionSize*3,
																0, subscriptionSize*2);
		System.out.println("P3-(S3 & !S1 & !S2): " + matchFirstNotOthers);
		
		matchFirstNotOthers = matchingFirstNotSecond(profDataPS, publicationSize*2, publicationSize*3,
																subscriptionSize, subscriptionSize*3,
																0, subscriptionSize*2);
		System.out.println("P3-(S3 & !S1 & !S2): " + matchFirstNotOthers);
	}

	
	{
//		int matchingboth1 = matchingBoth(profDataPS._pubs, 0, publicationSize, 
//										profDataPS._subs, 0, subscriptionSize, 
//										profDataPS._subs, subscriptionSize, subscriptionSize*2);
//		System.out.println("P1 to S1&&S2: " + matchingboth1);
//		
//		int matchingboth2 = matchingBoth(profDataPS._pubs, publicationSize, publicationSize*2, 
//											profDataPS._subs, 0, subscriptionSize, 
//											profDataPS._subs, subscriptionSize, subscriptionSize*2);
//		System.out.println("P2 to S1&&S2: " + matchingboth2);
//		
//		profilingTiming(profDataPS, 0, publicationSize*2, 0, subscriptionSize*2);
//		System.out.println(profDataPS.toStringShort("P12-S12"));
//		
//		profilingTiming(profDataPS, 0, publicationSize, 0, subscriptionSize*2);
//		System.out.println(profDataPS.toStringShort("P1-S12"));
//		
//		profilingTiming(profDataPS, publicationSize, publicationSize*2, 0, subscriptionSize*2);
//		System.out.println(profDataPS.toStringShort("P1-S12"));
	}
	
	public static void main(String[] argv) {
		runNetwork6();
	}
}

