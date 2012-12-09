package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.security.KeyPair;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.utils.security.dsa.DSAKeyGen;

import junit.framework.TestCase;

public class SimpleBFTDackIssuerProxyTest extends TestCase {

	protected final InetSocketAddress _iAddr = new InetSocketAddress("127.0.0.1", 1000);
	protected final InetSocketAddress _vAddr = new InetSocketAddress("127.0.0.1", 1001);
	protected final InetSocketAddress _dackIAddr = _vAddr;
	protected final InetSocketAddress _dackVAddr = _iAddr;
	protected final KeyPair keypair1 = DSAKeyGen.getInstance().generateKeys(null, null);
	protected final KeyPair keypair2 = DSAKeyGen.getInstance().generateKeys(null, null);

	protected final long DACK_RECEIPT_TIMEOUT = 1000;
	protected IBFTIssuer _issuer;
	protected IBFTDackIssuerProxy _dackIssuerProxy;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start();

		_issuer = new SimpleBFTIssuer(_iAddr, keypair2.getPrivate(), null);
		_dackIssuerProxy = new SimpleBFTDackIssuerProxy(_dackIAddr, _dackVAddr, DACK_RECEIPT_TIMEOUT);
	}
	
	public void test() {
		assertEquals(_dackIAddr, _dackIssuerProxy.getDackIsserAddress());
		assertEquals(_dackVAddr, _dackIssuerProxy.getDackVerifierAddress());

		assertEquals(0, _dackIssuerProxy.getLastProxyDiscardedOrderFromMe());
		assertEquals(0, _dackIssuerProxy.getLastProxyReceivedOrderFromMe());
		
		SequencePair sp1 = new SequencePair(_issuer, 0, 0, _vAddr, new byte[0], 0, 0);
		_dackIssuerProxy.setLastProxyDiscardedOrderFromMe(sp1);
		assertEquals(sp1._lastDiscardedOrder, _dackIssuerProxy.getLastProxyDiscardedOrderFromMe());
		assertEquals(0, _dackIssuerProxy.getLastProxyReceivedOrderFromMe());
		
		SequencePair sp2 = new SequencePair(_issuer, 0, 0, _vAddr, new byte[0], 10, 20);
		_dackIssuerProxy.setLastProxyDiscardedOrderFromMe(sp2);
		assertEquals(sp2._lastDiscardedOrder, _dackIssuerProxy.getLastProxyDiscardedOrderFromMe());
		assertEquals(0, _dackIssuerProxy.getLastProxyReceivedOrderFromMe());
		_dackIssuerProxy.setLastProxyReceivedOrderFromMe(sp2);
		assertEquals(sp2._lastDiscardedOrder, _dackIssuerProxy.getLastProxyDiscardedOrderFromMe());
		assertEquals(sp2._lastReceivedOrder, _dackIssuerProxy.getLastProxyReceivedOrderFromMe());
		
		SequencePair sp3 = new SequencePair(_issuer, 0, 0, _vAddr, new byte[0], 30, 10);
		_dackIssuerProxy.setLastProxyDiscardedOrderFromMe(sp3);
		assertEquals(sp2._lastDiscardedOrder, _dackIssuerProxy.getLastProxyDiscardedOrderFromMe());
		assertEquals(sp2._lastReceivedOrder, _dackIssuerProxy.getLastProxyReceivedOrderFromMe());
		_dackIssuerProxy.setLastProxyReceivedOrderFromMe(sp3);
		assertEquals(sp3._lastReceivedOrder, _dackIssuerProxy.getLastProxyReceivedOrderFromMe());
		assertEquals(sp2._lastDiscardedOrder, _dackIssuerProxy.getLastProxyDiscardedOrderFromMe());
	}
}
