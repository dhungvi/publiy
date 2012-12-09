package org.msrg.publiy.broker.core.plistManager;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqBreak;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReply;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReq;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.utils.SortableNodeAddress;
import org.msrg.publiy.utils.SortableNodeAddressSet;
import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.connectionManager.IConnectionManagerNC;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.contentManager.servingPolicy.ContentServingPolicy;
import org.msrg.publiy.broker.core.flowManager.FlowSelectionPolicy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;

import junit.framework.TestCase;

public class PListManagerTest extends TestCase {
	
	static final int ROWS = 100;
	static final int COLS = 1000;

	int _crossMatchingClientsCount = 10;
	int _localMatchingClientsCount = 20;
	Set<InetSocketAddress> _matchingHomeClients =
		Sequence.getIncrementalLocalAddressesSet(4001, 4001+_localMatchingClientsCount-1, 1);
	Set<InetSocketAddress> _matchingCrossClients =
		Sequence.getIncrementalLocalAddressesSet(5001, 5001+_crossMatchingClientsCount-1, 1);
	
	protected final InetSocketAddress _homeAddress = new InetSocketAddress("127.0.0.1", 4000);
	protected final InetSocketAddress _crossAddress = new InetSocketAddress("127.0.0.1", 5000);
	
	protected IBrokerShadow _brokerShadow_homeBroker;
	protected IConnectionManagerNC _connManNC_homeBroker;
	protected PListManager_ForTest _plistManager_homeBroker;
	
	protected IBrokerShadow _brokerShadow_crossBroker;
	protected IConnectionManagerNC _connManNC_crossBroker;
	protected PListManager_ForTest _plistManager_crossBroker;
	
	final InetSocketAddress _sourceAddress = new InetSocketAddress("127.0.0.1", 3000);
	final InetSocketAddress _localClientAddress = _matchingHomeClients.iterator().next();
	final InetSocketAddress _crossClientAddress = _matchingCrossClients.iterator().next();
	final Sequence _contentSequence = Sequence.getRandomSequence(_sourceAddress);

	TNetworkCoding_PListReply _sentPList;
	TMulticast_Publish_MP _sentTMPListRequest;

	protected SortableNodeAddressSet _sortableLaunchNodeSet;
	protected SortableNodeAddressSet _sortableNonLaunchNodeSet;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start(false);
		_sortableLaunchNodeSet = new SortableNodeAddressSet();
		_sortableNonLaunchNodeSet = new SortableNodeAddressSet();
		_brokerShadow_homeBroker =
				new BrokerShadow(NodeTypes.NODE_BROKER, _homeAddress).setDelta(3);
		_connManNC_homeBroker =
			new ConnectionManagerNC_ForTest(_brokerShadow_homeBroker.getLocalSequencer(),
					this, _brokerShadow_homeBroker,
					_matchingHomeClients, new HashSet<InetSocketAddress>(),
					_sortableLaunchNodeSet, _sortableNonLaunchNodeSet);
		_plistManager_homeBroker =
				new PListManager_ForTest(_connManNC_homeBroker);
		
