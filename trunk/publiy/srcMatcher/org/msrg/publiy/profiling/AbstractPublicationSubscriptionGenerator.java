package org.msrg.publiy.profiling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngine;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngineImp;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;


public abstract class AbstractPublicationSubscriptionGenerator {

	public final int _PUBS_COUNT;
	public final int _SUBS_COUNT;
	
	public final int _PUBS_GROUPS_COUNT;
	public final int _SUBS_GROUPS_COUNT;
	
	final Publication[][] _PUBLICATIONS;
	final Subscription[][] _SUBSCRIPTIONS;
	final int[] _SUBS_GENERATE_COUNTER;
	
	final int SEED = 100;
	final Random _rand = new Random(SEED);
	
	final int _NULL_CLASS_VAL = 0, _CLASS_VAL = 1;
	final String _CLASS_ATTR = "Class", _PUB_COUNTER_ATTR = "PubCounter";
	final SimplePredicate _PRED_CLASS = SimplePredicate.buildSimplePredicate(_CLASS_ATTR, '=', _CLASS_VAL);
	final SimplePredicate _NULL_PRED_CLASS = SimplePredicate.buildSimplePredicate(_CLASS_ATTR, '=', _NULL_CLASS_VAL);
	private MatchingEngine _me = new MatchingEngineImp();
	
	protected AbstractPublicationSubscriptionGenerator(int pubsCount, int subsCount, 
			int pubsGroupsCount, int subsGroupsCount) {
		_PUBS_COUNT = pubsCount;
		_SUBS_COUNT = subsCount;
		_PUBS_GROUPS_COUNT = pubsGroupsCount;
		_SUBS_GROUPS_COUNT = subsGroupsCount;
		
		_PUBLICATIONS = new Publication[_PUBS_GROUPS_COUNT][_PUBS_COUNT];
		_SUBSCRIPTIONS = new Subscription[_SUBS_GROUPS_COUNT][_SUBS_COUNT];
		
		_SUBS_GENERATE_COUNTER = new int[_SUBS_GROUPS_COUNT];
		for(int i=0 ; i<_SUBS_GROUPS_COUNT ; i++)
			_SUBS_GENERATE_COUNTER[i] = 0;
	}
	
	public abstract void generate();

	protected void generateRestAsRepeatedPublications() {
		int usedGroupsCounter = 0;
		for(int i=_PUBS_GROUPS_COUNT-1 ; i>=0 ; i--) {
			for(int j=_PUBS_COUNT-1 ; j>=0 ; j--) {
				if(_PUBLICATIONS[i][j] == null) {
					while (_PUBLICATIONS[(usedGroupsCounter/_PUBS_COUNT)%_PUBS_GROUPS_COUNT][usedGroupsCounter%_PUBS_GROUPS_COUNT]==null)
						usedGroupsCounter++;
					_PUBLICATIONS[i][j] = _PUBLICATIONS[(usedGroupsCounter/_PUBS_COUNT)%_PUBS_GROUPS_COUNT][usedGroupsCounter%_PUBS_GROUPS_COUNT];
				}
				usedGroupsCounter++;
			}
		}
	}
	
	protected void generateRestAsNullSubscriptions() {
		Subscription nullSubscription = new Subscription();
		nullSubscription.addPredicate(_NULL_PRED_CLASS);
		
		for(int i=0 ; i<_SUBS_GENERATE_COUNTER.length ; i++) {
			for(int j=_SUBS_GENERATE_COUNTER[i] ; j<_SUBS_COUNT ; j++)
				_SUBSCRIPTIONS[i][j] = nullSubscription;
		}
	}
	
	protected int countMatchingGroups(Subscription sub) {
		if(sub == null)
			throw new NullPointerException();

		int groupsThatMatch = 0;
		for(int i=0 ; i<_PUBS_GROUPS_COUNT ; i++) {
			for(int j=0 ; j<_PUBS_COUNT ; j++) {
				if(_me.match(sub, _PUBLICATIONS[i][j])) {
					groupsThatMatch++;
					break;
				}
			}
		}
		
		return groupsThatMatch;
	}
	
	protected int countMatchingGroups(Publication pub) {
		if(pub == null)
			throw new NullPointerException();
		
		int groupsThatMatch = 0;
		for(int i=0 ; i<_SUBS_GROUPS_COUNT ; i++) {
			for(int j=0 ; j<_SUBS_COUNT ; j++) {
				if(_me.match(_SUBSCRIPTIONS[i][j], pub)) {
					groupsThatMatch++;
					break;
				}
			}
		}
		
		return groupsThatMatch;
	}
	
