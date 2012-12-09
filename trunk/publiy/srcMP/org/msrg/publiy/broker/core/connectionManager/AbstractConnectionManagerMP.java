package org.msrg.publiy.broker.core.connectionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueueMP;
import org.msrg.publiy.pubsub.core.multipath.WorkingManagersBundle;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.IWorkingOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_UnSubscribe;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.subscriptionmanager.LocalSubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.multipath.IWorkingSubscriptionManager;
import org.msrg.publiy.pubsub.multipath.loadweights.LoadWeightRepository;
import org.msrg.publiy.pubsub.multipath.loadweights.LoadWeightSendTask;
import org.msrg.publiy.utils.HistoryType;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Utility;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.RTTLogger;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;

import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.communication.core.packet.types.TLoadWeight;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionMP;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.ISessionManagerMP;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.communication.core.sessions.SessionConsideration;
import org.msrg.publiy.communication.core.sessions.SessionConsiderationTransition;
import org.msrg.publiy.communication.core.sessions.SessionConsiderationTransitionType;
import org.msrg.publiy.communication.core.sessions.statistics.ISessionGainStatistics;
import org.msrg.publiy.communication.core.sessions.statistics.ISessionOutPublicationStatistics;
import org.msrg.publiy.communication.core.sessions.statistics.ISessionStatisticsSortedBundle;
import org.msrg.publiy.component.ComponentStatus;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public abstract class AbstractConnectionManagerMP extends ConnectionManagerPS implements IConnectionManagerMP {

	public static final int RANKS_HISTORY = 10;
	public static final double SESSION_NORMALIZATION_CONSTANT = 4;
	
	private WorkingManagersBundle _workingManagersBundle;
	private final LoadWeightRepository _loadWeightRepo;
	
	private final ISessionManagerMP _sessionManagerMP;
	private final SessionsConsiderationList _considerationList;
	
	protected AbstractConnectionManagerMP(
			String connectionManagerName,
			ConnectionManagerTypes type,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue mq,
			TRecovery_Join[] trjs,
			LoadWeightRepository loadWeightRepo) throws IOException{
		super(connectionManagerName, type, broker, brokerShadow, overlayManager, subscriptionManager, mq, trjs);
		
		_considerationList = new SessionsConsiderationList(brokerShadow.getTransitionsLogger());
		_loadWeightRepo = loadWeightRepo;
		_sessionManagerMP = (ISessionManagerMP) _sessionManager;
	}

	protected AbstractConnectionManagerMP(ConnectionManagerTypes type, ConnectionManagerRecovery conManRecovery) {
		super(conManRecovery, "ConMan-MP", type);
		updateAllSessionsDistance();
		updateCriticalSessions();
		
		_loadWeightRepo = ((_brokerShadow.getPublicationForwardingStrategy()._isLoadAware)?_brokerShadow.getLoadWeightRepository():null);
		_considerationList = new SessionsConsiderationList(_brokerShadow.getTransitionsLogger());
		_sessionManagerMP = (ISessionManagerMP) _sessionManager;
	}
	
	protected AbstractConnectionManagerMP(ConnectionManagerTypes type, ConnectionManagerJoin conManJoin) {
		super(conManJoin, "ConMan-MP", type);
		updateAllSessionsDistance();
		updateCriticalSessions();
		_loadWeightRepo = ((_brokerShadow.getPublicationForwardingStrategy()._isLoadAware)?_brokerShadow.getLoadWeightRepository():null);
		_considerationList = new SessionsConsiderationList(_brokerShadow.getTransitionsLogger());
		_sessionManagerMP = (ISessionManagerMP) _sessionManager;
	}

	protected void updateAllSessionsDistance() {
		ISession[] allSessions = getAllSessionsPrivately();
		for (int i=0 ; i<allSessions.length ; i++) {
			if (allSessions[i].isLocal())
				continue;
			boolean validDistance = updateDistance((ISessionMP)allSessions[i]);
			if (!validDistance)
				LoggerFactory.getLogger().warn(this, "Session's distncce is invalid: " + allSessions[i]);
		}
	}

	@Override
	public IWorkingOverlayManager getWorkingOverlayManager() {
		return _workingManagersBundle._workingOverlayManager;
	}

	@Override
	public IWorkingSubscriptionManager getWorkingSubscriptionManager() {
		return _workingManagersBundle._workingSubscriptionManager;
	}
	
	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_CON_MAN_MP;
	}	

	@Override
	protected void connectionManagerJustStarted() {
		super.connectionManagerJustStarted();

		scheduleNextTPing();
		PubForwardingStrategy strategy = _brokerShadow.getPublicationForwardingStrategy();
		if(strategy._isLoadAware)
			scheduleNextLoadWeightSend();
		
		switch(strategy) {
		case PUB_FORWARDING_STRATEGY_1:
			break;
			
		case PUB_FORWARDING_STRATEGY_0:
		case PUB_FORWARDING_STRATEGY_2:
		case PUB_FORWARDING_STRATEGY_3:
			scheduleNextCandidatesEvent();
			break;
			
		case PUB_FORWARDING_STRATEGY_4:
			scheduleNextCandidatesEvent();
			break;
			
		default:
			throw new UnsupportedOperationException("" + strategy);
		}
	}
	
	private boolean _workingBundleUpdated = false;
	@Override
	protected void handleMulticastMessage(ISession session, TMulticast tm) {
		InetSocketAddress remote = session==null?null:session.getRemoteAddress();
		super.handleMulticastMessage(session, tm);
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION:
		case T_MULTICAST_PUBLICATION_MP:
			_brokerShadow.getMessageProfilerLogger().logMessage(HistoryType.HIST_T_PUB_IN_BUTLAST, remote, tm.getContentSize());
			if (!_workingBundleUpdated) {
				_workingBundleUpdated = true;
				updateCriticalSessions();
			}
			return;
			
		case T_MULTICAST_SUBSCRIPTION:
			_brokerShadow.getMessageProfilerLogger().logMessage(HistoryType.HIST_T_SUB_IN_BUTLAST, remote, tm.getContentSize());
			return;
			
		case T_MULTICAST_CONF:
		case T_MULTICAST_DEPART:
		case T_MULTICAST_JOIN:
		case T_MULTICAST_UNKNOWN:
		case T_MULTICAST_UNSUBSCRIPTION:
			return;
			
		default:
			break;
		}
	}
	
	@Override
	public ISession getDefaultOutgoingSession(InetSocketAddress remote) {
		return ISessionManagerMP.getPubSubMPSession(_brokerShadow, remote);
	}
	
	public ISessionMP getCandidateSession(InetSocketAddress remote) {
		return ISessionManagerMP.getCandidatePubSubMPSession(_brokerShadow, remote);
	}

	protected void handleConnectionEvent_candidates(ConnectionEvent_Candidates connEvent) {
		Set<ISession> failedSessions = new HashSet<ISession>();
		synchronized(_sessionsLock)
		{
			RTTLogger rttLogger = _brokerShadow.getRTTLogger();
			ISessionStatisticsSortedBundle statisticsBundle = updateSessionRanksPrivately(rttLogger);
			ISessionGainStatistics[] sortedSessionsGain = statisticsBundle.getSessionsSortedByGain();
			ISessionOutPublicationStatistics[] sortedSessionsOutPublication = statisticsBundle.getSessionsSortedByOutPublications();
			
			Set<ISession> mqAddedSessions=new HashSet<ISession>();
			Set<ISession> mqRemovedSessions=new HashSet<ISession>();
			
			boolean criticalUpdate = updateSessionsArray(Broker.FANOUT, Broker.CANDIDATES, 
										_considerationList, sortedSessionsGain, sortedSessionsOutPublication,
										mqAddedSessions, mqRemovedSessions, failedSessions);
			if (criticalUpdate) {
				updateCriticalSessions();
				dumpAllSessions();
				_mq.replaceSessions(mqRemovedSessions, mqAddedSessions);
			}
		}
		
		Iterator<ISession> fartherSessionsWithImmediateFailedConnectionsIt = failedSessions.iterator();
		while (fartherSessionsWithImmediateFailedConnectionsIt.hasNext()) {
			ISession fartherSessionWithImmediateFailedConnection = fartherSessionsWithImmediateFailedConnectionsIt.next();
			conInfoUpdated(fartherSessionWithImmediateFailedConnection.getConInfoNL());
		}
	}

	protected boolean updateSessionsArray(final int maxFanout, final int maxCandidate, final SessionsConsiderationList considerationList, 
			final ISessionGainStatistics[] sortedSessionsGain, final ISessionOutPublicationStatistics[] sortedSessionsOutPublication,
			final Set<ISession> mqAddedSessions, final Set<ISession> mqRemovedSessions, final Set<ISession> failedSessions)
	{
		int primaryActiveCount = 0;
		for (int i=0 ; i<sortedSessionsGain.length ; i++)
			if (sortedSessionsGain[i]._session.isPrimaryActive() || sortedSessionsGain[i]._session.isBeingConnected())
				primaryActiveCount++;
		
		boolean performCriticalUpdate = false;
		final long now = SystemTime.currentTimeMillis();
		int usedFanoutCounter=primaryActiveCount, usedCandidateCounter=0;
		for (int i=0 ; i<sortedSessionsGain.length ; i++)
		{
			final ISessionGainStatistics sessionGain = sortedSessionsGain[i];
			final ISessionMP session = sessionGain._session;
			final InetSocketAddress remote = session.getRemoteAddress();
			final SessionConsideration existingConsideration = considerationList.getSessionConsideration(remote);
			
			if (session.isPrimaryActive() || !session.isLocallyActive())
				continue;

			if (existingConsideration == null)
				throw new NullPointerException(session.toString() + " _ " + considerationList);
				
			ConsiderationStatus considerationStatus = existingConsideration.getConsiderationStatus();
			final boolean isValid = existingConsideration.isValid(now);
			final boolean fast = _sessionManagerMP.isFast(remote);

			switch(considerationStatus) {
			case S_CONSIDERATION_INITIATED:
				if (isValid)
				{
					if (usedCandidateCounter < maxCandidate)
						usedCandidateCounter++;
					else
					{
						// remove candidate session
						mqRemovedSessions.add(session);
						ConsiderationStatus newConsiderationStatus = fast?ConsiderationStatus.S_CONSIDERATION_FAST_LOW:ConsiderationStatus.S_CONSIDERATION_SLOW_LOW;
						SessionConsideration newConsideration = new SessionConsideration(remote, newConsiderationStatus);
						SessionConsiderationTransition transition = 
							new SessionConsiderationTransition("Candidate is LOW(" + usedCandidateCounter + ")",
									remote, existingConsideration, newConsideration, 
									SessionConsiderationTransitionType.S_CONSIDER_TRANSITION_DEMOTE);
						considerationList.updateSessionsConsideration(transition);
						setSessionsConnectionType(session, SessionConnectionType.S_CON_T_DROPPED);
						removeFromAllSessionsPrivately(remote);
						performCriticalUpdate = true;
					}
				}
				else
				{
					if (usedFanoutCounter < maxFanout)
					{
						// promote to real session
						usedFanoutCounter++;
						SessionConsideration newConsideration = new SessionConsideration(remote, ConsiderationStatus.S_CONSIDERATION_ONGOING);
						SessionConsiderationTransition transition = 
							new SessionConsiderationTransition("Candidate is HIGH(" + usedCandidateCounter + ") -- promoting", 
									remote, existingConsideration, newConsideration,
									SessionConsiderationTransitionType.S_CONSIDER_TRANSITION_PROMOTE_SOFT);
						considerationList.updateSessionsConsideration(transition);

						if (session.isSessionItselfTypePubSubEnabled()) {
							session.setRealSession(session);
							setSessionsConnectionType(session, SessionConnectionType.S_CON_T_SOFT);
							dumpAllSessions();
							_mq.dumpPSTop();
						}
						else
						{
							if (session.getSessionConnectionType() != SessionConnectionType.S_CON_T_CANDIDATE)
								throw new IllegalStateException(session + " is not CANDIDATE. .. ");
							
							setSessionsConnectionType(session, SessionConnectionType.S_CON_T_SOFTING);
							session.setRealSession(session);
							if (!session.hasConnection())
							{
								IConInfoNonListening<?> newConInfoNL = _nioBinding.makeOutgoingConnection(session, this, this, this, session.getRemoteAddress());
								if (newConInfoNL == null)
									failedSessions.add(session);
							}
							else if (session.isConnected() && session.isSessionItselfTypePubSubEnabled()) {
								dumpAllSessions();
								_mq.dumpPSTop();
							}
							else {
								// Just wait for the session to connect .. 
//								throw new IllegalStateException("Has not been implemented yet! " + session + " (" + session.isConnected() + session.isSessionItselfTypePubSubEnabled() + ")");
							}
						}
					}
					else
					{
						// remove session
						mqRemovedSessions.add(session);
						ConsiderationStatus newConsiderationStatus = fast?ConsiderationStatus.S_CONSIDERATION_FAST_LOW:ConsiderationStatus.S_CONSIDERATION_SLOW_LOW;
						SessionConsideration newConsideration = new SessionConsideration(remote, newConsiderationStatus);
						SessionConsiderationTransition transition = 
							new SessionConsiderationTransition("Candidate is LOW - passed maxFanoutLimit(" + usedFanoutCounter + ")",
									remote, existingConsideration, newConsideration,
									SessionConsiderationTransitionType.S_CONSIDER_TRANSITION_DEMOTE);
						considerationList.updateSessionsConsideration(transition);
						setSessionsConnectionType(session, SessionConnectionType.S_CON_T_DROPPED);
						removeFromAllSessionsPrivately(remote);
						performCriticalUpdate = true;
					}
				}
				break;
				
			case S_CONSIDERATION_ONGOING:
				if (usedFanoutCounter < maxFanout)
					usedFanoutCounter++;
				else
				{
					// remove session
					mqRemovedSessions.add(session);
					SessionConsideration newConsideration = 
						new SessionConsideration(remote, (fast?ConsiderationStatus.S_CONSIDERATION_FAST_LOW: ConsiderationStatus.S_CONSIDERATION_SLOW_LOW));
					SessionConsiderationTransition transition = 
						new SessionConsiderationTransition("ONGOING is low(" + usedFanoutCounter + ")",
								remote, existingConsideration, newConsideration, 
								SessionConsiderationTransitionType.S_CONSIDER_TRANSITION_DEMOTE);
					considerationList.updateSessionsConsideration(transition);
					setSessionsConnectionType(session, SessionConnectionType.S_CON_T_INACTIVE);
					performCriticalUpdate = true;
				}
				break;
				
			case S_CONSIDERATION_FAST_LOW:
			case S_CONSIDERATION_SLOW_HIGH:
			case S_CONSIDERATION_SLOW_LOW:
				break;
				
			default:
				break;
			}
		}

		for (int indexInSortedOutPub = 0  
				; indexInSortedOutPub < sortedSessionsOutPublication.length && usedCandidateCounter<maxCandidate 
				; indexInSortedOutPub++)
		{
			ISessionMP highSession = sortedSessionsOutPublication[indexInSortedOutPub]._session;
			InetSocketAddress highRemote = highSession.getRemoteAddress();
			InetSocketAddress[] highsNeighbors = _overlayManager.getNeighbors(highRemote);
			if (highsNeighbors == null)
				continue;
			
			for (int i=1 ; i<highsNeighbors.length && usedCandidateCounter<maxCandidate ; i++) {
				InetSocketAddress highsNeighbor = highsNeighbors[i];
				ISession highsNeighborExistingSession = getSessionPrivately(highsNeighbor);
				if (highsNeighborExistingSession!=null)
					if (highsNeighborExistingSession.isPrimaryActive())
						continue;
				SessionConsideration highsNeighborsConsideration = considerationList.getSessionConsideration(highsNeighbor);
				if (highsNeighborsConsideration == null || !highsNeighborsConsideration.isValid(now)) {
					// create new candidate session .. 
					ISessionMP newCandidateSession = (ISessionMP) getSessionPrivately(highsNeighbor);
					if (newCandidateSession == null)
						newCandidateSession = getCandidateSession(highsNeighbor);
					else if (newCandidateSession.isInMQ())
						continue;
						
					newCandidateSession.setSessionConnectionType(SessionConnectionType.S_CON_T_CANDIDATE);
					Path<INode> pathFromCandidate = _overlayManager.getPathFrom(highsNeighbor);
					InetSocketAddress[] pathFromCandidateAddresses = pathFromCandidate.getAddresses();
					ISession realSession = null;
					for (InetSocketAddress possibleRealRemote : pathFromCandidateAddresses) {
						ISession possibleRealSession = getSessionPrivately(possibleRealRemote);
						if (possibleRealSession != null && possibleRealSession.isReal()) {
							realSession = possibleRealSession;
							break;
						}
					}
					if (realSession==null) {
							realSession = getSessionPrivately(pathFromCandidateAddresses[pathFromCandidateAddresses.length-1]);
							LoggerFactory.getLogger().warn(this, "Real session is null for: " +
									newCandidateSession + ". Using: " + realSession + " for now.");
					}
					newCandidateSession.setRealSession(realSession);
					SessionConsideration newCandidateSessionConsideration = new SessionConsideration(highsNeighbor, ConsiderationStatus.S_CONSIDERATION_INITIATED);
					SessionConsiderationTransition transition = 
						new SessionConsiderationTransition("Creating new candidates (" + usedCandidateCounter + ")",
								highsNeighbor, highsNeighborsConsideration, newCandidateSessionConsideration, 
								SessionConsiderationTransitionType.S_CONSIDER_TRANSITION_INITIATE_CANDIDATE);
					considerationList.updateSessionsConsideration(transition);
					setLastReceivedSequence2(newCandidateSession, _localSequencer.getNext(), false, false); //true?
					mqAddedSessions.add(newCandidateSession);
					addSessionPrivately(highsNeighbor, newCandidateSession);
					usedCandidateCounter++;
					performCriticalUpdate = true;
				}
			}
		}
		
		if (usedFanoutCounter>maxFanout && usedFanoutCounter!=primaryActiveCount)
			throw new IllegalStateException("usedFanoutCounter is: " + usedFanoutCounter + " more than maxFanout: " + maxFanout);
		if (usedCandidateCounter>maxCandidate)
			throw new IllegalStateException("usedCandidateCounter is: " + usedCandidateCounter + " more than max: " + maxCandidate);
		
		return performCriticalUpdate;
	}
	
	protected void updateCriticalSessions() {
//		ISession[] activeSessions = getAllLocallyActiveSessionsPrivately();
		ISession[] activeSessions = getAllInMQSessionsPrivately();
		
		_workingManagersBundle = WorkingManagersBundle.createWorkingManagersBundle(_overlayManager, activeSessions, _subscriptionManager);
		
		if(Broker.CORRELATE)
			TrafficCorrelator.getInstance().updateCriticalSessions();
	}
	
	@Override
	protected ISessionManager createSessionManager(LocalSequencer localSequencer) {
		return new ISessionManagerMP(localSequencer, _nioBinding, this);
	}

	@Override
	public void updateWorkingOverlayManager(IOverlayManager overlayManager, TMulticast tm) {
		synchronized(_sessionsLock) {
			updateCriticalSessions();
		}
	}

	@Override
	public void updateWorkingSubscriptionManager(TMulticast tm) {
		synchronized(_sessionsLock)
		{
			TMulticastTypes tmType = tm.getType();
			
			switch(tmType) {
			case T_MULTICAST_SUBSCRIPTION:
				TMulticast_Subscribe tms = (TMulticast_Subscribe) tm;
				InetSocketAddress from = tms.getFrom();
				Path<INode> pathFromFrom = _overlayManager.getPathFrom(from);
				_workingManagersBundle._workingSubscriptionManager.applySummary(tms, pathFromFrom);
				break;
				
			case T_MULTICAST_UNSUBSCRIPTION:
				_workingManagersBundle._workingSubscriptionManager.applySummary((TMulticast_UnSubscribe)tm);
				break;
				
			default:
				throw new IllegalArgumentException(tm.toStringTooLong());
			}
		}
	}
	
	@Override
	protected boolean handleConnectionEvent_Special(ConnectionEvent connEvent) {
		switch(connEvent._eventType) {
		case CONNECTION_EVENT_CANDIDATES:
			handleConnectionEvent_candidates((ConnectionEvent_Candidates)connEvent);
			return true;
			
		case CONNECTION_EVENT_SEND_LOAD_WEIGHT:
			handleConnectionEvent_sendLoadWeight((ConnectionEvent_sendLoadWeight)connEvent);
			return true;
			
		default:
			return super.handleConnectionEvent_Special(connEvent);
		}
	}

	@Override
	protected IMessageQueue createMessageQueue(IBrokerShadow brokerShadow, IMessageQueue oldMQ) {
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_MQ, "Creating MP-MQ");
		IMessageQueue mq = new MessageQueueMP(this, (MessageQueue)oldMQ);
		return mq;
	}

	protected boolean updateDistance(ISessionMP session) {
		InetSocketAddress remote = session.getRemoteAddress();
		if (remote == null)
			return true;
		
		Path<INode> path = _overlayManager.getPathFrom(remote);
		if (path != null) {
			int distance = path.getLength();
			session.setDistance(distance);
			return true;
		}
		else{
			session.setDistance(-1);
			return false;
		}
	}
	
	protected double getSessionNormalizationFactor(double normalizationBase, ISessionMP session) {
		RTTLogger rttLogger = _brokerShadow.getRTTLogger();
		if(rttLogger == null)
			return 1;
		
		return getSessionNormalizationFactor(normalizationBase, rttLogger, _overlayManager, session);
	}
	
