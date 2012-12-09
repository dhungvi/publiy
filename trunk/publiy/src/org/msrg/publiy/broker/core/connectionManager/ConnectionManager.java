package org.msrg.publiy.broker.core.connectionManager;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManagerQueriable;
import org.msrg.publiy.pubsub.core.messagequeue.FastConfMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.MQCleanupInfo;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueue;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.multicast.TDack;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastAnnotatorFactory;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.pubsub.core.packets.recovery.TempSessionRecoveryDataRepository;
import org.msrg.publiy.pubsub.core.subscriptionmanager.LocalSubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.utils.annotations.AnnotationEvent;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;
import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationDumpSpecifier;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.ISessionInfo;
import org.msrg.publiy.broker.info.JoinInfo;
import org.msrg.publiy.broker.info.PSSessionInfo;
import org.msrg.publiy.broker.info.SubscriptionInfo;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.communication.core.packet.types.SessionInitiationTypes;
import org.msrg.publiy.communication.core.packet.types.TCommand;
import org.msrg.publiy.communication.core.packet.types.TConf_Ack;
import org.msrg.publiy.communication.core.packet.types.TPing;
import org.msrg.publiy.communication.core.packet.types.TPingReply;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.component.ComponentStatus;

abstract class ConnectionManager extends AbstractConnectionManager {
	private Map<ISession, ISession> _replacing_to_repalced = new HashMap<ISession, ISession>();
	protected Map<InetSocketAddress, ISession> _allSessions = new HashMap<InetSocketAddress, ISession>();
	protected Object _sessionsLock = _allSessions;
	
	protected String _sessionsDumpFileName;
	
	protected IMessageQueue _mq;
	protected ISubscriptionManager _subscriptionManager;
	protected IOverlayManager _overlayManager;
	
	///////////////////////////////////////////////////////////////////////////
	//							ABSTRACT METHODS							 //
	///////////////////////////////////////////////////////////////////////////
	public abstract void tmConfirmed(TMulticast tm);
	
	protected abstract boolean performConnectionManagerTypeSpecificPreRegistration(ISession newSession);
	
	protected abstract void performConnectionManagerTypeSpecificPostRegistration(ISession newSession);

	protected abstract void performConnectionManagerTypeSpecificPostFailed(ISession fSession);
	
	protected abstract void sendAllRecoveryDataToRecoveringAndEndRecovery(ISession session);
	
	protected abstract void sendAllToRecoveringAndNotEndRecovery(ISession session);

	protected abstract void handleTConfAckMessage(ISession session, TConf_Ack tConfAck);
	
	protected abstract void handleRecoveryMessage(ISession session, TRecovery tr);
	
	protected abstract void handleSessionInitMessage(ISession session, TSessionInitiation sInit);
	
	protected abstract void handleMulticastMessage(ISession session, TMulticast tm);
	
	protected final void performMulticastMessageSendersTest(ISession session, TMulticast tm) {
		InetSocketAddress remote = session.getRemoteAddress();
		if(remote == null)
			return;
		
		InetSocketAddress sender = tm.getSenderAddress();
		if(sender == null)
			throw new IllegalStateException("Sender of '" + tm.toStringTooLong() + "' is null");
		if(!sender.equals(remote))
			throw new IllegalStateException("Sender '" + sender + "' of '" + tm.toStringTooLong() + "' is not equal to " + remote + " \t" + tm.getSequenceVector());
	}
	
	///////////////////////////////////////////////////////////////////////////
	//							CONSTRUCTORS								 //
	///////////////////////////////////////////////////////////////////////////
	protected ConnectionManager(String connectionManagerName, ConnectionManagerTypes type, ConnectionManager oldConMan) {
		super(connectionManagerName, type, oldConMan.getLocalSequencer());

		if(oldConMan._nioBinding.getComponentState() != ComponentStatus.COMPONENT_STATUS_PAUSED)
			throw new IllegalStateException("ConnectionManager cannot migrate since INIOBinging is not paused ('" + oldConMan._nioBinding.getComponentState() + "').");
		if(oldConMan._status != ComponentStatus.COMPONENT_STATUS_PAUSED)
			throw new IllegalStateException("ConnectionManager cannot migrate since prev conMan is not stopped ('" + oldConMan.getComponentState() + "').");
		
		oldConMan._nioBinding.switchListeners(this, oldConMan);
		
		{
			_status = oldConMan._status;
			oldConMan._status = ComponentStatus.COMPONENT_STATUS_STOPPED;
		}
		{
			_brokerShadow = oldConMan._brokerShadow;
//			_connectionManagerName = "ConnectionManagerPS-" + _broker.getBrokerID();
			oldConMan._brokerShadow = null;
		}
		{
			_broker = oldConMan._broker;
			oldConMan._broker = null;
		}
		{
			_nioBinding = oldConMan._nioBinding;
			oldConMan._nioBinding = null;
			_sessionManager.setNIOBinding(_nioBinding);
			
			_nioBinding.addNewComponentListener(this);
			_nioBinding.removeComponentListener(oldConMan);
		}
		{
			_sessionsDumpFileName = oldConMan._sessionsDumpFileName;
			oldConMan._sessionsDumpFileName = null;
		}
		{
//			_dackSendPending = oldConMan._dackSendPending;
//			oldConMan._dackSendPending = null;
		}
		{
			_localSequencer = oldConMan._localSequencer;
			oldConMan._localSequencer = null;
		}
		{
			_localAddress = oldConMan._localAddress;
			oldConMan._localAddress = null;
		}
		{
			_componentListeners = oldConMan._componentListeners;
			oldConMan._componentListeners = null;
		}
		{
			_allSessions = oldConMan._allSessions;
			oldConMan._allSessions = null;
		}
		{
			_sessionsLock = oldConMan._sessionsLock;
			oldConMan._sessionsLock = null;
		}
		{
			_conInfoL = oldConMan._conInfoL;
			oldConMan._conInfoL = null;
		}
		{
			_connectionEvents = oldConMan._connectionEvents;
//			oldConMan._connectionEvents = null;
		}
		{
			_overlayManager = oldConMan._overlayManager;
			oldConMan._overlayManager = null;
		}
		{
			_subscriptionManager = oldConMan._subscriptionManager;
			oldConMan._subscriptionManager = null;
		}
		{
			BrokerInternalTimer.inform("Switching MQ, ");
			_mq = createMessageQueue(_brokerShadow, oldConMan._mq);
			_mq.setConnectionManager(this);
			oldConMan._mq = null;
//			_mq.changeConnectionManager(this);
		}

		oldConMan.__nextConnectionManager = this;
		dumpAllSessions();
	}
	
	protected ConnectionManager(
			String connManName,
			ConnectionManagerTypes type,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue,
			TRecovery_Join[] trjs) throws IOException {
		super(connManName, type, brokerShadow.getLocalSequencer());
		_brokerShadow = brokerShadow;
		_broker = broker;
		_localSequencer = _brokerShadow.getLocalSequencer();

		if(_brokerShadow != null)
			_sessionsDumpFileName = _brokerShadow.getSessionsDumpFileName();
		else
			_sessionsDumpFileName = getDefaultSessionsDupmFileName(_localSequencer);
		
		_localAddress = _localSequencer.getLocalAddress();

		_overlayManager = overlayManager;
		_subscriptionManager = subscriptionManager;

		_mq = messageQueue;
		_mq.setConnectionManager(this);
		if(type == ConnectionManagerTypes.CONNECTION_MANAGER_RECOVERY) {
//			_mq = createMessageQueue(brokerShadow, _overlayManager, _subscriptionManager, false);
			_mq.allowConfirmations(false);
			initMessageQueue(trjs, _brokerShadow);
		} else {
//			_mq = createMessageQueue(brokerShadow, _overlayManager, _subscriptionManager, true);
			_mq.allowConfirmations(true);
			initMessageQueue(trjs, _brokerShadow);
		}
		dumpAllSessions();
	}

