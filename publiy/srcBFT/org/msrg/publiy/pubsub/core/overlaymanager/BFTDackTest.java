package org.msrg.publiy.pubsub.core.overlaymanager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


import org.msrg.publiy.client.subscriber.ISubscriptionListener;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.communication.core.sessions.SessionObjectTypes;
import org.msrg.publiy.communication.core.sessions.SessionTypes;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.BFTPublication;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.IBFTSubscriptionManager;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.messagequeue.BFTMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.IBFTMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.PSSession;
import org.msrg.publiy.pubsub.core.messagequeue.PSSessionBFT;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_UnSubscribe;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.pubsub.core.packets.recovery.TempSessionRecoveryDataRepository;
import org.msrg.publiy.pubsub.core.subscriptionmanager.BFTSubscriptionManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.LocalSubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;

import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.SimpleBFTSuspectedRepo;
import org.msrg.publiy.broker.core.IBFTConnectionManager;
import org.msrg.publiy.broker.core.connectionManager.BFTConnectionManager;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent_BFTDackSend;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent_purgeMQ;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerTypes;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxyRepo;
import org.msrg.publiy.broker.info.PublicationInfo;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.PropertyGrabber;
import org.msrg.publiy.utils.log.casuallogger.BFTDackLogger;

import junit.framework.TestCase;

public class BFTDackTest extends TestCase implements ISubscriptionListener {

	protected final static String TOP_FILENAME_DELTA2 = "." + FileUtils.separatorChar + "misc" + FileUtils.separatorChar + "testdata" + FileUtils.separatorChar + "tops" + FileUtils.separatorChar + "OverlayManagerTest_1.top";

	protected InetSocketAddress[] _allAddresses;
	protected IBFTOverlayManager[] _allOverlayManagers;
	protected IBFTBrokerShadow[] _allBrokerShadows;
	protected BFTMessageQueue_ForTest[] _allMessageQueues;
	protected IBFTSubscriptionManager[] _allSubscriptionManagers;
	protected LocalSequencer[] _allLocalSequencers;
	protected BFTConnectionManager_ForTest[] _allConnectionManagers;
	protected MockMessageSendReceive _mockMessageSendReceive;
	
	protected final String _tempdir = "." + FileUtils.separatorChar + "temp-" + getClass().getCanonicalName();
	protected final String _keysdir = _tempdir + FileUtils.separatorChar + "keys";

	protected final int _delta = 2;
	protected final int _numNodes = 21;
	protected final String _ipstr = "127.0.0.1";
	protected final String _nodeNamePrefix = "n-";
	protected final int _portOffset = 2000;
	
