package org.msrg.publiy.broker.core.contentManager;

import java.net.InetSocketAddress;
import java.util.List;



import org.msrg.publiy.networkcodes.PSCodedPiece;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPiece;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.IPSCodedBatch;
import junit.framework.TestCase;

public class ContentBreakPolicyTest extends TestCase implements IContentListener {

	protected static final int _rows = new Integer(System.getProperty("NC.ROWS", "200")).intValue();
	protected static final int _cols = new Integer(System.getProperty("NC.COLS", "10000")).intValue();
	
	static {
		SourcePSContent.initDefaultContent(_rows, _cols);
	}

	final int _WAIT_TIME_SEC = 1000;
	boolean _endTest = false;
	final int BROKERS_COUNT = new Integer(System.getProperty("NC.BROKERS", "20")).intValue();
	final InetSocketAddress _sourceAddress = Sequence.getIncrementalLocalAddresses(8880, 8880, 2)[0];
	final InetSocketAddress _dstAddress = Sequence.getIncrementalLocalAddresses(8890, 8890, 2)[0];
	final Publication _publication = new Publication().addPredicate("attr1", 50);
	final Sequence _contentSequence = Sequence.getRandomSequence(_sourceAddress);
	ContentManager _contentManagerSrc, _contentManagerDst;
	IBrokerShadow _brokerShadowSrc, _brokerShadowDst;
	
	final SourcePSContent _sourcePSContent =
		SourcePSContent.createDefaultSourcePSContent(_contentSequence, _publication, _rows, _cols);

	final int _minReceivedSlices = (int) (0.9 * _rows);
	final int _minRemainingSenders = 5;
	
	final ContentBreakPolicy _contentBreakPolicy = new ContentBreakPolicy(_minReceivedSlices, _minRemainingSenders, true);
		
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
		SourcePSContent.initDefaultContent(_rows, _cols);
		
		_brokerShadowSrc = new BrokerShadow(NodeTypes.NODE_BROKER, _sourceAddress).setNC(true).setNCCols(_cols).setNCRows(_rows);
		_contentManagerSrc = new ContentManager(null, _brokerShadowSrc);
		_contentManagerSrc.prepareToStart();
		_contentManagerSrc.startComponent();	

		_brokerShadowDst= new BrokerShadow(NodeTypes.NODE_BROKER, _dstAddress).setNC(true).setNCCols(_cols).setNCRows(_rows);
		_contentManagerDst = new ContentManager(null, _brokerShadowDst);
		_contentManagerDst.prepareToStart();
		_contentManagerDst.startComponent();	
	}

	@Override
	public void tearDown() {
		SourcePSContent.initDefaultContent();
	}
	
	public void test() {
		_contentManagerSrc.addContent(_sourcePSContent);
		_contentManagerDst.addContent(_sourceAddress, _contentSequence, null, _rows, _cols);
		
		for(int i=0 ; i<_rows ; i++) {
			_contentManagerSrc.encodeAndSend(_contentSequence, Sequence.getRandomLocalAddress(), this);
		}
		
		for(int i=0 ; i<_WAIT_TIME_SEC * 10 && !_endTest ; i++) {
			SystemTime.setSystemTime(i * 100);
			try{
				Thread.sleep(100);
			} catch (InterruptedException itx) { itx.printStackTrace(); }
		}
		
		if(_endTest)
			BrokerInternalTimer.inform("OK!");
		else
			fail("_endTest is: " + _endTest);
	}

	int i=0;
	final Object _LOCK = new Object();
	@Override
	public void codedContentReady(PSCodedPiece psCodedPiece, InetSocketAddress remote) {
		synchronized(_LOCK) {
			++i;
		}
		
		TNetworkCoding_CodedPiece tCodedPiece =
			new TNetworkCoding_CodedPiece(
					remote, null,
					psCodedPiece._sourceSeq,
					psCodedPiece.getCodedCoefficients(),
					psCodedPiece._codedContent, psCodedPiece._fromMainSeed);
		_contentManagerDst.addContent(remote, tCodedPiece);
		
		Content dstContent = _contentManagerDst.getContent(_contentSequence);
		List<InetSocketAddress> sendAddresses = _contentBreakPolicy.sendBreak(dstContent, true);
		if(sendAddresses != null)
			if(dstContent.getSendingRemotesSize() < _minRemainingSenders)
				assertTrue(dstContent.getSendingRemotesSize() >= _minRemainingSenders);
		
		if(i == _rows)
			_endTest = true;
	}

	@Override
	public void decodedContentInversed(Sequence sourceSequence) {
		BrokerInternalTimer.inform("BYE");
		return;
	}

	@Override
	public void decodedContentReady(boolean success,
			IPSCodedBatch psDecodedBatch) {
		BrokerInternalTimer.inform("DIE");
		return;
	}
}
