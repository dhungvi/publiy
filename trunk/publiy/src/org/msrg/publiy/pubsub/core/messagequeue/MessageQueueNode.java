package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastAnnotatorFactory;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;

import org.msrg.publiy.utils.HistoryType;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.utils.annotations.AnnotationEvent;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.ConfirmationDataLogger;
import org.msrg.publiy.utils.log.casuallogger.MessageProfilerLogger;
import org.msrg.publiy.utils.log.casuallogger.exectime.ExecutionTimeType;
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeEntity;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;
import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationDumpSpecifier;

class MessageQueueNode implements IMessageQueueNode, ILoggerSource {

	private static int _next_mqn_int_sequence = 0;
	protected final int _mqn_int_sequence = getNextMQNSequence();
	
	protected IMessageQueueNode _next;
	protected MessageQueue _mQ;
	
	protected final TMulticast _tm;
	protected final ITMConfirmationListener _confirmationListener;
	
	protected boolean _confirmed = false;
	
	protected Set<InetSocketAddress> _senders;
	protected Set<InetSocketAddress> _visitedSet = (Broker.RELEASE?null:new HashSet<InetSocketAddress>());
	protected Set<InetSocketAddress> _checkedOutSet = new HashSet<InetSocketAddress>();
	protected int _visitedSessions = 0;
	protected final Sequence _sequence;
	protected final Set<InetSocketAddress> _matchingSet;
	
	protected final long _creationTime = SystemTime.currentTimeMillis();
	protected long _confirmedTime = -1;

	protected final IExecutionTimeEntity _checkOutExecutionTimeEntity;
	
	protected MessageQueueNode(MessageQueue mQ, TMulticast tm, ITMConfirmationListener confirmationListener, Sequence seq) {
		_mQ = mQ;
		_tm = tm;
		_confirmationListener = confirmationListener;
		_sequence = seq;
		_matchingSet = computeMatchingSetForPublicationMessages(_mQ, _tm);
		
		// tm.setLocalSequence(_sequence);
		if(_mQ!=null) {
			ConfirmationDataLogger confirmationDataLogger = _mQ._brokerShadow.getConfirmationDataLogger();
			if(confirmationDataLogger!=null)
				confirmationDataLogger.logMQNodeCreation(_tm.getType());
		}
		
		if(Broker.CORRELATE)
			TrafficCorrelator.getInstance().mqnCreated(_tm, _sequence);
		
		_checkOutExecutionTimeEntity = getCheckOutExecutionTimerEntity(_tm.getType(), _mQ._brokerShadow);
	}
	
	protected IExecutionTimeEntity getCheckOutExecutionTimerEntity(TMulticastTypes tmType, IBrokerShadow brokerShadow) {
		switch (tmType) {
		case T_MULTICAST_PUBLICATION:
		case T_MULTICAST_PUBLICATION_MP:
			return (brokerShadow == null) ? null : brokerShadow.getExecutionTypeEntity(ExecutionTimeType.EXEC_TIME_CHECKOUT_PUB);
			
		default:
			return null;
		}
	}
	
	private static synchronized int getNextMQNSequence() {
		return ++_next_mqn_int_sequence;
	}
	
	@Override
	public int getMQNSequence() {
		return _mqn_int_sequence;
	}

	@Override
	public boolean isConfirmed() {
		return _confirmed;
	}
	
	@Override
	public Sequence getNodeSequence() {
		return _sequence;
	}
	
	@Override
	public Sequence getMessageSourceSequence() {
		return _tm.getSourceSequence();
	}
	
	@Override
	public IMessageQueueNode getNext() {
		return _next;
	}
	
	@Override
	public void setNext(IMessageQueueNode mqn) {
		_next = mqn;
	}

	public boolean visitedSetContains(InetSocketAddress remote) {
		return _visitedSet.contains(remote);
	}
	
