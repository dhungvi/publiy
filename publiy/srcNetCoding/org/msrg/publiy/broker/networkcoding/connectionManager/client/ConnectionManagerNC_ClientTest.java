package org.msrg.publiy.broker.networkcoding.connectionManager.client;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.core.sequence.SequenceCounter;
import junit.framework.TestCase;

public class ConnectionManagerNC_ClientTest extends TestCase {

	protected final InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 1000);
	protected final InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 2000);
	protected final Sequence contentSequence = Sequence.getRandomSequence(address1);
	
	public void test_canSendPListRequestForContentSequence() {
		Map<Sequence, Long> lastPListRequestSentForContentSequence =
			new HashMap<Sequence, Long>();
		long initTime = new Random().nextLong();
		initTime = initTime < 0 ? -initTime : initTime;
		SystemTime.setSystemTime(initTime);
		
		boolean bFist = ConnectionManagerNC_Client.canSendPListRequestForContentSequence(
				lastPListRequestSentForContentSequence, contentSequence);
		assertTrue(bFist);
		
		SystemTime.setSystemTime(initTime + Broker.PLIST_REQUEST_MIN_INTERVAL-1);
		boolean bEarly = ConnectionManagerNC_Client.canSendPListRequestForContentSequence(
				lastPListRequestSentForContentSequence, contentSequence);
		assertFalse(bEarly);
		
		SystemTime.setSystemTime(initTime + Broker.PLIST_REQUEST_MIN_INTERVAL-1);
		bEarly = ConnectionManagerNC_Client.canSendPListRequestForContentSequence(
				lastPListRequestSentForContentSequence, contentSequence);
		assertFalse(bEarly);
		
		SystemTime.setSystemTime(initTime + Broker.PLIST_REQUEST_MIN_INTERVAL);
		boolean bLate = ConnectionManagerNC_Client.canSendPListRequestForContentSequence(
				lastPListRequestSentForContentSequence, contentSequence);
		assertTrue(bLate);
		
		SystemTime.setSystemTime(initTime + 2 * Broker.PLIST_REQUEST_MIN_INTERVAL);
		boolean bTooLate = ConnectionManagerNC_Client.canSendPListRequestForContentSequence(
				lastPListRequestSentForContentSequence, contentSequence);
		assertTrue(bTooLate);
	}
	
	public void test_checkSequencePListCounter() {
		Map<Sequence, SequenceCounter> sequencePListCounter =
			new HashMap<Sequence, SequenceCounter>();
		
		boolean b;
		for(int i=0 ; i<ConnectionManagerNC_Client.SOURCE_PLISTS_REQUESTS ; i++) {
			b = ConnectionManagerNC_Client.checkSequencePListCounter(
					sequencePListCounter, contentSequence, true);
			assertTrue("Call #" + i + " must return true.", b);
		}
		
		b = ConnectionManagerNC_Client.checkSequencePListCounter(
				sequencePListCounter, contentSequence, true);
		assertFalse("Third call must return false.", b);
		
		for(int i=0 ; i<=ConnectionManagerNC_Client.SOURCE_PLISTS_REQUESTS ; i++) {
			ConnectionManagerNC_Client.updateSequencePListCounter(
					sequencePListCounter, contentSequence, address1);
		}
		
		for(int i=0 ; i<ConnectionManagerNC_Client.SOURCE_PLISTS_REQUESTS ; i++) {
			b = ConnectionManagerNC_Client.checkSequencePListCounter(
					sequencePListCounter, contentSequence, true);
			assertTrue("Call #" + i + " must return true.", b);
		}
		
		b = ConnectionManagerNC_Client.checkSequencePListCounter(
				sequencePListCounter, contentSequence, true);
		assertFalse("Third call must return false.", b);
	}
}
