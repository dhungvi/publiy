package org.msrg.publiy.utils.log.trafficcorrelator;

import org.msrg.publiy.pubsub.core.messagequeue.PSSession;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimerReading;

import org.msrg.publiy.communication.core.niobinding.ConnectionInfoNonListeningStatus;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.communication.core.sessions.SessionTypes;

abstract public class TrafficCorrelationEvent {
	
	private static int _event_counter = 0;
	protected final int _event_index;
	
	protected final BrokerInternalTimerReading _creationTime;
	protected final TrafficCorrelationEventType _type;
	protected final long _time;
	
	protected TrafficCorrelationEvent(TrafficCorrelationEventType type) {
		_type = type;
		_time = SystemTime.currentTimeMillis();
		_event_index = _event_counter++;
		_creationTime = BrokerInternalTimer.read();
	}
	
	@Override
	public int hashCode() {
		return _type.hashCode();
	}
	
	public int getEventIndex() {
		return _event_index;
	}
	
	public boolean lessThan(TrafficCorrelationEvent event) {
		return _event_index < event._event_index;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !obj.getClass().isInstance(this) )
			return false;
		
		TrafficCorrelationEvent event = (TrafficCorrelationEvent) obj;
		return _type == event._type;
	}

	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ")";
	}
};

class CorrelationEvent_MQNCreated extends TrafficCorrelationEvent {

	final Sequence _localSequence;
	
	protected CorrelationEvent_MQNCreated(Sequence localSequence) {
		this(TrafficCorrelationEventType.MQ_MQN_CREATED, localSequence);
	}
	
	protected CorrelationEvent_MQNCreated(TrafficCorrelationEventType type, Sequence localSequence) {
		super(type);
		
		_localSequence = localSequence;
	}

	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + "@" + _localSequence + ")";
	}
};

class CorrelationEvent_MQNConfCreated extends CorrelationEvent_MQNCreated {

	protected CorrelationEvent_MQNConfCreated(Sequence localSequence) {
		super(TrafficCorrelationEventType.MQ_MQN_CONF_CREATE, localSequence);
	}
};

class CorrelationEvent_End extends TrafficCorrelationEvent {

	protected CorrelationEvent_End() {
		super(TrafficCorrelationEventType.END);
	}
};

class CorrelationEvent_ComputeWorkingSet extends TrafficCorrelationEvent {

	protected CorrelationEvent_ComputeWorkingSet() {
		super(TrafficCorrelationEventType.MQ_COMPUTE_WORKING_SET);
	}
};

abstract class AbstractSessionsCorrelationEvent extends TrafficCorrelationEvent {

	protected AbstractSessionsCorrelationEvent(TrafficCorrelationEventType type) {
		super(type);
	}
};

class SessionsCorrelationEvent_SessionConnectionSet extends SessionsCorrelationEvent {
	
	final IConInfoNonListening _conInfoNL;
	
	protected SessionsCorrelationEvent_SessionConnectionSet(ISession session, IConInfoNonListening conInfoNL) {
		this(TrafficCorrelationEventType.S_CONNECTION_SET, session, conInfoNL);
	}
	
	protected SessionsCorrelationEvent_SessionConnectionSet(TrafficCorrelationEventType eventType, ISession session, IConInfoNonListening conInfoNL) {
		super(eventType, session);
		_conInfoNL = conInfoNL;
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + "{" + _session + ">>>" + _conInfoNL + "})";
	}
}

class SessionsCorrelationEvent_FDConnectionTimedOut extends ConnectionsCorrelationEvent {
	
	protected SessionsCorrelationEvent_FDConnectionTimedOut(IConInfoNonListening conInfoNL) {
		super(TrafficCorrelationEventType.FD_CONNECTION_TIMEOUT, conInfoNL);
	}
}

class SessionsCorrelationEvent_ConnectionCreated extends ConnectionsCorrelationEvent {
	
