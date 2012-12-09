package org.msrg.publiy.communication.core.sessions;

import java.net.InetSocketAddress;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.IHasBrokerInfo;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.pubsub.core.packets.recovery.TempSessionRecoveryDataRepository;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;
import org.msrg.publiy.utils.occurance.IEventOccurance;

import org.msrg.publiy.broker.core.nodes.OverlayNodeId;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.ISessionInfo;

import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.RawPacket;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.communication.core.packet.types.TConf_Ack;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;

public class ISession implements IHasBrokerInfo<ISessionInfo>, ISessionRetry, ILoggerSource {
	
	private int _lastMQNSequence = -1;
	
	public final int _session_index;
	private static int _session_counter = 0;
	private static Object _instanceCounterLock = new Object();
	
	private boolean _discarded = false;
	private boolean _lastTRecoveryJoinSent = false, _lastTRecoverySubscriptionSent = false;
	
	protected final IBrokerShadow _brokerShadow;
	protected volatile IConInfoNonListening<?> _conInfoNL;
	protected final SessionObjectTypes _sessionObjectType;
	protected volatile SessionTypes _type;	
	protected volatile SessionConnectionType _sessionConnectionType = SessionConnectionType.S_CON_T_UNKNOWN;
	
	protected int _receivedConfCount;
	protected TMulticast_Conf[] _receivedConfs;
	
	protected final IEventOccurance _retryEvent;
	
	final protected InetSocketAddress _localAddress;
	public final LocalSequencer _localSequencer;
	
	protected InetSocketAddress _remote;
	protected String _remoteId;
	protected Sequence _lastLocalSequenceAckedOrConfirmedAtOtherEndPoint;
	protected Sequence _lastLocalSeqauence;
	protected Sequence _lastRemoteSequence;
	
	protected List<IRawPacket> _pendingMsgs;
	private Sequence _lastConnectionUpdateSequence;
	private int _lastReceivedOrderedSequence = -1;
		
	public TempSessionRecoveryDataRepository  _tempRepositoryObject;

	public IConInfoNonListening<?> getConInfoNL() {
		return _conInfoNL;
	}
	
	public InetSocketAddress getLocalAddress() {
		return _localAddress;
	}
	
	public void resetEverythingForRecovery() {
		resetEverything(true);
	}
	
	public void assertOrderedSequence(int newOrderedSequence) {
		assert newOrderedSequence > _lastReceivedOrderedSequence;
		_lastReceivedOrderedSequence = newOrderedSequence;
	}
	
	public void resetEverything(boolean preserveRetryCount) {
		if(!preserveRetryCount)
			_retryEvent.pending();

		_receivedConfCount = 0;
		_receivedConfs = new TMulticast_Conf[Broker.AGGREGATED_CONFIRMATION_ACK_COUNT];
		_pendingMsgs = new LinkedList<IRawPacket>();
		_lastRemoteSequence = null;
		_lastLocalSequenceAckedOrConfirmedAtOtherEndPoint = null;
		_lastLocalSeqauence = null;
		_conInfoNL = null;
		_lastTRecoveryJoinSent = false;
		_lastTRecoverySubscriptionSent = false;
	}
	
	public boolean connectionHasBeenProcessed() {
		if(_lastConnectionUpdateSequence == null)
			return false;
		if(_conInfoNL.getLastUpdateSequence().succeeds(_lastConnectionUpdateSequence))
			return false;
		return true;
	}
	
	public void connectionProcessed() {
		_lastConnectionUpdateSequence = _localSequencer.getNext();
	}
	
	boolean isMoreRecent(ISession session2) {
		if(this._type == SessionTypes.ST_PEER_UNKNOWN)
			throw new IllegalStateException("ISession::isMoreRecent(.) - ERROR, this session is not yet established.");
		if(session2 == null || session2._type == SessionTypes.ST_PEER_UNKNOWN)
			throw new IllegalArgumentException("ISession::isMoreRecent(.) - ERROR, session '" + session2 + "' is not esablished.");
		if(!this._remote.equals(session2._remote))
			throw new IllegalArgumentException("ISession::isMoreRecent(.) - ERROR, sessions do not correspond to the same remoteCC");
		
		if(this._lastRemoteSequence.succeeds(session2._lastRemoteSequence))
			return true;
		return false;
	}
	
	protected ISession(IBrokerShadow brokerShadow, SessionObjectTypes sessionObjectType, SessionTypes type) {
		synchronized(_instanceCounterLock) {
			_session_index = _session_counter++;
		}
		
		_brokerShadow = brokerShadow;
		_localSequencer = _brokerShadow.getLocalSequencer();
		_sessionObjectType = sessionObjectType;
		_localAddress = _brokerShadow.getLocalAddress();
		ISessionManager.setType(this, type);
		_retryEvent = ISessionManager.createNewSessionRetryEventOccurance(this, "SessionRetry");
		resetEverything(false);
	}
	
