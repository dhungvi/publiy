package org.msrg.publiy.communication.core.packet.types;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import junit.framework.TestCase;

public class TBFTMessageManipulator_FilterPublicationTest extends TestCase {

	protected final String _tempdir = "." + FileUtils.separatorChar + "temp-" + getClass().getCanonicalName();
	protected final String _keysdir = _tempdir + FileUtils.separatorChar + "keys";
	protected final int _delta = 3;
	protected final Set<InetSocketAddress> _affectRemotes = new HashSet<InetSocketAddress>();
	protected final Set<InetSocketAddress> _affectSources = new HashSet<InetSocketAddress>();
	protected final Set<InetSocketAddress> _affectSubscribers = new HashSet<InetSocketAddress>();
	
	protected BrokerIdentityManager _idMan;
	protected TBFTMessageManipulator_FilterPublication _manipulator;
	
	@Override
	public void setUp() throws IOException {
		BrokerInternalTimer.start();
		assertTrue(FileUtils.prepareTopologyDirectory(_tempdir, _keysdir, 10, "n-", "127.0.0.1", 2000));
		_idMan = new BrokerIdentityManager(new InetSocketAddress("127.0.0.1", 2000),  _delta);
		_idMan.loadIdFile(_tempdir + FileUtils.separatorChar + "identityfile");
		int i=0;
		_affectRemotes.add(new InetSocketAddress("127.0.0.1", 2000 + (i++)));
		_affectRemotes.add(new InetSocketAddress("127.0.0.1", 2000 + (i++)));
		_affectRemotes.add(new InetSocketAddress("127.0.0.1", 2000 + (i++)));
		_affectSources.add(new InetSocketAddress("127.0.0.1", 2000 + (i++)));
		_affectSources.add(new InetSocketAddress("127.0.0.1", 2000 + (i++)));
		_affectSources.add(new InetSocketAddress("127.0.0.1", 2000 + (i++)));
		_affectSources.add(new InetSocketAddress("127.0.0.1", 2000 + (i++)));
		_affectRemotes.add(new InetSocketAddress("127.0.0.1", 2000 + (i++)));
	}
	
	@Override
	public void tearDown() {
		assertTrue(FileUtils.deleteDirectory(_tempdir));
		_affectRemotes.clear();
		_affectSources.clear();
		_affectSubscribers.clear();
	}
	
	public void testEncodeDecode() {
		_manipulator = new TBFTMessageManipulator_FilterPublication(3, _affectRemotes, _affectSources, _affectSubscribers);
		String str = _manipulator.toString(_idMan);
		TBFTMessageManipulator reconstructorManipulator = TBFTMessageManipulator.decode(str, _idMan);
		String str2 = reconstructorManipulator.toString(_idMan);
		assertEquals(_manipulator, reconstructorManipulator);
		assertEquals(str,  str2);
		BrokerInternalTimer.inform(str);
	}
	
	public void testEncodeDecodeNegative() {
		_manipulator = new TBFTMessageManipulator_FilterPublication(-1, _affectRemotes, _affectSources, _affectSubscribers);
		String str = _manipulator.toString(_idMan);
		TBFTMessageManipulator reconstructorManipulator = TBFTMessageManipulator.decode(str, _idMan);
		String str2 = reconstructorManipulator.toString(_idMan);
		assertEquals(_manipulator, reconstructorManipulator);
		assertEquals(str,  str2);
		BrokerInternalTimer.inform(str);
	}

	public void testManipulationNegative() {
		_manipulator = new TBFTMessageManipulator_FilterPublication(3, _affectRemotes, _affectSources, _affectSubscribers);
		assertEquals(3, _manipulator._dropCount);
	}
}
