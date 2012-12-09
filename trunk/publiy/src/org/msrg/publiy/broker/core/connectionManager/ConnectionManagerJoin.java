package org.msrg.publiy.broker.core.connectionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;



import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.pubsub.core.packets.recovery.TempSessionRecoveryDataRepository;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.occurance.IEventOccurance;
import org.msrg.publiy.utils.occurance.TimedEventOccurance;

import org.msrg.publiy.broker.core.IConnectionManagerJoin;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.client.subscriber.ISubscriptionListener;
import org.msrg.publiy.communication.core.niobinding.IConInfoListening;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.niobinding.SessionRenewTask;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.TConf_Ack;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.ISessionManagerJoin;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.communication.core.sessions.SessionTypes;
import org.msrg.publiy.component.ComponentStatus;

public class ConnectionManagerJoin extends ConnectionManager implements IConnectionManagerJoin {

	public final int MAX_RETRY_DURATION = 50000;
	private final IEventOccurance _joinRetryRetryEvent;
	
//	public final int MAX_RETRY_JOIN = 100;
//	protected int _retryJoin = 0;
	
	protected Map<InetSocketAddress, ISession> _pendingSessionsWhileJoin = new HashMap<InetSocketAddress, ISession>();
	protected ISession _joinSessionInProgress;
	protected TMulticast_Join _joinMessage;
	protected boolean _joinMessageConfirmed = false;
	protected InetSocketAddress _joinPointAddress;
		
	////////////////////////////////////////////////////////////////////////
	//							private methods							  //
	////////////////////////////////////////////////////////////////////////
	protected ConnectionManagerJoin(
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue) throws IOException {
		super((broker == null) ? ("ConManJoin-" + getDefaultBrokerName(brokerShadow)) : ("ConManJoin-" + broker.getBrokerID()),
				ConnectionManagerTypes.CONNECTION_MANAGER_JOIN, broker, brokerShadow, overlayManager, subscriptionManager, messageQueue, null);
		
		_joinRetryRetryEvent = new TimedEventOccurance(MAX_RETRY_DURATION, this, "JRetry");
		_joinRetryRetryEvent.isPending();
	}
	
	@Override
	protected void handleSessionInitMessage(ISession session, TSessionInitiation sInit) {

//		if(session == _joinSessionInProgress)
			_sessionManager.update(_brokerShadow, session, sInit);
//		else
//		{
//			session.storeSessionInitiationData(sInit);
//			_pendingSessionsWhileJoin.put(sInit.getSourceAddress(), session);
//		}
	}
		
	@Override
	protected void handleTConfAckMessage(ISession session, TConf_Ack tConfAck) {
		return;
	}
	
	@Override
	protected void handleMulticastMessage(ISession session, TMulticast tm) {
		if(!session.isRealSessionTypePubSubEnabled())
			throw new IllegalStateException("Session is not PubSubEnabled " + session.toStringShort());
		
		performMulticastMessageSendersTest(session, tm);
		
		if(tm.getType() == TMulticastTypes.T_MULTICAST_CONF)
		{
			acknowledgeTMulticast_Conf(session, (TMulticast_Conf)tm, false);
			
			TMulticast_Conf tmc = (TMulticast_Conf) tm;
			if(tmc.isConfirmationOf(_joinMessage)) {
				_mq.addNewMessage(tm);
//				_joinMessageConfirmed = true;
//				if(testCompletionOfJoin(session)) 
//					_broker.joinComplete();
			}
		}
		else{
			if(session == _joinSessionInProgress) {
				_mq.applySpecial(tm);
				
				Sequence tmConfLocalSequence = _localSequencer.getNext();
				TMulticast_Conf tmc = tm.getConfirmation(session.getRemoteAddress(), _localAddress, tmConfLocalSequence);
//				tmc.setLocalSequence(tmConfLocalSequence);
				TMulticast_Conf tmcClone = tmc.getCloneAndShift(1, tmConfLocalSequence);
				IRawPacket raw = PacketFactory.wrapObject(_localSequencer, tmcClone);
				_sessionManager.send(session, raw);
			}
			else
				session.storeSessionUnconfirmedMulticastData(tm);
		}
	}
	