		 _brokerShadow_crossBroker =
					new BrokerShadow(NodeTypes.NODE_BROKER, _crossAddress).setDelta(3);
		_connManNC_crossBroker =
			new ConnectionManagerNC_ForTest(_brokerShadow_crossBroker.getLocalSequencer(),
					this, _brokerShadow_crossBroker,
					_matchingCrossClients, new HashSet<InetSocketAddress>(),
					_sortableLaunchNodeSet, _sortableNonLaunchNodeSet);
		_plistManager_crossBroker =
				new PListManager_ForTest(_connManNC_crossBroker);
	}
	
	@Override
	public void tearDown() {
		SystemTime.resetTime();
		_sortableLaunchNodeSet = null;
		_sortableNonLaunchNodeSet = null;
		_connManNC_crossBroker = null;
		_connManNC_homeBroker = null;
	}

	public void runTestEncodeDecodePListRequest(Publication plistReqPublication, Sequence expectedContentSequence, InetSocketAddress expectedHomeBroker) {
		assertFalse(PListManager.isPListReply(plistReqPublication));
		assertTrue(PListManager.isPListRequest(plistReqPublication));
		Sequence contentSequence = PListManager.getPListContentSequence(plistReqPublication);
		assertTrue(contentSequence.equalsExact(expectedContentSequence));
		InetSocketAddress homeBroker = PListManager.getPListHomeBroker(plistReqPublication);
		assertTrue(homeBroker.equals(expectedHomeBroker));
	}
	
	
	public void runTestEncodeDecodePListReply(
			Publication plistReplyPublication,
			Sequence expectedContentSequence,
			InetSocketAddress expectedHomeBroker,
			InetSocketAddress expectedReplyingBroker) {
		assertFalse(PListManager.isPListRequest(plistReplyPublication));
		assertTrue(PListManager.isPListReply(plistReplyPublication));
		Sequence contentSequence = PListManager.getPListContentSequence(plistReplyPublication);
		assertTrue(contentSequence.equalsExact(expectedContentSequence));
		InetSocketAddress homeBroker = PListManager.getPListHomeBroker(plistReplyPublication);
		assertTrue(homeBroker.equals(expectedHomeBroker));
		
		InetSocketAddress replyingBroker = PListManager.getPListReplyingBroker(plistReplyPublication);
		if(replyingBroker == null)
			replyingBroker = null;
		assertTrue(replyingBroker.equals(expectedReplyingBroker));
	}
	
	public void testEncodeDecode() {
		Sequence contentSequence = Sequence.getRandomSequence();
		InetSocketAddress homeBroker = Sequence.getRandomAddress();

		for(int i=0 ; i<11100 ; i++) {
			Publication basePublication =
				new Publication().
				addPredicate("ATTR1", 12123).
				addStringPredicate("ATTR2", "HI");
			Publication plistReqPublication =
				PListManager.turnIntoPListRequest(
						basePublication, homeBroker, contentSequence,
						ROWS, COLS);
			try {
				runTestEncodeDecodePListRequest(plistReqPublication, contentSequence, homeBroker);
			}catch(Exception x) {
				System.err.println("ERROR[" + i + "]: " + basePublication);
				System.err.println("ERROR: " + plistReqPublication);
				throw new IllegalStateException(x);
			}
			
			
			basePublication =
				new Publication().
				addPredicate("ATTR1", 12123).
				addStringPredicate("ATTR2", "HI");
			InetSocketAddress replyingBroker = Sequence.getRandomAddress();
			Publication plistReplyPublication =
				PListManager.turnIntoPListBrokerReply(plistReqPublication, replyingBroker, new LinkedList<InetSocketAddress>());
			try {
				runTestEncodeDecodePListReply(plistReplyPublication, contentSequence, homeBroker, replyingBroker);
			}catch(Exception x) {
				System.err.println("ERROR[" + i + "]: " + basePublication);
				System.err.println("ERROR: " + plistReplyPublication);
				throw new IllegalStateException(x);
			}
		}
		
		BrokerInternalTimer.inform("OK!");
	}
	
	public void testTurnIntos() {
		Publication pub = new Publication().
			addPredicate("ATTR1", 12312).addStringPredicate("ATTRSTR", "HI");
		InetSocketAddress homeBroker = Sequence.getRandomAddress();
		InetSocketAddress replyingBroker = Sequence.getRandomAddress();
		List<InetSocketAddress> matchingLocalClients =
			new LinkedList<InetSocketAddress>(Sequence.getRandomAddressesSet(40));
		Sequence contentSequence = Sequence.getRandomSequence();
		SortableNodeAddressSet sortableNodeSet = new SortableNodeAddressSet();
		
		Publication pubPListReq =
			PListManager.turnIntoPListRequest(
					pub, homeBroker, contentSequence,
					ROWS, COLS);
		assertNotNull(pubPListReq);
		assertTrue(pubPListReq.getStringPredicate("ATTRSTR").contains("HI"));
		assertTrue(pubPListReq.getPredicate("ATTR1").contains(new Integer(12312)));
		assertTrue(pubPListReq == pub);
		
		Publication pubPListReply =
			PListManager.turnIntoPListBrokerReply(pubPListReq, replyingBroker, matchingLocalClients);
		assertNotNull(pubPListReply);
		assertTrue(pubPListReply != pubPListReq);
		assertTrue(pubPListReply != pub);
		assertNull(pubPListReply.getStringPredicate("ATTRSTR"));
		assertNull(pubPListReply.getStringPredicate("ATTR1"));
		
		assertTrue(
				matchingLocalClients.equals(
						PListManager.getLocalMatchingClientsFromPListBrokersReply(
								sortableNodeSet, pubPListReply)));
		
		Publication pubPListReplyEmptyMatchingClients =
			PListManager.turnIntoPListBrokerReply(pubPListReq, replyingBroker, new LinkedList<InetSocketAddress>());
		assertNotNull(pubPListReply);
		assertTrue(pubPListReply != pubPListReq);
		assertTrue(pubPListReply != pub);
		assertNull(pubPListReply.getStringPredicate("ATTRSTR"));
		assertNull(pubPListReply.getStringPredicate("ATTR1"));
		assertTrue(
				PListManager.getLocalMatchingClientsFromPListBrokersReply(
						sortableNodeSet, pubPListReplyEmptyMatchingClients).size() == 0);
	}
	
	public void testPListRequestReply() {
		Publication sourcePublicaiton =
			new Publication().
			addPredicate("ATTR1", 1).
			addStringPredicate("ATTRSTR", "HI");
		
		{
			Publication pListRequestPublication =
				PListManager.turnIntoPListRequest(
						sourcePublicaiton, _homeAddress, _contentSequence,
						ROWS, COLS);
			assertNotNull(pListRequestPublication);
			assertTrue(pListRequestPublication == sourcePublicaiton);
			TNetworkCoding_PListReq pListReqFromSource =
				new TNetworkCoding_PListReq(
						_sourceAddress, _contentSequence,
						ROWS, COLS,
						pListRequestPublication, true);
			_plistManager_homeBroker.handlePListClientRequest(pListReqFromSource);
			PList pList_homeBroker = _plistManager_homeBroker.getPList(_contentSequence);
			assertNotNull(pList_homeBroker);
			assertTrue(pList_homeBroker._contentSequence.equals(_contentSequence));
//			assertTrue(pList_homeBroker.getCrossClientsSize() == 0);
			assertTrue(pList_homeBroker.getLocalClientsSize() == _matchingHomeClients.size());
			assertTrue(_matchingHomeClients.containsAll((pList_homeBroker.getLocalClients())));
		}
		
		{
			TNetworkCoding_PListReply sentPList = _sentPList;
			_sentPList = null;
			assertNotNull(sentPList);
			assertTrue(_matchingHomeClients.containsAll(sentPList._remotes));
			assertTrue(sentPList._remotes.size() == PListManager.MAX_PLIST_LOCAL_SIZE_SRC);
		}

		
		{
			assertNotNull(_sentTMPListRequest);
			TMulticast_Publish_MP sentTMPListRequest = _sentTMPListRequest;
			_sentTMPListRequest = null;
			_plistManager_crossBroker.handlePListBrokerRequest(sentTMPListRequest.getPublication());
			PList pList_crossBroker = _plistManager_crossBroker.getPList(_contentSequence);
			assertNotNull(pList_crossBroker);
			assertTrue(pList_crossBroker.getLocalClients().size() == _matchingCrossClients.size());
			assertTrue(_matchingCrossClients.containsAll(pList_crossBroker.getLocalClients()));
		}
		
		
		PList pList_home = null;
		{
			TMulticast_Publish_MP sentTMPListRequest = _sentTMPListRequest;
			_sentTMPListRequest = null;
			assertNotNull(sentTMPListRequest);
			Publication pListBrokerReply = sentTMPListRequest.getPublication();
			_plistManager_homeBroker.handlePListBrokerReply(pListBrokerReply);
			pList_home = _plistManager_homeBroker.getPList(_contentSequence);
			assertNotNull(pList_home);
			assertTrue(_matchingCrossClients.containsAll(pList_home.getCrossClients()));
			assertTrue(pList_home.getCrossClientsSize() ==
				PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY + PListManager.MAX_PLIST_LOCAL_SIZE_SRC);
		}
		
		{
			TNetworkCoding_PListReq pListReqFromSource2 =
				new TNetworkCoding_PListReq(
						_sourceAddress, _contentSequence,
						ROWS, COLS,
						sourcePublicaiton, true);
			_plistManager_homeBroker.handlePListClientRequest(pListReqFromSource2);
			TNetworkCoding_PListReply sentPList = _sentPList;
			_sentPList = null;
			assertNotNull(sentPList);
			for(InetSocketAddress matchingClient : sentPList._remotes)
				assertTrue(_matchingHomeClients.contains(matchingClient) | _matchingCrossClients.contains(matchingClient));
			assertTrue(sentPList._remotes.size() ==
				PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY + PListManager.MAX_PLIST_LOCAL_SIZE_SRC);
//				(_matchingCrossClients.size() > PListManager.MAX_PLIST_LOCAL_SIZE?PListManager.MAX_PLIST_LOCAL_SIZE:_matchingCrossClients.size()));
		}

		
		{
			TNetworkCoding_PListReq pListReqFromSibling =
				new TNetworkCoding_PListReq(
						_localClientAddress, _contentSequence,
						ROWS, COLS,
						sourcePublicaiton, false);
			_plistManager_homeBroker.handlePListClientRequest(pListReqFromSibling);
			TNetworkCoding_PListReply sentPList = _sentPList;
			_sentPList = null;
			assertNotNull(sentPList);
			for(InetSocketAddress matchingClient : sentPList._remotes)
				assertTrue(_matchingHomeClients.contains(matchingClient) | _matchingCrossClients.contains(matchingClient));
//			assertTrue(sentPList._remotes.size() >
//				(_matchingCrossClients.size() > PListManager.MAX_PLIST_LOCAL_SIZE?PListManager.MAX_PLIST_LOCAL_SIZE:_matchingCrossClients.size()));
			assertTrue(pList_home.getLocalClientsSize() == _localMatchingClientsCount);
			assertTrue(pList_home.getCrossClientsSize() == PListManager.MAX_PLIST_CROSS_SIZE_BROKER_BROKER_REPLY);
			assertTrue(sentPList._remotes.size() ==
				PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY + PListManager.MAX_PLIST_LOCAL_SIZE_SRC);
		}
	
	
		{
			TNetworkCoding_PListReq pListReqFromSibling2 =
				new TNetworkCoding_PListReq(
						_localClientAddress, _contentSequence,
						ROWS, COLS,
						sourcePublicaiton, false);
			_plistManager_homeBroker.handlePListClientRequest(pListReqFromSibling2);
			TNetworkCoding_PListReply sentPList = _sentPList;
			_sentPList = null;
			assertNotNull(sentPList);
			for(InetSocketAddress matchingClient : sentPList._remotes)
				assertTrue(_matchingHomeClients.contains(matchingClient) | _matchingCrossClients.contains(matchingClient));
//			assertTrue(sentPList._remotes.size() == PListManager.MAX_PLIST_LOCAL_SIZE + PListManager.MAX_PLIST_CROSS_SIZE);
			assertTrue(pList_home.getLocalClientsSize() == _localMatchingClientsCount);
			assertTrue(pList_home.getCrossClientsSize() ==
				PListManager.MAX_PLIST_CROSS_SIZE_BROKER_BROKER_REPLY - PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY);
			assertTrue(sentPList._remotes.size() ==
				PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY + PListManager.MAX_PLIST_LOCAL_SIZE_SRC);
		}
		
		{
			TNetworkCoding_PListReq pListReqFromSibling3 =
				new TNetworkCoding_PListReq(
						_localClientAddress, _contentSequence,
						ROWS, COLS,
						sourcePublicaiton, false);
			_plistManager_homeBroker.handlePListClientRequest(pListReqFromSibling3);
			TNetworkCoding_PListReply sentPList = _sentPList;
			_sentPList = null;
			assertNotNull(sentPList);
			for(InetSocketAddress matchingClient : sentPList._remotes)
				assertTrue(_matchingHomeClients.contains(matchingClient) | _matchingCrossClients.contains(matchingClient));
//			assertTrue(sentPList._remotes.size() == PListManager.MAX_PLIST_LOCAL_SIZE + PListManager.MAX_PLIST_CROSS_SIZE);
			assertTrue(pList_home.getLocalClientsSize() == _localMatchingClientsCount);
			assertTrue(pList_home.getCrossClientsSize() ==
				PListManager.MAX_PLIST_CROSS_SIZE_BROKER_BROKER_REPLY - PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY);
			assertTrue(sentPList._remotes.size() ==
				PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY + PListManager.MAX_PLIST_LOCAL_SIZE_SRC);
		}
		
		{
			TNetworkCoding_PListReq pListReqFromSibling4 =
				new TNetworkCoding_PListReq(
						_localClientAddress, _contentSequence,
						ROWS, COLS,
						sourcePublicaiton, false);
			_plistManager_homeBroker.handlePListClientRequest(pListReqFromSibling4);
			TNetworkCoding_PListReply sentPList = _sentPList;
			_sentPList = null;
			assertNotNull(sentPList);
			for(InetSocketAddress matchingClient : sentPList._remotes)
				assertTrue(_matchingHomeClients.contains(matchingClient) | _matchingCrossClients.contains(matchingClient));
//			assertTrue(sentPList._remotes.size() == PListManager.MAX_PLIST_LOCAL_SIZE + PListManager.MAX_PLIST_CROSS_SIZE);
			assertTrue(pList_home.getLocalClientsSize() == _localMatchingClientsCount);
			assertTrue(pList_home.getCrossClientsSize() ==
				PListManager.MAX_PLIST_CROSS_SIZE_BROKER_BROKER_REPLY - PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY);
			assertTrue(sentPList._remotes.size() ==
				PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY + PListManager.MAX_PLIST_LOCAL_SIZE_SRC);
		}
		
		{
			TNetworkCoding_PListReq pListReqFromSibling4 =
				new TNetworkCoding_PListReq(
						_localClientAddress, _contentSequence,
						ROWS, COLS,
						sourcePublicaiton, false);
			_plistManager_homeBroker.handlePListClientRequest(pListReqFromSibling4);
			TNetworkCoding_PListReply sentPList = _sentPList;
			_sentPList = null;
			assertNotNull(sentPList);
			for(InetSocketAddress matchingClient : sentPList._remotes)
				assertTrue(_matchingHomeClients.contains(matchingClient) | _matchingCrossClients.contains(matchingClient));
//			assertTrue(sentPList._remotes.size() == PListManager.MAX_PLIST_LOCAL_SIZE + PListManager.MAX_PLIST_CROSS_SIZE);
			assertTrue(pList_home.getLocalClientsSize() == _localMatchingClientsCount);
			assertTrue(pList_home.getCrossClientsSize() ==
				PListManager.MAX_PLIST_CROSS_SIZE_BROKER_BROKER_REPLY - 2 * PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY);
			assertTrue(sentPList._remotes.size() ==
				PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY + PListManager.MAX_PLIST_LOCAL_SIZE_SRC);
		}
		
		{
			assertTrue(_plistManager_homeBroker.getPList(_contentSequence).getLocalClientsSize() == _localMatchingClientsCount);
			for(InetSocketAddress localClientAddress : _matchingHomeClients) {
				int sizeBeforeBreakHome = pList_home.getLocalClientsSize();
				TNetworkCoding_CodedPieceIdReqBreak tBreakFromLocal =
					new TNetworkCoding_CodedPieceIdReqBreak(localClientAddress, _contentSequence, 0);
				_plistManager_homeBroker.handlePListBreak(tBreakFromLocal);
				int sizeAfterBeakHome = pList_home.getLocalClientsSize();
				assertTrue(sizeBeforeBreakHome - 1 == sizeAfterBeakHome);
			}
			assertTrue(_plistManager_homeBroker.getPList(_contentSequence).getLocalClientsSize() == 0);
		}
		
		{
			assertTrue(_plistManager_crossBroker.getPList(_contentSequence).getLocalClientsSize() == _crossMatchingClientsCount);
			for(InetSocketAddress crossClientAddress : _matchingCrossClients) {
				PList pList_cross = _plistManager_crossBroker.getPList(_contentSequence);
				int sizeBeforeBreakCross = pList_cross.getLocalClientsSize();
				TNetworkCoding_CodedPieceIdReqBreak tBreakFromCross =
					new TNetworkCoding_CodedPieceIdReqBreak(crossClientAddress, _contentSequence, 0);
				_plistManager_crossBroker.handlePListBreak(tBreakFromCross);
				int sizeAfterBreakCross = pList_cross.getLocalClientsSize();
				assertTrue(sizeBeforeBreakCross - 1 == sizeAfterBreakCross);
			}
			assertTrue(_plistManager_crossBroker.getPList(_contentSequence).getLocalClientsSize() == 0);
		}
		
		BrokerInternalTimer.inform("OK!");
	}
	
	void sentTMPListRequest(TMulticast_Publish_MP tmp) {
		_sentTMPListRequest = tmp;
	}
	
	void sendPListReply(TNetworkCoding_PListReply pList, InetSocketAddress requester) {
		_sentPList = pList;
	}
}

