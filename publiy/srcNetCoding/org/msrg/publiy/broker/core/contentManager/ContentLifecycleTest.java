package org.msrg.publiy.broker.core.contentManager;

import java.net.InetSocketAddress;
import java.util.Iterator;

import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import junit.framework.TestCase;

public class ContentLifecycleTest extends TestCase {

	static final int _PEERS_COUNT = 10;
	static {
		SystemTime.resetTime();
		BrokerInternalTimer.start();
	}
	
	ContentLifecycle _contentLC;
	
	final int _creationTime = 5;
	final int _decodeStartTime = 20;
	final int _decodeFinishTime = 100;
	
	InetSocketAddress[] _remotesSentPeice = Sequence.getRandomAddresses(_PEERS_COUNT);
	InetSocketAddress[] _remotesSentMeta = Sequence.getRandomAddresses(_PEERS_COUNT);;
	InetSocketAddress[] _remotesArrivedPiece = Sequence.getRandomAddresses(_PEERS_COUNT);
	InetSocketAddress[] _remotesArrivedMeta = Sequence.getRandomAddresses(_PEERS_COUNT);
	
	@Override
	public void setUp() {
		_contentLC = new ContentLifecycle(null);
	}

	public void testTimeToRequetMetadata() {
		int msLntervalBetweenRequests = 30;
		int tick = 100;
		long lastTime = 0;
		
		for(int msTime=0 ; msTime<100 ; msTime++) {
			SystemTime.setSystemTime(msTime * tick);
			if(_contentLC.requestMetaData(msLntervalBetweenRequests))
				lastTime = SystemTime.currentTimeMillis();
			
			assertTrue(lastTime - SystemTime.currentTimeMillis() < msLntervalBetweenRequests + tick);
			assertFalse(lastTime - SystemTime.currentTimeMillis() < 0);
		}
	}
	
	public void test() {
		runTest(100000);
	}
	
	@Override
	public void tearDown() {
		SystemTime.resetTime();
	}
	
	public void runTest(int testDuration) {
		for(int i=_creationTime ; i<testDuration*_PEERS_COUNT ; i++) {
			SystemTime.setSystemTime(i);
			if(SystemTime.currentTimeMillis() == _creationTime)
				_contentLC = new ContentLifecycle(null);
			else if(SystemTime.currentTimeMillis() == _decodeStartTime)
				_contentLC.setDecodeStartTime();
			else if(SystemTime.currentTimeMillis() == _creationTime)
				_contentLC.setDecodeCompleteTime();
			
			switch(i%4) {
			case 0:
				_contentLC.newPieceSent(_remotesSentPeice[i%_PEERS_COUNT]);
				break;
				
			case 1:
				_contentLC.newPieceArrived(_remotesArrivedPiece[i%_PEERS_COUNT]);
				break;
				
			case 2:
				_contentLC.metadataArrived(_remotesArrivedMeta[i%_PEERS_COUNT]);
				break;
				
			case 3:
				_contentLC.metadataSent(_remotesSentMeta[i%_PEERS_COUNT]);
				break;
			}
		}
		
		Iterator<ContentLifecycleEvent> pieceArrivedIt = _contentLC.getPiecesArrivalTimes().iterator();
		Iterator<ContentLifecycleEvent> pieceSentIt = _contentLC.getPiecesSentTimes().iterator();
		Iterator<ContentLifecycleEvent> metaArrivedIt = _contentLC.getMetadataArrivedTimes().iterator();
		Iterator<ContentLifecycleEvent> metaSentIt = _contentLC.getMetadataSentTimes().iterator();
		
		ContentLifecycleEvent event = null;
		for(int i=_creationTime ; i<testDuration*_PEERS_COUNT ; i++) {
			switch(i%4) {
			case 0:
				event = pieceSentIt.next();
				if(event._arrivalTime.timeMillis() != i)
					assertTrue(event._arrivalTime.timeMillis() == i);
				assertTrue(event._senderRemote.equals(_remotesSentPeice[i%_PEERS_COUNT]));
				break;
				
			case 1:
				event = pieceArrivedIt.next();
				assertTrue(event._arrivalTime.timeMillis() == i);
				assertTrue(event._senderRemote.equals(_remotesArrivedPiece[i%_PEERS_COUNT]));
				break;
				
			case 2:
				event = metaArrivedIt.next();
				assertTrue(event._arrivalTime.timeMillis() == i);
				assertTrue(event._senderRemote.equals(_remotesArrivedMeta[i%_PEERS_COUNT]));
				break;
				
			case 3:
				event = metaSentIt.next();
				assertTrue(event._arrivalTime.timeMillis() == i);
				assertTrue(event._senderRemote.equals(_remotesSentMeta[i%_PEERS_COUNT]));
				break;
			}
		}
	}
}
