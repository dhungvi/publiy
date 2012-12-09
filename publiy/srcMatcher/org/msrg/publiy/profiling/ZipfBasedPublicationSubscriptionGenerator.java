package org.msrg.publiy.profiling;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;
import org.msrg.publiy.utils.ZipfGenerator;

public class ZipfBasedPublicationSubscriptionGenerator {

	final static boolean OVERRIDE_FILES = true; 
	static double _MULTIPLIER = 7;
	final int _PUBS_COUNT_PER_BROKER;
	final int _PUBS_GROUPS;
	final int _SUBS_GROUPS;
	final double _SKEW;
	private ZipfGenerator _zipfGenerator;
	private Random _random;
	
	ZipfBasedPublicationSubscriptionGenerator(
			int pubsCountPerBroker,
			int pubsGroups,
			int subsGroups,
			double skew) {
		_PUBS_COUNT_PER_BROKER = pubsCountPerBroker;
		_PUBS_GROUPS = pubsGroups;
		_SUBS_GROUPS = subsGroups;
		_SKEW = skew;
		_zipfGenerator = new ZipfGenerator(_PUBS_COUNT_PER_BROKER, _SKEW);
		_random = new Random();
	}

	protected Map<Integer, Set<Integer>> generatePerSourceBroker() {
		Map<Integer, Set<Integer>> per_source_pub_sub_matching_map = new HashMap<Integer, Set<Integer>>();
		for ( int i=0 ; i<_PUBS_COUNT_PER_BROKER ; i++) {
			Set<Integer> matching_subs_index_set = new HashSet<Integer>();
			double zipfProbability = _zipfGenerator.getProbability(i+1);
			int subsDestinationCount = (int) Math.floor(zipfProbability * _MULTIPLIER * _SUBS_GROUPS);
			for (int j=0 ; j<subsDestinationCount ; j++) {
				int subscribersBrokerIndex = (int) (_SUBS_GROUPS * _random.nextDouble());
				matching_subs_index_set.add(new Integer(subscribersBrokerIndex));
			}
			per_source_pub_sub_matching_map.put(new Integer(i), matching_subs_index_set);
		}
		
		return per_source_pub_sub_matching_map;
	}
	
	public Map<Integer, Map<Integer, Set<Integer>>> generate() {
		Map<Integer, Map<Integer, Set<Integer>>> pub_sub_index_matching_map =
			new HashMap<Integer, Map<Integer,Set<Integer>>>();
		for (int sourceIndex=0 ; sourceIndex<_PUBS_GROUPS ; sourceIndex++) {
			if (sourceIndex==99)
				sourceIndex = 99;
			Map<Integer, Set<Integer>> matching_subs_index = generatePerSourceBroker();
			pub_sub_index_matching_map.put(new Integer(sourceIndex), matching_subs_index);
		}
		
		return pub_sub_index_matching_map;
	}
	
	private Subscription produceSubscription(int pubSourceIndex, int pubIndex) {
		Subscription sub = new Subscription();
		SimplePredicate pubSouceIndexPredicate =
			SimplePredicate.buildSimplePredicate("SRC", '=', pubSourceIndex);
		sub.addPredicate(pubSouceIndexPredicate);
		
		SimplePredicate pubIndexPredicate =
			SimplePredicate.buildSimplePredicate("PUBINDEX", '=', pubIndex);
		sub.addPredicate(pubIndexPredicate);
		
		return sub;
	}
	
	private Publication producePublication(int pubSourceIndex, int pubIndex) {
		Publication pub = new Publication();
		pub.addPredicate("SRC", pubSourceIndex);
		pub.addPredicate("PUBINDEX", pubIndex);
		return pub;
	}

	public Map<Integer, Set<Subscription>> generateSubs(Map<Integer, Map<Integer, Set<Integer>>> matching_map) {
		Map<Integer, Set<Subscription>> all_subs = new HashMap<Integer, Set<Subscription>>();
		for (Map.Entry<Integer, Map<Integer, Set<Integer>>> matchingEntry : matching_map.entrySet()) {
			Integer pubSource = matchingEntry.getKey();
			for (Map.Entry<Integer, Set<Integer>> pubEntry : matchingEntry.getValue().entrySet()) {
				int pubIndex = pubEntry.getKey();
				Set<Integer> subsIndexSet = pubEntry.getValue();
				Subscription sub = produceSubscription(pubSource, pubIndex);
				for (Integer subIndex : subsIndexSet) {
					Set<Subscription> subs = all_subs.get(subIndex);
					if (subs == null) {
						subs = new HashSet<Subscription>();
						all_subs.put(subIndex, subs);
					}
					subs.add(sub);
				}
			}
		}
		
		return all_subs;
	}
	