	public static ISession createDummySession(IBrokerShadow brokerShadow) {
		return createDummySession(brokerShadow, SessionTypes.ST_DUMMY);
	}

	public static ISession createDummySession(IBrokerShadow brokerShadow, SessionTypes type) {
		return ISessionManager.createBaseSession(brokerShadow, type);
	}
	
	public void setIConInfo(IConInfoNonListening<?> conInfoNL) {
		if(_conInfoNL != null && conInfoNL != null)
			throw new IllegalStateException("Connection info is already set to: " + _conInfoNL + ". Can't reset it to: " + conInfoNL);
		
		_conInfoNL = conInfoNL;
		if (Broker.CORRELATE)
			TrafficCorrelator.getInstance().sessionConnectionSet(this, _conInfoNL);
	}
	
	public SessionTypes getSessionType() {
		return _type;			
	}
	
	public InetSocketAddress getRemoteAddress() {
		return _remote;
	}
	
	public ISession setRemoteCC(InetSocketAddress remote) {
		if(_discarded)
			throw new IllegalArgumentException(this.toString() + " vs. " + remote);

		// RemoteCC can only be set once!
		if(_remote != null && !_remote.equals(remote))
			throw new IllegalStateException("ISession::setRemoteCC(.) - ERROR, cannot set remoteCC for a non INVALID session");
		
		_remote = remote;
		BrokerIdentityManager idMan = _brokerShadow.getBrokerIdentityManager();
		OverlayNodeId remoteNodeId = (idMan == null ? null : idMan.getBrokerId(_remote));
		_remoteId = remoteNodeId == null ? _remote.toString() : remoteNodeId.getNodeIdentifier();
		return this;
	}
	
	public void storeSessionInitiationData(TSessionInitiation sInit) {
		_tempRepositoryObject.addNewSInitMessage(sInit);
	}
	
	public void storeSessionRecoveryData(TRecovery tr) {
		_tempRepositoryObject.addNewRecoveryMessage(tr);
	}
	
	public void storeSessionConfirmedMulticastData(TMulticast tm) {
		_tempRepositoryObject.addNewConfirmedMessage(tm);
	}
	
	public void storeSessionUnconfirmedMulticastData(TMulticast tm) {
		_tempRepositoryObject.addNewUnconfirmedMessage(tm);
	}
	
	public boolean isOutgoing() {
		if(_conInfoNL.getRemoteAddress().equals(this._remote))
			return true;
		return false;
	}
	
	public String toStringVeryShort() {
		return toStringPrefix() + "[" + (_remote==null?null:_remote.getPort()) + "]";
	}
	
	public String toStringShort() {
		return toStringPrefix() + "[" + 
				_sessionConnectionType.toStringShort() + "-" +
				_type.toStringShort() + "_" +
				_localAddress.getPort() + "=>" +
				((_remoteId==null)?null:_remoteId) + "]";
//				((_remote==null)?null:_remote.getPort()) + "]";
	}
	
	protected String toStringPrefix() {
		return "S(#" + _session_index + "##" + (_conInfoNL==null?null:_conInfoNL.toStringShort()) + ")";
	}
	
	public String toStringElaborate() {
		int pendingMsgCount = -1;
		if(_pendingMsgs != null)
			pendingMsgCount = _pendingMsgs.size();
		if(_remote != null)
			return "ISession (" + _localAddress.getPort() + "---" + _remote.toString().substring(1) + ") @ "+ _type + "{" + getRetryString() + "/" + pendingMsgCount + "}";
		return "ISession (" + _localAddress.getPort() + "---" + _remote + ") @ "+ _type + "{" + getRetryString() + "/" + pendingMsgCount + "}";
	}
	
	@Override
	public String toString() {
		return toStringShort();
	}
	
	public boolean hasConnection() {
		return _conInfoNL != null;
	}
	
	public boolean isConnected() {
		if(_conInfoNL == null)
			return false;
		if(_conInfoNL.isConnected())
			return true;
		return false;
	}

	public String getRetryString() {
			return _retryEvent.toString();
	}
	
	public TempSessionRecoveryDataRepository getTempSessionDataRepository() {
		return _tempRepositoryObject;
	}
		
	public void flushTempRepositoryData() {
		if(_tempRepositoryObject == null)
			return;
		_tempRepositoryObject.flushTempSessionRecoveryData();
	}
	