	@Override
	protected final void processMessage(ISession session, IRawPacket raw) {
		PacketableTypes packetType = raw.getType();
		
		switch( packetType) {
		case TSESSIONINITIATION:
			TSessionInitiation sInit = (TSessionInitiation) PacketFactory.unwrapObject(_brokerShadow, raw, true);
			handleSessionInitMessage(session, sInit);
			break;
		
		case TMULTICAST:
			TMulticast tm = (TMulticast) PacketFactory.unwrapObject(_brokerShadow, raw);//.getTMulticastObject(raw.getBody());
			InetSocketAddress sender = tm.getSenderAddress();
			if(sender == null)
				throw new NullPointerException(session + " vs. " + raw.getObject());
			if(!session.getRemoteAddress().equals(sender))
				throw new IllegalStateException(session + " vs. " + sender + " vs. " + raw.getObject());
			TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(_localSequencer, raw, AnnotationEvent.ANNOTATION_EVENT_MESSAGE_HANDLING, "Handling msg");
			handleMulticastMessage(session, tm);
			break;
		
		case TCONF_ACK:
			TConf_Ack tConfAck = (TConf_Ack) PacketFactory.unwrapObject(_brokerShadow, raw);
			handleTConfAckMessage(session, tConfAck);
			break;
		
		case TRECOVERY:
			TRecovery tr = (TRecovery) PacketFactory.unwrapObject(_brokerShadow, raw);
			handleRecoveryMessage(session, tr);
			break;
		
		case TDACK:
			TDack tDack = (TDack) PacketFactory.unwrapObject(_brokerShadow, raw);
			handleDackMessage(session, tDack);
			break;
		
		case TPING:
			TPing tping = (TPing) TPing.getObjectFromBuffer(raw.getBody());
			handleTPingMessage(session, tping);
			break;
		
		case TPINGREPLY:
			TPingReply tpingReply = (TPingReply) PacketFactory.unwrapObject(_brokerShadow, raw);
			handleTPingReplyMessage(session, tpingReply);
			break;
			
		case TCOMMAND:
			TCommand inTCommand = (TCommand) PacketFactory.unwrapObject(_brokerShadow, raw);
			handleTCommandDisseminateMessage(inTCommand, session.getRemoteAddress());
			break;

		default:
			if(!handleSpecialMessage(session, raw))
				throw new IllegalStateException("ConnectionManager::processMessagePS - ERROR, invalid msg type '" + raw.getType() + "'.");
		}
	}
	
	protected boolean handleSpecialMessage(ISession session, IRawPacket raw) {
		return false;
	}
	
	protected final void handleTPingReplyMessage(ISession session, TPingReply tpingReply) {
		InetSocketAddress remote = tpingReply.getInetSocketAddress();
		_brokerShadow.getRTTLogger().updatefromTPingReply(remote, tpingReply);
	}
	
	protected final void handleTPingMessage(ISession session, TPing tping) {
		double cpu = _brokerShadow.getCpuCasualReader().getAverageCpuPerc();
		double inputPubRate = _brokerShadow.getMessageProfilerLogger().getInputPublicationRate();
		double outputPubRate = _brokerShadow.getMessageProfilerLogger().getOutputPublicationRate();
		TPingReply tpingReply = new TPingReply(tping, cpu, inputPubRate, outputPubRate, _localAddress);
		IRawPacket rawReply = PacketFactory.wrapObject(_localSequencer, tpingReply);
		_sessionManager.send(session, rawReply);
	}
	
	protected final void handleDackMessage(ISession session, TDack tDack) {
		LoggerFactory.getLogger().debug(this, "Processing TDACK from " + session.toStringShort() + ": " + tDack);
		_mq.processTDack(tDack);
	}
	