//	Broker.BW_OUT * Broker.USED_BW_THRESHOLD
	protected double getSessionNormalizationFactor(double normalizationBase, RTTLogger rttLogger, IOverlayManager overlayManager, ISessionMP session) {
		if(normalizationBase <= 0)
			return 1;
		
		double factor = 1;
		InetSocketAddress remote = session.getRemoteAddress();
		Path<INode> path = overlayManager.getPathFrom(remote);
		for(InetSocketAddress intemediateRemote : path.getAddresses()) {
			if(intemediateRemote == null)
				continue;
			
			if(intemediateRemote.equals(remote))
				continue;
			
			double outPubRate = rttLogger.getAverageOutputPublicationRate(intemediateRemote);
			double subfactor = outPubRate <= 0 ? 0 : (outPubRate - normalizationBase) / normalizationBase;
			factor *= subfactor <= 0 ? 1 : (1 + subfactor) * SESSION_NORMALIZATION_CONSTANT;
		}
		
		return Utility.max(1, factor);
	}
	
	protected double getAverageNormalizationBase(RTTLogger rttLogger, ISessionMP[] sessions) {
		double nonzerototal = 0, nonzerocount = 0;
		for (int i=0 ; i<sessions.length ; i++) {
			if(sessions[i] == null)
				continue;
			
			InetSocketAddress remote = sessions[i].getRemoteAddress();
			if(remote == null)
				continue;
			
			double outpub = rttLogger.getAverageOutputPublicationRate(remote);
			if(outpub > 0) {
				nonzerototal += outpub;
				nonzerocount++;
			}
		}
		
		return nonzerocount > 0 ? (nonzerototal / nonzerocount) : 0;
	}
	
	protected ISessionStatisticsSortedBundle updateSessionRanksPrivately(RTTLogger rttLogger) {
		ISession[] sessionsBase = getAllSessionsPrivately();
		ISessionMP[] sessions = new ISessionMP[sessionsBase.length];
		double normalizationBase = getAverageNormalizationBase(rttLogger, sessions);
		
		for (int i=0 ; i<sessions.length ; i++) {
			if (sessionsBase[i].isLocal())
				continue;
			sessions[i] = (ISessionMP)sessionsBase[i];
			double sessionNormalizationFactor = getSessionNormalizationFactor(normalizationBase, sessions[i]);
			sessions[i].updatePublicationAverages(sessionNormalizationFactor);
			if (!sessions[i].isDistanceSet()) {
				boolean distanceValid = updateDistance(sessions[i]);
				if (!distanceValid)
					LoggerFactory.getLogger().warn(this, "Session's distncce is invalid: " + sessions[i]);
			}
		}
		
		ISessionStatisticsSortedBundle statisticsBundle = new ISessionStatisticsSortedBundle(sessions);
		_brokerShadow.getSessionsRanksLogger().logRanks(statisticsBundle);
		return statisticsBundle;
	}
	
	protected void reconsiderAllConsideredSessions() {
		InetSocketAddress[] consideredAddresses = _considerationList.getAllConsiderationAddresses();
		for (int i=0 ; i<consideredAddresses.length ; i++)
		{
			InetSocketAddress remote  = consideredAddresses[i];
			ISessionMP session = (ISessionMP) getSessionPrivately(remote);
			SessionConsideration consideration = _considerationList.getSessionConsideration(remote);
			if (session!=null && consideration != null && session.isPrimaryActive()) {
				SessionConsiderationTransition transition = new SessionConsiderationTransition("Reconsidering All sessions", remote, consideration, null, SessionConsiderationTransitionType.S_CONSIDER_TRANSITION_REMOVE);
				_considerationList.updateSessionsConsideration(transition);
			}
		}
	}
	
	@Override
	protected void handleConnectionEvent_writeConnection(ConnectionEvent_writeConnection connEvent) {
		super.handleConnectionEvent_writeConnection(connEvent);
		
		IConInfoNonListening<?> writeConInfoNL = connEvent._conInfoNL;
		ISession writeSession = writeConInfoNL.getSession();

		Set<ISessionMP> redirectedSessions = ((ISessionMP)writeSession).getRedirectedSessions();
		if (redirectedSessions == null)
			return;
		else
		{
			Iterator<ISessionMP> redirectedSessionsIt = redirectedSessions.iterator();
			while (redirectedSessionsIt.hasNext()) {
				ISessionMP redirectedSession = redirectedSessionsIt.next();
				_mq.proceed(redirectedSession.getRemoteAddress());
			}
		}
	}

	@Override
	public PubForwardingStrategy getPublicationForwardingStrategy() {
		return _brokerShadow.getPublicationForwardingStrategy();
	}
	
	private Object _loadWeightSendLock = new Object();
	private boolean _loadWeightSendPending = false;
	protected void scheduleNextLoadWeightSend() {
		if (!_brokerShadow.isMP() || !_brokerShadow.getPublicationForwardingStrategy()._isLoadAware)
			throw new UnsupportedOperationException("Not supported for: " + _brokerShadow.isMP() + " && " + _brokerShadow.getPublicationForwardingStrategy());

		synchronized (_loadWeightSendLock)
		{
			if (_status != ComponentStatus.COMPONENT_STATUS_RUNNING) {
				_loadWeightSendPending = false;
				return;
			}
		
			if (_loadWeightSendPending == true)
				return;

			_loadWeightSendPending = true;
			LoadWeightSendTask loadWeightsSendTask = new LoadWeightSendTask(this);
			scheduleTaskWithTimer(loadWeightsSendTask, Broker.LOAD_WEIGHTS_SEND_INTERVAL);
		}
	}

	@Override
	public void sendLoadWeights() {
		synchronized (_componentStatusChangeWaitObject) {
			if (_status != ComponentStatus.COMPONENT_STATUS_RUNNING) {
				synchronized (_loadWeightSendLock) {
					_loadWeightSendPending = false;
				}
				return;
			}
		}

		synchronized (_loadWeightSendLock)
		{
			_loadWeightSendPending = false;

			LocalSequencer localSequencer = _localSequencer;
			if(localSequencer == null)
				return;
			ConnectionEvent connEventSendDack = new ConnectionEvent_sendLoadWeight(localSequencer);
			addConnectionEventHead(connEventSendDack);
		}
		
		scheduleNextLoadWeightSend();
	}
	
	protected void handleConnectionEvent_sendLoadWeight(ConnectionEvent_sendLoadWeight connEvent) {
		List<ISession> sessions = getAllPrimaryActiveSessionsPrivately();
		for (ISession session : sessions)
			sendLoadWeightOnSession(session);
	}
	
	protected void sendLoadWeightOnSession(ISession session) {
		if (!_brokerShadow.isMP() || !_brokerShadow.getPublicationForwardingStrategy()._isLoadAware)
			throw new UnsupportedOperationException("Not supported for: " + _brokerShadow.isMP() + " && " + _brokerShadow.getPublicationForwardingStrategy());

		synchronized (_loadWeightSendLock)
		{
			if (!session.isConnected())
				return;
		
			InetSocketAddress remoteAddress = session.getRemoteAddress();
			
			TLoadWeight tLoadWeight = _loadWeightRepo.getTLoadWeight(remoteAddress);
			
			IRawPacket raw = PacketFactory.wrapObject(_localSequencer, tLoadWeight);
			_sessionManager.send(session, raw);
		}
	}
	