	public void flushPendingMessages() {
		if(_pendingMsgs == null)
			return;
		
		synchronized (_pendingMsgs) {
			for(IRawPacket raw : _pendingMsgs) {
				if(raw.getType()==PacketableTypes.TMULTICAST) {
					TMulticast tm = (TMulticast)raw.getObject();
					if (Broker.CORRELATE)
						TrafficCorrelator.getInstance().messageLost(tm, this);
				}
			}
			
			_pendingMsgs.clear();
		}
	}
	
	public TempSessionRecoveryDataRepository clearTempRepositoryData() {
		TempSessionRecoveryDataRepository tmp = _tempRepositoryObject;
		_tempRepositoryObject = null;
		return tmp;
	}
	
	public void setLastReceivedSequence(Sequence lastReceivedSequence) {
		if(lastReceivedSequence != null)
			if (!lastReceivedSequence.getAddress().equals(_localAddress))
				throw new IllegalStateException("Sequence is not from local address: " + lastReceivedSequence + " vs. " + _localAddress);
		
//		if(_lastLocalSequenceAckedOrConfirmedAtOtherEndPoint != null && seq != null)
//			if(_lastLocalSequenceAckedOrConfirmedAtOtherEndPoint.succeeds(seq))
//				throw new IllegalStateException("Setting _lastLocalSequenceAckedOrConfirmedAtOtherEndPoint to:'" + seq + "' preceding the old value: '" + _lastLocalSequenceAckedOrConfirmedAtOtherEndPoint + "': " + this);

		if (Broker.CORRELATE)
			TrafficCorrelator.getInstance().sessionReportsLastReceived(this, lastReceivedSequence);
		
		if(_lastLocalSequenceAckedOrConfirmedAtOtherEndPoint != null)
			if(_lastLocalSequenceAckedOrConfirmedAtOtherEndPoint.succeeds(lastReceivedSequence))
				throw new IllegalStateException(_lastLocalSequenceAckedOrConfirmedAtOtherEndPoint + " vs. " + lastReceivedSequence);
		_lastLocalSequenceAckedOrConfirmedAtOtherEndPoint = lastReceivedSequence;
	}
	
	public Sequence getLastReceivedOrConfirmedLocalSequenceAtOtherEndPoint() {
		return _lastLocalSequenceAckedOrConfirmedAtOtherEndPoint;
	}

	public boolean receiveTMulticast_Conf(TMulticast_Conf conf) {
		synchronized(_receivedConfs) {
			if(conf != null)
				_receivedConfs[_receivedConfCount++] = conf;
			
			if(_receivedConfCount == Broker.AGGREGATED_CONFIRMATION_ACK_COUNT) {
				return true;
			}
			return false;
		}
	}
	
	public TConf_Ack flushTConf_Acks() {
		synchronized(_receivedConfs) {
			if(_receivedConfCount == 0)
				return null;
			
			TConf_Ack tConfAck = new TConf_Ack(_localAddress, _remote, _receivedConfs, _receivedConfCount);
			_receivedConfCount = 0;
			if(Broker.DEBUG)
				LoggerFactory.getLogger().debug(this, tConfAck.toString());
			
			return tConfAck;
		}
	}
	
	// all invokations of setSessionConnectionType must synchronize on _sessionsLock.
	public ISession setSessionConnectionType(SessionConnectionType sessionConnectionType) {
		if(_discarded)
			new IllegalArgumentException(this.toString() + " vs. " + sessionConnectionType).toString();
		
		if(sessionConnectionType == SessionConnectionType.S_CON_T_DROPPED)
			discard();
		
		if(!Broker.RELEASE || Broker.DEBUG)
			LoggerFactory.getLogger().info(this, "Setting SessionConnectionType of '" + this + "' changed from " + _sessionConnectionType + "' to '" + sessionConnectionType + "'");
		
		if (Broker.CORRELATE)
			TrafficCorrelator.getInstance().sessionConnectionType(this, sessionConnectionType);
		
		_sessionConnectionType = sessionConnectionType;
		
		return this;
	}
	
	public SessionConnectionType getSessionConnectionType() {
		return _sessionConnectionType;
	}
	
	public boolean isEnded() {
		return _type == SessionTypes.ST_END;
	}
	
	public boolean isRealSessionTypePubSubEnabled() {
		return getRealSession()._type._pubSubEnabled;
	}
	
	public boolean isSessionItselfTypePubSubEnabled() {
		return _type._pubSubEnabled;
	}
	
	public boolean isLocallyActive() {
		return _sessionConnectionType._isLocallyActive;
	}
	
	public boolean isInMQ() {
		return _sessionConnectionType._isInMQ;
	}
	
	public boolean isReal() {
		return _sessionConnectionType._isReal;
	}
	
	public boolean shouldMoveAlongTheMQ() {
		return isLocallyActive();
	}
	
	public boolean isPeerRecovering() {
		return _type._isPeerRecovering;
	}