	@Override
	public void setUp() throws IOException {
		BrokerInternalTimer.start();
		Properties arguments = new Properties();
		arguments.setProperty(PropertyGrabber.PROPERTY_KEYS_DIR, _keysdir);

		_allBrokerShadows = new IBFTBrokerShadow[_numNodes];
		_allLocalSequencers = new LocalSequencer[_numNodes];
		_allConnectionManagers = new BFTConnectionManager_ForTest[_numNodes];
		_allAddresses = new InetSocketAddress[_numNodes];//FileCommons.getAllInetAddressesFromLines(localSequencer, TOP_FILENAME);
		_allMessageQueues = new BFTMessageQueue_ForTest[_numNodes];
		_allSubscriptionManagers = new IBFTSubscriptionManager[_numNodes];
		_allOverlayManagers = new IBFTOverlayManager[_numNodes];
		
		for(int i=0 ; i<_numNodes ; i++)
			_allAddresses[i] = new InetSocketAddress("127.0.0.1", 2000 + i);
		_mockMessageSendReceive = new MockMessageSendReceive();
		assertTrue(FileUtils.prepareTopologyDirectory(_tempdir, _keysdir, _numNodes, _nodeNamePrefix, _ipstr, 2000));

		for(int i=0 ; i<_numNodes ; i++) {
			_allBrokerShadows[i] =
					new BFTBrokerShadow(NodeTypes.NODE_BROKER, _delta, _allAddresses[i], _tempdir, _tempdir, _tempdir + FileUtils.separatorChar + "identityfile", arguments);
			new BFTDackLogger((BFTBrokerShadow) _allBrokerShadows[i]);
			new SimpleBFTSuspectedRepo((BFTBrokerShadow) _allBrokerShadows[i]);
			_allLocalSequencers[i] = _allBrokerShadows[i].getLocalSequencer();
			_allOverlayManagers[i] =
					initOverlayManager(2, _allAddresses[i],TOP_FILENAME_DELTA2);
			_allSubscriptionManagers[i] =
					new BFTSubscriptionManager_ForTest(
							this, _allBrokerShadows[i], _allAddresses); //_allOverlayManagers[i].getNeighbors(_allAddresses[i]));
			_allMessageQueues[i] =
					new BFTMessageQueue_ForTest(this, _allBrokerShadows[i], null, _allOverlayManagers[i], _allSubscriptionManagers[i]);
			_allConnectionManagers[i] =
					new BFTConnectionManager_ForTest(
							"conn-mann-" + _allBrokerShadows[i],
							ConnectionManagerTypes.CONNECTION_MANAGER_PUBSUB,
							null,
							_allBrokerShadows[i],
							_allOverlayManagers[i],
							_allSubscriptionManagers[i],
							_allMessageQueues[i],
							null);
			_mockMessageSendReceive.register(_allAddresses[i], _allConnectionManagers[i]);
		}
		
		for(int i=0 ; i<_numNodes ; i++) {
			InetSocketAddress[] immediateNeighbors =
					_allOverlayManagers[i].getNeighbors(_allAddresses[i]);
			
			Set<ISession> newSessions = new HashSet<ISession>();
			for(InetSocketAddress immediateNeighbor : immediateNeighbors) {
				if(immediateNeighbor.equals(_allAddresses[i]))
					continue;
				
				ISession session = new ISessionBFT_ForTest(
						this, _allBrokerShadows[i], SessionTypes.ST__PUBSUB_PEER_PUBSUB).
							setRemoteCC(immediateNeighbor).
							setSessionConnectionType(SessionConnectionType.S_CON_T_ACTIVE);
				
				newSessions.add(session);
			}
			int neighborsCount = newSessions.size();
			_allMessageQueues[i].replaceSessions(null, newSessions);
			assertEquals("Not all immediate neighbors were added: " + i,
					neighborsCount,
					_allMessageQueues[i].getPSSessionSize());
		}
		
//		Thread t = new Thread(_mockMessageSendReceive);
//		t.start();
	}
	
	@Override
	public void tearDown() throws Exception {
		assertTrue(FileUtils.deleteDirectory(_tempdir));
		_mockMessageSendReceive.stop();
	}
	
	protected IBFTOverlayManager initOverlayManager(int delta, InetSocketAddress iAddr, String topologyFilename) {
		LocalSequencer localSequencer = LocalSequencer.init(null, iAddr);
		Properties arguments = new Properties();
		arguments.setProperty(PropertyGrabber.PROPERTY_KEYS_DIR, _keysdir);
		BFTBrokerShadow brokerShadow =
				new BFTBrokerShadow(
						NodeTypes.NODE_BROKER, delta, iAddr, _tempdir, _tempdir, _tempdir + FileUtils.separatorChar + "identityfile", arguments);
		new SimpleBFTIssuerProxyRepo(brokerShadow);

		TRecovery_Join[] trjs =
				ConnectionManagerFactory.readRecoveryTopology(
						localSequencer, topologyFilename);
		IBFTOverlayManager bftOverlayMananger =
				(IBFTOverlayManager) new BFTOverlayManager(brokerShadow);
		bftOverlayMananger.applyAllJoinSummary(trjs);
		return bftOverlayMananger;
	}

	private int getIndex(InetSocketAddress key) {
		return key.getPort() - _portOffset;
	}

	protected List<ReceiverMessagePair> _checkedOutMessages =
			new LinkedList<ReceiverMessagePair>();
	protected List<TMulticast> _locallyDeliveredMessages =
			new LinkedList<TMulticast>();
	protected List<TMulticast> _locallyConfirmedMessages =
			new LinkedList<TMulticast>();
	
