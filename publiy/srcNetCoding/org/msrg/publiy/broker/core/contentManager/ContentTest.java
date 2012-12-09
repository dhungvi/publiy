package org.msrg.publiy.broker.core.contentManager;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class ContentTest extends TestCase {
	
	static {
		System.setProperty("NC.COLS", "100");
		System.setProperty("NC.ROWS", "10");
	}
	
	static int addressesSize = 10;
	static InetSocketAddress[] addresses =
		new InetSocketAddress[addressesSize];
	
	public void setUp() {
		for(int i=0 ; i<addressesSize ; i++)
			addresses[i] = new InetSocketAddress(i);
	}

	public void testGetRandomNodesPrivately() {
		{
			// Test empty results set
			int nodeCount = 10;
			Set<InetSocketAddress> emptyInputset =
				new HashSet<InetSocketAddress>();
			Set<InetSocketAddress> resultsSet =
				new HashSet<InetSocketAddress>();
			assertEquals(0, Content.getRandomNodesPrivately(
					emptyInputset, nodeCount, resultsSet));
			assertEquals(0, resultsSet.size());
			assertTrue(emptyInputset.containsAll(resultsSet));
		}

		{
			// Input is small: addresses 0-inputsize
			int inputSize = 5;
			int outputSize = 10;
			Set<InetSocketAddress> smallInputSet =
				new HashSet<InetSocketAddress>();
			for(int i=0 ; i<inputSize ; i++)
				smallInputSet.add(addresses[i]);
			Set<InetSocketAddress> resultsSet =
				new HashSet<InetSocketAddress>();
			assertEquals(inputSize, Content.getRandomNodesPrivately(
					smallInputSet, outputSize, resultsSet));
			assertEquals(inputSize, resultsSet.size());
			assertTrue(smallInputSet.containsAll(resultsSet));
		}
		
		{
			// Input is large
			int inputSize = 10;
			int outputSize = 5;
			Set<InetSocketAddress> largeInputSet =
				new HashSet<InetSocketAddress>();
			for(int i=0 ; i<inputSize ; i++)
				largeInputSet.add(addresses[i]);
			Set<InetSocketAddress> resultsSet =
				new HashSet<InetSocketAddress>();
			int ret = Content.getRandomNodesPrivately(
					largeInputSet, outputSize, resultsSet);
			assertTrue(outputSize >= ret);
			assertEquals(ret, resultsSet.size());
			assertTrue(largeInputSet.containsAll(resultsSet));
		}
		
		{
			// Invoke three times
			int inputSize = 10;
			int outputSize1 = 5;
			int outputSize2 = 10;
			int outputSize3 = 11;
			Set<InetSocketAddress> largeInputSet =
				new HashSet<InetSocketAddress>();
			for(int i=0 ; i<inputSize ; i++)
				largeInputSet.add(addresses[i]);
			Set<InetSocketAddress> resultsSet =
				new HashSet<InetSocketAddress>();
			int ret1 = Content.getRandomNodesPrivately(
					largeInputSet, outputSize1, resultsSet);
			assertTrue(outputSize1 >= ret1);
			assertEquals(ret1, resultsSet.size());
			assertTrue(largeInputSet.containsAll(resultsSet));
			System.out.println("Results1=" + resultsSet.size());
			
			int ret2 = Content.getRandomNodesPrivately(
					largeInputSet, outputSize2, resultsSet);
			assertTrue(outputSize2 >= ret2);
			assertEquals(ret1 + ret2, resultsSet.size());
			assertTrue(largeInputSet.containsAll(resultsSet));
			System.out.println("Results2=" + resultsSet.size());

			int ret3 = Content.getRandomNodesPrivately(
					largeInputSet, outputSize3, resultsSet);
			assertTrue(outputSize3 >= ret3);
			assertEquals(ret1 + ret2 + ret3, resultsSet.size());
			assertTrue(largeInputSet.containsAll(resultsSet));
			System.out.println("Results3=" + resultsSet.size());
		}
	}
}
