package org.msrg.publiy.broker.core.contentManager;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.connectionManager.IConnectionManagerNC;
import org.msrg.publiy.broker.core.contentManager.servingPolicy.ContentServingPolicy;
import org.msrg.publiy.broker.core.flowManager.FlowSelectionPolicy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.IPSCodedBatch;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSBulkMatrix;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;

import org.msrg.publiy.networkcodes.PSCodedPiece;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReply;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.utils.SortableNodeAddress;
import org.msrg.publiy.utils.SortableNodeAddressSet;
import junit.framework.TestCase;

public class ContentManagerTest extends TestCase implements IContentListener {

	public static final int WAIT_CODING_FINISH_SEC = 2000;
	public static final int WAIT_DECODING_FINISH_SEC = 1500;
	
	protected final int _rows = Broker.ROWS;
	protected final int _cols = Broker.COLS;
	
	int _codedPieceReceived = 0;
	Sequence _sourceSequence;
	Publication _publication;
	
	IContentManager _contentManagerSrc;
	IContentManager _contentManagerDst;
	
	SourcePSContent _srcPSContent;
	IPSCodedBatch _psDeodedBatch;
	
	InetSocketAddress _remote, _localUDPAddress;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
		InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2000);
		_localUDPAddress = Broker.getUDPListeningSocket(localAddress); // Sequence.getRandomAddress();
		_remote = Sequence.getRandomAddress();
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setLocalUDPAddress(_localUDPAddress).setNC(true).setNCRows(_rows).setNCCols(_cols);
		_sourceSequence = brokerShadow.getLocalSequencer().getNext();
		_publication = new Publication().addPredicate("HELLO", 123123);
		
		_contentManagerSrc = new ContentManager(new ConnectionManagerNC_ForTest(brokerShadow, _localUDPAddress), brokerShadow);
		_contentManagerSrc.prepareToStart();
		_contentManagerSrc.startComponent();
		
		_contentManagerDst = new ContentManager(new ConnectionManagerNC_ForTest(brokerShadow, _remote), brokerShadow);
		_contentManagerDst.prepareToStart();
		_contentManagerDst.startComponent();
		
		_srcPSContent =
			SourcePSContent.createDefaultSourcePSContent(_sourceSequence, _publication, _rows, _cols);
	}

	@Override
	public void tearDown() { }
	
	public void test() {
		BrokerInternalTimer.inform("Start To Encode: " + _srcPSContent.getSize());

		_contentManagerSrc.addContent(_srcPSContent);
		for(int i=0 ; i<_rows ; i++)
			_contentManagerSrc.encodeAndSend(_sourceSequence, _remote, this);
		
		TNetworkCoding_CodedPieceId codedPieceId =
			_contentManagerSrc.getCodedPieceId(_sourceSequence, _remote);
		_contentManagerDst.addContent(codedPieceId);
		
		waitForCodingToFinish(WAIT_CODING_FINISH_SEC, WAIT_DECODING_FINISH_SEC);
		BrokerInternalTimer.inform("OK!");
	}

	private void waitForCodingToFinish(int waitCodingFinishSec, int waitDeodingFinishSec) {
		for(int i=0 ; i<waitCodingFinishSec * 10 ; i++) {
			try{
				Thread.sleep(100);
				if(i%10 == 0)
					BrokerInternalTimer.inform("Waiting...");
				if(i%100 == 0)
					startToDeocode("Let's ");
			} catch (InterruptedException itx) { fail("Interrupted!"); }
			
			if(_codedPieceReceived == _rows)
				break;
		}
		
		if(_codedPieceReceived != _rows)
			fail("Not all coded pieces received " + _codedPieceReceived);
		
		startToDeocode("Final ");
		
		for(int i=0 ; i<waitDeodingFinishSec * 10 ; i++) {
			try{
				Thread.sleep(100);
				if(i%10 == 0)
					BrokerInternalTimer.inform("Waiting...");
			} catch (InterruptedException itx) { fail("Interrupted!"); }
			
			if(_psDeodedBatch != null)
				break;
		}
		
		checkDecodedContent(_psDeodedBatch);
	}

	private void startToDeocode(String prefix) {
		BrokerInternalTimer.inform(prefix + "Start To Deocode: " + _srcPSContent.getSize());

//		assertTrue(_codedPieceReceived == _rows);
		_contentManagerDst.decode(_sourceSequence, this);
	}

	@Override
	public void codedContentReady(PSCodedPiece psCodedPiece, InetSocketAddress remote) {
		assertTrue(remote.equals(_remote));
		assertTrue(psCodedPiece._sourceSeq.equalsExact(_sourceSequence));
		
		_contentManagerDst.addContent(remote, psCodedPiece);
		_codedPieceReceived++;
	}

	@Override
	public void decodedContentReady(boolean success, IPSCodedBatch psDecodedBatch) {
		if(success)
			_psDeodedBatch = psDecodedBatch;
		else {
			assertNull(psDecodedBatch);
			Content content = _contentManagerDst.getContent(_sourceSequence);
			BrokerInternalTimer.inform("Decoding did not succeed, codedResultsReceived: " +
					(content==null?"NULL":content._codeBatch.getAvailableCodedPieceCount()));
		}
	}
	
	protected void checkDecodedContent(IPSCodedBatch psCodedBatch) {
		assertNotNull(psCodedBatch);
		
		assertTrue(_publication.equals(psCodedBatch.getPublication()));
		assertTrue(_sourceSequence.equals(psCodedBatch.getSourceSequence()));
		
		PSBulkMatrix psBM = (PSBulkMatrix) psCodedBatch.getBulkMatrix();
		assertTrue(psBM._cols == _cols);
		assertTrue(psBM._rows == _rows);
		assertTrue(psBM._sourceSeq.equalsExact(_sourceSequence));
		
		assertTrue(_srcPSContent._codeBatch.equalsExact(psCodedBatch));
	}

	@Override
	public void decodedContentInversed(Sequence sourceSequence) {
		return;		
	}
}

