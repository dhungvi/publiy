package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.ITimestamp;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.PSSessionInfo;

import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionLocal;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;

public abstract class AbstractMessageQueue implements IMessageQueue, ILoggerSource {

	protected Map<InetSocketAddress, PSSession> _psSessions;
	protected InetSocketAddress _localAddress;
	protected IConnectionManager _connectionManager;
	protected ISubscriptionManager _subscriptionManager;
	protected IOverlayManager _overlayManager;
	protected final IBrokerShadow _brokerShadow;
	protected final LocalSequencer _localSequencer;
	protected ITimestamp _arrivalTimestamp;
	
	protected AbstractMessageQueue(IBrokerShadow brokerShadow, IConnectionManager connectionManager, IOverlayManager overlayManager, ISubscriptionManager subscriptionManager) {
		_connectionManager = connectionManager;
		_brokerShadow = brokerShadow;
		_localSequencer = _brokerShadow.getLocalSequencer();
		_localAddress = _localSequencer.getLocalAddress();
		_psSessions = new HashMap<InetSocketAddress, PSSession>();
		_arrivalTimestamp = new Timestamp(_brokerShadow);
		_overlayManager = overlayManager;
		_subscriptionManager = subscriptionManager;
	}

	protected AbstractMessageQueue(IConnectionManager connectionManager, AbstractMessageQueue oldMQ) {
		_connectionManager = connectionManager;
		oldMQ._connectionManager = null;
		
		_overlayManager = oldMQ._overlayManager;
		oldMQ._overlayManager = null;
		
		_subscriptionManager = oldMQ._subscriptionManager;
		oldMQ._subscriptionManager = null;
		
		_localAddress = oldMQ._localAddress;
		oldMQ._localAddress = null;
		
		_arrivalTimestamp = oldMQ._arrivalTimestamp;
		oldMQ._arrivalTimestamp = null;
		
		_localSequencer = oldMQ._localSequencer;
		_brokerShadow = oldMQ._brokerShadow;
		
		_psSessions = oldMQ._psSessions;
		oldMQ._psSessions = null;
	}
	
	@Override
	public void setConnectionManager(IConnectionManager connectionManager) {
		_connectionManager = connectionManager;
	}
	
	@Override
	public abstract ISessionLocal addPSSessionLocalForRecoveryWithENDEDIsession();

	@Override
	public abstract void applyAllSummary(TRecovery_Join[] trjs);

	@Override
	public abstract void applyAllSummary(TRecovery_Subscription[] trss);

	@Override
	public abstract void applySpecial(TMulticast tm);

	@Override
	public abstract void applySummary(TRecovery tr);
	
	@Override
	public abstract void disableProceed();

	@Override
	public boolean dumpOverlay(String filename) {
		if(filename == null)
			filename = Broker.BROKER_RECOVERY_TOPOLOGY_FILE;
		
		return getOverlayManager().dumpOverlay();
	}
	
	@Override
	public abstract boolean dumpPSTop();

	@Override
	public abstract void enableProceed();

	@Override
	public abstract boolean getAllowedToConfirm();

	@Override
	public ITimestamp getArrivedTimestamp() {
		return _arrivalTimestamp;
	}
	
	@Override
	public BrokerOpState getBrokerOpState() {
		return _connectionManager.getBrokerOpState();
	}
	
	@Override
	public abstract Sequence getLastReceivedSequence(InetSocketAddress remote);

	@Override
	public abstract int getPSSessionSize();

	@Override
	public abstract Set<ISession> getPSSessionsAsISessions();
	
	@Override
	public abstract void proceed(InetSocketAddress remote);
	
	@Override
	public abstract void proceedAll();

	@Override
	public abstract void replaceSessions(Set<ISession> oldSessions, Set<ISession> newSessions);

	@Override
	public abstract void resetSession(ISession session, Sequence seq, boolean initializeMQNode);
	
	@Override
	public abstract void substituteSession(ISession oldSession, ISession newSession);

	@Override
	public abstract PSSessionInfo[] getPSSessionInfos();

	protected ISubscriptionManager getSubscriptionManager() {
		return _subscriptionManager;
	}
	
	protected IOverlayManager getOverlayManager() {
		return _overlayManager;
	}
	
	protected int removeSessions(Set<ISession> removedSessions) {
		if(removedSessions == null)
			return 0;
		
		boolean removingSessionLocal = false;
		int removedSize = 0;

		for(ISession removedSession : removedSessions) {
			if (Broker.CORRELATE)
				TrafficCorrelator.getInstance().sessionRemoved(removedSession);
			if(removedSession.isLocal()) // ISessionLocal.class.isInstance(oldS))
				removingSessionLocal = true;
			InetSocketAddress oldAddr = removedSession.getRemoteAddress();
			
			PSSession removedPSS = _psSessions.remove(oldAddr);
			if (Broker.CORRELATE)
				TrafficCorrelator.getInstance().psSessionRemoved(removedPSS);
			
			if(removedPSS == null)
				continue; 

			removedPSS.cancel();
			if(Broker.DEBUG) 
				LoggerFactory.getLogger().debug(this, "Removed PSSSession: " + removedPSS);
			removedSize++;
		}
		
		if(removingSessionLocal)
			allowConfirmations(true);
		
		return removedSize;
	}
}
