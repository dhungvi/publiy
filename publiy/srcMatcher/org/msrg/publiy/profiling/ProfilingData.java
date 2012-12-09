package org.msrg.publiy.profiling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngine;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngineImp;


public class ProfilingData {

	public final int _pubCount;
	public final int _subCount;
	public final boolean _skipMatching;
	
	public ProfilingData(int subscriptionSize, int publicationSize, boolean skipRest) {
		_pubCount = publicationSize;
		_subCount = subscriptionSize;
		_skipMatching = skipRest;
	}
	
	public Publication[] _pubs;
	public int[] _publicationMatching;
	public Subscription[] _subs;
	public int[] _subscriptionMatching;
	
	public int _matchCount = -1;
	public int _uniqueMatchCount = -1;
	public int _nonMatchCount = -1;
	
	public long _totalMatchingTime = -1;
	public long _averageMatchingTime = -1;
	
	public String toStringShort(String str) {
		return str + ": " + _matchCount;
	}
	
	@Override
	public String toString() {
		String str = (_skipMatching?"S-":"NS-") + "Matching '" + _pubCount + "' pubs against '" + _subCount + "' subs took: " + _averageMatchingTime/1000 + "," + _averageMatchingTime%1000 + "\t match: " + (((double)_uniqueMatchCount/_pubCount))*100 + "%\t Total sub matching: " + getAverageSubscriptionMatching() + "\t Total pub matching: " + _matchCount + ", Total pub unique matching: " + getAveragePublicationMatching();
		return str;
	}
	
	public int matchedAtleastOncePublications(int a_p, int b_p) {
		int total = 0;
		for(int i=a_p ; i<b_p ; i++)
			if(_publicationMatching[i] != 0)
				total++;
		return total;
	}
	
	public int getAverageSubscriptionMatching() {
		int totalMatching = 0;
		for(int i=0 ; i<_subscriptionMatching.length ; i++)
			totalMatching += _subscriptionMatching[i];
		
		return totalMatching;
	}
	
	public int getAveragePublicationMatching() {
		int totalMatching = 0;
		for(int i=0 ; i<_publicationMatching.length ; i++)
			totalMatching += _publicationMatching[i];
		
		return totalMatching;
	}
	
	public void printAllSubscriptionMatching() {
		System.out.print("SubMatching: ");
		for(int i=0 ; i<_subscriptionMatching.length ; i++)
			System.out.print((i%50==0?"\n":"") + _subscriptionMatching[i] + ",");
		System.out.println();
	}

	public void printAllPublicationMatching() {
		System.out.print("PubMatching: ");
		for(int i=0 ; i<_publicationMatching.length ; i++)
			System.out.print((i%50==0?"\n":"") + _publicationMatching[i] + ",");
		System.out.println();
	}
	
	public int[] getBuckettedSubscriptions(int buckSize) {
		int buckCount = (_subCount / buckSize);
		int[] bucks = new int[buckCount];
		
		for(int i=0 ; i<_publicationMatching.length ; i++)
			bucks[_publicationMatching[i]/buckSize]++;
		
		return bucks;
	}
	
	public int[] getBuckettedPublications(int buckSize) {
		int buckCount = (_pubCount / buckSize);
		int[] bucks = new int[buckCount];
		
		for(int i=0 ; i<_subscriptionMatching.length ; i++)
			bucks[_subscriptionMatching[i]/buckSize]++;
		
		return bucks;
	}
	
	public void printBuckettedPublicationMatching(int buckSize, int cutBunchCount) {
		int[] bucks = getBuckettedPublications(buckSize);
		if(cutBunchCount == -1)
			cutBunchCount = bucks.length;
		
		for(int i=0 ; i<bucks.length && i<cutBunchCount ; i++)
			System.out.print((i%5==0?"\n":"") + bucks[i] +":[" + i*buckSize + "-" + ((i+1)*buckSize-1) + "], ");

		System.out.println();
	}
	
