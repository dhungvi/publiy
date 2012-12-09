package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


import org.msrg.publiy.client.subscriber.ISubscriptionListener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.communication.core.sessions.SessionObjectTypes;
import org.msrg.publiy.communication.core.sessions.SessionTypes;



import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.BFTPublication;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;
import org.msrg.publiy.pubsub.core.IBFTSubscriptionManager;
import org.msrg.publiy.pubsub.core.overlaymanager.BFTOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.IBFTOverlayManager;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT_Dack;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TempSessionRecoveryDataRepository;
import org.msrg.publiy.pubsub.core.subscriptionmanager.BFTSubscriptionManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.LocalSubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;
import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.PropertyGrabber;
import org.msrg.publiy.utils.log.casuallogger.BFTDackLogger;
import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.SimpleBFTSuspectedRepo;
import org.msrg.publiy.broker.core.IBFTConnectionManager;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;
import org.msrg.publiy.broker.core.sequence.IBFTIssuer;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.core.sequence.SequencePair;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuer;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxyRepo;
import org.msrg.publiy.broker.info.PublicationInfo;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import junit.framework.TestCase;

public class BFTMessageQueueTest extends TestCase implements ISubscriptionListener {

	protected final static String TOP_FILENAME = "." + FileUtils.separatorChar + "misc" + FileUtils.separatorChar + "testdata" + FileUtils.separatorChar + "tops" + FileUtils.separatorChar + "OverlayManagerTest_1.top";

	protected InetSocketAddress[] _allAddresses;
	protected IBFTIssuer[] _allIssuers;
	protected LocalSequencer[] _allLocalSequencers;
	protected final int _delta = 2;

	protected final String _tempdir = "." + FileUtils.separatorChar + "temp-" + getClass().getCanonicalName();
	protected final String _keysdir = _tempdir + FileUtils.separatorChar + "keys";
	
	protected final int _numNodes = 21;
	protected final String _ipstr = "127.0.0.1";
	protected final String _nodeNamePrefix = "n-";
	protected final InetSocketAddress _iAddr = new InetSocketAddress(_ipstr, 2000);
	protected final InetSocketAddress _sAddr = new InetSocketAddress(_ipstr, 2003);
	protected final LocalSequencer _sourceSequencer = LocalSequencer.init(null, _sAddr);
	protected TMulticast_Publish_BFT _tmPublishBFTBeingProcessed;
	
	protected IBFTBrokerShadow _bftBrokerShadow;
	protected IBFTOverlayManager _bftOverlayMananger;
	protected IBFTSubscriptionManager _bftSubscriptionManager;
	protected IBFTMessageQueue _bftMessageQueue;
	protected LocalSequencer _localSequencer;
	
	@Override
	public void setUp() throws Exception {
		BrokerInternalTimer.start();
		_allAddresses = new InetSocketAddress[_numNodes];//FileCommons.getAllInetAddressesFromLines(localSequencer, TOP_FILENAME);
		_allIssuers = new IBFTIssuer[_numNodes];
		_allLocalSequencers = new LocalSequencer[_numNodes];
		for(int i=0 ; i<_allAddresses.length ; i++) {
			_allAddresses[i] = new InetSocketAddress("127.0.0.1", 2000 + i);
			_allLocalSequencers[i] = LocalSequencer.init(null, _allAddresses[i]);
		}
		assertTrue(FileUtils.prepareTopologyDirectory(_tempdir, _keysdir, _numNodes, _nodeNamePrefix, _ipstr, 2000));

		
		Properties arguments = new Properties();
		arguments.setProperty(PropertyGrabber.PROPERTY_KEYS_DIR, _keysdir);
		BFTBrokerShadow brokerShadow =
				new BFTBrokerShadow(NodeTypes.NODE_BROKER, _delta, _iAddr, _tempdir, _tempdir, _tempdir + FileUtils.separatorChar + "identityfile", arguments);
		new SimpleBFTIssuerProxyRepo(brokerShadow);
		
		_bftOverlayMananger = (IBFTOverlayManager) createNewPopulatedOverlayManager(brokerShadow, TOP_FILENAME);
		_bftBrokerShadow = (IBFTBrokerShadow) _bftOverlayMananger.getBrokerShadow();
		new BFTDackLogger((BFTBrokerShadow) _bftBrokerShadow);
		_localSequencer = _bftBrokerShadow.getLocalSequencer();
		_bftSubscriptionManager = createNewPopulatedSubscriptionManager(_bftBrokerShadow);
		_bftMessageQueue = new BFTMessageQueue_ForTest(_bftBrokerShadow, null, _bftOverlayMananger, _bftSubscriptionManager);

		for(int i=0 ; i<_allAddresses.length ; i++)
			_allIssuers[i] = new SimpleBFTIssuer(_allAddresses[i], _bftBrokerShadow.getKeyManager().getPrivateKey(_allAddresses[i]), null);
}
	