	protected SessionsCorrelationEvent_ConnectionCreated(IConInfoNonListening conInfoNL) {
		super(TrafficCorrelationEventType.CONNECTION_CREATED, conInfoNL);
	}
}

class SessionsCorrelationEvent_ConnectionStateChanged extends ConnectionsCorrelationEvent {
	
	final ConnectionInfoNonListeningStatus _newStatus;
	final ConnectionInfoNonListeningStatus _oldStatus;
	
	protected SessionsCorrelationEvent_ConnectionStateChanged(IConInfoNonListening conInfoNL, ConnectionInfoNonListeningStatus oldStatus, ConnectionInfoNonListeningStatus newStatus) {
		super(TrafficCorrelationEventType.CONNECTION_STATE_CHANGED, conInfoNL);
		_oldStatus = oldStatus;
		_newStatus = newStatus;
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + "{" + _oldStatus + "->" + _newStatus + "})";
	}
}

class SessionsCorrelationEvent_ConnectionDiscarded extends ConnectionsCorrelationEvent {
	
	protected SessionsCorrelationEvent_ConnectionDiscarded(IConInfoNonListening conInfoNL) {
		super(TrafficCorrelationEventType.CONNECTION_DISCARDED, conInfoNL);
	}
}

abstract class PSSessionsCorrelationEvent extends AbstractSessionsCorrelationEvent {

	protected final PSSession _psSession;
	protected final String _toStringShort;
	
	protected PSSessionsCorrelationEvent(TrafficCorrelationEventType type,
			PSSession psSession) {
		super(type);
		_psSession = psSession;
		_toStringShort = ""; // _psSession.lastProcessedMQNodeString();
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":PSSession<<" + _psSession.getRemoteAddress().getPort() + ":" + _toStringShort + ">>)";
	}
};

class SessionsCorrelationEvent_PSSessionAdded extends PSSessionsCorrelationEvent {

	protected SessionsCorrelationEvent_PSSessionAdded(PSSession psSession) {
		super(TrafficCorrelationEventType.PSS_ADD, psSession);
	}
};

class SessionsCorrelationEvent_PSSessionReset extends PSSessionsCorrelationEvent {

	protected SessionsCorrelationEvent_PSSessionReset(PSSession psSession) {
		super(TrafficCorrelationEventType.PSS_RSET, psSession);
	}
};

class SessionsCorrelationEvent_PSSessionRemoved extends PSSessionsCorrelationEvent {

	protected SessionsCorrelationEvent_PSSessionRemoved(PSSession psSession) {
		super(TrafficCorrelationEventType.PSS_RMV, psSession);
	}
};

abstract class SessionsCorrelationEvent extends AbstractSessionsCorrelationEvent {
	protected final ISession _session;
	
	protected SessionsCorrelationEvent(TrafficCorrelationEventType type,
			ISession newSession) {
		super(type);
		
		_session = newSession;
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":" + _session.toStringVeryShort() + ")";
	}
};

abstract class ConnectionsCorrelationEvent extends AbstractSessionsCorrelationEvent {
	protected final IConInfoNonListening _conInfoNL;
	protected final ISession _session;
	
	protected ConnectionsCorrelationEvent(TrafficCorrelationEventType type,
			IConInfoNonListening conInfoNL) {
		super(type);
		
		_conInfoNL = conInfoNL;
		_session = _conInfoNL.getSession();
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":" + _conInfoNL + ")";
	}
};

class SessionsCorrelationEvent_SessionRenewed extends SessionsCorrelationEvent {
	SessionsCorrelationEvent_SessionRenewed(ISession session) {
		super(TrafficCorrelationEventType.S_RENEW, session);
	}
}

class SessionsCorrelationEvent_SessionConnectionTyped extends SessionsCorrelationEvent {

	protected final SessionConnectionType _newType;
	