	public void informCheckout(ISessionBFT_ForTest sessionBFT, TMulticast tm, IRawPacket raw) {
		// verify who checked out what!
		InetSocketAddress remote = sessionBFT.getRemoteAddress();
		BrokerInternalTimer.inform((raw==null ? " NO: " : "YES: ") + sessionBFT + ": " + tm);
		if(raw != null) {
			ReceiverMessagePair pair = new ReceiverMessagePair(remote, (TMulticast_Publish_BFT) raw.getObject());
			_checkedOutMessages.add(pair);
		}
	}

	@Override
	public void tmConfirmed(TMulticast tm) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_BFT:
			_locallyConfirmedMessages.add(tm);
			break;
			
		case T_MULTICAST_PUBLICATION_BFT_DACK:
			break;
			
		default:
			throw new IllegalStateException("Did not expect this: " + tm);
		}
	}

	@Override
	public void matchingPublicationDelivered(TMulticast_Publish tmp) {
		_locallyDeliveredMessages.add(tmp);
	}

	@Override
	public void matchingPublicationDelivered(Sequence sourceSequence, Publication publication) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PublicationInfo[] getReceivedPublications() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getCount() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Integer> getDeliveredPublicationCounterPerPublisher() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Long> getLastPublicationDeliveryTimesPerPublisher() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Publication> getLastPublicationDeliveredPerPublisher() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PublicationInfo getLastReceivedPublications() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getLastPublicationReceiptTime() {
		throw new UnsupportedOperationException();
	}
	
	public void test() {
		for(int i=0 ; i<_numNodes ; i++)
			assertEquals("OM: " + _allOverlayManagers[i],
					_allOverlayManagers[i].getNeighborhoodSize(),
					_numNodes);
		
		{
			BFTPublication publication2000 =
					new BFTPublication(_allAddresses[0]).
						addStringPredicate("Tester", getClass().getCanonicalName());
			TMulticast_Publish_BFT bftPublication2000 =
					new TMulticast_Publish_BFT(
							publication2000, _allAddresses[0], _allLocalSequencers[0].getNext());
			_allOverlayManagers[0].issueLocalMessageSequencePair(bftPublication2000);
			_allMessageQueues[0].addNewMessage(bftPublication2000);
			assertEquals(3, _checkedOutMessages.size());
			
			int i=0;
			for(i=0 ; _checkedOutMessages.size() != 0 ; i++) {
				ReceiverMessagePair pair = _checkedOutMessages.remove(0);
				TMulticast tm = pair._tm;
				InetSocketAddress remote = pair._receiver;;
				int index = getIndex(remote);
				_allMessageQueues[index].addNewMessage(tm);
				BrokerInternalTimer.inform(i + ": " + remote.getPort() + ": " + tm);
			}
			assertEquals(0, _checkedOutMessages.size()); //Obvious
			assertEquals(_numNodes-1, i);
			// edge nodes confirm immediately
			assertEquals(12, _locallyConfirmedMessages.size());
			
			for(int j=0 ; j<_numNodes ; j++) {
				int pendingMessageCountBefore = _checkedOutMessages.size();
				ConnectionEvent_BFTDackSend_ForTest event =
						new ConnectionEvent_BFTDackSend_ForTest(_allLocalSequencers[j]);
				_allConnectionManagers[j].handleConnectionEvent_Special(event);
				int pendingMessageCountAfter = _checkedOutMessages.size();
				int msgsGeneratedCount = pendingMessageCountAfter - pendingMessageCountBefore;
				assertEquals("At node: [" + j + "] OM: " + _allOverlayManagers[j],
						_allOverlayManagers[j].getNeighbors(_allAddresses[j]).length - 1,
						msgsGeneratedCount);
				
				for(; _checkedOutMessages.size() != 0 ; i++) {
					ReceiverMessagePair pair = _checkedOutMessages.remove(0);
					TMulticast tm = pair._tm;
					InetSocketAddress remote = pair._receiver;;
					int index = getIndex(remote);
					_allMessageQueues[index].addNewMessage(tm);
					BrokerInternalTimer.inform(i + ": " + remote.getPort() + ": " + tm);
				}
			}
			assertEquals(0, _checkedOutMessages.size()); //Obvious
			
			for(int j=0 ; j<_numNodes ; j++) {
				ConnectionEvent_purgeMQ event = new ConnectionEvent_purgeMQ(_allLocalSequencers[j]);
				_allConnectionManagers[j].handleConnectionEvent_purgeMQ(event);
			}
			assertEquals(_numNodes, _locallyConfirmedMessages.size());
			
			for(int j=0 ; j<_numNodes ; j++) {
				assertEquals("Node [" + j + "] has non-confirmed message queue.",
						0, _allMessageQueues[j].getHeadToToeUnConfrmedSize());
				assertEquals("Node [" + j + "] has non-empty message queue.",
						0, _allMessageQueues[j].getHeadToToeSupposedlySize());
			}
		}
	}
}