//	@Override
//	protected boolean handleSpecialMessage(ISession session, IRawPacket raw) {
//		PacketableTypes packetType = raw.getType();
//		switch(packetType) {
//			
//		default:
//			super.processMessage(session, raw);
//		}
//	}

	public final boolean loadPrepareSubscription(SubscriptionEntry subEntry) {
		if(!super.loadPrepareSubscription(subEntry))
			return false;
	
		IWorkingSubscriptionManager workingSubscriptionManager = _workingManagersBundle._workingSubscriptionManager;
		if(workingSubscriptionManager==null)
			return true;
		
		if(subEntry.isLocal())
			workingSubscriptionManager.addNewLocalSubscriptionEntry((LocalSubscriptionEntry)subEntry);
		else
			workingSubscriptionManager.addNewSubscriptionEntry(subEntry);
		
		return true;
	}
	
	@Override
	protected boolean isMP() {
		return true;
	}
	
	@Override
	protected boolean handleSpecialMessage(ISession session, IRawPacket raw) {
		PacketableTypes packetType = raw.getType();
		
		switch(packetType) {
		case TLOCALLOADWEIGHT:
			TLoadWeight tLoadWeight = (TLoadWeight)PacketFactory.unwrapObject(_brokerShadow, raw, true);
			_loadWeightRepo.addFreshIncomingLoadWeight(tLoadWeight);			
			return true;

		default:
			return super.handleSpecialMessage(session, raw);
		}		
	}
}