	@Override
	public void cancelCheckoutUnVisited(InetSocketAddress remote) {
		if(_confirmed)
			return;
		
		if(!Broker.RELEASE)
			if(_visitedSet.contains(remote))
			throw new IllegalStateException(this.toStringLong(false) + " vs. " + remote);

		checkForConfirmation();
		return;
	}
	
	protected void decrementVisitedCount(InetSocketAddress remote) {
		if(!Broker.RELEASE)
			if(!_visitedSet.remove(remote))
				throw new IllegalStateException(remote + " did not visit this node: " + this.toStringLong(true));
		
		--_visitedSessions;
		if(!Broker.RELEASE)
			if(_visitedSessions != _visitedSet.size())
				throw new IllegalStateException(this.toStringLong(true));
		
		LoggerFactory.getLogger().debug(this, "Remote '" + remote + "' dec: " + _visitedSessions + "\t" + this);
	}
	
	@Override
	public void cancelCheckoutVisited(InetSocketAddress remote) {
		if(_confirmed)
			return;
		
		decrementVisitedCount(remote);
		_checkedOutSet.remove(remote);
		checkForConfirmation();
	}

	protected final void incrementVisitedCount(InetSocketAddress remote) {
		if(!Broker.RELEASE)
			if(!_visitedSet.add(remote)) {
				if(Broker.CORRELATE) {
					TrafficCorrelationDumpSpecifier dumpSpecifier =
						TrafficCorrelationDumpSpecifier.getMessageCorrelationDump(
								_tm, "FATAL:error_while_incrementVisitedCount_for_" + remote, false);
					TrafficCorrelator.getInstance().dump(dumpSpecifier, true);
				}
				
				throw new IllegalStateException(this.toStringLong(true) + " remote: " + remote);
			}
		
		++_visitedSessions;
		if(!Broker.RELEASE)
			if(_visitedSessions != _visitedSet.size())
				throw new IllegalStateException(this.toStringLong(true));
		
		LoggerFactory.getLogger().debug(this, "'" + remote + "' inc: " + _visitedSessions + "\t" + this);
	}

