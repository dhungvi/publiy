package org.msrg.publiy.utils.log.trafficcorrelator;

import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.niobinding.ConnectionInfoNonListeningStatus;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.communication.core.sessions.SessionTypes;

import org.msrg.publiy.pubsub.core.ITimestamp;
import org.msrg.publiy.pubsub.core.messagequeue.PSSession;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationDumpSpecifier;
import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationMessageDumpSpecifier;

public class TrafficCorrelator implements ILoggerSource {

	static final boolean CLEAR_CONFIRMED = false;
	static final boolean CLEAR_ACKED = false;
	static final boolean MERGE = false;
	static final boolean CORRELATE_TM = true;
	
	private static TrafficCorrelator _instance = new TrafficCorrelator();
	private Map<Sequence, TrafficCorrelation> _correlationsSet = new HashMap<Sequence, TrafficCorrelation>();
	private List<AbstractSessionsCorrelationEvent> _sessionsEvent = new LinkedList<AbstractSessionsCorrelationEvent>();
	
	public static TrafficCorrelator getInstance() {
		return _instance;
	}

	private TrafficCorrelation getCorrelationPrivately(Sequence tmSourceSequence) {
		TrafficCorrelation correlation = _correlationsSet.get(tmSourceSequence);
		if ( correlation == null ) {
			correlation = new TrafficCorrelation(tmSourceSequence);
			_correlationsSet.put(tmSourceSequence, correlation);
		}
		
		return correlation;
	}
	
	private TrafficCorrelation getCorrelationPrivately(TMulticast tm) {
		Sequence seq = tm.getSourceSequence();
		return getCorrelationPrivately(seq);
	}
	