class PListManager_ForTest extends PListManager {

	public PListManager_ForTest(IConnectionManagerNC connManNC) {
		super(connManNC);
	}
}

class ConnectionManagerNC_ForTest implements IConnectionManagerNC {

	final PListManagerTest _tester;
	final Set<InetSocketAddress> _matchingCrossClients;
	final Set<InetSocketAddress> _matchingLocalClients;
	final SortableNodeAddressSet _sortableLaunchNodeSet;
	final SortableNodeAddressSet _sortableNonLaunchNodeSet;
	final LocalSequencer _localSequencer;
	
	IBrokerShadow _brokerShadow;
	ConnectionManagerNC_ForTest(
			LocalSequencer localSequencer,
			PListManagerTest tester,
			IBrokerShadow brokerShadow,
			Set<InetSocketAddress> matchingLocalClients,
			Set<InetSocketAddress> matchingCrossClients,
			SortableNodeAddressSet sortableLaunchNodeSet,
			SortableNodeAddressSet sortableNonLaunchNodeSet) {
		_localSequencer = localSequencer;
		_tester = tester;
		_brokerShadow = brokerShadow;
		_matchingCrossClients = matchingCrossClients;
		_matchingLocalClients = new HashSet<InetSocketAddress>();
		_sortableLaunchNodeSet = sortableLaunchNodeSet;
		_sortableNonLaunchNodeSet = sortableNonLaunchNodeSet;
		
		for(InetSocketAddress mLocalClient : matchingLocalClients)
			_matchingLocalClients.add(_sortableLaunchNodeSet.getSortableNodeAddress(mLocalClient));
	}
	