//class ContentManager_ForTest extends ContentManager {
//
//	public ContentManager_ForTest(IConnectionManagerNC connectionManagerNC) {
//		super(connectionManagerNC);
//	}
//
//	@Override
//	protected CasualContentLogger getContentLogger() {
//		return null;
//	}
//}

class ConnectionManagerNC_ForTest implements IConnectionManagerNC {

	protected final InetSocketAddress _localUDPAddress;
	protected final LocalSequencer _localSequencer;
	protected final IBrokerShadow _brokerShadow;
	
	ConnectionManagerNC_ForTest(IBrokerShadow brokerShadow, InetSocketAddress localUDPAddress) {
		_brokerShadow = brokerShadow;
		_localSequencer = _brokerShadow.getLocalSequencer();
		_localUDPAddress = localUDPAddress;
	}
	
	@Override
	public InetSocketAddress getUDPLocalAddress() {
		return _localUDPAddress;
	}

	@Override
	public void applyContentServingPolicy(Content content) {
	}

	@Override
	public ContentServingPolicy getContentServingPolicy() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isServingContent(Content content) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDefaultFlowPacketSize() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FlowSelectionPolicy getFlowSectionPolicy() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendCodedPieceIdReqBreak(Sequence sourceSequence,
			InetSocketAddress remote, int breakingSize) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<InetSocketAddress> getMatchingSet(Publication publication, boolean local) {
		return new HashSet<InetSocketAddress>();
	}

	public void addContentFlows(Content content) {
		return;
	}

	@Override
	public void publishContent(PSSourceCodedBatch psSourceCodedBatch) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendCodedPieceId(Content content, InetSocketAddress remote) {
		return;
	}

	@Override
	public boolean serveContent(Content content) {
		return false;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		throw new UnsupportedOperationException(); 
	}

	@Override
	public void sendPListReplyPublication(Publication plistReplyPublication) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBrokerShadow getBrokerShadow() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendPListReply(TNetworkCoding_PListReply pList, InetSocketAddress requester) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendTMPListRequest(TMulticast_Publish_MP tmp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendCodedPieceIdReqBreakDeclined(Sequence sourceSequence,
			InetSocketAddress requester, int outstandingPieces) {
		return;
	}

	@Override
	public SortedSet<SortableNodeAddress> convertToSortableLaunchNodeAddress(
			Set<InetSocketAddress> remotes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortedSet<SortableNodeAddress> convertToSortableNonLaunchNodeAddress(
			Set<InetSocketAddress> remotes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortableNodeAddressSet getSortableLaunchNodesSet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SortableNodeAddressSet getSortableNonLaunchNodesSet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void incrementLaunchSortableNodeAddress(
			Collection<InetSocketAddress> plistClients, double val) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void incrementNonLaunchSortableNodeAddress(
			Collection<InetSocketAddress> plistClients, double val) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public LocalSequencer getLocalSequencer() {
		return _localSequencer;
	}
}