	public void sessionConnectionSet(ISession session, IConInfoNonListening conInfoNL) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			SessionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_SessionConnectionSet(session, conInfoNL);
			_sessionsEvent.add(sessionEvent);
		}
	}
	
	public void duplicateDetected(ISession session, TMulticast tm) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent duplicateEvent = 
							new SessionsCorrelationEvent_DuplicateDetected(session);
			correlation.addCorrelationEvent(duplicateEvent);
		}
	}

	public void sessionOutEnqueue(ISession session, TMulticast tm) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent outEnqueueEvent = 
							new TrafficCorrelationEvent_SessionOutPendingEnqueue(session);
			correlation.addCorrelationEvent(outEnqueueEvent);
		}
	}
	
	public void substituteSessions(ISession oldSession, ISession newSession) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			SessionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_SessionSubstituted(oldSession, newSession);
			_sessionsEvent.add(sessionEvent);
		}
	}

	public void setRealSession(ISession session, ISession oldRealSession, ISession newRealSession) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			SessionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_SetRealSession(session, oldRealSession, newRealSession);
			_sessionsEvent.add(sessionEvent);
		}
	}
	
	public void psSessionAdded(PSSession psSession) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			AbstractSessionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_PSSessionAdded(psSession);
			_sessionsEvent.add(sessionEvent);
		}
	}
	
	public void psSessionRemoved(PSSession psSession) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			AbstractSessionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_PSSessionRemoved(psSession);
			_sessionsEvent.add(sessionEvent);
		}
	}
	public void resetPSSession(PSSession psSession) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			AbstractSessionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_PSSessionReset(psSession);
			_sessionsEvent.add(sessionEvent);
		}
	}
	
	public void sessionAdded(ISession session) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			SessionsCorrelationEvent sessionEvent = 
							new SessionsCorrelationEvent_SessionAdded(session);
			_sessionsEvent.add(sessionEvent);
		}
	}

	public void sessionDropped(ISession session) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			SessionsCorrelationEvent sessionEvent = 
							new SessionsCorrelationEvent_SessionDropped(session);
			_sessionsEvent.add(sessionEvent);
		}
	}

	public void sessionRemoved(ISession session) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			SessionsCorrelationEvent sessionEvent = 
							new SessionsCorrelationEvent_SessionRemoved(session);
			_sessionsEvent.add(sessionEvent);
		}
	}
	
	public void connectionOutEnqueue(IConInfoNonListening conInfoNL, TMulticast tm) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent outEnqueueEvent = 
							new TrafficCorrelationEvent_ConnectionOutPendingEnqueue(conInfoNL, tm.getType()==TMulticastTypes.T_MULTICAST_CONF);
			correlation.addCorrelationEvent(outEnqueueEvent);
		}
	}
	
	public void connectionOutDequeue(IConInfoNonListening conInfoNL, TMulticast tm) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent outEnqueueEvent = 
							new TrafficCorrelationEvent_ConnectionOutPendingDequeue(conInfoNL, tm.getType()==TMulticastTypes.T_MULTICAST_CONF);
			correlation.addCorrelationEvent(outEnqueueEvent);
		}
	}
	
	public void sessionOutDequeue(ISession session, TMulticast tm) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent outEnqueueEvent = 
							new TrafficCorrelationEvent_OutPendingDequeue(session);
			correlation.addCorrelationEvent(outEnqueueEvent);
		}
	}
	
	public void duplicate(ITimestamp arrivalTimestamp, TMulticast tm) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;

		InetSocketAddress remote = tm.getSourceAddress();
		Sequence currentArrivalSeq = arrivalTimestamp.getLastReceivedSequence(remote);
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent duplicateEvent = new TrafficCorrelationEvent_Duplicate(currentArrivalSeq);
			correlation.addCorrelationEvent(duplicateEvent);
		}
	}
	
	public void messageConfirmed(TMulticast tm) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			if (tm.getType() == TMulticastTypes.T_MULTICAST_CONF)
				correlation.acknowledged();
			else
				correlation.confirmed();
		}
	}
	
	public void messageArrived(TMulticast tm, ISession session) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			if (!Broker.RELEASE)
				System.out.println("ARRIVED_FROM:" + session.toStringWithConInfoNL() + ":" + tm.getSourceSequence() + ":" + tm.getType());
			
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent_Session arrivedEvent = 
				(tm.getType() == TMulticastTypes.T_MULTICAST_CONF?
						new TrafficCorrelationEvent_ConfArrived(session):
							new TrafficCorrelationEvent_Arrived(session));
			correlation.addCorrelationEvent(arrivedEvent);
		}
	}
	
	public void connectionInDequeue(TMulticast tm, IConInfoNonListening conInfoNL) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent arrivedEvent = 
							new TrafficCorrelationEvent_ConnectionInPendingDequeue(conInfoNL, tm.getType()==TMulticastTypes.T_MULTICAST_CONF);
			correlation.addCorrelationEvent(arrivedEvent);
		}
	}

	public void connectionInEnqueue(TMulticast tm, IConInfoNonListening conInfoNL) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent arrivedEvent = 
							new TrafficCorrelationEvent_ConnectionInPendingEnqueue(conInfoNL, tm.getType()==TMulticastTypes.T_MULTICAST_CONF);
			correlation.addCorrelationEvent(arrivedEvent);
		}
	}

	public void messageLost(TMulticast tm, ISession session) {
		System.out.println("MSG LOST: " + (tm==null?null:tm.getSourceSequence())  + " on " + session);
		
		if (!Broker.CORRELATE || tm == null)
			return;

		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent_Session arrivedEvent = new TrafficCorrelationEvent_Lost(session);
			correlation.addCorrelationEvent(arrivedEvent);
		}
	}
	
	public void messageTransferred(TMulticast tm, ISession oldSession, ISession newSession) {
		System.out.println("MSG TRANSFERRED: " + (tm==null?null:tm.getSourceSequence())  + " on " + oldSession + " _ " + newSession);
		
		if (!Broker.CORRELATE || tm == null)
			return;

		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			SessionsCorrelationEvent arrivedEvent =
				new SessionsCorrelationEvent_SessionTransfers(oldSession, newSession);
			correlation.addCorrelationEvent(arrivedEvent);
		}
	}
	
	public void messageSent(TMulticast tm, ISession session) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent_Session sentEvent = 
				(tm.getType() == TMulticastTypes.T_MULTICAST_CONF ?
						new TrafficCorrelationEvent_ConfSent(session):
							new TrafficCorrelationEvent_Sent(session));
			correlation.addCorrelationEvent(sentEvent);
		}
	}

	public void messageForked(TMulticast orgTm, TMulticast newTm) {
		synchronized (_correlationsSet) {
			throw new UnsupportedOperationException();
		}
	}
	
	public void mqnCreated(TMulticast tm, Sequence localSequence) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			
			TrafficCorrelationEvent event = null;
			if(tm.getType() == TMulticastTypes.T_MULTICAST_CONF)
				event = new CorrelationEvent_MQNConfCreated(localSequence);
			else
				event = new CorrelationEvent_MQNCreated(localSequence);
			
			correlation.addCorrelationEvent(event);
		}
	}
	
	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_TRAFFIC_CORRELATOR;
	}

	public void sessionCheckedout(ISession session, TMulticast tm, int pssessionsSize) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent_MQCheckout event = new TrafficCorrelationEvent_MQCheckout(session, tm.getType() == TMulticastTypes.T_MULTICAST_CONF, pssessionsSize);
			correlation.addCorrelationEvent(event);
		}
	}
	
	public void sessionVisited(ISession session, TMulticast tm, int pssessionsSize) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent_MQVisit event = new TrafficCorrelationEvent_MQVisit(session, tm.getType() == TMulticastTypes.T_MULTICAST_CONF, pssessionsSize);
			correlation.addCorrelationEvent(event);
		}
	}
	
	public void sessionCancelled(ISession session, TMulticast tm, int pssessionsSize) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent_MQCancel event = new TrafficCorrelationEvent_MQCancel (session, tm.getType() == TMulticastTypes.T_MULTICAST_CONF, pssessionsSize);
			correlation.addCorrelationEvent(event);
		}
	}

	public void workingSetComputed(TMulticast tm) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tm);
			TrafficCorrelationEvent event = new CorrelationEvent_ComputeWorkingSet();
			correlation.addCorrelationEvent(event);
		}
	}
	
	public void confirmationInsertedIntoMQ(ISession session, TMulticast_Conf tmc) {
		if (!Broker.CORRELATE || !CORRELATE_TM)
			return;
		
		synchronized (_correlationsSet) {
			TrafficCorrelation correlation = getCorrelationPrivately(tmc);
			TrafficCorrelationEvent event = new TrafficCorrelationEvent_MQConfInserted (session);
			correlation.addCorrelationEvent(event);
		}
	}

	protected void dumpPrivately(TrafficCorrelation correlation, boolean merge, Writer ioWriter) throws IOException {
		if (correlation == null) {
			ioWriter.append("ERROR_EMPTY_CORREALTION");
			return;
		}
		
		if (merge)
			ioWriter.append(correlation.merge(_sessionsEvent, false));
		else
			ioWriter.append(correlation.toString());
	}
	
	public void renewingSession(ISession session) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			SessionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_SessionRenewed(session);
			_sessionsEvent.add(sessionEvent);
		}
	}
	
	public void connectionTimedOut(IConInfoNonListening conInfoNL) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			ConnectionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_FDConnectionTimedOut(conInfoNL);
			_sessionsEvent.add(sessionEvent);
		}
	}
	
	public void dump(TrafficCorrelationDumpSpecifier dumpSpecifier, boolean logOutput) {
		if (!Broker.CORRELATE)
			return;

		Writer ioWriter = dumpSpecifier.getWriter();
		String comment = dumpSpecifier._comment;
		boolean merge = dumpSpecifier._merge;
		try {
			ioWriter.append(comment);
			switch(dumpSpecifier._type) {
			case DUMP_ONE:
				dumpSessionEvents(ioWriter);
				TrafficCorrelationMessageDumpSpecifier msgDumpSpec = (TrafficCorrelationMessageDumpSpecifier) dumpSpecifier;
				Sequence tmSourceSequence = msgDumpSpec._tmSourceSequence;
				synchronized (_correlationsSet) {
					TrafficCorrelation correlation = getCorrelationPrivately(tmSourceSequence);
					dumpPrivately(correlation, merge, ioWriter);
				}
				break;
				
			case DUMP_ALL:
				break;
				
			case DUMP_SESSIONS_EVENTS:
				dumpSessionEvents(ioWriter);
				break;
				
			default:
				throw new UnsupportedOperationException("" + dumpSpecifier);
			}
			ioWriter.append("\n");
		} catch (IOException iox) {
			iox.printStackTrace();
		}
		
		if(logOutput)
			LoggerFactory.getLogger().info(this, ioWriter.toString());
	}
	
	private void dumpSessionEvents(Writer ioWriter) throws IOException {
		ioWriter.append("CorrelationSessionsEvents: ");
		for(AbstractSessionsCorrelationEvent sessionEvent : _sessionsEvent)
			ioWriter.append("\t" + sessionEvent);
	}
	
	protected void dumpAllCorrelationsPrivately(boolean merge, Writer ioWriter) throws IOException {
		for(TrafficCorrelation correlation : _correlationsSet.values())
			dumpPrivately(correlation, merge, ioWriter);
	}
	
	public void sessionConnectionType(ISession session, SessionConnectionType newType) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			SessionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_SessionConnectionTyped(session, newType);
			_sessionsEvent.add(sessionEvent);
		}		
	}

	public void sessionType(ISession session, SessionTypes newType) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			SessionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_SessionTyped(session, newType);
			_sessionsEvent.add(sessionEvent);
		}		
	}

	public void sessionReportsLastReceived(ISession session, Sequence seq) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			SessionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_SessionLastreceivedSequence(session, seq);
			_sessionsEvent.add(sessionEvent);
		}
	}
	
	public void sessionRegistered(ISession newSession, boolean mp) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			SessionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_SessionRegistered(newSession, mp);
			_sessionsEvent.add(sessionEvent);
		}
	}

	public void conInfoNLCreated(IConInfoNonListening conInfoNonListening, ISession session) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			ConnectionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_ConnectionCreated(conInfoNonListening);
			_sessionsEvent.add(sessionEvent);
		}
	}
	
	public void conInfoNLStateChanged(IConInfoNonListening conInfoNonListening, ConnectionInfoNonListeningStatus oldStatus, ConnectionInfoNonListeningStatus newStatus) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			ConnectionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_ConnectionStateChanged(conInfoNonListening, oldStatus, newStatus);
			_sessionsEvent.add(sessionEvent);
		}
	}
	
	public void conInfoNLDiscarded(IConInfoNonListening conInfoNonListening, ISession session) {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			ConnectionsCorrelationEvent sessionEvent = 
				new SessionsCorrelationEvent_ConnectionDiscarded(conInfoNonListening);
			_sessionsEvent.add(sessionEvent);
		}
	}

	public void updateCriticalSessions() {
		if (!Broker.CORRELATE)
			return;
		
		synchronized (_correlationsSet) {
			AbstractSessionsCorrelationEvent sessionEvent = new CorrelationEvent_CriticalUpdate();
			_sessionsEvent.add(sessionEvent);
		}
	}
}