	protected SessionsCorrelationEvent_SessionConnectionTyped(ISession session, SessionConnectionType newType) {
		super(TrafficCorrelationEventType.S_CONNECTION_TYPE, session);
		
		_newType = newType;
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":" + _session.toStringVeryShort() + "=>" + _newType + ")";
	}
};


class SessionsCorrelationEvent_SessionLastreceivedSequence extends SessionsCorrelationEvent {

	final Sequence _reportedSequence;
	
	protected SessionsCorrelationEvent_SessionLastreceivedSequence(ISession session, Sequence reportedSequence) {
		super(TrafficCorrelationEventType.S_LST_RCVD, session);
		_reportedSequence = reportedSequence;
	}

	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":" + _session.toStringVeryShort() + "~~~" + _reportedSequence + ")";
	}
	
};

class SessionsCorrelationEvent_SessionTyped extends SessionsCorrelationEvent {

	protected final SessionTypes _newType;
	
	protected SessionsCorrelationEvent_SessionTyped(ISession session, SessionTypes newType) {
		super(TrafficCorrelationEventType.S_TYPE, session);
		
		_newType = newType;
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":" + _session.toStringVeryShort() + "=>" + _newType + ")";
	}
};

class SessionsCorrelationEvent_SessionRegistered extends SessionsCorrelationEvent {

	final boolean _mp;
	
	protected SessionsCorrelationEvent_SessionRegistered(ISession session, boolean mp) {
		super(TrafficCorrelationEventType.S_REG, session);
		_mp = mp;
	}

	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + (_mp?"_MP":"") + ":" + _session.toStringVeryShort() + ")";
	}
};

class SessionsCorrelationEvent_DuplicateDetected extends SessionsCorrelationEvent {

	protected SessionsCorrelationEvent_DuplicateDetected(ISession session) {
		super(TrafficCorrelationEventType.MQ_DUPLICATE, session);
	}
};

class SessionsCorrelationEvent_SessionAdded extends SessionsCorrelationEvent {

	protected SessionsCorrelationEvent_SessionAdded(ISession session) {
		super(TrafficCorrelationEventType.S_ADD, session);
	}
};

class SessionsCorrelationEvent_SessionDropped extends SessionsCorrelationEvent {

	protected SessionsCorrelationEvent_SessionDropped(ISession session) {
		super(TrafficCorrelationEventType.S_DROP, session);
	}
};

class SessionsCorrelationEvent_SessionSubstituted extends SessionsCorrelationEvent {

	final ISession _newSession;
	
	protected SessionsCorrelationEvent_SessionSubstituted(ISession oldSession, ISession newSession) {
		this(TrafficCorrelationEventType.S_SUBSTITUTE, oldSession, newSession);
	}
	
	protected SessionsCorrelationEvent_SessionSubstituted(TrafficCorrelationEventType type, ISession oldSession, ISession newSession) {
		super(type, oldSession);
		_newSession = newSession;
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":" + _session.toStringVeryShort() + "=>" + _newSession.toStringVeryShort() + ")";
	}
};

class SessionsCorrelationEvent_SetRealSession extends SessionsCorrelationEvent_SessionSubstituted {
	
	final ISession _changedSession;
	
	protected SessionsCorrelationEvent_SetRealSession(ISession session, ISession oldRealSession, ISession newRealSession) {
		super(TrafficCorrelationEventType.S_REAL_SESSION, oldRealSession, newRealSession);
		_changedSession = session;
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":" + _changedSession.toStringVeryShort() + "{" + _session.toStringVeryShort() + "=>" + _newSession.toStringVeryShort() + "})";
	}
};

class SessionsCorrelationEvent_SessionTransfers extends SessionsCorrelationEvent_SessionSubstituted {
	protected SessionsCorrelationEvent_SessionTransfers(ISession oldSession, ISession newSession) {
		super(TrafficCorrelationEventType.S_TM_TRANSFER, oldSession, newSession);
	}
}

class SessionsCorrelationEvent_SessionRemoved extends SessionsCorrelationEvent {