	@Override
	public void passThrough(InetSocketAddress remote) {
		if(_confirmed)
			return;
		
		incrementVisitedCount(remote);
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, remote.toString() + " passed through " + this);
		checkForConfirmation();
	}
	
	public boolean visitedBy(InetSocketAddress remote) {
		if(Broker.RELEASE)
			throw new IllegalStateException("Should not be here when Broker.RELEASE=" + Broker.RELEASE);

		return _visitedSet.contains(remote);
	}

	@Override
	public final IRawPacket checkOutAndmorph(ISession session, boolean asGuided) {
		if(_confirmed)
			return null;
		
		if(_checkOutExecutionTimeEntity != null)
			_checkOutExecutionTimeEntity.executionStarted();
		
		double processingStartTime = SystemTime.nanoTime();
	
		IRawPacket raw = checkOutAndmorphPrivately(session, asGuided);
		
		if(Broker.ANNOTATE) {
			double processingTime = SystemTime.nanoTime() - processingStartTime;
			TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(_mQ._localSequencer, raw, AnnotationEvent.ANNOTATION_EVENT_MQ_PROCESSING_TIME, "Took " + processingTime + " to process");
			TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(_mQ._localSequencer, raw, AnnotationEvent.ANNOTATION_EVENT_MQ_EXIT, "Message checked out from MQ");
		}
		
		if(_checkOutExecutionTimeEntity != null && !_confirmed)
			_checkOutExecutionTimeEntity.executionEnded();
		
		return raw;
	}
	
	protected IRawPacket checkOutAndmorphPrivately(ISession session, boolean asGuided) {
		InetSocketAddress remote = session.getRemoteAddress();

		if(_confirmed)
			return null;
		incrementVisitedCount(remote);

		BrokerIdentityManager idMan = _mQ._brokerShadow.getBrokerIdentityManager();
		InetSocketAddress sender = _tm.getSenderAddress();
		switch(_tm.getType()) {
		case T_MULTICAST_PUBLICATION:
		case T_MULTICAST_PUBLICATION_MP:
		case T_MULTICAST_PUBLICATION_NC:
		case T_MULTICAST_PUBLICATION_BFT:
		case T_MULTICAST_PUBLICATION_BFT_DACK:
			sender = ((TMulticast_Publish)_tm).getFrom();
			break;
			
		default:
			break;
		}
		if(sender == null)
			throw new NullPointerException("Sender is null: " + " vs. " + _tm.toString(idMan));
//			sender = _tm.getSourceAddress();
		IOverlayManager overlayManager = _mQ.getOverlayManager();
		Path<INode> pathFromSender = overlayManager.getPathFrom(sender);
		Path<INode> pathFromRemote = overlayManager.getPathFrom(remote);

		String fromStr = "";
		if(_tm.getType() == TMulticastTypes.T_MULTICAST_PUBLICATION_BFT)
			fromStr = " from:" + idMan.getBrokerId(((TMulticast_Publish_BFT)_tm).getFrom());
		
		if(pathFromSender == null)
			throw new NullPointerException("PathFromSender is null: " + Writers.write(sender, idMan) + ", " + overlayManager + " vs. " + _tm.toString(idMan) + fromStr + " vs. " + Writers.write(_tm.getSequenceVector()));
//			return null;

		if(pathFromRemote == null)
			throw new NullPointerException("PathFromRemote is null: " + Writers.write(remote, idMan) + ", " + overlayManager + " vs. " + _tm.toString(idMan));

		if(pathFromSender.intersect(pathFromRemote)) {
//			BrokerInternalTimer.inform("Intersects: " + Writers.write(sender) + " vs. " + Writers.write(remote) + " vs. " + _tm.toString(idMan));
			return null;
		}

		IRawPacket raw = morph(session, asGuided);
		if(raw == null) {
			checkForConfirmation();
			return null;
		}
		
		if(session.getSessionConnectionType() == SessionConnectionType.S_CON_T_DELTA_VIOLATED) {
			if(_mQ._violationsLogger != null)
				_mQ._violationsLogger.deltaViolatingPublication();
			
			return null;
		}
		
		_checkedOutSet.add(remote);

		return raw;
	}

	protected void logMessageWithMessageLogger(InetSocketAddress remote, TMulticast tm, boolean outgoing) {
		MessageProfilerLogger messageProfilerLogger = _mQ._brokerShadow.getMessageProfilerLogger();
		if(messageProfilerLogger == null)
			return;
		
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION:
		case T_MULTICAST_PUBLICATION_MP:
			if(outgoing)
				messageProfilerLogger.logMessage(HistoryType.HIST_T_PUB_OUT_BUTLAST, remote, tm.getContentSize());
			else
				messageProfilerLogger.logMessage(HistoryType.HIST_T_PUB_IN_BUTLAST, remote, tm.getContentSize());
			break;
			
		case T_MULTICAST_SUBSCRIPTION:
			if(outgoing)
				messageProfilerLogger.logMessage(HistoryType.HIST_T_SUB_OUT_BUTLAST, remote, tm.getContentSize());
			else
				messageProfilerLogger.logMessage(HistoryType.HIST_T_SUB_IN_BUTLAST, remote, tm.getContentSize());
			break;
			
		case T_MULTICAST_CONF:
		case T_MULTICAST_DEPART:
		case T_MULTICAST_JOIN:
		case T_MULTICAST_UNKNOWN:
		case T_MULTICAST_UNSUBSCRIPTION:
			break;
			
		default:
			break;
		}
	}
	
	protected final Set<InetSocketAddress> trimGuidedRecipient(Set<InetSocketAddress> recipients, InetSocketAddress remote) {
		Iterator<InetSocketAddress> recipientsIt = recipients.iterator();
		while (recipientsIt.hasNext()) 
		{
			InetSocketAddress recipient = recipientsIt.next();
			Path<INode> recipientPath = _mQ.getOverlayManager().getPathFrom(recipient);

			if(!recipientPath.passes(remote))
				recipientsIt.remove();
		}

		return recipients;
	}

	protected final IRawPacket morphG_TMulticastJoin(ISession session, boolean asGuided) {
		if(!canCheckOutTMulticast(session, TMulticastTypes.T_MULTICAST_JOIN))
			return null;

		InetSocketAddress remote = session.getRemoteAddress();
		TMulticast_Join tmj = (TMulticast_Join) _tm;
		boolean shallSend = false;
		IOverlayManager overlayManager = _mQ.getOverlayManager();
		Set<InetSocketAddress> recipients = tmj.getClonedGuidedInfo();
		Iterator<InetSocketAddress> recipientsIt = recipients.iterator();
		while (recipientsIt.hasNext()) {
			InetSocketAddress recipient = recipientsIt.next();
			Path<INode> pathFromRecipient = overlayManager.getPathFrom(recipient);
			if(pathFromRecipient.passes(remote)) {
				shallSend = true;
				break;
			}
		}

		if(!shallSend)
			return null;

		Path<INode> pathFromRemote = overlayManager.getPathFrom(remote);
		TMulticast_Join tmClonedCopy = tmj.getShiftedClone(pathFromRemote.getLength(), _sequence);
		if(asGuided) {
			Set<InetSocketAddress> trimmedRecipients = trimGuidedRecipient(recipients, remote);
			tmClonedCopy.loadGuidedInfo(trimmedRecipients);
		}

		logMessageWithMessageLogger(remote, _tm, true);
		IRawPacket raw = PacketFactory.wrapObject(_mQ._localSequencer, tmClonedCopy);
		return raw;
	}

	protected boolean canCheckOutTMulticast(ISession session, TMulticastTypes tmType) {
		switch(session.getSessionConnectionType()) {
		case S_CON_T_DELTA_VIOLATED:
		case S_CON_T_ACTIVE:
			return true;

		case S_CON_T_SOFTING:
		case S_CON_T_SOFT:
		case S_CON_T_CANDIDATE:
			switch(tmType) {
			case T_MULTICAST_PUBLICATION_MP:
			case T_MULTICAST_PUBLICATION:
				return true;
				
			default:
				return false;
			}
			
		default:
			throw new IllegalStateException(this + " vs. " + session);
		}
	}
	
	protected final IRawPacket morphTMulticastJoin(ISession session, boolean asGuided) {
		if(!canCheckOutTMulticast(session, TMulticastTypes.T_MULTICAST_JOIN))
			return null;

		InetSocketAddress remote = session.getRemoteAddress();
		TMulticast_Join tmj = (TMulticast_Join) _tm;
		InetSocketAddress joinPointAddress = tmj.getJoinPoint();
		InetSocketAddress joiningAddress = tmj.getJoiningNode();

		if(joinPointAddress.equals(remote) && session.getLocalAddress().equals(joiningAddress)) {
			Path<INode> pathFromRemote = _mQ.getOverlayManager().getPathFrom(remote);
			TMulticast_Join tmClonedCopy = tmj.getShiftedClone(pathFromRemote.getLength(), _sequence);

			logMessageWithMessageLogger(remote, _tm, true);
			IRawPacket raw = PacketFactory.wrapObject(_mQ._localSequencer, tmClonedCopy);
			return raw;
		}

		IOverlayManager overlayManager = _mQ.getOverlayManager();
		Path<INode> pathFromRemote = overlayManager.getPathFrom(remote);
		int remoteDistance;
		if(pathFromRemote == null)
			remoteDistance = 1;
		else
			remoteDistance = pathFromRemote.getLength();

		int joinPointDistance = 0;
		if(!joinPointAddress.equals(_mQ._localAddress)) {
			Path<INode> pathFromJoinPoint = overlayManager.getPathFrom(joinPointAddress);
			if(pathFromJoinPoint == null)
				throw new IllegalStateException("Joinpoint '" + tmj.getJoinPoint() + "' must have already been in the overlay, before '" + tmj.getJoiningNode() + "' can join:: " + tmj);

			if(pathFromJoinPoint.passes(remote))
				return null;
	
			joinPointDistance = pathFromJoinPoint.getLength();
		}

		if(remoteDistance + joinPointDistance >= overlayManager.getNeighborhoodRadius())
			return null;

		TMulticast_Join tmClonedCopy = tmj.getShiftedClone(remoteDistance, _sequence);

		if(asGuided) {
			List<InetSocketAddress> recipients = overlayManager.getFartherNeighborsOrderedList(remote);
			Set<InetSocketAddress> trimmedRecipients = new HashSet<InetSocketAddress>(recipients);
			tmClonedCopy.loadGuidedInfo(trimmedRecipients);
		}

		logMessageWithMessageLogger(remote, _tm, true);
		IRawPacket raw = PacketFactory.wrapObject(_mQ._localSequencer, tmClonedCopy);
		return raw;
	}

	protected IRawPacket morphG_TMulticastPublish(ISession session, boolean asGuided) {
		if(!canCheckOutTMulticast(session, TMulticastTypes.T_MULTICAST_PUBLICATION))
			return null;

		InetSocketAddress remote = session.getRemoteAddress();
		TMulticast_Publish tmp = (TMulticast_Publish) _tm;
		InetSocketAddress from = tmp.getFrom();
		
		boolean shallSend = false;
		IOverlayManager overlayManager = _mQ.getOverlayManager();
		Set<InetSocketAddress> recipients = tmp.getClonedGuidedInfo();
		Iterator<InetSocketAddress> recipientsIt = recipients.iterator();
		while (recipientsIt.hasNext()) {
			InetSocketAddress recipient = recipientsIt.next();
			Path<INode> pathFromRecipient = overlayManager.getPathFrom(recipient);
			if(pathFromRecipient.passes(remote)) {
				shallSend = true;
				break;
			}
		}

		if(!shallSend) {
			return null;
		}

		InetSocketAddress newFrom = overlayManager.getNewFromForMorphedMessage(remote, from);
		if(newFrom == null)
			throw new IllegalStateException("NewFrom should not be null.");

		Path<INode> pathFromRemote = overlayManager.getPathFrom(remote);
		TMulticast_Publish tmpClone = tmp.getShiftedClone(pathFromRemote.getLength(), _sequence);

		tmpClone.setFrom(newFrom);
		if(asGuided) {
			Set<InetSocketAddress> trimmedRecipients = trimGuidedRecipient(recipients, remote);
			tmpClone.loadGuidedInfo(trimmedRecipients);
		}

		logMessageWithMessageLogger(remote, _tm, true);
		IRawPacket raw = PacketFactory.wrapObject(_mQ._localSequencer, tmpClone);
		return raw;
	}

	protected Set<InetSocketAddress> computeMatchingSetForPublicationMessages(MessageQueue mq, TMulticast tm) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION:
			Publication publication = ((TMulticast_Publish)tm).getPublication();
			return _mQ.getSubscriptionManager().getMatchingSet(publication);
			
		default:
			return null;
		}
	}

	protected IRawPacket morphTMulticastPublish(ISession session, boolean asGuided) {
		if(!canCheckOutTMulticast(session, TMulticastTypes.T_MULTICAST_PUBLICATION))
			return null;

		InetSocketAddress remote = session.getRemoteAddress();
		TMulticast_Publish tmp = (TMulticast_Publish) _tm;
		InetSocketAddress from = tmp.getFrom();

		boolean shallISend = false;
		IOverlayManager overlayManager = _mQ.getOverlayManager();
		for(InetSocketAddress matchingRemote : _matchingSet) { 
			Path<INode> matchingRemotePath = overlayManager.getPathFrom(matchingRemote);
			if(matchingRemote == null)
				throw new NullPointerException("" + matchingRemote + " is null");
			if(matchingRemotePath.passes(remote)) {
				shallISend = true;
				break;
			}
		}

		if(!shallISend)
			return null;

		InetSocketAddress newFrom = overlayManager.getNewFromForMorphedMessage(remote, from);
		if(newFrom == null)
			throw new IllegalStateException("NewFrom should not be null.");

		Path<INode> pathFromRemote = _mQ.getOverlayManager().getPathFrom(remote);
		TMulticast_Publish tmpClone = tmp.getShiftedClone(pathFromRemote.getLength(), _sequence);

		tmpClone.setFrom(newFrom);
		if(asGuided) {
			Set<InetSocketAddress> recipients = trimGuidedRecipient(_matchingSet, remote);
			tmpClone.loadGuidedInfo(recipients);
		}

		if(Broker.LOG_VIOLATIONS) {
			_mQ.checkLegitViolating(tmpClone, remote);
			_mQ.checkLegitViolated(tmpClone, remote);
		}
		
		logMessageWithMessageLogger(remote, _tm, true);
		IRawPacket raw = PacketFactory.wrapObject(_mQ._localSequencer, tmpClone);
		return raw;
	}

	protected IRawPacket morphG_TMulticastSubscribe(ISession session, boolean asGuided) {
		if(!canCheckOutTMulticast(session, TMulticastTypes.T_MULTICAST_SUBSCRIPTION))
			return null;

		InetSocketAddress remote = session.getRemoteAddress();
		TMulticast_Subscribe tms = (TMulticast_Subscribe) _tm;
		boolean shallSend = false;
		IOverlayManager overlayManager = _mQ.getOverlayManager();
		Set<InetSocketAddress> recipients = tms.getClonedGuidedInfo();
		Iterator<InetSocketAddress> recipientsIt = recipients.iterator();
		while (recipientsIt.hasNext()) {
			InetSocketAddress recipient = recipientsIt.next();
			Path<INode> pathFromRecipient = overlayManager.getPathFrom(recipient);
			if(pathFromRecipient.passes(remote)) {
				shallSend = true;
				break;
			}
		}

		if(!shallSend)
			return null;

		InetSocketAddress oldFrom = tms.getFrom();
		InetSocketAddress newFrom = overlayManager.getNewFromForMorphedMessage(remote, oldFrom);

		if(newFrom == null)
			return null;

		// TMulticast_Subscribe tmsClone = tms.getClone();
		Path<INode> pathFromRemote = overlayManager.getPathFrom(remote);
		TMulticast_Subscribe tmsClone = tms.getShiftedClone(pathFromRemote.getLength(), _sequence);

		tmsClone.setFrom(newFrom);
		if(asGuided) {
			Set<InetSocketAddress> trimmedGuidedRecipients = trimGuidedRecipient(recipients, remote);
			tmsClone.loadGuidedInfo(trimmedGuidedRecipients);
		}

		logMessageWithMessageLogger(remote, _tm, true);
		IRawPacket raw = PacketFactory.wrapObject(_mQ._localSequencer, tmsClone);
		return raw;

	}

	protected final IRawPacket morphTMulticastSubscribe(ISession session, boolean asGuided) {
		if(!canCheckOutTMulticast(session, TMulticastTypes.T_MULTICAST_SUBSCRIPTION))
			return null;

		InetSocketAddress remote = session.getRemoteAddress();
		IOverlayManager overlayManager = _mQ.getOverlayManager();
		Path<INode> pathFromRemote = overlayManager.getPathFrom(remote);
		if(pathFromRemote == null)
			throw new IllegalStateException("Cannot morph subscription for remote '" + remote + "' since it is not part of the topology.");

		TMulticast_Subscribe tms = (TMulticast_Subscribe) _tm;
		InetSocketAddress oldFrom = tms.getFrom();

		InetSocketAddress newFrom = overlayManager.getNewFromForMorphedMessage(remote, oldFrom);
		if(newFrom == null)
			return null;

		// TMulticast_Subscribe tmsClone = tms.getClone();
		TMulticast_Subscribe tmsClone = tms.getShiftedClone(pathFromRemote.getLength(), _sequence);

		tmsClone.setFrom(newFrom);
		if(asGuided) {
			List<InetSocketAddress> recipients = overlayManager.getFartherNeighborsOrderedList(remote);
			Set<InetSocketAddress> trimmedRecipients = new HashSet<InetSocketAddress>(recipients);
			tmsClone.loadGuidedInfo(trimmedRecipients);
		}

		logMessageWithMessageLogger(remote, _tm, true);
		IRawPacket raw = PacketFactory.wrapObject(_mQ._localSequencer, tmsClone);
		return raw;
	}

	protected synchronized IRawPacket morph(ISession session, boolean asGuided) {
		TMulticastTypes tmType = _tm.getType();
		if(_tm.isGuided()) // && _mQ.getBrokerOpState() == BrokerOpState.BRKR_RECOVERY) 
		{
			switch(tmType) {
			case T_MULTICAST_JOIN:
				IRawPacket rawGJoin = morphG_TMulticastJoin(session, asGuided);
				return rawGJoin;

			case T_MULTICAST_SUBSCRIPTION:
				IRawPacket rawGSub = morphG_TMulticastSubscribe(session, asGuided);
				return rawGSub;

			case T_MULTICAST_PUBLICATION_MP:
			case T_MULTICAST_PUBLICATION:
				IRawPacket rawGPub = morphG_TMulticastPublish(session, asGuided);
				return rawGPub;
				
			default:
				throw new UnsupportedOperationException("Donno how to morph this type ('" + tmType + "') : " + this);
			}
		}

		else// if(!_tm.isGuided() && _mQ.getBrokerOpState() != BrokerOpState.BRKR_RECOVERY)
		{
			switch(tmType) {
			case T_MULTICAST_JOIN:
				IRawPacket rawJoin = morphTMulticastJoin(session, asGuided);
				return rawJoin;

			case T_MULTICAST_SUBSCRIPTION:
				IRawPacket rawSub = morphTMulticastSubscribe(session, asGuided);
				return rawSub;

			case T_MULTICAST_PUBLICATION_MP:
			case T_MULTICAST_PUBLICATION:
				IRawPacket rawPub = morphTMulticastPublish(session, asGuided);
				return rawPub;
				
			default:
				throw new UnsupportedOperationException("Donno how to morph this type ('" + tmType + "') : " + this);
			}
		}
	}

	@Override
	public String getConfirmationStatus() {
		return "CKOUT: " + _checkedOutSet.size() + " VSTSESN: " + _visitedSessions + "/" + _mQ.getActiveSessionsCount() + "->" + _confirmed;
	}

	@Override
	public final void checkForConfirmation() {
		if(_confirmed)
			return;

		if(Broker.DEBUG) 
			LoggerFactory.getLogger().debug(this,"Checking for confirmation: " + getConfirmationStatus());
		
		if(checkMQNodeForConfirmation())
			_mQ.confirm(this);
	}

	protected boolean checkMQNodeForConfirmation() {
		int activeSessionsCount = _mQ.getActiveSessionsCount();
		return _checkedOutSet.size() == 0 && activeSessionsCount == _visitedSessions;
	}

	@Override
	public TMulticast_Conf getConfirmation(InetSocketAddress confirmedTo, InetSocketAddress from, Sequence confirmationFromSequence) {
		return new TMulticast_Conf(_tm, confirmedTo, from, confirmationFromSequence);
	}

	@Override
	public TMulticast getTMulticast() {
		return _tm;
	}

	@Override
	public InetSocketAddress getSender() {
		return _tm.getSenderAddress();
	}

	@Override
	public InetSocketAddress[] getSenders() {
		if(_senders == null)
			return null;
		return (InetSocketAddress[])_senders.toArray(new InetSocketAddress[0]);
	}

	@Override
	public void addToSenders(InetSocketAddress sender) {
		if(_confirmed)
			return;
		if(_senders == null)
			_senders = new HashSet<InetSocketAddress>();
		_senders.add(sender);
	}

	@Override
	public ITMConfirmationListener getConfirmationListener() {
		return _confirmationListener;
	}

	@Override
	public String toString() {
		String confStr = getPrefix();
		confStr += "[" + checkMQNodeForConfirmation() + "]";
		return confStr + "_" + _checkedOutSet.size() + "/"
				+ _visitedSessions + "/" + _mQ.getPSSessionSize()
				+  "!!" + _sequence + "!!:" + _tm;
	}
	
	protected String getPrefix() {
		return "MQN_" + (_confirmed ? "C" : "NC");
	}
	
	@Override
	public String toStringLong(boolean testForConfirmation) {
		return getPrefix()
				+ "_#" + _mqn_int_sequence + "_"
				+ ((testForConfirmation)?("["
				+ checkMQNodeForConfirmation()
				+ "]"):"")
				+ " CHKOUT(" + _checkedOutSet.size() + ")::" + Writers.write(_checkedOutSet)
				+ "/"
				+ (!Broker.RELEASE?(" VISIT(" + _visitedSessions + ")::" + Writers.write(_visitedSet)):"")
				+ "/"
				+ _mQ.getActiveSessionsCount()
				+ Writers.write(_mQ.getPSSessionsAsISessions().toArray(
						new ISession[0])) + "!!" + _sequence + "!!:"
				+ _tm.toStringTooLong();
	}

	@Override
	public void confirmationReceieved(TMulticast_Conf tmc) {
		LoggerFactory.getLogger().debug(this,"Confirmation received: " + tmc);
		if(tmc.isConfirmationOf(_tm))
			if(_checkedOutSet.remove(tmc.getConfirmedFrom()))
				checkForConfirmation();
	}

	@Override
	public final boolean confirmed(boolean isConfirmed) {
		boolean oldValue = _confirmed;

		if(oldValue != isConfirmed & isConfirmed) {
			_confirmedTime = SystemTime.currentTimeMillis();

			if(_mQ != null) {
				ConfirmationDataLogger confirmationLogger = _mQ._brokerShadow.getConfirmationDataLogger();
				if(confirmationLogger != null)
					confirmationLogger.logMQNodeConfirmed(_tm.getType(), _confirmedTime - _creationTime);
			}
			// TODO: ULTRA URGENT: change below to debug
//			if(Broker.DEBUG)
				LoggerFactory.getLogger().debug(this, "Got confirmed: " + _tm);
		}

		_confirmed = isConfirmed;
		_checkedOutSet.clear();
		if(!Broker.RELEASE)
			_visitedSet.clear();
		
		if(Broker.CORRELATE)
			TrafficCorrelator.getInstance().messageConfirmed(_tm);
		
		if(_confirmed && _checkOutExecutionTimeEntity != null)
			_checkOutExecutionTimeEntity.finalized(true);

		return oldValue;
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_MQN;
	}

	@Override
	public boolean checkedOutSetContains(InetSocketAddress remoteInBetween) {
		return _checkedOutSet.contains(remoteInBetween);
	}

	@Override
	public TMulticast getMessage() {
		return _tm;
	}

	@Override
	public boolean canFastConfirm() {
		return false;
	}
	
	@Override
	public TMulticastTypes getType() {
		return _tm.getType();
	}
}