	public Map<Integer, Set<Publication>> generatePubs(Map<Integer, Map<Integer, Set<Integer>>> matching_map) {
		Map<Integer, Set<Publication>> all_pubs = new HashMap<Integer, Set<Publication>>();
		for (Map.Entry<Integer, Map<Integer, Set<Integer>>> matchingEntry : matching_map.entrySet()) {
			Integer pubSource = matchingEntry.getKey();
			Set<Publication> pubs = new HashSet<Publication>();
			for (Map.Entry<Integer, Set<Integer>> pubEntry : matchingEntry.getValue().entrySet()) {
				int pubIndex = pubEntry.getKey();
//				Set<Integer> subsIndexSet = pubEntry.getValue();
				Publication pub = producePublication(pubSource, pubIndex);
				pubs.add(pub);
			}
			all_pubs.put(pubSource, pubs);
		}
		return all_pubs;
	}
	
	static int printPublications(Map<Integer, Set<Publication>> pubs) {
		int totalPubSize = 0;
		for (Map.Entry<Integer, Set<Publication>> pubEntry : pubs.entrySet()) {
			int source = pubEntry.getKey();
			Set<Publication> sourcePubs = pubEntry.getValue();
			int size = sourcePubs.size();
			totalPubSize += size;
			System.out.println("Source" + source + "(" + size + "): " + sourcePubs);
		}
		System.out.println();
		
		return totalPubSize;
	}
	
	static int printSubscriptions(Map<Integer, Set<Subscription>> subs) {
		int totalSubSize = 0;
		for (Map.Entry<Integer, Set<Subscription>> subEntry : subs.entrySet()) {
			int dest = subEntry.getKey();
			Set<Subscription> destSubs = subEntry.getValue();
			int size = destSubs.size();
			totalSubSize += size;
			System.out.println("Dest" + dest + "(" + size + "): " + destSubs);
		}
		
		return totalSubSize;
	}
	
	void writePubs(Writer ioWriter, Set<Publication> publications) throws IOException {
		for (Publication pub : publications) {
			ioWriter.write(Publication.encode(pub) + "\n");
		}
	}
	
	void writeSubs(Writer ioWriter, Set<Subscription> subscriptions) throws IOException {
		for (Subscription sub : subscriptions) {
			ioWriter.write(Subscription.encode(sub) + "\n");
		}
	}
	
	void writeAllSubs(Map<Integer, Set<Subscription>> subs) throws IOException {
		for (Map.Entry<Integer, Set<Subscription>> subEntry : subs.entrySet()) {
			int dest = subEntry.getKey();
			Set<Subscription> destSubs = subEntry.getValue();
			String filename = getSubscriptionFilename(dest);
			
			FileWriter fWriter = new FileWriter(filename);
			writeSubs(fWriter, destSubs);
			fWriter.close();
		}
	}
	
	void writeAllPubs(Map<Integer, Set<Publication>> pubs) throws IOException {
		for (Map.Entry<Integer, Set<Publication>> pubEntry : pubs.entrySet()) {
			int dest = pubEntry.getKey();
			Set<Publication> destSubs = pubEntry.getValue();
			String filename = getPublicationFilename(dest);
			
			FileWriter fWriter = new FileWriter(filename);
			writePubs(fWriter, destSubs);
			fWriter.close();
		}
	}
	
	String getSubscriptionFilename(int dest) {
		return getDestDir() + "/" + "SubFile_" + dest + ".sub";	
	}
	
	String getPublicationFilename(int dest) {
		return getDestDir() + "/" + "PubFile_" + dest + ".pub";
	}
	
	String getTopNameTemplate() {
		return "fout3-8-24";
	}
	
	String getDestDir() {
		return "./data/" + getDataDescriptor();
	}
	
	String getDataDescriptor() {
		return "zipf" +
		"-MULT" + _MULTIPLIER + "-SKW" + _SKEW +
		"-PG" + _PUBS_GROUPS + "-PC" + _PUBS_COUNT_PER_BROKER +
		"-SG" + _SUBS_GROUPS;
	}