	protected SessionsCorrelationEvent_SessionRemoved(ISession session) {
		super(TrafficCorrelationEventType.S_RMV, session);
	}
};

class TrafficCorrelationEvent_Duplicate extends TrafficCorrelationEvent {
	private final Sequence _currentArrivalSeq;
	
	protected TrafficCorrelationEvent_Duplicate(Sequence currentArrivalSeq) {
		super(TrafficCorrelationEventType.DUPLICATE);
		
		_currentArrivalSeq = currentArrivalSeq;
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":" + _currentArrivalSeq + ")";
	}
};

class TrafficCorrelationEvent_Confirmed extends TrafficCorrelationEvent {
	protected TrafficCorrelationEvent_Confirmed() {
		super(TrafficCorrelationEventType.CONFIRMED);
	}
};

class TrafficCorrelationEvent_Acknowledged extends TrafficCorrelationEvent {
	protected TrafficCorrelationEvent_Acknowledged() {
		super(TrafficCorrelationEventType.ACKNOWLEDGED);
	}
};

abstract class TrafficCorrelationEvent_Session extends TrafficCorrelationEvent {
	
	protected final ISession _session;

	protected TrafficCorrelationEvent_Session(TrafficCorrelationEventType type, ISession session) {
		super(type);
		
		_session = session;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() + _session.getRemoteAddress().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( !super.equals(obj) )
			return false;
		
		if ( !obj.getClass().isInstance(this) )
			return false;
		
		TrafficCorrelationEvent_Session eventArrived = (TrafficCorrelationEvent_Session) obj;
		return _session.getRemoteAddress().equals(eventArrived);
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":" + _session.toStringVeryShort() + ")";
	}
};

class TrafficCorrelationEvent_Arrived extends TrafficCorrelationEvent_Session {
	TrafficCorrelationEvent_Arrived(ISession session) {
		super(TrafficCorrelationEventType.ARRIVED_FROM, session);
	}
};

class TrafficCorrelationEvent_ConfArrived extends TrafficCorrelationEvent_Session {
	TrafficCorrelationEvent_ConfArrived(ISession session) {
		super(TrafficCorrelationEventType.CONF_ARRIVED_FROM, session);
	}
};

abstract class TrafficCorrelationEvent_OutPending extends TrafficCorrelationEvent_Session {
	TrafficCorrelationEvent_OutPending(TrafficCorrelationEventType type, ISession session) {
		super(type, session);
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":" + _session.toStringVeryShort() + ")";
	}
};

abstract class ConnectionsCorrelationEvent_tmType extends ConnectionsCorrelationEvent {
	final boolean _conf;
	
	ConnectionsCorrelationEvent_tmType(TrafficCorrelationEventType type, IConInfoNonListening conInfoNL, boolean conf) {
		super(type, conInfoNL);
		_conf = conf;
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + (_conf?"_CONF:":":") + _session.toStringVeryShort() + ")";
	}
}

class TrafficCorrelationEvent_ConnectionInPendingEnqueue extends ConnectionsCorrelationEvent_tmType {
	TrafficCorrelationEvent_ConnectionInPendingEnqueue(IConInfoNonListening conInfoNL, boolean conf) {
		super(TrafficCorrelationEventType.C_IN_PENDING_ENQ, conInfoNL, conf);
	}
};

class TrafficCorrelationEvent_ConnectionInPendingDequeue extends ConnectionsCorrelationEvent_tmType {
	TrafficCorrelationEvent_ConnectionInPendingDequeue(IConInfoNonListening conInfoNL, boolean conf) {
		super(TrafficCorrelationEventType.C_IN_PENDING_DEQ, conInfoNL, conf);
	}
};

class TrafficCorrelationEvent_ConnectionOutPendingEnqueue extends ConnectionsCorrelationEvent_tmType {
	TrafficCorrelationEvent_ConnectionOutPendingEnqueue(IConInfoNonListening conInfoNL, boolean conf) {
		super(TrafficCorrelationEventType.C_OUT_PENDING_ENQ, conInfoNL, conf);
	}
};