	public void printBuckettedSubscriptionMatching(int buckSize, int cutBunchCount) {
		int[] bucks = getBuckettedSubscriptions(buckSize);
		if(cutBunchCount == -1)
			cutBunchCount = bucks.length;
		
		for(int i=0 ; i<bucks.length && i<cutBunchCount ; i++)
			System.out.print((i%5==0?"\n":"") + bucks[i] +":[" + i*buckSize + "-" + ((i+1)*buckSize-1) + "], ");
		
		System.out.println();
	}
	
	public int[] getPublicationsInBunches(int bunchSize) {
		int bunchCount = (_pubCount/bunchSize);
		int[] bunches = new int[bunchCount];
		
		for(int i=0 ; i<_pubCount ; i++)
			bunches[i/bunchSize] += _publicationMatching[i];
		
		return bunches;
	}
	
	public int[] getSubscriptionsInBunches(int bunchSize) {
		int bunchCount = (_subCount/bunchSize);
		int[] bunches = new int[bunchCount];
		
		for(int i=0 ; i<_subCount ; i++)
			bunches[i/bunchSize] += _subscriptionMatching[i];
		
		return bunches;
	}
	
//	public void printPublicationBunchesReceivedPublications(int bunchSize) {
//		int[] bunches = getPublicationsInBunches(bunchSize);
//		for(int i=0 ; i<bunches.length ; i++)
//			System.out.print("pub[" + (i*bunchSize) + "-" + (((i+1)*bunchSize)+1) + "] rcvd: " + bunches[i] + " pubs.");
//	}
	
	public void printSubscriptionBunchesReceivedPublications(int bunchSize) {
		int[] bunches = getSubscriptionsInBunches(bunchSize);
		for(int i=0 ; i<bunches.length ; i++)
			System.out.print((i%5==0?"\n":"") + "sub[" + (i*bunchSize) + "-" + (((i+1)*bunchSize)-1) + "] rcvd: " + bunches[i] + " pubs.\t");
	}
	
	public boolean dumpSubscriptionsInFile(String filename, String extention, int bunchSize) {
		int bunchCount = _subs.length/bunchSize;
		for(int j=0 ; j<bunchCount ; j++) {
			try {
				File file = new File(filename+ "_" + j + extention);
				FileOutputStream fileOutStream = new FileOutputStream(file);
				
				for(int i=0 ; i<bunchSize ; i++) {
					String encodedSub = Subscription.encode(_subs[i+j*bunchSize]) + "\n";
					fileOutStream.write(encodedSub.getBytes());
				}
				
				fileOutStream.close();
			}catch(IOException iox) {
				iox.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	public boolean dumpPublicationsInFile(String filename, String extention, int bunchSize) {
		int bunchCount = _pubs.length/bunchSize;
		for(int j=0 ; j<bunchCount ; j++) {
			try {
				File file = new File(filename+ "_" + j + extention);
				FileOutputStream fileOutStream = new FileOutputStream(file);
				
				for(int i=0 ; i<bunchSize ; i++) {
					String encodedPub = Publication.encode(_pubs[i+j*bunchSize]) + "\n";
					fileOutStream.write(encodedPub.getBytes());
				}
				
				fileOutStream.close();
			}catch(IOException iox) {
				iox.printStackTrace();
				return false;
			}
		}
		
		return true;
	}
	
	public int matchingOnly(int a_p, int b_p, int a_s1, int b_s1, int a_S, int b_S) {
		int matchingFirstNotSecond = 0;
		MatchingEngine me = new MatchingEngineImp();

		for(int i=a_p ; i<b_p ; i++) {
			boolean match1 = false;
			for(int j=a_s1 ; j<b_s1 && match1==false ; j++)
				if(me.match(_subs[j], _pubs[i]))
					match1 = true;
		
				boolean match2 = false;
				for(int j=a_S ; j<b_S && match1 ; j++) {
					if(j >= a_s1 && j < b_s1)
						continue;
					
					if(me.match(_subs[j], _pubs[i])) {
						match2 = true;
						break;
					}
				}
		
				if(match1 && !match2)
					matchingFirstNotSecond++;
		}
		
		return matchingFirstNotSecond;
	}

}
//-DBrokerController.DEFAULT_LOCAL_IP_INDEX=1