class ISessionBFT_ForTest extends ISession {

	protected final BFTDackTest _tester;
	
	ISessionBFT_ForTest(BFTDackTest tester, IBFTBrokerShadow brokerShadow, SessionTypes type) {
		super(brokerShadow, SessionObjectTypes.ISESSION_BFT, type);
		
		_tempRepositoryObject = new TempSessionRecoveryDataRepository(this);
		_tester = tester;
	}
	
	@Override
	public IRawPacket hasCheckedout(TMulticast tm, IRawPacket raw) {
		_tester.informCheckout(this, tm, raw);
		return raw;
	}
}



class MockMessageSendReceive implements Runnable {
	
	class PendingMessageEntry {
		final InetSocketAddress _receiver;
		final TMulticast _tm;
		
		PendingMessageEntry(InetSocketAddress receiver, TMulticast tm) {
			_tm = tm;
			_receiver = receiver;
		}
	}
	
	private final Map<InetSocketAddress, BFTConnectionManager_ForTest> _registeredConnectionManagers =
			new HashMap<InetSocketAddress, BFTConnectionManager_ForTest>();
	private final List<PendingMessageEntry> _pendingSendMessages =
			new LinkedList<PendingMessageEntry>();
	private final Object _lock = new Object();
	
	private boolean _stopped = false;
	
	public void stop() {
		_stopped = true;
	}
	
	public void register(InetSocketAddress remote, BFTConnectionManager_ForTest connectionManager) {
		synchronized(_lock) {
			if(_registeredConnectionManagers.put(remote, connectionManager) != null)
				throw new IllegalStateException("Connection manager for " + remote + " already registered.");
			_lock.notify();
		}
	}
	
	@Override
	public void run() {
		while(!_stopped) {
			TMulticast tm = null;
			BFTConnectionManager_ForTest connectionManager = null;
			
			synchronized(_lock) {
				if(_pendingSendMessages.size() == 0)
					try {
						_lock.wait();
					} catch (InterruptedException e) { throw new IllegalStateException(e); }

				PendingMessageEntry pendingMessage = _pendingSendMessages.get(0);
				InetSocketAddress receiver = pendingMessage._receiver;
				tm = pendingMessage._tm;
				connectionManager = _registeredConnectionManagers.get(receiver);
			}
			
			connectionManager.handleMulticastMessage(null, tm);
		}
	}
}

class BFTConnectionManager_ForTest extends BFTConnectionManager {
	
	protected BFTConnectionManager_ForTest(
			String connectionManagerName,
			ConnectionManagerTypes type, IBroker broker,
			IBFTBrokerShadow bftBrokerShadow,
			IBFTOverlayManager bftOverlayManager,
			IBFTSubscriptionManager bftSubscriptionManager,
			IBFTMessageQueue bftMessageQueue,
			TRecovery_Join[] trjs) throws IOException {
		super(connectionManagerName, type, broker, bftBrokerShadow, bftOverlayManager,
				bftSubscriptionManager, bftMessageQueue, trjs);
	}

	@Override
	protected void handleMulticastMessage(ISession session, TMulticast tm) {
		super.handleMulticastMessage(session, tm);
	}
	
	@Override
	public boolean handleConnectionEvent_Special(ConnectionEvent connEvent) {
		return super.handleConnectionEvent_Special(connEvent);
	}
	
	@Override
	public BrokerOpState getBrokerOpState() {
		return BrokerOpState.BRKR_PUBSUB_PS;
	}

	@Override
	protected void handleConnectionEvent_purgeMQ(ConnectionEvent_purgeMQ connEvent) {
		super.handleConnectionEvent_purgeMQ(connEvent);
	}
}

class BFTMessageQueue_ForTest extends BFTMessageQueue {

	private final BFTDackTest _tester;
	
