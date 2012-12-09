package org.msrg.publiy.pubsub.core.messagequeue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.TimerTask;

import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;



import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Depart;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManager;

import org.msrg.publiy.client.subscriber.ISubscriptionListener;
import org.msrg.publiy.communication.core.niobinding.IConInfo;
import org.msrg.publiy.communication.core.niobinding.IConInfoListening;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.packet.types.TCommand;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.component.ComponentStatus;
import org.msrg.publiy.component.IComponent;
import org.msrg.publiy.component.IComponentListener;

import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerTypes;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.ISessionInfo;
import org.msrg.publiy.broker.info.JoinInfo;
import org.msrg.publiy.broker.info.PSSessionInfo;
import org.msrg.publiy.broker.info.SubscriptionInfo;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import junit.framework.TestCase;

public class MessageQueueTest extends TestCase {
	
	protected final int _delta = 3;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
	}
	
	@Override
	public void tearDown() { }
	
	public void testNoSessions() {
		BrokerOpState brokerOpState = BrokerOpState.BRKR_PUBSUB_PS;
		InetSocketAddress localAddress =
				new InetSocketAddress("127.0.0.1", 1000);
		IBrokerShadow brokerShadow =
				new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(_delta);
		ISubscriptionManager subscriptionManager = new SubscriptionManager(brokerShadow, null, false);
		ConnectionManager_ForTest _connMan =
				new ConnectionManager_ForTest(brokerOpState, brokerShadow, null, subscriptionManager);
		MessageQueue_ForTest _mq = new MessageQueue_ForTest(brokerShadow, _connMan, _connMan.getOverlayManager(), subscriptionManager,  true);

		InetSocketAddress subscriber = Sequence.getRandomAddress();
		Subscription sub = new Subscription(). addPredicate(SimplePredicate.buildSimplePredicate("ATTR1", '=', 100));
		TMulticast_Subscribe tms = new TMulticast_Subscribe(sub, subscriber, brokerShadow.getLocalSequencer());
		_mq.addNewMessage(tms);
		assertEquals(0, _mq.getMQSetSize());
		assertEquals(1, subscriptionManager.getSubscriptionEntries().size());
	}
	
	public void testMQPorocessingJoinDepartMessages() throws IOException {
		InetSocketAddress []a = new InetSocketAddress[10];
		for(int i=0 ; i<10 ; i++)
			a[i] = new InetSocketAddress("127.0.0.1", 1000 + i);

		InetSocketAddress localAddress = a[2];
		IBrokerShadow brokerShadow =
				new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(_delta);
		IOverlayManager overlayManager =
				new OverlayManager(brokerShadow);
		ISubscriptionManager subscriptionManager =
				new SubscriptionManager(brokerShadow, null, false);
		IConnectionManager connectionManager =
				new ConnectionManager_ForTest(BrokerOpState.BRKR_PUBSUB_PS, brokerShadow, overlayManager, subscriptionManager);
		
		LocalSequencer localSequencer = brokerShadow.getLocalSequencer();
		MessageQueue_ForTest mq =
				new MessageQueue_ForTest(brokerShadow, connectionManager, overlayManager, subscriptionManager, true);
		
		TMulticast_Join [] tmjs = new TMulticast_Join[8];
		tmjs[0] = new TMulticast_Join(a[1], NodeTypes.NODE_BROKER, a[2], NodeTypes.NODE_BROKER, localSequencer);
		tmjs[1] = new TMulticast_Join(a[0], NodeTypes.NODE_BROKER, a[1], NodeTypes.NODE_BROKER, localSequencer);
		tmjs[2] = new TMulticast_Join(a[3], NodeTypes.NODE_BROKER, a[1], NodeTypes.NODE_BROKER, localSequencer);
		tmjs[3] = new TMulticast_Join(a[4], NodeTypes.NODE_BROKER, a[0], NodeTypes.NODE_BROKER, localSequencer);
		tmjs[4] = new TMulticast_Join(a[5], NodeTypes.NODE_BROKER, a[4], NodeTypes.NODE_BROKER ,localSequencer);
		tmjs[5] = new TMulticast_Join(a[6], NodeTypes.NODE_BROKER, a[4], NodeTypes.NODE_BROKER, localSequencer);
		tmjs[6] = new TMulticast_Join(a[7], NodeTypes.NODE_BROKER, a[3], NodeTypes.NODE_BROKER, localSequencer);
		tmjs[7] = new TMulticast_Join(a[8], NodeTypes.NODE_BROKER, a[3], NodeTypes.NODE_BROKER, localSequencer);

		for(int i=0 ; i<tmjs.length ; i++)
			mq.addNewMessage(tmjs[i]);
		
		assertEquals(9, mq.getOverlayManager().getNeighborhoodSize());
		assertEquals(_delta+1, mq.getOverlayManager().getNeighborhoodRadius());
		
		TMulticast_Depart [] tmds = new TMulticast_Depart[8];
		tmds[7] = new TMulticast_Depart(a[1], a[2], localSequencer);
		tmds[6] = new TMulticast_Depart(a[0], a[1], localSequencer);
		tmds[5] = new TMulticast_Depart(a[3], a[1], localSequencer);
		tmds[4] = new TMulticast_Depart(a[4], a[0], localSequencer);
		tmds[3] = new TMulticast_Depart(a[5], a[4], localSequencer);
		tmds[2] = new TMulticast_Depart(a[6], a[4], localSequencer);
		tmds[1] = new TMulticast_Depart(a[7], a[3], localSequencer);
		tmds[0] = new TMulticast_Depart(a[8], a[3], localSequencer);
		
		for(int i=0 ; i<tmds.length ; i++)
			mq.addNewMessage(tmds[i]);

		assertEquals(1, mq.getOverlayManager().getNeighborhoodSize());
		assertEquals(_delta+1, mq.getOverlayManager().getNeighborhoodRadius());
	}
}