	protected void printAll() {
		System.out.println("SUBCRIPTIONS: ");
		for(int i=0 ; i<_SUBS_GENERATE_COUNTER.length ; i++) {
			for(int j=0 ; j<_SUBS_GENERATE_COUNTER[i] ; j++)
				System.out.print(_SUBSCRIPTIONS[i][j] + ", ");
			System.out.println();
		}
		
		System.out.println("\n\nPUBLICATIONS: ");
		for(int i=0 ; i<_PUBS_GROUPS_COUNT ; i++) {
			for(int j=0 ; j<_PUBS_COUNT ; j++)
				System.out.print(_PUBLICATIONS[i][j]+ ", ");
			System.out.println();
		}
	}
	
	protected void printPublicationMatching() {
		System.out.println("Per Publications");
		for(int i=0 ; i<_PUBS_GROUPS_COUNT ; i++)
			for(int j=0 ; j<_PUBS_COUNT ; j++) {
				Publication pub = _PUBLICATIONS[i][j];
				if(pub==null)
					throw new NullPointerException("i: " + i + ", " + j);
				System.out.println(i + "-" + j + ": " + countMatchingGroups(pub));
			}
	}
	
	protected void printSubscriptionMatching() {
		System.out.println("Per Subscriptions");
		for(int i=0 ; i<_SUBS_GROUPS_COUNT ; i++)
			for(int j=0 ; j<_SUBS_COUNT ; j++) {
				Subscription sub = _SUBSCRIPTIONS[i][j];
				if(sub==null)
					throw new NullPointerException("i: " + i + ", " + j);
				System.out.println(i + "-" + j + ": " + countMatchingGroups(sub));
			}

	}
	
	protected void printSummary() {
		System.out.println("Summary");
		for(int i=0 ; i<_SUBS_GROUPS_COUNT ; i++) {
			int matchingpublications = 0;
			for(int j=0 ; j<_SUBS_COUNT ; j++) {
				Subscription sub = _SUBSCRIPTIONS[i][j];
				for(int k=0 ; k<_PUBS_GROUPS_COUNT ; k++)
					for(int l=0 ; l<_PUBS_COUNT ; l++)
						if(_me.match(sub, _PUBLICATIONS[k][l]))
							matchingpublications++;
			}
			System.out.println("SubscriptionGroup #" + i + " matches total of: " + matchingpublications + " publications");
		}
	}

	protected String printSummary2() {
		System.out.println("Summary2: Publication_to_Subscription");
		String ret = "";
		for(int i=0 ; i<_PUBS_GROUPS_COUNT ; i++) {
			int matchingSubscriptions = 0;
			int matchingSubscriptionGroups = 0;
			for(int j=0 ; j<_PUBS_COUNT ; j++) {
				Publication pub = _PUBLICATIONS[i][j];
				ret += "\n" + "<" + i + "," + j + ">: ";
				for(int k=0 ; k<_SUBS_GROUPS_COUNT ; k++) {
					boolean matchingInThisSubscriptionGroup = false;
					for(int l=0 ; l<_SUBS_COUNT ; l++)
						if(_me.match(_SUBSCRIPTIONS[k][l], pub)) {
							matchingSubscriptions++;
							if(!matchingInThisSubscriptionGroup) {
								matchingSubscriptionGroups++;
								matchingInThisSubscriptionGroup = true;
							}
							ret += "<" + k + "," + l + ">";
						}
				}
			}
			System.out.println("PublicationGroup #" + i + " matches total of: " + matchingSubscriptions + " subscriptions, from " + matchingSubscriptionGroups + " groups.");
		}
		return ret;
	}

	public boolean dumpSubscriptionsInFile(String filename, String extention) {
		for(int j=0 ; j<_SUBS_GROUPS_COUNT ; j++) {
			try {
				File file = new File(filename+ _SUBS_GROUPS_COUNT + "_" + j + extention);
				FileOutputStream fileOutStream = new FileOutputStream(file);
				
				for(int i=0 ; i<_SUBS_COUNT ; i++) {
					if(_SUBSCRIPTIONS[j][i] == null)
						continue;
					
					String encodedSub = Subscription.encode(_SUBSCRIPTIONS[j][i]) + "\n";
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
	
	public boolean dumpPubicationsInFile(String filename, String extention) {
		for(int j=0 ; j<_PUBS_GROUPS_COUNT ; j++) {
			try {
				File file = new File(filename+ _PUBS_GROUPS_COUNT + "_" + j + extention);
				FileOutputStream fileOutStream = new FileOutputStream(file);
				
				for(int i=0 ; i<_PUBS_COUNT ; i++) {
					String encodedPub = Publication.encode(_PUBLICATIONS[j][i]) + "\n";
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
}