	protected final void sendEndRecoveryMessage(ISession session) {
		TSessionInitiation sInit = new TSessionInitiation(_localSequencer, session.getRemoteAddress(), SessionInitiationTypes.SINIT_PUBSUB, null);
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, sInit);
		_sessionManager.send(session, raw);
	}
	
	protected final void sendLastRecoverySubscribeMessage(ISession session) {
		session.tRecoverySubscriptionIsSent();
		
		TRecovery lastTRS = TRecovery.getLastTRecoverySubscription(_localSequencer.getNext());
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, lastTRS);
		_sessionManager.send(session, raw);
	}
	
	protected final void sendLastRecoveryJoinMessage(ISession session) {
		session.tRecoveryJoinIsSent();
		
		TRecovery lastTRJ = TRecovery.getLastTRecoveryJoin(_localSequencer.getNext());
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, lastTRJ);
		_sessionManager.send(session, raw);
	}

	protected final void sendAllSubscriptionEntrySummaries(ISession session) {
		if(!session.tRecoverySubscriptionCanBeSent())
			throw new IllegalStateException();
		
		IOverlayManager overlayManager = getOverlayManager();
		InetSocketAddress remote = session.getRemoteAddress();
		ISubscriptionManager subscriptionManager = getSubscriptionManager();
		TRecovery_Subscription[] trss = subscriptionManager.getAllSummary(remote, overlayManager);
		for(int i=0 ; i<trss.length ; i++) {
			IRawPacket raw = trss[i].morph(session, overlayManager);
			_sessionManager.send(session, raw);
		}
		return;
	}
	
	protected final void sendAllOverlayEntrySummaries(ISession session) {
		if(!session.tRecoveryJoinCanBeSent())
			throw new IllegalStateException();
		
		IOverlayManager overlayManager = getOverlayManager();
		InetSocketAddress remote = session.getRemoteAddress();
		TRecovery_Join[] trjs = overlayManager.getSummaryFor(remote);

		for(int i=0 ; i<trjs.length ; i++) {
			IRawPacket raw = trjs[i].morph(session, overlayManager);
			_sessionManager.send(session, raw);
		}
	}

	@Override
	public final Sequence getLastReceivedSequence(InetSocketAddress remote) {
		return _mq.getLastReceivedSequence(remote);
	}
	
	protected final void initMessageQueue(TRecovery_Join [] trjs, IBrokerShadow brokerShadow) {
		if(trjs == null)
			return;
		
		_mq.applyAllSummary(trjs);
	}
	
	protected IMessageQueue createMessageQueue(
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager, boolean allowToConfirm) {
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_MQ, "Creating Normal-MQ");
		IMessageQueue mq = new MessageQueue(
				brokerShadow, this, overlayManager, subscriptionManager, allowToConfirm);
		mq.allowConfirmations(allowToConfirm);
		return mq;
	}
	
	protected IMessageQueue createMessageQueue(IBrokerShadow brokerShadow, IMessageQueue oldMQ) {
		if(!brokerShadow.isMP() && brokerShadow.isFastConf()) {
			LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_MQ, "Creating FastConf-MQ");
			IMessageQueue mq = new FastConfMessageQueue(this, oldMQ);
			return mq;
		}

		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_MQ, "Creating Normal-MQ");
		IMessageQueue mq = new MessageQueue(this, oldMQ);
		return mq;
	}
	
	protected final void applyTempSessionRepositoryData(ISession session) {
		TempSessionRecoveryDataRepository tempRep = session.getTempSessionDataRepository();
		if(tempRep == null)
			return;
		TSessionInitiation [] sInits = tempRep.getAllSInitMessages();
		for(int i=0 ; i<sInits.length ; i++) {
			_sessionManager.update(_brokerShadow, session, sInits[i]);
		}
		
		TRecovery_Join[] trjs = tempRep.getAllRecoveryJoinMessages();
		_mq.applyAllSummary(trjs);
		
		TRecovery_Subscription[] trss = tempRep.getAllRecoverySubscriptionMessages();
		_mq.applyAllSummary(trss);
		
		TMulticast [] tms = tempRep.getAllUnconfirmedMulticastMessages();
		for(int i=0 ; i<tms.length ; i++) {
			handleMulticastMessage(session, tms[i]);
		}
		
		session.clearTempRepositoryData();
	}
	
	@Override
	public ISessionManager getSessionManager() {
		return _sessionManager;
	}
	
	@Override
	public IOverlayManager getOverlayManager() {
		return _overlayManager;
	}
	
	@Override
	public ISubscriptionManager getSubscriptionManager() {
		return _subscriptionManager;
	}

	@Override
	protected void connectionManagerJustStarted() {
		// Do all schedulings ... 
		scheduleNextForcedConfAck();
		
		addAllSessionsForFirstRevision();
	}
	
	@Override
	public SubscriptionInfo[] getSubscriptionInfos() {
		ISubscriptionManagerQueriable subscriptionManager = getSubscriptionManager();
		if(subscriptionManager == null)
			return null;
		
		SubscriptionInfo[] subInfos = subscriptionManager.getSubscriptionInfos();
		return subInfos;
	}
	
	@Override
	public JoinInfo[] getTopologyLinks() {
		IOverlayManager overlayManager = getOverlayManager();
		JoinInfo[] joinInfos = overlayManager.getTopologyLinks();
		
		return joinInfos;
	}
	
	@Override
	public PSSessionInfo[] getPSSessions() {
		if(_mq == null)
			return null;
		
		return _mq.getPSSessionInfos();
	}
	
	@Override
	public ISessionInfo[] getSessionInfos() {
		Set<ISessionInfo> sessionInfos = new HashSet<ISessionInfo>();

		ISession[] sessions = getAllSessions();
		for(int i=0 ; i<sessions.length ; i++) {
			if(sessions[i]!=null && !sessions[i].isLocal()) // ISessionLocal.class.isInstance(session))
				sessionInfos.add(sessions[i].getInfo());
		}

		return sessionInfos.toArray(new ISessionInfo[0]);
	}
	
	@Override
	public ISessionInfo[] getActiveSessions() {
		Set<ISessionInfo> sessionInfos = new HashSet<ISessionInfo>();

		synchronized(_sessionsLock) {
			ISession[] allSessions = getAllSessionsPrivately();
			for(int i=0 ; i<allSessions.length ; i++) {
				if(allSessions[i]!=null && allSessions[i].getSessionConnectionType() == SessionConnectionType.S_CON_T_ACTIVE)
					sessionInfos.add(allSessions[i].getInfo());
			}
		}

		return sessionInfos.toArray(new ISessionInfo[0]);
	}
	
	@Override
	public ISessionInfo[] getInactiveSessions() {
		Set<ISessionInfo> sessionInfos = new HashSet<ISessionInfo>();

		synchronized(_sessionsLock) {
			ISession[] allSessions = getAllSessionsPrivately();
			for(int i=0 ; i<allSessions.length ; i++) {
				if(allSessions[i]!=null && allSessions[i].getSessionConnectionType() == SessionConnectionType.S_CON_T_INACTIVE)
					sessionInfos.add(allSessions[i].getInfo());
			}
		}

		return sessionInfos.toArray(new ISessionInfo[0]);
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	//						ISESSION RELATED METHODS						 //
	///////////////////////////////////////////////////////////////////////////
	
	protected ISession getSessionPrivately(InetSocketAddress remote) {
		return _allSessions.get(remote);
	}
	
	protected void addSessionPrivately(InetSocketAddress remote, ISession session) {
		if(session.isDiscarded())
			throw new IllegalArgumentException(session.toString());
		
		if(!Broker.RELEASE)
			LoggerFactory.getLogger().info(this, "Adding Session Privately: " + session);
		
		_allSessions.put(remote, session);
	}
	
	@Override
	public ISession[] getAllSessions() {
		synchronized(_sessionsLock) {
			return getAllSessionsPrivately();
		}
	}
	
	protected ISession[] getAllSessionsPrivately() {
		return _allSessions.values().toArray(new ISession[0]);
	}
	
	@Override
	public final void registerUnjoinedSessionAsJoined(InetSocketAddress remote) {
		synchronized(_sessionsLock) {
			ISession unjoinedSession = null;
			unjoinedSession = getSessionPrivately(remote);
			if(unjoinedSession == null || unjoinedSession.getSessionConnectionType() != SessionConnectionType.S_CON_T_UNJOINED)
				return;
			
			IOverlayManager overlayManager = getOverlayManager();
			Path<INode> p = overlayManager.getPathFrom(remote);
			
			if(p == null)
				throw new IllegalStateException("In order to register '" + remote + "' as a non-`S_CON_T_UNJOINED' session, it must be in the overlaymanager.");
			
			if(p.getLength() == 1)
			{
				// Register as ACTIVE
				// No need to do a replace: since UNJOINED sessions are already registered with the MQ.
				setSessionsConnectionType(unjoinedSession, SessionConnectionType.S_CON_T_ACTIVE);
				dumpAllSessions();
				_mq.replaceSessions(null, null);
//				_mq.proceed(remote);
				_mq.proceedAll();
			}
			else
			{
				throw new IllegalStateException("An S_CON_T_UNJOINED to '" + remote + "' cannot be re-registered as S_CON_T_INACTIVE.");
			}
		}
	}
	
	protected boolean dumpAllSessions() {
		try{
			FileWriter fwriter = new FileWriter(_sessionsDumpFileName, true);
			fwriter.write("\n" + new Date().toString() + " \n");
			String content = "";
			ISession[] allSessions = getAllSessionsPrivately();
			for(int i=0 ; i<allSessions.length ; i++) {
				SessionConnectionType sessionConnectionType = allSessions[i].getSessionConnectionType();
				String replacingStr = "";
				if(sessionConnectionType == SessionConnectionType.S_CON_T_REPLACING_BEINGREPLACED || 
				     sessionConnectionType == SessionConnectionType.S_CON_T_REPLACING) {
					ISession replacedSession = _replacing_to_repalced.get(allSessions[i]);
					InetSocketAddress remoteAddress = replacedSession.getRemoteAddress();
					replacingStr = " ====>>> " + remoteAddress.toString();
				}
				content += "\t" + //_localAddress + "   " + 
								allSessions[i] + "\t" + replacingStr + "\n";
			}
			
			fwriter.write( content);
			for(int i=0 ; i<40 ; i++)
				fwriter.write("=");
			
			fwriter.close();
		}catch (IOException iox) {
			return false;
		}
		return true;
	}
	
	protected void setSessionsConnectionType(ISession session, SessionConnectionType newSessionConnectionType) {
		if(newSessionConnectionType ==  SessionConnectionType.S_CON_T_DELTA_VIOLATED)
			System.out.println("DELTA_VIOLATED_SESSION: " + session);
		
		session.setSessionConnectionType(newSessionConnectionType);
	}
	
	private void failedPrivately(ISession fSession, Set<ISession> fartherSessionsWithImmediateFailedConnections) {
		fSession.clearTempRepositoryData();
		
		InetSocketAddress remote = fSession.getRemoteAddress();
		if(remote!=null && !isPresentInAllSessionsPrivately(remote)) {
			LoggerFactory.getLogger().warn(this, "Failed session not present in _allSessions: " + fSession);
			setSessionsConnectionType(fSession, SessionConnectionType.S_CON_T_FAILED);
			return;
		}
		
		SessionConnectionType fSessionConnectionType = fSession.getSessionConnectionType();
		switch (fSessionConnectionType) {
		case S_CON_T_DROPPED:
			return;
		
		case S_CON_T_USELESS:
		case S_CON_T_INACTIVE:
			setSessionsConnectionType(fSession, SessionConnectionType.S_CON_T_FAILED);
			removeFromAllSessionsPrivately(remote);
			return;
		
		// these cannot fail again
		case S_CON_T_DELTA_VIOLATED:
		case S_CON_T_BEINGREPLACED:
		case S_CON_T_REPLACING_BEINGREPLACED:
		// these have been handled by the caller..
		case S_CON_T_CANDIDATE:
		case S_CON_T_SOFT:
		case S_CON_T_SOFTING:
			throw new IllegalStateException(fSession.toString());
			
		default:
			break;
		}
		
		ISession registeredFSession = getSessionPrivately(remote);	
		if(registeredFSession == null && fSessionConnectionType != SessionConnectionType.S_CON_T_UNKNOWN)
			return;
		
		else if(registeredFSession != fSession && fSessionConnectionType != SessionConnectionType.S_CON_T_UNKNOWN)
			throw new IllegalStateException("FSession '" + remote + "' is '" + fSessionConnectionType + "' (not S_CON_T_UNKNOWN), but is not registered with allSessions.");

		switch(fSessionConnectionType) {
		case S_CON_T_UNJOINED:
		{
			Set<ISession> mqRemovedSessions = new HashSet<ISession>();
			removeFromAllSessionsPrivately(remote);	
			setSessionsConnectionType(fSession, SessionConnectionType.S_CON_T_FAILED);
			mqRemovedSessions.add(fSession);
			_mq.replaceSessions(mqRemovedSessions, null);
			return;			
		}

		case S_CON_T_ACTIVE:
		case S_CON_T_REPLACING: 
		case S_CON_T_UNKNOWN:
		{
			Set<ISession> mqAddedSessions = new HashSet<ISession>();
			Set<ISession> mqRemovedSessions = new HashSet<ISession>();
			// Both S_CON_T_ACTIVE, S_CON_T_REPLACING are in the mq
			// But we can only add them to mqRemoved, only if there is no farther neighbor.
			IOverlayManager overlayManager = getOverlayManager();
			InetSocketAddress[] neighborAddresses = overlayManager.getNeighbors(remote);
			
			if(neighborAddresses.length <= 1)
			{
				if(Broker.LOG_VIOLATIONS) {
					ISession replacingSessionSBeingReplacedSession = _replacing_to_repalced.remove(fSession);
					
					setSessionsConnectionType(fSession, SessionConnectionType.S_CON_T_DELTA_VIOLATED);
					if(fSessionConnectionType == SessionConnectionType.S_CON_T_REPLACING) {
						_replacing_to_repalced.remove(fSession);
						mqAddedSessions.add(fSession);
						_mq.replaceSessions(null, mqAddedSessions);
					} else {
						_mq.replaceSessions(null, null);
					}
					
					testBeingReplacedSession(replacingSessionSBeingReplacedSession);
					return;
				} else {
					// There's no farther neighbor, thus we can't do much.
					ISession replacingSessionSBeingReplacedSession = _replacing_to_repalced.remove(fSession);
					setSessionsConnectionType(fSession, SessionConnectionType.S_CON_T_FAILED);
					removeFromAllSessionsPrivately(remote);	
					mqRemovedSessions.add(fSession);
					_mq.replaceSessions(mqRemovedSessions, null);
					testBeingReplacedSession(replacingSessionSBeingReplacedSession);
					return;
				}
			}
			
			if(fSessionConnectionType == SessionConnectionType.S_CON_T_ACTIVE ||
				 fSessionConnectionType == SessionConnectionType.S_CON_T_UNKNOWN)
				setSessionsConnectionType(fSession, SessionConnectionType.S_CON_T_BEINGREPLACED);
			else if(fSessionConnectionType == SessionConnectionType.S_CON_T_REPLACING)
				setSessionsConnectionType(fSession, SessionConnectionType.S_CON_T_REPLACING_BEINGREPLACED);

			for(int i=1 ; i<neighborAddresses.length ; i++)
			{
				InetSocketAddress fartherAddress = neighborAddresses[i];
				ISession fartherSession = null;
				fartherSession = getSessionPrivately(fartherAddress);

				if(fartherSession != null)
				{
					SessionConnectionType fartherSessionConnectionType = fartherSession.getSessionConnectionType();
					switch (fartherSessionConnectionType) {
					case S_CON_T_ACTIVE:
						break;
					
					case S_CON_T_USELESS:
						setSessionsConnectionType(fartherSession, SessionConnectionType.S_CON_T_REPLACING);
						_replacing_to_repalced.put(fartherSession, fSession);
						break;
				
					case S_CON_T_BEINGREPLACED: // fartherSession must have been dropped.
						throw new IllegalStateException("A fartherSession '" + fartherAddress + "' could not have been S_CON_T_BEINGREPLACED, while the closer session '" + remote + "' has failed.");
					
					case S_CON_T_DROPPED: // fartherSession must not have been in _allSessions.
						throw new IllegalStateException("An S_CON_T_DROPPED to '" + fartherAddress + "' must not be in allSessions.");
					
					case S_CON_T_SOFT:
						setSessionsConnectionType(fartherSession, SessionConnectionType.S_CON_T_ACTIVE);
						fartherSession.setRealSession(fartherSession);
						break;
						
					case S_CON_T_SOFTING:
						setSessionsConnectionType(fartherSession, SessionConnectionType.S_CON_T_REPLACING);
						fartherSession.setRealSession(null);
						break;
						
					case S_CON_T_CANDIDATE:
						setSessionsConnectionType(fartherSession, SessionConnectionType.S_CON_T_ACTIVE);
						fartherSession.setRealSession(fartherSession);
						if(!fartherSession.isConnected()) {
							IConInfoNonListening<?> newConInfoNL = _nioBinding.makeOutgoingConnection(fartherSession, this, this, this, fartherAddress);
							if(newConInfoNL == null) {
								fartherSessionsWithImmediateFailedConnections.add(fartherSession);
							}
						}
						break;
						
					case S_CON_T_INACTIVE: 
						setSessionsConnectionType(fartherSession, SessionConnectionType.S_CON_T_ACTIVE);
						mqAddedSessions.add(fartherSession);
						break;
					
					case S_CON_T_REPLACING: // fartherSession must have been S_CON_T_USELESS.
						throw new IllegalStateException("If the farther session '" + fartherAddress + "' is S_CON_T_REPLACING, the failedSession '" + remote + "' could not have failed.");
					
					case S_CON_T_UNJOINED: // There cannot be a fartherSession as S_CON_T_UNJOINED.
						throw new IllegalStateException("S_CON_T_UNJOINED session to '" + fartherAddress + "' canont be the farther address of failed session '" + remote + "'.");
					
					case S_CON_T_UNKNOWN: // No S_CON_T_UNKNOWN session is in _allSessions.
						throw new IllegalStateException("There must be no S_CON_T_UNKNOWN session '" + fartherAddress +"' in allSessions.");
						
					case S_CON_T_FAILED:
					case S_CON_T_REPLACING_BEINGREPLACED:
						break;
						
					default:
						break;
					}
				}
				
				else if(fartherSession == null)
				{
					fartherSession = getDefaultOutgoingSession(fartherAddress);
					setSessionsConnectionType(fartherSession, SessionConnectionType.S_CON_T_REPLACING);
					addSessionPrivately(fartherAddress, fartherSession);	
					_replacing_to_repalced.put(fartherSession, fSession);
					
					LoggerFactory.getLogger().info(this, "Connecting to farther node: " + fartherAddress);
					IConInfoNonListening<?> newConInfoNL = _nioBinding.makeOutgoingConnection(fartherSession, this, this, this, fartherAddress);
					if(newConInfoNL == null) {
						fartherSessionsWithImmediateFailedConnections.add(fartherSession);
					}
//					_nioBinding.renewConnection(fartherSession);//, this, this, this, fartherAddress);
				}
				
			}
			//TODO: shouldn't it be <addresses.length-1 ??
			Set<ISession> killSessions = new HashSet<ISession>();
			Path<INode> pathFromFSession = overlayManager.getPathFrom(remote);
			InetSocketAddress[] addresses = pathFromFSession.getAddresses();
			for(int i=0 ; i<addresses.length ; i++)
			{
				InetSocketAddress closerNeighbor = addresses[i];
				ISession closerSession = null;
				closerSession = getSessionPrivately(closerNeighbor);
				if(closerSession == null)
					break;
				SessionConnectionType closerSessionConnectionType = closerSession.getSessionConnectionType();
				if(closerSessionConnectionType == SessionConnectionType.S_CON_T_BEINGREPLACED) {
					Set<ISession> replacingSessions = getReplacingSessionsForBeingReplacedSession(_replacing_to_repalced, closerSession);
					if(replacingSessions.size() == 0)
					{
						_replacing_to_repalced.remove(closerSession);
						killSessions.add(closerSession);
						break;
					}
					else
						break;
				}
				else if(closerSessionConnectionType == SessionConnectionType.S_CON_T_REPLACING_BEINGREPLACED)
				{
					Set<ISession> replacingSessions = getReplacingSessionsForBeingReplacedSession(_replacing_to_repalced, closerSession);
					if(replacingSessions.size() == 0)
					{
						_replacing_to_repalced.remove(closerSession);
						killSessions.add(closerSession);
					}
				}
			}
			
			Iterator<ISession> killIt = killSessions.iterator();
			while(killIt.hasNext())
			{
				if(Broker.LOG_VIOLATIONS && false) {
					ISession killSession = killIt.next();
					setSessionsConnectionType(killSession, SessionConnectionType.S_CON_T_DELTA_VIOLATED);
					_replacing_to_repalced.remove(killSession);
				} else {
					ISession killSession = killIt.next();
					mqRemovedSessions.add(killSession);
					setSessionsConnectionType(killSession, SessionConnectionType.S_CON_T_FAILED);
					ISession replacingSessionSBeingReplacedSession = _replacing_to_repalced.remove(killSession);
					testBeingReplacedSession(replacingSessionSBeingReplacedSession);
					removeFromAllSessionsPrivately(killSession.getRemoteAddress());
				}
			}
			_mq.replaceSessions(mqRemovedSessions, mqAddedSessions);
			return;
		}
		
		default:
			break;
		}
	}
	
	protected void failedSession(ISession fSession) {
		writeDownSummary("Before Failed: ", fSession);
		
		Set<ISession> fartherSessionsWithImmediateFailedConnections = new HashSet<ISession>();
		
		synchronized(_sessionsLock) {
			InetSocketAddress fRemote = fSession.getRemoteAddress();
			if(fRemote == null)
				return;
			
			SessionConnectionType fSessionConnectionType = fSession.getSessionConnectionType();
			
			if(fSessionConnectionType == SessionConnectionType.S_CON_T_DROPPED)
				return;
			
			failedPrivately(fSession, fartherSessionsWithImmediateFailedConnections);
			dumpAllSessions();
			
			performConnectionManagerTypeSpecificPostFailed(fSession);
		}
		
		Iterator<ISession> fartherSessionsWithImmediateFailedConnectionsIt = fartherSessionsWithImmediateFailedConnections.iterator();
		while(fartherSessionsWithImmediateFailedConnectionsIt.hasNext()) {
			ISession fartherSessionWithImmediateFailedConnection = fartherSessionsWithImmediateFailedConnectionsIt.next();
			conInfoUpdated(fartherSessionWithImmediateFailedConnection.getConInfoNL());
		}
		
		writeDownSummary("After Failed: ", fSession);
	}
	
	@Override
	public final void registerSession(ISession newSession) {
		writeDownSummary("Before register: ", newSession);

		if(Broker.CORRELATE)
			TrafficCorrelator.getInstance().sessionRegistered(newSession, isMP());

		synchronized(_sessionsLock) {
			registerSessionPrivately(newSession);
		}
		
		writeDownSummary("After register: ", newSession);
	}
	
	protected boolean isMP() {
		return false;
	}
	
	protected void registerSessionPrivately(ISession newSession) {
		InetSocketAddress remote = newSession.getRemoteAddress();
		//TODO: exception below should be removed: although we require registerSession be called on only
		//		pubSubEnabled(), sessionType may change by the time we get here, since we release lock on session.
		if(!newSession.isRealSessionTypePubSubEnabled()) {
			LoggerFactory.getLogger().warn(this, "NewSession '" + remote + "' being registered is not pubSubEnabled");
		}
		
		boolean shouldContinue = performConnectionManagerTypeSpecificPreRegistration(newSession);
		if(!shouldContinue)
			return;
		
		if(registerSessionGenerally(newSession) != newSession)
			return;

		dumpAllSessions();
		
		performConnectionManagerTypeSpecificPostRegistration(newSession);
	}
	
	private boolean allNodesDeadInBetween(IOverlayManager overlayManager, InetSocketAddress remote) {
		Path<INode> remotePath = overlayManager.getPathFrom(remote);
		
		for(int i=1 ; i<remotePath.getLength() ; i++) {
			InetSocketAddress closerRemote = remotePath.get(i).getAddress();
			ISession closerSession = getSessionPrivately(closerRemote);
			if(closerSession == null)
				continue;
			if(closerSession.isLocallyActive())
				return false;
		}
		
		return true;
	}
	
	protected ISession surviveASessionPrivately(ISession oldSession, ISession newSession) {
		LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_CON_MAN, "There has been an old session: " + oldSession);
		if(newSession == null)
			throw new IllegalArgumentException(oldSession + " vs. " + newSession);
		

		InetSocketAddress remote = newSession.getRemoteAddress();
		if(oldSession == null || oldSession == newSession) {
			addSessionPrivately(remote, newSession);
			return newSession;
		}
			
		if(remote == null || !remote.equals(oldSession.getRemoteAddress()))
			throw new IllegalArgumentException("Remotes do not match: " + remote + " vs. " + oldSession.getRemoteAddress());
			 
		SessionConnectionType oldSessionConnectionType = oldSession.getSessionConnectionType();
		if(oldSessionConnectionType == SessionConnectionType.S_CON_T_BEINGREPLACED ||
			 oldSessionConnectionType == SessionConnectionType.S_CON_T_FAILED || 
			 oldSessionConnectionType == SessionConnectionType.S_CON_T_DROPPED) {
			addSessionPrivately(remote, newSession);
			return newSession;
		}

		// We have to drop either of the sessions, 
		// `survivedSession' will hold the survived one.
		ISession survivedSession = null;
		boolean oldOutgoing = oldSession.isOutgoing();
		boolean newOutgoing = newSession.isOutgoing();
		boolean outgoingValid = isOutgoingValid(_localAddress, remote);
		
		if(oldOutgoing && newOutgoing)
		{
			throw new IllegalStateException("Cannot have two outgoing sessions to '" + remote + "'.");
		}
		
		else if(oldOutgoing && !newOutgoing)
		{
			if(outgoingValid)
			{
				// drop newSession
				survivedSession = oldSession;
				setSessionsConnectionType(newSession, SessionConnectionType.S_CON_T_DROPPED);
				_sessionManager.dropSession(newSession, true);
			}
			else
			{
				// drop oldSession
				survivedSession = newSession;
				replaceSessionInReplacinBeingReplaced(_replacing_to_repalced, oldSession, newSession);
				setSessionsConnectionType(oldSession, SessionConnectionType.S_CON_T_DROPPED);
				_sessionManager.dropSession(oldSession, true);
			}
		}
		
		else if(!oldOutgoing && newOutgoing)
		{
			if(outgoingValid)
			{
				// drop oldSession
				survivedSession = newSession;
				replaceSessionInReplacinBeingReplaced(_replacing_to_repalced, oldSession, newSession);
				setSessionsConnectionType(oldSession, SessionConnectionType.S_CON_T_DROPPED);
				_sessionManager.dropSession(oldSession, true);
			}
			else
			{
				// drop newSession
				survivedSession = oldSession;
				setSessionsConnectionType(newSession, SessionConnectionType.S_CON_T_DROPPED);
				_sessionManager.dropSession(newSession, true);
			}
		}
		
		else if(!oldOutgoing && !newOutgoing)
		{
			// drop oldSession
			survivedSession = newSession;
			replaceSessionInReplacinBeingReplaced(_replacing_to_repalced, oldSession, survivedSession);
			setSessionsConnectionType(oldSession, SessionConnectionType.S_CON_T_DROPPED);
			_sessionManager.dropSession(oldSession, true);
		}
		
		addSessionPrivately(remote, survivedSession);
		return survivedSession;
	}
	
	protected ISession registerSessionGenerally(ISession newSession) {
		ISession survivedSession, oldSession;
		InetSocketAddress remote = newSession.getRemoteAddress();

		if(remote == null)
			throw new IllegalStateException("Remote cannot be 'null' when the session is being registered.");
		
		// Since we do not synchronize on the newSession, it is possible that its _type chances.
		// But it only can chance to ST_END, in which case, 'failed()' will be called (synchronized with this method).
		if(!newSession.isRealSessionTypePubSubEnabled() && !newSession.isEnded())
			throw new IllegalArgumentException("NewSession being registered '" + remote + "' is not PubSub enabled, '" + newSession.getSessionType() + "'."); 

		// Checking for double session
		oldSession = getSessionPrivately(remote);
		boolean wasInMQ = false;
		if(oldSession != null)
			wasInMQ = oldSession.isInMQ();
		survivedSession  = surviveASessionPrivately(oldSession, newSession);
		if(getSessionPrivately(remote) != survivedSession)
			throw new IllegalStateException("Survived Session '" + survivedSession + "'has not been registered in _allSessoins: " + _allSessions);
		
		System.out.println("SURVIVED: " + survivedSession + " \t" + (survivedSession==oldSession) + Writers.write(_mq.getPSSessionsAsISessions().toArray(new ISession[0])));
		Set<ISession> mqAddedSessions = new HashSet<ISession>();
		Set<ISession> mqRemovedSessions = new HashSet<ISession>();
		if(survivedSession != oldSession) {
//			addSessionPrivately(remote, survivedSession);

			if(oldSession != null && wasInMQ)
				replaceOldSessionWithNewSession(oldSession, survivedSession, mqAddedSessions, mqRemovedSessions);
		}
		
		checkCloserSessionsWhileRegister(survivedSession, mqAddedSessions, mqRemovedSessions);
		checkFartherSessionsWhileRegister(survivedSession, mqAddedSessions, mqRemovedSessions);
		_mq.replaceSessions(mqRemovedSessions, mqAddedSessions);
		
		if(getSessionPrivately(remote) != survivedSession)
			throw new IllegalStateException("Survived Session '" + remote + "'has not been registered in _allSessoins.");

		return survivedSession;
	}
	
	protected boolean checkCloserSessionsWhileRegister(ISession survivedSession, Set<ISession> mqAddedSessions, Set<ISession> mqRemovedSessions) {
		InetSocketAddress remote = survivedSession.getRemoteAddress();
		IOverlayManager overlayManager = getOverlayManager();
		
		Path<INode> pathFromRemoteSession = overlayManager.getPathFrom(remote);
		if(pathFromRemoteSession == null)
		{
			// This can only become a S_CON_T_UNJOINED.
			setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_UNJOINED);
//			addSession(remote, survivedSession);
			mqAddedSessions.add(survivedSession);
			return true;
		}

		InetSocketAddress [] pathAddresses = pathFromRemoteSession.getAddresses();
		if(pathAddresses.length == 1)
		{
			// No intermediate node; but still may be 'REPLACING' the local node.
			ISession possibleLocal = _replacing_to_repalced.remove(survivedSession);
			if(possibleLocal != null && possibleLocal.isLocal()) // ISessionLocal.class.isInstance(possibleLocal))
			{
				Set<ISession> replacingSessions = getReplacingSessionsForBeingReplacedSession(_replacing_to_repalced, possibleLocal);
				if(replacingSessions.size() == 0) {
					mqRemovedSessions.add(possibleLocal);
					removeFromAllSessionsPrivately(possibleLocal.getRemoteAddress());
				}
			}
			setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_ACTIVE);
			mqAddedSessions.add(survivedSession);
			return true;
		}
		
		if(!remote.equals(pathAddresses[0]))
			throw new IllegalStateException("Path from '" + remote + "' must have the address as index 0.");

		boolean allNodesDeadInBetween = allNodesDeadInBetween(overlayManager, remote); //overlayManager.allNodesDeadInBetween(remote);
		
		if(! allNodesDeadInBetween) {
			setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_INACTIVE);
			return false;
		}
		
		setSessionsConnectionType(survivedSession, SessionConnectionType.S_CON_T_ACTIVE);
		mqAddedSessions.add(survivedSession);
		_replacing_to_repalced.remove(survivedSession);
		
		// check to remove replacing relationships
		for(int i=1 ; i<pathAddresses.length ; i++)
		{
			InetSocketAddress closerNeighbor = pathAddresses[i];
			ISession closerSession = getSessionPrivately(closerNeighbor);
			if(closerSession == null)
				continue;
			SessionConnectionType closerSessionConnectionType = closerSession.getSessionConnectionType();
			if(closerSessionConnectionType == SessionConnectionType.S_CON_T_BEINGREPLACED ||
				 closerSessionConnectionType == SessionConnectionType.S_CON_T_REPLACING_BEINGREPLACED)
			{
				Set<ISession> replacings = getReplacingSessionsForBeingReplacedSession(_replacing_to_repalced, closerSession);
				if(replacings.size() == 0)
				{
					if(closerSessionConnectionType == SessionConnectionType.S_CON_T_REPLACING_BEINGREPLACED) {
						ISession possibleLocal = _replacing_to_repalced.remove(closerSession);
						if(possibleLocal != null && possibleLocal.isLocal()) // ISessionLocal.class.isInstance(possibleLocal))
						{
							Set<ISession> replacingSessions = getReplacingSessionsForBeingReplacedSession(_replacing_to_repalced, possibleLocal);
							if(replacingSessions.size() == 0) {
								mqRemovedSessions.add(possibleLocal);
								removeFromAllSessionsPrivately(_localAddress);
							}
						}
					}
					removeFromAllSessionsPrivately(closerNeighbor);
					mqRemovedSessions.add(closerSession);
					setSessionsConnectionType(closerSession, SessionConnectionType.S_CON_T_FAILED);
				}
			}
		}
		return true;
	}
	
	protected void checkFartherSessionsWhileRegister(ISession survivedSession, Set<ISession> mqAddedSessions, Set<ISession> mqRemovedSessions) {
		if(survivedSession.getSessionConnectionType() != SessionConnectionType.S_CON_T_ACTIVE)
			return;
			
		InetSocketAddress remote = survivedSession.getRemoteAddress();
		IOverlayManager overlayManager = getOverlayManager();
		List<InetSocketAddress> fartherAddresses = overlayManager.getFartherNeighborsOrderedList(remote);			
		
		Iterator<InetSocketAddress> fartherIt = fartherAddresses.iterator();
		while(fartherIt.hasNext())
		{
			InetSocketAddress fartherAddress = fartherIt.next();
			ISession fartherSession = null;

			fartherSession = getSessionPrivately(fartherAddress);
			
			if(fartherSession == null)
				continue;
			
			if(fartherAddress.equals(survivedSession.getRemoteAddress()))
				continue;
			
			SessionConnectionType fartherSessionConnectionType = fartherSession.getSessionConnectionType();
			switch( fartherSessionConnectionType) {
			case S_CON_T_ACTIVE:
				mqRemovedSessions.add(fartherSession);
				setSessionsConnectionType(fartherSession, SessionConnectionType.S_CON_T_INACTIVE);
				break;
			
			case S_CON_T_DROPPED:
				throw new IllegalStateException("Cannot have a DROPPED session '" + fartherAddress + "' in the _allSession set.");
			
			case S_CON_T_INACTIVE:
				break;
			
			case S_CON_T_REPLACING:
				setSessionsConnectionType(fartherSession, SessionConnectionType.S_CON_T_USELESS);
				if(_replacing_to_repalced.remove(fartherSession) == null) //TODO: changed `!=' to `=='
					throw new IllegalStateException("S_CON_T_REPLACING session '" + fartherAddress + "' is not associated with a BEING_REPLACED session in _replacing_to_replaced mapping.");
				break;

			case S_CON_T_DELTA_VIOLATED:
			case S_CON_T_BEINGREPLACED:
				setSessionsConnectionType(fartherSession, SessionConnectionType.S_CON_T_FAILED);
				removeFromAllSessionsPrivately(fartherAddress);	
				mqRemovedSessions.add(fartherSession);
				break;
			
			case S_CON_T_REPLACING_BEINGREPLACED:
				setSessionsConnectionType(fartherSession, SessionConnectionType.S_CON_T_FAILED);
				removeFromAllSessionsPrivately(fartherAddress);	
				mqRemovedSessions.add(fartherSession);
				break;
			
			case S_CON_T_UNJOINED:
				throw new IllegalStateException("An UNJOINED session '" + fartherAddress + "' cannot be farther of any session.");

			default:
				break;
			}
		}
	}

	private static Set<ISession> getReplacingSessionsForBeingReplacedSession(Map<ISession, ISession> replacingToReplaced, ISession beingReplacedSession) {
		SessionConnectionType beingReplacedSessionConnectionType = beingReplacedSession.getSessionConnectionType();
		if(beingReplacedSessionConnectionType != SessionConnectionType.S_CON_T_BEINGREPLACED &&
			 beingReplacedSessionConnectionType != SessionConnectionType.S_CON_T_REPLACING_BEINGREPLACED)
			throw new IllegalStateException("Replacing session '" + beingReplacedSession.getRemoteAddress() + "' is not S_CON_T_REPLACING; it is " + beingReplacedSessionConnectionType);
		
		Set<ISession> replacingSessions = new HashSet<ISession>();
		
		Set<Entry<ISession, ISession>> set = replacingToReplaced.entrySet();
		Iterator<Entry<ISession, ISession>> it = set.iterator();
		while(it.hasNext())
		{
			Entry<ISession, ISession> entry = it.next();
			ISession replacingSession = entry.getKey();
			ISession replacedSession = entry.getValue();
			
			if(replacedSession == beingReplacedSession)
			{
				if(replacingSession.getSessionConnectionType() != SessionConnectionType.S_CON_T_REPLACING && 
					 replacingSession.getSessionConnectionType() != SessionConnectionType.S_CON_T_REPLACING_BEINGREPLACED)
					throw new IllegalStateException("Beingreplaced session's '" + beingReplacedSession + "' replacing session '" + replacingSession + "' is not S_CON_T_REPLACING. It is " + replacingSession.getSessionConnectionType() + ".");
				
				replacingSessions.add(replacingSession);
			}
		}
		
		return replacingSessions;
	}
	
	protected Set<ISession> getSessionsOfSessionConnectionType(SessionConnectionType sessionConnectionType) {
		Set<ISession> typeSessions = new HashSet<ISession>();
		
		synchronized(_sessionsLock) {
			ISession[] sessions = getAllSessionsPrivately();
			for(int i=0 ; i<sessions.length ; i++)
				if(sessions[i].getSessionConnectionType() == sessionConnectionType)
					typeSessions.add(sessions[i]);
		}
		
		return typeSessions;
	}
	
	private void testBeingReplacedSession(ISession beingReplacedSession) {
		if(beingReplacedSession == null)
			return;
		
		ISession replacingABeingReplacedSessionAsWell = null;
		
		SessionConnectionType sessionConnectionType = beingReplacedSession.getSessionConnectionType();
		if(sessionConnectionType == SessionConnectionType.S_CON_T_REPLACING_BEINGREPLACED)
			replacingABeingReplacedSessionAsWell = _replacing_to_repalced.get(beingReplacedSession);
		else if(sessionConnectionType != SessionConnectionType.S_CON_T_BEINGREPLACED)
			throw new IllegalStateException("beingReplacedSession is neither BEINGREPLACED/nor REPLACING_BEINGREPLACED, it is '" + sessionConnectionType);
	
		Set<ISession> replacingSessions = getReplacingSessionsForBeingReplacedSession(_replacing_to_repalced, beingReplacedSession);
		
		if(replacingSessions.size() == 0 && isPresentInAllSessionsPrivately(beingReplacedSession.getRemoteAddress())) {
			ISession registeredbeingReplaced = removeFromAllSessionsPrivately(beingReplacedSession.getRemoteAddress());
			if(registeredbeingReplaced != beingReplacedSession)
				throw new IllegalStateException("BeingregisteredSession '" + beingReplacedSession.getRemoteAddress() + "' does not match the one in allSessions.");
			
			Set<ISession> mqRemovedSet = new HashSet<ISession>();
			mqRemovedSet.add(beingReplacedSession);
			removeFromAllSessionsPrivately(beingReplacedSession.getRemoteAddress());
			setSessionsConnectionType(beingReplacedSession, SessionConnectionType.S_CON_T_FAILED);
			_mq.replaceSessions(mqRemovedSet, null);
			
			_replacing_to_repalced.remove(beingReplacedSession);
			testBeingReplacedSession(replacingABeingReplacedSessionAsWell);
		}
	}
	
	protected void replaceSessionInReplacinBeingReplaced(Map<ISession, ISession> replacingToReplaced, ISession oldSession, ISession newSession) {
		SessionConnectionType oldSessionConnectionType = oldSession.getSessionConnectionType();
		if(oldSessionConnectionType == SessionConnectionType.S_CON_T_BEINGREPLACED ||
			 oldSessionConnectionType == SessionConnectionType.S_CON_T_REPLACING_BEINGREPLACED)
		{
			Set<ISession> replacingSessions = getReplacingSessionsForBeingReplacedSession(replacingToReplaced, oldSession);
			Iterator<ISession> it = replacingSessions.iterator();
			while(it.hasNext()) {
				ISession replacingSession = it.next();
				ISession replacedSession = replacingToReplaced.remove(replacingSession);
				if(replacedSession != oldSession)
					throw new IllegalStateException("BeingReplaced Session does not match oldSession '" + oldSession.getRemoteAddress() + "'.");
				replacingToReplaced.put(replacingSession, newSession);
			}
		}
		
		if(oldSessionConnectionType == SessionConnectionType.S_CON_T_REPLACING ||
			 oldSessionConnectionType == SessionConnectionType.S_CON_T_REPLACING_BEINGREPLACED)
		{
			ISession replacedSession = replacingToReplaced.remove(oldSession);
			replacingToReplaced.put(newSession, replacedSession);
		}
		setSessionsConnectionType(newSession, oldSessionConnectionType);
	}
	
	// Replaces all references to two ISession instances corresponding to the same remote
	// So that _allSessions, _replacing_to_replaced do not have any reference to oldSession.
	protected void replaceOldSessionWithNewSession(ISession oldSession, ISession newSession, Set<ISession> mqAddedSessions, Set<ISession> mqRemovedSessions) {
		if(oldSession == newSession)
			return;

		LoggerFactory.getLogger().debug(this, "Replacing old session: " + oldSession + " with new session: " + newSession);
		InetSocketAddress oldRemote = oldSession.getRemoteAddress();
		InetSocketAddress newRemote = newSession.getRemoteAddress();
		
		if(!newRemote.equals(oldRemote))
			throw new IllegalArgumentException("Old remote and new remote do not match.");
		
		InetSocketAddress remote = oldRemote;
		removeFromAllSessionsPrivately(remote);
		addSessionPrivately(remote, newSession);
		
		if(oldSession.isLocallyActive())
		{
			mqAddedSessions.add(newSession);
			mqRemovedSessions.add(oldSession);
		}
	}
	
	protected void writeDownSummary(String str1, ISession session) {
		if(Broker.RELEASE)
			return;
		
		LoggerFactory.getLogger().info(this, "### " + str1 + session);
		
		
		LoggerFactory.getLogger().info(this, "### " + " all Sessions: [" + Writers.write(getAllSessions()) + "]");

		String replacingStr = "";
		Iterator<Entry<ISession,ISession>> replacingIt = _replacing_to_repalced.entrySet().iterator();
		while(replacingIt.hasNext()) {
			Entry<ISession, ISession> entry = replacingIt.next();	
			replacingStr += (entry.getKey().getRemoteAddress() + "==>" + 
							 entry.getValue().getRemoteAddress());
		}
		LoggerFactory.getLogger().info(this, "### " + " Replaings: [" + replacingStr + "]");
	}
	
	@Override
	public ISession[] getAllLocallyActiveSessions() {
		synchronized(_sessionsLock) {
			return getAllLocallyActiveSessionsPrivately();
		}
	}

	protected ISession[] getAllLocallyActiveSessionsPrivately() {
		List<ISession> retSessions = new LinkedList<ISession>();
		ISession[] allSessions = getAllSessionsPrivately();
		for(int i=0 ; i<allSessions.length ; i++)
			if(allSessions[i].isLocallyActive())
				retSessions.add(allSessions[i]);
		
		return retSessions.toArray(new ISession[0]);
	}

	protected ISession[] getAllInMQSessionsPrivately() {
		List<ISession> retSessions = new LinkedList<ISession>();
		ISession[] allSessions = getAllSessionsPrivately();
		for(int i=0 ; i<allSessions.length ; i++)
			if(allSessions[i].isInMQ())
				retSessions.add(allSessions[i]);
		
		return retSessions.toArray(new ISession[0]);
	}