	@Override
	public ISessionInfo getInfo() {
		ISessionInfo sessionInfo = new ISessionInfo(this, _localAddress);
		return sessionInfo;
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_ISESSION;
	}

	public static ISession[] getDummyISessions(IBrokerShadow brokerShadow, InetSocketAddress[] remotes, SessionTypes sType, SessionConnectionType sConnType) {
		ISession[] sessions = new ISession[remotes.length];
		for ( int i=0 ; i<remotes.length ; i++) {
			sessions[i] = ISessionManager.createBaseSession(brokerShadow, sType);
			sessions[i].setSessionConnectionType(sConnType);
			sessions[i].setRemoteCC(remotes[i]);
		}

		return sessions;
	}
	
	public boolean tRecoveryJoinCanBeSent() {
		return !_lastTRecoveryJoinSent;
	}
	
	public boolean tRecoverySubscriptionCanBeSent() {
		return !_lastTRecoverySubscriptionSent;
	}
	
	public void tRecoveryJoinIsSent() {
		_lastTRecoveryJoinSent = true;
	}
	
	public void tRecoverySubscriptionIsSent() {
		_lastTRecoverySubscriptionSent = true;
	}

	public static Set<InetSocketAddress> getISessionsAsInetSocketAddresses(ISession[] sessions) {
		Set<InetSocketAddress> set = new HashSet<InetSocketAddress>();
		for ( int i=0 ; i<sessions.length ; i++) {
			InetSocketAddress address = sessions[i]._remote;
			set.add(address);
		}
		
		return set;
	}
	
	public static Set<ISession> getISessionsAsSessionsSet(ISession[] sessions) {
		Set<ISession> set = new HashSet<ISession>();
		for ( int i=0 ; i<sessions.length ; i++) {
			set.add(sessions[i]);
		}
		
		return set;
	}

	// Method must not be overwritten
	@Override
	public final int hashCode() {
		return super.hashCode();
	}
	
	// Method must not be overwritten
	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	public ISession getRealSession() {
		return this;
	}
	
	public ISession getRealSession(IRawPacket raw) {
		return this;
	}
	
	public void setRealSession(ISession realSession) {
		if(realSession != this)
			throw new IllegalArgumentException(this + " vs. " + realSession);
	}
	
	public boolean isPrimaryActive() {
		return _sessionConnectionType._isPrimaryActive;
	}
	
	public boolean isBeingConnected() {
		return _sessionConnectionType._isConnecting;
	}
	
	public boolean isLocal() {
		return false;
	}

	public void discard() {
		if(_discarded)
			LoggerFactory.getLogger().info(this, new IllegalArgumentException(this.toString()).toString());
		
		_discarded = true;
	}
	
	public boolean isDiscarded() {
		return _discarded;
	}
	
	public void resetRetryCountToPendingReset() {
		_retryEvent.pending(true);
	}
	
	public void resetRetryCountToPending() {
		_retryEvent.pending();
	}
	
	public void resetRetryCountToFailed() {
		_retryEvent.fail();
	}
	
	public void resetRetryCountToSuccess() {
		_retryEvent.success();
	}
	
	public void updateLastMQNSequence(int mqnSequence) {
		if (_lastMQNSequence + 1 != mqnSequence && _lastMQNSequence != -1)
			LoggerFactory.getLogger().info(this, "ERROR" + (_lastMQNSequence > mqnSequence?"+":"") + ": Session(" + _remote + ") skipped from '" + _lastMQNSequence + "' to '" + mqnSequence + "'");
		
		_lastMQNSequence = mqnSequence;
	}
	
	public boolean mustGoBackInMQ() {
		return true;
	}

	@Override
	public boolean testRetry() {
		return _retryEvent.test();
	}

	@Override
	public void resetRetry() {
		_retryEvent.reset();
	}

	public void transferMessages(ISession oldSession) {
		IRawPacket[] oldMessages;
		synchronized(oldSession._pendingMsgs) {
			oldMessages = oldSession._pendingMsgs.toArray(new RawPacket[0]);
			oldSession._pendingMsgs.clear();
		}
		
		synchronized (_pendingMsgs) {
			for(IRawPacket raw : oldMessages) {
				if (raw.getType() == PacketableTypes.TMULTICAST) {
					TMulticast tm = (TMulticast) raw.getObject();
					if (Broker.CORRELATE)
						TrafficCorrelator.getInstance().messageTransferred(tm, oldSession, this);
					_pendingMsgs.add(raw);
				}
			}
		}
	}
	
	public String toStringWithConInfoNL() {
		return toStringPrefix();
	}
	
	public IRawPacket hasCheckedout(TMulticast tm, IRawPacket raw) {
		return raw;
	}
}
