package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.broker.core.connectionManager.BFTConnectionManager;
import junit.framework.TestCase;

public class SlidingWindowBFTDackIssuerProxyTest extends TestCase {

	protected final long _dackReceiptTimeout = BFTConnectionManager.BFT_DACK_RECEIVE_INTERVAL;

	protected final InetSocketAddress _dackIAddr = new InetSocketAddress("127.0.0.1", 4142);
	protected final InetSocketAddress _dackVAddr = new InetSocketAddress("127.0.0.1", 5142);
	
	protected SlidingWindowBFTDackIssuerProxy _iDackProxy;
	
	@Override
	public void setUp() {
		SystemTime.setSystemTime(0);
		_iDackProxy = new SlidingWindowBFTDackIssuerProxy(_dackIAddr, _dackVAddr, _dackReceiptTimeout);
	}

	@Override
	public void tearDown() { }
	
	public void testDackTooLate() {
		long time = SystemTime.currentTimeMillis();
		
		{
			SystemTime.setSystemTime(time);
			_iDackProxy.updateLastDackReceivedTime(SystemTime.currentTimeMillis());
			assertFalse(_iDackProxy.isDackReciptTooLate());
		}
		
		{
			time += (BFTConnectionManager.BFT_DACK_RECEIVE_INTERVAL - 1);
			SystemTime.setSystemTime(time);
			assertFalse(_iDackProxy.isDackReciptTooLate());
		}

		{
			time += 1;
			SystemTime.setSystemTime(time);
			assertTrue(_iDackProxy.isDackReciptTooLate());
		}
}
}