class MessageQueue_ForTest extends MessageQueue {

	public MessageQueue_ForTest(
			IBrokerShadow brokerShadow,
			IConnectionManager connectionManager,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			boolean allowedToConfirm) {
		super(brokerShadow, connectionManager, overlayManager, subscriptionManager, allowedToConfirm);
	}
	
	public int getMQSetSize() {
		return _mQSet.size();
	}
	
	@Override
	protected void apply(TMulticast tm) {
		super.apply(tm);
	}
	
	@Override
	protected void sendConfirmations(IMessageQueueNode node) {
		return;
	}
	
	@Override
	public BrokerOpState getBrokerOpState() {
		return BrokerOpState.BRKR_PUBSUB_PS;
	}
}


class ConnectionManager_ForTest implements IConnectionManager {

	protected final LocalSequencer _localSequencer;
	protected final BrokerOpState _brokerOpState;
	protected final IBrokerShadow _brokerShadow;
	protected final IOverlayManager _overlayManager;
	protected final ISubscriptionManager _subscriptionManager;
	ConnectionManager_ForTest(BrokerOpState brokerOpState, IBrokerShadow brokerShadow, IOverlayManager overlayManager, ISubscriptionManager subscriptionManager) {
		_brokerOpState = brokerOpState;
		_brokerShadow = brokerShadow;
		_localSequencer = _brokerShadow.getLocalSequencer();
		_overlayManager = overlayManager;
		_subscriptionManager = subscriptionManager;
	}
	
	@Override
	public void enableDisableMQProceed(boolean enable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void failed(ISession fSession) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISession[] getAllLocallyActiveSessions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InOutBWEnforcer getBWEnforcer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public BrokerOpState getBrokerOpState() {
		return _brokerOpState;
	}

	@Override
	public String getBrokerRecoveryFileName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBrokerSessionsDumpFileName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBrokerSubDumpFileName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISession getDefaultOutgoingSession(InetSocketAddress remote) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Sequence getLastReceivedSequence(InetSocketAddress remote) {
		throw new UnsupportedOperationException();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LocalSequencer getLocalSequencer() {
		return _localSequencer;
	}

	@Override
	public IConnectionManager getNextConnctionManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOverlayManager getOverlayManager() {
		return _overlayManager;
	}

	@Override
	public ISessionManager getSessionManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISubscriptionManager getSubscriptionManager() {
		return _subscriptionManager;
	}

	@Override
	public ConnectionManagerTypes getType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void insertCandidatesEvent() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void insertForceConfAckEvent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean issueTCommandDisseminateMessage(TCommand tCommand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean loadPrepareSubscription(SubscriptionEntry subEntry) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void loadPrepareSubscriptionsFile(String subscriptionFile,
			BrokerIdentityManager idManager) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void purgeMQ() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void registerSession(ISession newSession) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void registerUnjoinedSessionAsJoined(InetSocketAddress remote) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void renewSessionsConnection(ISession session) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void scheduleTaskWithTimer(TimerTask task, long delay) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void scheduleTaskWithTimer(TimerTask task, long delay, long period) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void sendDackOnAllSessions() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void sendTMulticast(TMulticast tm,
			ITMConfirmationListener tmConfirmationListener) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void sendTMulticast(TMulticast[] tms,
			ITMConfirmationListener tmConfirmationListener) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void sendTPingOnAllSessions() {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public void setLastReceivedSequence2(ISession sesion, Sequence seq,
			boolean doProceed, boolean initializeMQNode) {
		throw new UnsupportedOperationException();
		
	}

	@Override
	public boolean upgradeUnjoinedSessionToActive(
			InetSocketAddress completedJoinAddress) {
		return true;
	}

	@Override
	public ISessionInfo[] getActiveSessions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISessionInfo[] getInactiveSessions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PSSessionInfo[] getPSSessions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISessionInfo[] getSessionInfos() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SubscriptionInfo[] getSubscriptionInfos() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JoinInfo[] getTopologyLinks() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addNewComponentListener(IComponentListener comListener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void awakeFromPause() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getComponentName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ComponentStatus getComponentState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void pauseComponent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void prepareToStart() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeComponentListener(IComponentListener comListener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void startComponent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void stopComponent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void componentStateChanged(IComponent component) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void becomeMyAcceptingListener(IConInfoListening<?> conInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void newIncomingConnection(IConInfoListening<?> conInfoL,
			IConInfoNonListening<?> newConInfoNL) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void becomeMyListener(IConInfo<?> conInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void conInfoUpdated(IConInfo<?> conInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void becomeMyReaderListener(IConInfoNonListening<?> conInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void conInfoGotFirstDataItem(IConInfoNonListening<?> conInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void becomeMyWriteListener(IConInfoNonListening<?> conInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void conInfoGotEmptySpace(IConInfoNonListening<?> conInfo) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void tmConfirmed(TMulticast tm) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TMulticast_Subscribe issueSubscription(
			Subscription subscription,
			ISubscriptionListener subscriptionListener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean checkOutputBWAvailability() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}
}