	void writeDataDescriptor() throws IOException {
		String dirname = getDestDir();
		String dataDescriptor = getDataDescriptor();
		File dataDescriptorFile = new File(dirname + "/" + "data_descriptor");
		Writer fWriter = new FileWriter(dataDescriptorFile);
		fWriter.write(dataDescriptor);
		fWriter.close();
	}
	
	void writeAll(Map<Integer, Set<Publication>> pubs, Map<Integer, Set<Subscription>> subs) {
		try {
			String dirname = getDestDir();
			File dir = new File(dirname + "/");
			if (dir.exists() && !OVERRIDE_FILES)
				throw new IllegalStateException("Destination dir exists: " + dirname);
				
			if (!dir.mkdirs() && !OVERRIDE_FILES)
				throw new IllegalStateException("Error creating destination dir: " + dirname);
			
			writeAllPubs(pubs);
			writeAllSubs(subs);
			
			writeDataDescriptor();
			
		}catch(IOException iox) {
			iox.printStackTrace();
		}
	}
	
	Map<Integer, Map<Integer, Set<Integer>>> generateAll(boolean write) {
		Map<Integer, Map<Integer, Set<Integer>>> matching_map = generate();
		Map<Integer, Set<Publication>> pubs = generatePubs(matching_map);
		Map<Integer, Set<Subscription>> subs = generateSubs(matching_map);
		if (write)
			writeAll(pubs, subs);
		
		return matching_map;
	}
	
	void writeOutStats(Map<Integer, Map<Integer, Set<Integer>>> matching_map) {
		int totalMatchingSize = 0;
		for (Map.Entry<Integer, Map<Integer, Set<Integer>>> sourceMatchingEntry : matching_map.entrySet()) {
			Integer pubSource = sourceMatchingEntry.getKey();
//			System.out.print("Source#" + pubSource + ":");
			Map<Integer, Set<Integer>> matchings = sourceMatchingEntry.getValue();
			for(Map.Entry<Integer, Set<Integer>> matchingEntry : matchings.entrySet())
				totalMatchingSize += matchingEntry.getValue().size();
//			System.out.println(matchings);
		}
		System.out.println("Total matchings: " + totalMatchingSize);
		
		Map<Integer, Integer> matchings_count = new HashMap<Integer, Integer>();
		for (Map.Entry<Integer, Map<Integer, Set<Integer>>> sourceMatchingEntry : matching_map.entrySet()) {
			for(Map.Entry<Integer, Set<Integer>> matchingEntry : sourceMatchingEntry.getValue().entrySet()) {
				int matching_subs_count = matchingEntry.getValue().size();
				Integer stored_matching_subs_count = matchings_count.get(matching_subs_count);
				if (stored_matching_subs_count==null)
					stored_matching_subs_count = new Integer(0);
				matchings_count.put(matching_subs_count, stored_matching_subs_count + 1);
			}
		}
		int totalPubCount = 0;
		for (Map.Entry<Integer, Integer> matching_count : matchings_count.entrySet()) {
			int pubCount = matching_count.getValue();
			totalPubCount += pubCount;
			System.out.println("" + totalPubCount + "\t" + pubCount + " publications are delivered to: " + matching_count.getKey() + " desinations");
		}
	}
	public static void main(String[] argv){
		// General parameters
		int PUB_COUNT = 10;
		int PUB_GROUPS = 24;
		int SUB_GROUPS = 24;
		double skew = 1;

		int[] ms = {10};
//		int[] ms = {1, 10, 25, 50, 100};
		for(int m : ms) {
			System.out.println("multiplier=" + m);
			_MULTIPLIER = m;
			ZipfBasedPublicationSubscriptionGenerator generator =
				new ZipfBasedPublicationSubscriptionGenerator(PUB_COUNT, PUB_GROUPS, SUB_GROUPS, skew);
			Map<Integer, Map<Integer, Set<Integer>>> matching_map = generator.generateAll(true);
			generator.writeOutStats(matching_map);
		}
		
//		int totalSubSize = printPublications(generator.generatePubs(matching_map));
//		int totalPubSize = printSubscriptions(generator.generateSubs(matching_map));
//		System.out.println("Total Subs: " + totalSubSize);
//		System.out.println("Total Pubs: " + totalPubSize);
	}
}