class TrafficCorrelationEvent_ConnectionOutPendingDequeue extends ConnectionsCorrelationEvent_tmType {
	TrafficCorrelationEvent_ConnectionOutPendingDequeue(IConInfoNonListening conInfoNL, boolean conf) {
		super(TrafficCorrelationEventType.C_OUT_PENDING_DEQ, conInfoNL, conf);
	}
};

class TrafficCorrelationEvent_SessionOutPendingEnqueue extends TrafficCorrelationEvent_OutPending {
	TrafficCorrelationEvent_SessionOutPendingEnqueue(ISession session) {
		super(TrafficCorrelationEventType.S_OUT_PENDING_ENQ, session);
	}
};

class TrafficCorrelationEvent_OutPendingDequeue extends TrafficCorrelationEvent_OutPending {
	TrafficCorrelationEvent_OutPendingDequeue(ISession session) {
		super(TrafficCorrelationEventType.S_OUT_PENDING_DEQ, session);
	}
};

class TrafficCorrelationEvent_Sent extends TrafficCorrelationEvent_Session {
	TrafficCorrelationEvent_Sent(ISession session) {
		super(TrafficCorrelationEventType.SENT_TO, session);
	}
};

class TrafficCorrelationEvent_Lost extends TrafficCorrelationEvent_Session {
	TrafficCorrelationEvent_Lost(ISession session) {
		super(TrafficCorrelationEventType.LOST, session);
	}
};

class TrafficCorrelationEvent_ConfSent extends TrafficCorrelationEvent_Session {
	TrafficCorrelationEvent_ConfSent(ISession session) {
		super(TrafficCorrelationEventType.CONF_SENT_TO, session);
	}
};

abstract class TrafficCorrelationEvent_MQ extends TrafficCorrelationEvent_Session {
	final char _action;
	final int _pssessionsSize;
	
	protected TrafficCorrelationEvent_MQ(ISession session, boolean confirmationMQN, char action, int pssessionsSize) {
		super((confirmationMQN?TrafficCorrelationEventType.MQ_PROC_CONF:TrafficCorrelationEventType.MQ_PROC),
				session);
		_action = action;
		_pssessionsSize = pssessionsSize;
	}
	
	@Override
	public String toString() {
		return "(E#" + _event_index + "@" + _creationTime + ":" + _type + ":%" + _pssessionsSize + "%" + _session.toStringVeryShort() + _action + ")";
	}
};

class TrafficCorrelationEvent_MQCancel extends TrafficCorrelationEvent_MQ {
	protected TrafficCorrelationEvent_MQCancel(ISession session, boolean confirmationMQN, int pssessionsSize) {
		super(session, confirmationMQN, '-', pssessionsSize);
	}
};

class TrafficCorrelationEvent_MQCheckout extends TrafficCorrelationEvent_MQ {
	protected TrafficCorrelationEvent_MQCheckout(ISession session, boolean confirmationMQN, int pssessionsSize) {
		super(session, confirmationMQN, '*', pssessionsSize);
	}
};

class TrafficCorrelationEvent_MQVisit extends TrafficCorrelationEvent_MQ {
	protected TrafficCorrelationEvent_MQVisit(ISession session, boolean confirmationMQN, int pssessionsSize) {
		
		super(session, confirmationMQN, '+', pssessionsSize);
	}
};

class TrafficCorrelationEvent_MQConfInserted extends TrafficCorrelationEvent_MQ {
	protected TrafficCorrelationEvent_MQConfInserted (ISession session) {
		super(session, false, '~', -1);
	}
};

class CorrelationEvent_CriticalUpdate extends AbstractSessionsCorrelationEvent {
	CorrelationEvent_CriticalUpdate() {
		super(TrafficCorrelationEventType.CRITICAL_UPDATE);
	}
}