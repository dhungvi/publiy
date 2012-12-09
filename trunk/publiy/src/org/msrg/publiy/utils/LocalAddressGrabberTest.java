package org.msrg.publiy.utils;

import java.net.InetSocketAddress;

import junit.framework.TestCase;

public class LocalAddressGrabberTest extends TestCase {

	public void testLocalAddress() {
		assertTrue(LocalAddressGrabber.isLocal(new InetSocketAddress("127.0.0.1", 2000)));
	}
}