	private boolean testCompletionOfJoin(ISession session) {
		if(_joinPointAddress == null)
			return true;
		
		if(session != _joinSessionInProgress)
			return false;
		TempSessionRecoveryDataRepository tempSessionRecoveryData = session.getTempSessionDataRepository();
		if(tempSessionRecoveryData.isAllRecoveryDataReceived() && _joinMessageConfirmed) {
			return true;
		}
		return false;
	}
	
	@Override
	protected void handleRecoveryMessage(ISession session, TRecovery tr) {
		if(session == _joinSessionInProgress) {
			if(tr.isLastJoin() || tr.isLastSubscription())
				session.storeSessionRecoveryData(tr);
			
			_mq.applySummary(tr);
		}
		else
			session.storeSessionRecoveryData(tr);
		
		if(testCompletionOfJoin(session)) {
			_joinRetryRetryEvent.success();
			_broker.joinComplete();	
		}
	}

	@Override
	protected void performConnectionManagerTypeSpecificPostRegistration(ISession newSession) {
		SessionTypes sessionType = newSession.getSessionType();
		
		InetSocketAddress remote = newSession.getRemoteAddress();
		if(!remote.equals(_joinPointAddress))
			throw new IllegalStateException(newSession.toString());
		
		else
			_joinSessionInProgress = newSession;
		
		ISession registeredSessionInAllSessions = getSessionPrivately(_joinPointAddress);

		if(registeredSessionInAllSessions != _joinSessionInProgress)
			throw new IllegalStateException("RegisteredSession '" + registeredSessionInAllSessions + "' is not equal to newSession '" + newSession + "'.");

		joinSessionRegistered();
		
		if(sessionType == SessionTypes.ST__PUBSUB_PEER_PUBSUB)
			return;
		
		else
			throw new IllegalArgumentException("ConnectionManagerJoin cannot register newSession '" + newSession.getRemoteAddress() + "' with SessionType='" + sessionType + "'.");
	}
	
	@Override
	public void join(InetSocketAddress joinPointAddress) {
		IConInfoNonListening<?> joinConInfoNL = null;
		synchronized(_sessionsLock) {
			_joinPointAddress = joinPointAddress;
			LoggerFactory.getLogger().info(this, "Joining: " + joinPointAddress);
			
			if(getBrokerOpState()!=BrokerOpState.BRKR_PUBSUB_JOIN)
				throw new IllegalStateException("ConnectionManager::join(.) - ERROR, brokerstate is not BRKR_PUBSUB_JOIN.");
			
			if(_joinSessionInProgress!=null)
				throw new IllegalStateException("ConnectionManager::join(.) - ERROR, another join session '" + _joinSessionInProgress + "' is still in progress.");
	
			if(_joinPointAddress == null) {
//				try{
//					waitForRunningPausingStoppingState();
//				}catch(InterruptedException itx) {
//					LoggerFactory.getLogger().infoX(this, itx, "Exception in '" + this + "'");
//					System.exit(-1);
//				}
				
				ComponentStatus status = getComponentState();
				if(status != ComponentStatus.COMPONENT_STATUS_RUNNING)
					throw new IllegalStateException("State must be RUNNING, it is: " + status);
				
				_mq.dumpOverlay(getBrokerRecoveryFileName());
				_broker.joinComplete();
				return;
			}

			//initiate connection to joinPointAddress
			_joinSessionInProgress = getDefaultOutgoingSession(_joinPointAddress);
			_joinSessionInProgress.setRealSession(_joinSessionInProgress);
			joinConInfoNL = _nioBinding.makeOutgoingConnection(_joinSessionInProgress, this, this, this, _joinPointAddress);
			addSessionPrivately(_joinPointAddress, _joinSessionInProgress);
			_joinSessionInProgress.setSessionConnectionType(SessionConnectionType.S_CON_T_UNKNOWN);
		}
		
		if(joinConInfoNL == null)
			conInfoUpdated(_joinSessionInProgress.getConInfoNL());
	}
	
