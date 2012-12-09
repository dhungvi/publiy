package org.msrg.publiy.utils.security.dsa;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.SimpleBFTDigestable;
import org.msrg.publiy.broker.core.sequence.IBFTDigestable;
import junit.framework.TestCase;

public class DigestUtilsTest extends TestCase {

	byte[] _data = {1, 2, 3, 4};
	InetSocketAddress _sAddr = new InetSocketAddress("127.0.0.1", 1000);

	@Override
	public void setUp() { }
	
	public void testDigest() {
		IBFTDigestable svobj = new SimpleBFTDigestable(_sAddr, _data);
		DigestUtils du = new DigestUtils();
		
		byte[] digest = svobj.computeDigest(du);
		assertNotNull(digest);
		assert(digest.length == du.getDigestByteLength());
	}

}
