package org.msrg.publiy.profiling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngine;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngineImp;


public class Test {
	
	public static void main(String[] argv) {
		String pFilename = "./data/ComparePubSet_0.pub";
		String sFilename = "./data/CompareSubSet_0.sub";
		
		List<Subscription> subsList = new LinkedList<Subscription>();
		List<Publication> pubsList = new LinkedList<Publication>();
		
		try {
			File sFile = new File(sFilename);
			FileReader sFReader = new FileReader(sFile);
			BufferedReader sBFReader = new BufferedReader(sFReader);
			String line = null;
			while((line =sBFReader.readLine())!=null) {
				Subscription sub = Subscription.decode(line);
				subsList.add(sub);
			}
			
			File pFile = new File(pFilename);
			FileReader pFReader = new FileReader(pFile);
			BufferedReader pBFReader = new BufferedReader(pFReader);
			while((line =pBFReader.readLine())!=null) {
				Publication pub = Publication.decode(line);
				pubsList.add(pub);
			}
		} catch (IOException iox) {}
		
		Publication[] pubs = pubsList.toArray(new Publication[0]);
		Subscription[] subs = subsList.toArray(new Subscription[0]);
		MatchingEngine me = new MatchingEngineImp();
		int total = 0;
		
		for(int i=0 ; i<pubs.length ; i++) {
			for(int j=0 ; j<subs.length ; j++) {
				if(me.match(subs[j], pubs[i])) {
					total++;
					break;
				}
			}
		}
		
		System.out.println(pubs.length + " vs. " + subs.length + "=" + total);
	}

}