//	protected ISession[] getAllLocallyActiveSessionsCanProcessSessionConnectionTypePrivately() {
//		List<ISession> retSessions = new LinkedList<ISession>();
//		ISession[] allSessions = getAllSessionsPrivately();
//		for(int i=0 ; i<allSessions.length ; i++)
//			if(allSessions[i].isLocallyActive())
//				if(canProcessSessionConnectionType(allSessions[i].getSessionConnectionType()))
//					retSessions.add(allSessions[i]);
//		
//		return retSessions.toArray(new ISession[0]);
//	}
	
	private void addAllSessionsForFirstRevision() {
		synchronized(_sessionsLock) {
			ISession[] sessions = getAllSessionsPrivately();
			for(int i=0 ; i<sessions.length ; i++) {
				IConInfoNonListening<?> conInfoNL = sessions[i].getConInfoNL();
				if(conInfoNL != null) {
					conInfoGotEmptySpace(conInfoNL);
					conInfoGotFirstDataItem(conInfoNL);
				}
			}
			
			_mq.proceedAll();
		}
	}
	
	// Handler Functions
	@Override
	protected void handleConnectionEvent_writeConnection(ConnectionEvent_writeConnection connEvent) {
		IConInfoNonListening<?> writeConInfoNL = connEvent._conInfoNL;
		ISession writeSession = writeConInfoNL.getSession();
		_sessionManager.resend(writeSession);
		
		if(writeSession.isRealSessionTypePubSubEnabled())
			_mq.proceed(writeSession.getRemoteAddress());
	}

	@Override
	protected void handleConnectionEvent_NewLocalOutgoing(ConnectionEvent_NewLocalOutgoing connEvent) {
		TM_TMListener tmConfListener = connEvent._tmTmListener;
		TMulticast tm = tmConfListener._tm;
		
		_mq.addNewMessage(tm, tmConfListener._listener);
	}
	
	
	@Override
	protected void handleConnectionEvent_sendDack(ConnectionEvent_sendDack connEvent) {
		getOverlayManager().updateAllNodesCache(_mq.getArrivedTimestamp(), _mq.getDiscardedTimestamp());
		
		ISession[] sessions = getAllSessions();
		for(int i=0 ; i<sessions.length ; i++)
			sendDackOnSession(sessions[i]);
	}
	
	@Override
	protected void handleConnectionEvent_purgeMQ(ConnectionEvent_purgeMQ connEvent) {
		long purgeStartTime = SystemTime.currentTimeMillis();
		MQCleanupInfo purgeInfo = _mq.purge();
		LoggerFactory.getLogger().debug(this, "Purging MQ took " + (SystemTime.currentTimeMillis() - purgeStartTime) + " ms to discard " + purgeInfo + ".");
	}
	
	@Override
	public void enableDisableMQProceed(boolean enable) {
		if(enable)
			_mq.enableProceed();
		else
			_mq.disableProceed();
	}
	
	protected boolean isPresentInAllSessionsPrivately(InetSocketAddress remote) {
		if(remote==null)
			return false;
		return _allSessions.containsKey(remote);
	}
	
	protected ISession removeFromAllSessionsPrivately(InetSocketAddress remote) {
		ISession session = _allSessions.remove(remote);
		if(!Broker.RELEASE) 
			LoggerFactory.getLogger().info(this, "Removing Session Privately: " + session);
		
		return session;
	}

	protected List<ISession> getAllPrimaryActiveSessionsPrivately() {
		List<ISession> allPrimaryActiveSessions = new LinkedList<ISession>();
		ISession[] allSessions = getAllSessionsPrivately();
		for(int i=0 ; i<allSessions.length ; i++)
			if(allSessions[i].isPrimaryActive())
				allPrimaryActiveSessions.add(allSessions[i]);
		
		return allPrimaryActiveSessions;
	}

	@Override
	public void setLastReceivedSequence2(ISession sesion, Sequence seq, boolean doProceed, boolean initializeMQNode) {
		boolean isProceedEnabled = _mq.isProceedEnabled();
		if(!doProceed)
			_mq.disableProceed();
		
		_mq.resetSession(sesion, seq, initializeMQNode);
		
		if(!doProceed || isProceedEnabled)
			_mq.enableProceed();
	}
	
	@Override
	protected void handleConnectionEvent_FastTCommandProcessing(ConnectionEvent_FastTCommandProcessing connEvent) {
		super.handleConnectionEvent_FastTCommandProcessing(connEvent);

		TCommand tCommand = connEvent._tCommand;
		String comment = tCommand.getComment();
		_mq.dumpAll(comment);
		
		if(Broker.CORRELATE) {
			TrafficCorrelationDumpSpecifier dumpSpecifier = tCommand.getTrafficCorrelationDumpSpecifier();
			TrafficCorrelator.getInstance().dump(dumpSpecifier, true);
		}
	}
	
	@Override
	public boolean loadPrepareSubscription(SubscriptionEntry subEntry) {
		if(subEntry.isLocal())
			_subscriptionManager.addNewLocalSubscriptionEntry((LocalSubscriptionEntry)subEntry);
		else
			_subscriptionManager.addNewSubscriptionEntry(subEntry);
		return true;
	}
}