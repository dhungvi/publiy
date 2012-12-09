package org.msrg.publiy.broker.core.contentManager.servingPolicy;

import java.io.IOException;
import java.net.InetSocketAddress;


import org.msrg.publiy.networkcodes.PSCodedPiece;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.connectionManager.IConnectionManagerNC;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.contentManager.ContentManager;
import org.msrg.publiy.broker.core.contentManager.IContentListener;
import org.msrg.publiy.broker.core.contentManager.SourcePSContent;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.IPSCodedBatch;
import org.msrg.publiy.broker.networkcoding.connectionManager.client.ConnectionManagerNC_Client;
import junit.framework.TestCase;

public class ContentServingPolicyTest extends TestCase implements IContentListener {
	static final int _rows = 100;
	static final int _cols = 20000;
	
	static {
		assertTrue(
				"VM-ARGS: -DBroker.CODING=true -DNC.ROWS=" + _rows + " -DNC.COLS=" + _cols,
				Broker.CODING && Broker.ROWS == _rows && Broker.COLS == _cols);
	}
	int _minReceivedPieces = 10;
	double _minReceivedPercentage = 0.5;
	ContentServingPolicy_ServeHalfReceived _servingPolicyHalfPieces, _servingPolicyHalfPercentage;
	ContentServingPolicy_ServeFullReceived _servingPolicyFull;
	
	InetSocketAddress _sourceAddress = new InetSocketAddress("127.0.0.1", 2000);
	InetSocketAddress _destinationAddress = new InetSocketAddress("127.0.0.1", 3000);
	ContentManager _contentManagerSrc;
	ContentManager _contentManagerDst;
	SourcePSContent _contentSrc;
	Content _contentDst;
	double _receivedCount = 0;
	Sequence _sourceSequence = Sequence.getRandomSequence();
	Publication _publication = new Publication().addPredicate("hi", 90);
	
	boolean _startedToDecode = false;
	boolean _finishedDecode = false;
	boolean _endTest = false;
	final int _WAIT_TIME_SEC = 100;
	Object _LOCK = new Object();
	
	@Override
	public void setUp() throws IOException {
		SystemTime.resetTime();
		BrokerInternalTimer.start();
		
		IBrokerShadow brokerShadowSrc = new BrokerShadow(NodeTypes.NODE_BROKER, _sourceAddress).setNC(true).setNCRows(_rows).setNCCols(_cols);
		_contentManagerSrc = new ContentManager_ForTest(null, brokerShadowSrc);
		_contentManagerSrc.prepareToStart();
		_contentManagerSrc.startComponent();
		IBrokerShadow brokerShadowDst = new BrokerShadow(NodeTypes.NODE_BROKER, _destinationAddress).setNC(true).setNCRows(_rows).setNCCols(_cols);
		_contentManagerDst = new ContentManager_ForTest(null, brokerShadowDst);
		_contentManagerDst.prepareToStart();
		_contentManagerDst.startComponent();
		
		_servingPolicyHalfPieces =
			new ContentServingPolicy_ServeHalfReceived(_minReceivedPieces);
		_servingPolicyHalfPercentage =
			new ContentServingPolicy_ServeHalfReceived(_minReceivedPercentage);
		_servingPolicyFull =
			new ContentServingPolicy_ServeFullReceived();
		
		_contentSrc = SourcePSContent.createDefaultSourcePSContent(
				_sourceSequence, _publication, _rows, _cols);
		_contentManagerSrc.addContent(_contentSrc);
		
		_contentDst = _contentManagerDst.addContent(
				null, _sourceSequence, _publication, _rows, _cols);

	}

	public void testHalfPolicy() {
		assertTrue(_servingPolicyHalfPercentage.canServe(_contentSrc));
		assertTrue(_servingPolicyHalfPieces.canServe(_contentSrc));
		
		assertFalse(_servingPolicyHalfPercentage.canServe(_contentDst));
		assertFalse(_servingPolicyHalfPieces.canServe(_contentDst));
		
		for(int i=0 ; i<_rows ; i++) {
			_contentManagerSrc.encodeAndSend(
					_sourceSequence, Sequence.getRandomAddress(), this);
		}
		
		for(int i=0 ; i<_WAIT_TIME_SEC * 10 ; i++) {
			SystemTime.setSystemTime(i * 100);
			try{
				Thread.sleep(100);
			} catch (InterruptedException itx) { itx.printStackTrace(); }

			if(_finishedDecode)
				BrokerInternalTimer.inform("Is decoded: " + _contentDst.isSolved());
			
			if(_endTest)
				break;
			
			if(i % 10 == 0)
				if(_startedToDecode)
					BrokerInternalTimer.inform("Decoding...");
				else
					BrokerInternalTimer.inform("Received " + (int)_receivedCount + "...");
		}
	}
	
	public void testFullPolicy() {
		assertTrue(_servingPolicyFull.canServe(_contentSrc));
		assertFalse(_servingPolicyFull.canServe(_contentDst));
	}

	@Override
	public void codedContentReady(PSCodedPiece psCodedPiece,
			InetSocketAddress remote) {
		synchronized(_LOCK) {
			_receivedCount++;
			_contentManagerDst.addContent(remote, psCodedPiece);
			
//			BrokerInternalTimer.inform("" +
//					_receivedCount + " vs. " + _minReceivedPieces + " vs. " +
//					_receivedCount + "/" + _cols + " vs. " + _minReceivedPercentage);
			if(_receivedCount < _minReceivedPieces)
				assertFalse(_servingPolicyHalfPieces.canServe(_contentDst));
			else
				assertTrue(_servingPolicyHalfPieces.canServe(_contentDst));
		
			if((_receivedCount/_rows) < _minReceivedPercentage)
				assertFalse(_servingPolicyHalfPercentage.canServe(_contentDst));
			else
				assertTrue(_servingPolicyHalfPercentage.canServe(_contentDst));
			
			if(_receivedCount == _rows) {
				_startedToDecode = true;
				_contentManagerDst.decode(_sourceSequence, this);
			}
		}
	}

	@Override
	public void decodedContentReady(boolean success,
			IPSCodedBatch psDecodedBatch) {
		_finishedDecode = true;
		assertTrue(success);
		if(!_servingPolicyFull.canServe(_contentDst))
			assertTrue(_servingPolicyFull.canServe(_contentDst));
		assertTrue(_servingPolicyHalfPieces.canServe(_contentDst));
		assertTrue(_servingPolicyHalfPercentage.canServe(_contentDst));
		_endTest = true;
	}

	@Override
	public void decodedContentInversed(Sequence sourceSequence) {
		throw new UnsupportedOperationException();		
	}
}

class ConnectionManagerNC_Client_ForTest extends ConnectionManagerNC_Client {

	protected ConnectionManagerNC_Client_ForTest() throws IOException {
		super(null, null, null, null, null, null, null, null);
	}
	
}

class ContentManager_ForTest extends ContentManager {

	public ContentManager_ForTest(IConnectionManagerNC connectionManagerNC, IBrokerShadow brokerShadow) {
		super(connectionManagerNC, brokerShadow);
	}
}