	@Override
	public void addContentFlows(Content content) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void applyContentServingPolicy(Content content) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}

	@Override
	public ContentServingPolicy getContentServingPolicy() {
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
	public InetSocketAddress getLocalAddress() {
		return _brokerShadow.getLocalAddress();
	}

	@Override
	public InetSocketAddress getUDPLocalAddress() {
		return _brokerShadow.getLocalUDPAddress();
	}

	@Override
	public boolean isServingContent(Content content) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<InetSocketAddress> getMatchingSet(Publication publication, boolean local) {
		return _matchingLocalClients;
	}

	@Override
	public void publishContent(PSSourceCodedBatch psSourceCodedBatch) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendCodedPieceId(Content content, InetSocketAddress remote) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendCodedPieceIdReqBreak(
			Sequence sourceSequence,
			InetSocketAddress remote, int breakingSize) {
		return;
	}

	@Override
	public void sendPListReply(TNetworkCoding_PListReply pList, InetSocketAddress requester) {
		_tester.sendPListReply(pList, requester);
	}

	@Override
	public void sendTMPListRequest(TMulticast_Publish_MP tmp) {
		_tester.sentTMPListRequest(tmp);
	}

	@Override
	public boolean serveContent(Content content) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void sendPListReplyPublication(Publication plistReplyPublication) {
		TMulticast_Publish_MP tmp =
			new TMulticast_Publish_MP(
					plistReplyPublication,
					_brokerShadow.getLocalAddress(),
					0, (byte)0,
					_brokerShadow.getPublicationForwardingStrategy(),
					_localSequencer.getNext());
		_tester.sentTMPListRequest(tmp);
	}
	
	@Override
	public String toString() {
		return "ConnectionManagerNC_ForTest:" + getLocalAddress().getPort();
	}

	protected Map<SortableNodeAddress, SortableNodeAddress> _sortedMatchingRemotesSet =
		new HashMap<SortableNodeAddress, SortableNodeAddress>();
	
	@Override
	public void incrementLaunchSortableNodeAddress(Collection<InetSocketAddress> remotes, double val) {
		if(remotes == null)
			return;
		
		for(InetSocketAddress remote : remotes)
			updateSortedMatchingRemotesSet(remote, val);
	}
	
	protected void updateSortedMatchingRemotesSet(InetSocketAddress remote, double val) {
		SortableNodeAddress sortedRemote = _sortedMatchingRemotesSet.get(remote);
		SortableNodeAddress newSortedRemote = _sortableLaunchNodeSet.getSortableNodeAddress(sortedRemote);
		_sortedMatchingRemotesSet.put(newSortedRemote, newSortedRemote);
	}

	@Override
	public void sendCodedPieceIdReqBreakDeclined(Sequence sourceSequence,
			InetSocketAddress requester, int outstandingPieces) {
		return;
	}

	@Override
	public void incrementNonLaunchSortableNodeAddress(
			Collection<InetSocketAddress> plistClients, double val) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortedSet<SortableNodeAddress> convertToSortableLaunchNodeAddress(
			Set<InetSocketAddress> remotes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortedSet<SortableNodeAddress> convertToSortableNonLaunchNodeAddress(
			Set<InetSocketAddress> remotes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortableNodeAddressSet getSortableLaunchNodesSet() {
		return _sortableLaunchNodeSet;
	}

	@Override
	public SortableNodeAddressSet getSortableNonLaunchNodesSet() {
		return _sortableNonLaunchNodeSet;
	}

	@Override
	public LocalSequencer getLocalSequencer() {
		return _localSequencer;
	}
}