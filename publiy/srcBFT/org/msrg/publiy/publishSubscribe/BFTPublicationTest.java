package org.msrg.publiy.publishSubscribe;

import java.net.InetSocketAddress;

import org.msrg.publiy.publishSubscribe.Publication;


import junit.framework.TestCase;

public class BFTPublicationTest extends TestCase {

	InetSocketAddress _sAddr;
	
	@Override
	public void setUp() {
		_sAddr = new InetSocketAddress("127.0.0.1", 1000);
	}
	
	@Override
	public void tearDown() { }
	
	public void testEncodeDecode() {
		Publication bftPub1 = new BFTPublication(_sAddr).addPredicate("attr1", 10).addStringPredicate("saddr1", "val10");
		String enc = bftPub1.encode();
		Publication bftPub2 = BFTPublication.decode(enc);
		assertEquals(bftPub1, bftPub2);
	}
}