	private void joinSessionRegistered() {
		//prepare join message
		InetSocketAddress joinPointAddress = _joinSessionInProgress.getRemoteAddress();
		LoggerFactory.getLogger().info(this, "JoinSession to '" + joinPointAddress + "' is PubSubEnabled.");
		
//		_retryJoin = 0;
		_joinRetryRetryEvent.success();
		
		_joinSessionInProgress.setSessionConnectionType(SessionConnectionType.S_CON_T_ACTIVE);
		_joinMessage = new TMulticast_Join(this._localAddress, LocalSequencer.getNodeTypeGeneric(), joinPointAddress, NodeTypes.NODE_BROKER, _localSequencer);
		//insert join message into the messageQ
		getOverlayManager().joinNode(joinPointAddress);
		_mq.addNewMessage(_joinMessage, this);
	}

	@Override
	public void tmConfirmed(TMulticast tm) {
		LoggerFactory.getLogger().debug(this, "TMConfirmed: " + tm);
		if(!tm.equals(_joinMessage))
			throw new IllegalStateException("There must be no registration of this ConMan for Confirmation: there's no msgQ while join " + tm);

		_joinMessageConfirmed = true;
		if(testCompletionOfJoin(_joinSessionInProgress))
			_broker.joinComplete();
	}
	
	@Override
	public ConnectionManagerTypes getType() {
		assert _type == ConnectionManagerTypes.CONNECTION_MANAGER_JOIN;
		return _type;
	}

	@Override
	public ISession getDefaultOutgoingSession(InetSocketAddress remote) {
		return ISessionManager.getPubSubSession(_brokerShadow, remote, true);
	}
	
	@Override
	public void sendAllRecoveryDataToRecoveringAndEndRecovery(ISession session) {
		LoggerFactory.getLogger().debug(this, "SendingALLtoRecoveringAndEndRecovery: " + session.getRemoteAddress());
		throw new UnsupportedOperationException("ConnectionManagerJoin has not joined, and cannot send'All'ToRecovering.");
	}

	@Override
	public void sendAllToRecoveringAndNotEndRecovery(ISession session) {
		LoggerFactory.getLogger().debug(this, "SendingALLtoRecoveringAndNOTEndRecovery: " + session.getRemoteAddress());
		throw new UnsupportedOperationException("ConnectionManagerJoin has not joined, and cannot send'All'ToRecovering.");
	}
	
	public boolean isThisJoinSessionInProgress(ISession Session) {
		return _joinSessionInProgress == Session;
	}
	
	@Override
	public void failed(ISession fSession) {
		LoggerFactory.getLogger().debug(this, "Failed: " + fSession.getRemoteAddress());
		synchronized(_sessionsLock) {
			if(fSession == _joinSessionInProgress)
			{
				if(_joinRetryRetryEvent.test()) // ++_retryJoin < MAX_RETRY_JOIN)
				{
					System.out.println("111");					
					LoggerFactory.getLogger().info(this, "Retrying: " + _joinRetryRetryEvent);
					_joinSessionInProgress._tempRepositoryObject.flushTempSessionRecoveryData();
					ISessionManager.setType(_joinSessionInProgress, SessionTypes.ST_PUBSUB_INITIATED);
					_joinSessionInProgress.resetRetryCountToPendingReset();
					SessionRenewTask joinSessionRenewTask = new SessionRenewTask(this, _joinSessionInProgress);
					scheduleTaskWithTimer(joinSessionRenewTask, ISessionManager.RETRY_DELAY);
					return;
				}
				else{
					System.out.println("222");
					_joinRetryRetryEvent.fail();
					_joinSessionInProgress.resetRetryCountToFailed();
					throw new IllegalStateException("Cannot reach the joinpoint '" + _joinPointAddress + "' after so many retries.");
				}
			}
			
			else
			{
				fSession.clearTempRepositoryData();
				fSession.flushPendingMessages();

				InetSocketAddress remote = fSession.getRemoteAddress();
				if(remote != null)
					_pendingSessionsWhileJoin.remove(remote);
				
				return;
			}
		}
	}
	