	protected BFTMessageQueue_ForTest(BFTDackTest tester, 
			IBFTBrokerShadow bftBrokerShadow,
			IBFTConnectionManager bftConnectionManager,
			IBFTOverlayManager bftOverlayManager,
			IBFTSubscriptionManager bftSubscriptionManager) {
		super(bftBrokerShadow, bftConnectionManager, bftOverlayManager,
				bftSubscriptionManager);
		
		_tester = tester;
	}

	@Override
	public BrokerOpState getBrokerOpState() {
		return BrokerOpState.BRKR_PUBSUB_PS;
	}
	
	@Override
	protected PSSession createNewPSSession(ISession newSession) {
		return new PSSessionBFT_ForTest(newSession, this, _bftOverlayManager);
	}
	
	@Override
	protected boolean isDuplicate(TMulticast tm) {
		return false;
	}
	
	@Override
	public void addNewMessage(TMulticast tm, ITMConfirmationListener tmConfirmationListener) {
		if(tmConfirmationListener != null)
			throw new IllegalArgumentException();
		
		super.addNewMessage(tm, _tester);
	}
}

class PSSessionBFT_ForTest extends PSSessionBFT {

	protected PSSessionBFT_ForTest(ISession session, BFTMessageQueue bftMessageQueue, IBFTOverlayManager overlayManager) {
		super(session, bftMessageQueue, overlayManager);
	}

	@Override
	public boolean send(IRawPacket raw) {
		return true;
	}
}

class BFTSubscriptionManager_ForTest extends BFTSubscriptionManager {

	protected final InetSocketAddress[] _matchingSet;
	protected final BFTDackTest _tester;
	
	public BFTSubscriptionManager_ForTest(
			BFTDackTest tester, IBFTBrokerShadow brokerShadow, InetSocketAddress[] matchingSet) {
		super(brokerShadow, null, false);
		
		_matchingSet = matchingSet;
		_tester = tester;
	}
	
	@Override
	public InetSocketAddress getLocalAddress() {
		return super.getLocalAddress();
	}
	
	@Override
	public TRecovery_Subscription[] getAllSummary() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public TRecovery_Subscription[] getAllSummary(InetSocketAddress remote, IOverlayManager overlayManager) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public TRecovery_Subscription[] getLocalSummary(InetSocketAddress remote, IOverlayManager overlayManager) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void addNewSubscriptionEntry(SubscriptionEntry newSubEntry) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void addNewLocalSubscriptionEntry(LocalSubscriptionEntry newSubEntry) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void applySummary(TRecovery_Subscription trs) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean dumpSubscriptions(String dumpFileName) {
		throw new UnsupportedOperationException();
	}
	
	@Override	
	public Set<InetSocketAddress> getMatchingSet(Publication publication) {
		Set<InetSocketAddress> ret = new HashSet<InetSocketAddress>();
		for(InetSocketAddress remote : _matchingSet)
			if(!remote.equals(_localAddress))
				ret.add(remote);
		return ret;
	}
	
	@Override
	public Set<InetSocketAddress> getLocalMatchingSet(Publication publication) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Set<Subscription> getMatchingSubscriptionSet(Publication publication) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void handleMessage(TMulticast_Subscribe tms) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void handleMessage(TMulticast_UnSubscribe tmus) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void applyAllSubscriptionSummary(TRecovery_Subscription[] trss) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void informLocalSubscribers(TMulticast_Publish tmp) {
		_tester.matchingPublicationDelivered(tmp);
	}
	
	@Override
	public String getSubscriptionsDumpFilename() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Collection<LocalSubscriptionEntry> getLocalSubscriptionEntries() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Collection<SubscriptionEntry> getSubscriptionEntries() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public IBrokerShadow getBrokerShadow() {
		throw new UnsupportedOperationException();
	}
}

class ConnectionEvent_BFTDackSend_ForTest extends ConnectionEvent_BFTDackSend {

	protected ConnectionEvent_BFTDackSend_ForTest(LocalSequencer localSequence) {
		super(localSequence);
		// TODO Auto-generated constructor stub
	}
}

class ReceiverMessagePair {
	protected final TMulticast _tm;
	protected final InetSocketAddress _receiver;
	
	ReceiverMessagePair(InetSocketAddress receiver, TMulticast tm) {
		_tm = tm;
		_receiver = receiver;
	}
}