package org.msrg.publiy.broker.security.keys;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.utils.FileUtils;

import junit.framework.TestCase;

public class KeyManagerTest extends TestCase {

	protected final String _tempdir = "." + FileUtils.separatorChar + "temp-" + getClass().getCanonicalName();
	protected final String _keysdir = _tempdir + FileUtils.separatorChar + "keys";
	protected final String identityfilename = _tempdir + FileUtils.separatorChar + "identityfile";

	protected final static int NUM_NODES = 10;
	protected final static int PORT_OFFSET = 1000;
	
	@Override
	public void setUp() {
		assertTrue(FileUtils.prepareTopologyDirectory(_tempdir, _keysdir, NUM_NODES, "n-", "127.0.0.1", PORT_OFFSET));
	}
	

	@Override
	public void tearDown() {
		assertTrue(FileUtils.deleteDirectory(_tempdir));
	}
	
	public void testLoadingKeys() throws IOException {
		BrokerIdentityManager idMan = new BrokerIdentityManager(Sequence.getRandomAddress(), 3);
		assertTrue("Loading identity file failed: " + identityfilename, idMan.loadIdFile(identityfilename));
		KeyManager km = new KeyManager(_keysdir, idMan);
		
		for(int i=0 ; i<NUM_NODES ; i++) {
			String idstr = "n-" + i;
			InetSocketAddress addr = new InetSocketAddress("127.0.0.1", PORT_OFFSET + i);
			
			PrivateKey prikey1 = km.getPrivateKey(idstr);
			PrivateKey prikey2 = km.getPrivateKey(addr);
			assertNotNull("Private key is null", prikey1);
			assertNotNull("Private key is null", prikey2);
			assertTrue("Private keys maintained for id=" + idstr + ", and addr=" + addr + ", mismatch.", prikey1 == prikey2);
			
			PublicKey pubkey1 = km.getPublicKey(idstr);
			PublicKey pubkey2 = km.getPublicKey(addr);
			assertNotNull("Public key is null", pubkey1);
			assertNotNull("Public key is null", pubkey2);
			assertTrue("Public keys maintained for id=" + idstr + ", and addr=" + addr + ", mismatch.", pubkey1 == pubkey2);
		}
	}
}