	@Override
	protected boolean performConnectionManagerTypeSpecificPreRegistration(ISession newSession) {
		LoggerFactory.getLogger().info(this, "PreRegistration: " + newSession);
		
		if(!newSession.isRealSessionTypePubSubEnabled())
			throw new IllegalStateException("NewSession(" + newSession + "): " + newSession.toStringShort() + ", is not PubSubEnabled.");

		InetSocketAddress remote = newSession.getRemoteAddress();
		if(remote.equals(_joinPointAddress))
			return true;
		
		ISession oldSession = _pendingSessionsWhileJoin.get(remote);
		if(oldSession != null)
			_sessionManager.dropSession(oldSession, true);
		
		_pendingSessionsWhileJoin.put(remote, newSession);
		if(newSession._tempRepositoryObject == null)
			newSession._tempRepositoryObject = new TempSessionRecoveryDataRepository(newSession);

		return false;
	}
	
	@Override
	public boolean upgradeUnjoinedSessionToActive(InetSocketAddress completedJoinAddress) {
		throw new UnsupportedOperationException("ConnectionManagerJoin cannot upgradeUnjoinedSessionToActive '" + completedJoinAddress + "', since it could not accept a joiningSession in the first place.");		
	}

	@Override
	protected void performConnectionManagerTypeSpecificPostFailed(ISession session) {
	}

	@Override
	public void sendTMulticast(TMulticast tm, ITMConfirmationListener tmConfirmationListener) {
		if(tm.getType() != TMulticastTypes.T_MULTICAST_CONF) // !TMulticast_Conf.class.isInstance(tm))
			throw new UnsupportedOperationException("ConManJoin-" + _brokerShadow.getBrokerID() + " - cannot sendTMulticast message: " + tm);
	}

	@Override
	public void sendTMulticast(TMulticast[] tms, ITMConfirmationListener tmConfirmationListener) {
		for(int i=0 ; i<tms.length ; i++)
			if(tms[i].getType() != TMulticastTypes.T_MULTICAST_CONF) // !TMulticast_Conf.class.isInstance(tms[i]))
				throw new UnsupportedOperationException("ConManJoin-" + _brokerShadow.getBrokerID() + " - cannot sendTMulticast message: " + tms[i]);
	}

	@Override
	public TMulticast_Subscribe issueSubscription(Subscription subscription, ISubscriptionListener subscriptionListener) {
		throw new UnsupportedOperationException("ConManJoin does not support the subscribe operation.");
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_CON_MAN_JOIN;
	}
	
	//INIO_A_Listener
	@Override
	public void newIncomingConnection(IConInfoListening<?> conInfoL, IConInfoNonListening<?> newConInfoNL) {
		if(conInfoL != _conInfoL)
			throw new IllegalStateException("ConnectionManger::newIncomingConnection(.) - ERROR, this is not my listening connection.");
	
		ISessionManager.attachUnknownPeerSession(newConInfoNL, true);
	}

	@Override
	protected ISessionManager createSessionManager(LocalSequencer localSequencer) {
		return new ISessionManagerJoin(localSequencer, _nioBinding, this);
	}

	@Override
	public InOutBWEnforcer getBWEnforcer() {
		return _broker.getBWEnforcer();
	}
	
	@Override
	public String getThreadName() {
		return "ConMan-Join";
	}
}