	protected IBFTSubscriptionManager createNewPopulatedSubscriptionManager(
			IBFTBrokerShadow bftBrokerShadow) {
		IBFTSubscriptionManager bftSubscriptionManager =
				new BFTSubscriptionManager(_bftBrokerShadow, null, false);
		for(int i=0 ; i<_allAddresses.length ; i++) {
			Subscription subscription =
					new Subscription().addPredicate(
							SimplePredicate.buildSimplePredicate("recipientAddress", '=', 2000 + i));
			if(_allAddresses[i].equals(_iAddr)) {
				LocalSubscriptionEntry localSubscriptionEntry =
						new LocalSubscriptionEntry(null, subscription, _allAddresses[i], this);
				bftSubscriptionManager.addNewLocalSubscriptionEntry(localSubscriptionEntry);
			} else {
				SubscriptionEntry subscriptionEntry =
						new SubscriptionEntry(null, subscription, _allAddresses[i], true);
				bftSubscriptionManager.addNewSubscriptionEntry(subscriptionEntry);
			}
		}
		return bftSubscriptionManager;
	}
	
	protected IBFTOverlayManager createNewPopulatedOverlayManager(
			BFTBrokerShadow bftBrokerShadow, String topologyFilename) {
		new SimpleBFTSuspectedRepo(bftBrokerShadow);
		IBFTOverlayManager bftOverlayManager = new BFTOverlayManager(bftBrokerShadow);
		LocalSequencer localSequencer = _allLocalSequencers[0];
		TRecovery_Join[] trjs =
				ConnectionManagerFactory.readRecoveryTopology(localSequencer, topologyFilename);
		bftOverlayManager.applyAllJoinSummary(trjs);
		
		return bftOverlayManager;
	}
	
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		assertTrue(FileUtils.deleteDirectory(_tempdir));
	}
	
	public void testCheckoutOfTMulticastPublishBFT() {
		assertEquals(0, _bftMessageQueue.getPSSessionSize());

//		ISessionManager.initMinimal(_localSequencer);
		ISession is2009 =
				new ISessionBFT_ForTest(this, _bftBrokerShadow, SessionTypes.ST__PUBSUB_PEER_PUBSUB).
					setRemoteCC(_allAddresses[9]).setSessionConnectionType(SessionConnectionType.S_CON_T_ACTIVE);
		
		ISession is2010 =
				new ISessionBFT_ForTest(this, _bftBrokerShadow, SessionTypes.ST__PUBSUB_PEER_PUBSUB).
					setRemoteCC(_allAddresses[10]).setSessionConnectionType(SessionConnectionType.S_CON_T_BYPASSING_AND_ACTIVE);
		
		Set<ISession> newSessions = new HashSet<ISession>();
		newSessions.add(is2009); newSessions.add(is2010);
		_bftMessageQueue.replaceSessions(null, newSessions);
		assertEquals(2, _bftMessageQueue.getPSSessionSize());
		
		{
			// This message does not match anybody's subscription
			BFTPublication publication =
					new BFTPublication(_sAddr).
							addPredicate("recipientAddress", 0);
			_tmPublishBFTBeingProcessed =
					new TMulticast_Publish_BFT(
							publication, _sAddr, _sourceSequencer.getNext());
			_bftOverlayMananger.issueLocalMessageSequencePair(_tmPublishBFTBeingProcessed);
			_bftMessageQueue.addNewMessage(_tmPublishBFTBeingProcessed, this);
			assertEquals(0, _checkedOutMessages.size());
			assertEquals(0, _locallyDeliveredMessages.size());
			assertEquals(1, _locallyConfirmedMessages.size());
			_checkedOutMessages.clear();
			_locallyConfirmedMessages.clear();
			_locallyDeliveredMessages.clear();
		}
		
		{
			// Message going to ip:2009
			BFTPublication publication =
					new BFTPublication(_sAddr).
							addPredicate("recipientAddress", 2009);
			_tmPublishBFTBeingProcessed =
					new TMulticast_Publish_BFT(
							publication, _sAddr, _sourceSequencer.getNext());
			_bftMessageQueue.addNewMessage(_tmPublishBFTBeingProcessed, this);
			assertEquals(1, _checkedOutMessages.size());
			assertEquals(1,  ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[9])).getSequencePairs().size());
			assertEquals(0, _locallyDeliveredMessages.size());
			assertEquals(0, _locallyConfirmedMessages.size());
			
			SequencePair spFor2009 = ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[9])).getSequencePair(_iAddr, _allAddresses[9]);
			TMulticast_Publish_BFT_Dack dack2009 =
					new TMulticast_Publish_BFT_Dack(
							_allAddresses[9], _allLocalSequencers[9].getNext());
			_bftOverlayMananger.issueLocalMessageSequencePair(dack2009);
			dack2009.addSequencePair(new SequencePair(_allIssuers[9], 0, 1, _iAddr, dack2009.getDigestable(), spFor2009._order, spFor2009._order));
			_bftMessageQueue.addNewMessage(dack2009);
			_bftMessageQueue.purge();
			assertEquals(1, _locallyConfirmedMessages.size());
			
			_checkedOutMessages.clear();
			_locallyConfirmedMessages.clear();
			_locallyDeliveredMessages.clear();
		}

		{
			BFTPublication publication =
					new BFTPublication(_sAddr).
							addPredicate("recipientAddress", 2010);
			_tmPublishBFTBeingProcessed =
					new TMulticast_Publish_BFT(
							publication, _sAddr, _sourceSequencer.getNext());
			_bftOverlayMananger.issueLocalMessageSequencePair(_tmPublishBFTBeingProcessed);
			_bftMessageQueue.addNewMessage(_tmPublishBFTBeingProcessed, this);
			assertEquals(2, _checkedOutMessages.size());
			assertEquals(2,  ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[9])).getSequencePairs().size());
			assertEquals(1,  ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[10])).getSequencePairs().size());
			assertEquals(0, _locallyDeliveredMessages.size());
			assertEquals(0, _locallyConfirmedMessages.size());
			SequencePair spFor2009 = ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[9])).getSequencePair(_iAddr, _allAddresses[9]);
			TMulticast_Publish_BFT_Dack dack2009 =
					new TMulticast_Publish_BFT_Dack(
							_allAddresses[9], _allLocalSequencers[9].getNext());
			_bftOverlayMananger.issueLocalMessageSequencePair(dack2009);
			dack2009.addSequencePair(new SequencePair(_allIssuers[9], 0, 1, _iAddr, dack2009.getDigestable(), spFor2009._order, spFor2009._order));
			_bftMessageQueue.addNewMessage(dack2009);
			_bftMessageQueue.purge();
			assertEquals(0, _locallyConfirmedMessages.size());
			
			SequencePair spFor2010 = ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[10])).getSequencePair(_iAddr, _allAddresses[10]);
			TMulticast_Publish_BFT_Dack dack2010 =
					new TMulticast_Publish_BFT_Dack(
							_allAddresses[10], _allLocalSequencers[10].getNext());
			_bftOverlayMananger.issueLocalMessageSequencePair(dack2010);
			dack2010.addSequencePair(new SequencePair(_allIssuers[10], 0, 1, _iAddr, dack2010.getDigestable(), spFor2010._order, spFor2010._order));
			_bftMessageQueue.addNewMessage(dack2010);
			_bftMessageQueue.purge();
			assertEquals(1, _locallyConfirmedMessages.size());
			
			_checkedOutMessages.clear();
			_locallyConfirmedMessages.clear();
			_locallyDeliveredMessages.clear();
		}

		{
			BFTPublication publication =
					new BFTPublication(_sAddr).
							addPredicate("recipientAddress", 2012);
			_tmPublishBFTBeingProcessed =
					new TMulticast_Publish_BFT(
							publication, _sAddr, _sourceSequencer.getNext());
			_bftOverlayMananger.issueLocalMessageSequencePair(_tmPublishBFTBeingProcessed);
			_bftMessageQueue.addNewMessage(_tmPublishBFTBeingProcessed, this);
			assertEquals(0, _checkedOutMessages.size());
			assertEquals(0, _locallyDeliveredMessages.size());

			ISession is2012 =
					new ISessionBFT_ForTest(this, _bftBrokerShadow, SessionTypes.ST__PUBSUB_PEER_PUBSUB).
						setRemoteCC(_allAddresses[12]).setSessionConnectionType(SessionConnectionType.S_CON_T_ACTIVE);
			
			Set<ISession> newSessions2012 = new HashSet<ISession>();
			newSessions2012.add(is2012);
			_bftMessageQueue.replaceSessions(null, newSessions2012);
			_bftMessageQueue.proceedAll();
			assertEquals(3, _bftMessageQueue.getPSSessionSize());
			assertEquals(1, _checkedOutMessages.size());
			assertEquals(0, _locallyDeliveredMessages.size());
			
			
			SequencePair spFor2012 = ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[12])).getSequencePair(_iAddr, _allAddresses[12]);
			TMulticast_Publish_BFT_Dack dack2012 =
					new TMulticast_Publish_BFT_Dack(
							_allAddresses[12], _allLocalSequencers[12].getNext());
			_bftOverlayMananger.issueLocalMessageSequencePair(dack2012);
			dack2012.addSequencePair(new SequencePair(_allIssuers[12], 0, 1, _iAddr, dack2012.getDigestable(), spFor2012._order, spFor2012._order));
			_bftMessageQueue.addNewMessage(dack2012);
			_bftMessageQueue.purge();
			assertEquals(1, _locallyConfirmedMessages.size());

			
			_checkedOutMessages.clear();
			_locallyConfirmedMessages.clear();
			_locallyDeliveredMessages.clear();
		}

		{
			BFTPublication publication =
					new BFTPublication(_sAddr).
							addPredicate("recipientAddress", 2019);
			_tmPublishBFTBeingProcessed =
					new TMulticast_Publish_BFT(
							publication, _sAddr, _sourceSequencer.getNext());
			_bftOverlayMananger.issueLocalMessageSequencePair(_tmPublishBFTBeingProcessed);
			_bftMessageQueue.addNewMessage(_tmPublishBFTBeingProcessed, this);
			assertEquals(2, _checkedOutMessages.size());
			assertEquals(3,  ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[9])).getSequencePairs().size());
			assertEquals(2,  ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[10])).getSequencePairs().size());
			assertEquals(0, _locallyDeliveredMessages.size());
			assertEquals(0, _locallyConfirmedMessages.size());
			SequencePair spFor2009 = ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[9])).getSequencePair(_iAddr, _allAddresses[9]);
			TMulticast_Publish_BFT_Dack dack2009 =
					new TMulticast_Publish_BFT_Dack(
							_allAddresses[9], _allLocalSequencers[9].getNext());
			_bftOverlayMananger.issueLocalMessageSequencePair(dack2009);
			dack2009.addSequencePair(new SequencePair(_allIssuers[9], 0, 1, _iAddr, dack2009.getDigestable(), spFor2009._order, spFor2009._order));
			_bftMessageQueue.addNewMessage(dack2009);
			_bftMessageQueue.purge();
			assertEquals(0, _locallyConfirmedMessages.size());
			
			SequencePair spFor2010 = ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[10])).getSequencePair(_iAddr, _allAddresses[10]);
			TMulticast_Publish_BFT_Dack dack2010 =
					new TMulticast_Publish_BFT_Dack(
							_allAddresses[10], _allLocalSequencers[10].getNext());
			_bftOverlayMananger.issueLocalMessageSequencePair(dack2010);
			dack2010.addSequencePair(new SequencePair(_allIssuers[10], 0, 1, _iAddr, dack2010.getDigestable(), spFor2010._order, spFor2010._order));
			_bftMessageQueue.addNewMessage(dack2010);
			_bftMessageQueue.purge();
			assertEquals(0, _locallyConfirmedMessages.size());
			
			SequencePair spFor2019 = ((TMulticast_Publish_BFT)_checkedOutMessages.get(_allAddresses[9])).getSequencePair(_iAddr, _allAddresses[19]);
			TMulticast_Publish_BFT_Dack dack2019 =
					new TMulticast_Publish_BFT_Dack(
							_allAddresses[19], _allLocalSequencers[19].getNext());
			_bftOverlayMananger.issueLocalMessageSequencePair(dack2019);
			dack2019.addSequencePair(new SequencePair(_allIssuers[19], 0, 1, _iAddr, dack2019.getDigestable(), spFor2019._order, spFor2019._order));
			_bftMessageQueue.addNewMessage(dack2019);
			_bftMessageQueue.purge();
			assertEquals(1, _locallyConfirmedMessages.size());
			
			_checkedOutMessages.clear();
			_locallyConfirmedMessages.clear();
			_locallyDeliveredMessages.clear();
		}
	}

	Map<InetSocketAddress, TMulticast> _checkedOutMessages = new HashMap<InetSocketAddress, TMulticast>();
	List<TMulticast> _locallyDeliveredMessages = new LinkedList<TMulticast>();
	List<TMulticast> _locallyConfirmedMessages = new LinkedList<TMulticast>();
	
	public void informCheckout(ISessionBFT_ForTest sessionBFT, TMulticast tm, IRawPacket raw) {
		// verify who checked out what!
		InetSocketAddress remote = sessionBFT.getRemoteAddress();
		BrokerInternalTimer.inform((raw==null ? " NO: " : "YES: ") + sessionBFT + ": " + tm);
		if(raw != null) {
			IPacketable packet = ((TMulticast_Publish_BFT)PacketFactory.unwrapObject(
					_bftBrokerShadow, raw, false));
			
			if(packet.getObjectType() == PacketableTypes.TMULTICAST) {
				TMulticast receivedMsg = (TMulticast) packet;
				if(receivedMsg.getType() == TMulticastTypes.T_MULTICAST_PUBLICATION_BFT) {
					assertEquals(_tmPublishBFTBeingProcessed.getPublication(),
							((TMulticast_Publish_BFT)receivedMsg).getPublication());
					assertNull(_checkedOutMessages.put(remote, (TMulticast_Publish_BFT) raw.getObject()));
				}
			}
		}
	}

	@Override
	public void tmConfirmed(TMulticast tm) {
		_locallyConfirmedMessages.add(tm);
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
	public PublicationInfo getLastReceivedPublications() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getLastPublicationReceiptTime() {
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
}

class ISessionBFT_ForTest extends ISession {

	protected final BFTMessageQueueTest _tester;
	
	ISessionBFT_ForTest(BFTMessageQueueTest tester, IBFTBrokerShadow brokerShadow, SessionTypes type) {
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

class BFTMessageQueue_ForTest extends BFTMessageQueue {

	protected BFTMessageQueue_ForTest(IBFTBrokerShadow bftBrokerShadow,
			IBFTConnectionManager bftConnectionManager,
			IBFTOverlayManager bftOverlayManager,
			IBFTSubscriptionManager bftSubscriptionManager) {
		super(bftBrokerShadow, bftConnectionManager, bftOverlayManager,
				bftSubscriptionManager);
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
