package org.msrg.publiy.communication.core.sessions;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.niobinding.DestroyConnectionTask;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.niobinding.INIOBinding;
import org.msrg.publiy.communication.core.niobinding.SessionRenewTask;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.communication.core.packet.types.SessionInitiationTypes;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastAnnotatorFactory;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.recovery.TempSessionRecoveryDataRepository;
import org.msrg.publiy.utils.annotations.AnnotationEvent;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;
import org.msrg.publiy.utils.occurance.EventOccuranceFactory;
import org.msrg.publiy.utils.occurance.IEventOccurance;

public abstract class ISessionManager implements ILoggerSource {
	
	public static final int MAX_RETRY_DELAY = 10000;
	public static final int MAX_RETRY_COUNT = 12;
	public static final int RETRY_DELAY = 1000;
	public static final int DROP_CONNECTION_DELAY = 200000;
	
	private static int _orderedSeqeuence = 0;

	protected final InetSocketAddress _localAddress;
	protected final LocalSequencer _localSequencer;
	protected INIOBinding _nioBinding;
	protected final IConnectionManager _connectionManager;

	protected ISessionManager(LocalSequencer localSequencer, INIOBinding nioBinding) {
		_localSequencer = localSequencer;
		_localAddress = _localSequencer.getLocalAddress();		
		_nioBinding = nioBinding;
		_connectionManager = null;
	}
	
	protected ISessionManager(LocalSequencer localSequencer, INIOBinding nioBinding, IConnectionManager connectionManager) {
		_localSequencer = localSequencer;
		_localAddress = _localSequencer.getLocalAddress();		
		_nioBinding = nioBinding;
		_connectionManager = connectionManager;
		if(_connectionManager == null)
			throw new NullPointerException();
	}
	
	public static ISession attachUnknownPeerSession(IConInfoNonListening<?> conInfoNL, boolean withTempRepositoryObject) {
		LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_ISESSIONMANAGER, "attachUnknownPeerSession(.) - " + conInfoNL);
		ISession session = conInfoNL.getSession(); //new ISession(SessionTypes.ST_PEER_UNKNOWN, conInfoNL);//, _localSequencer.getNext());
		ISessionManager.setType(session, SessionTypes.ST_PEER_UNKNOWN);
		if(withTempRepositoryObject)
			session._tempRepositoryObject = new TempSessionRecoveryDataRepository(session);
		return session;
	}

	public ISession attachPubSubSession(IConInfoNonListening<?> conInfoNL, boolean withTempRepositoryObject) {
		LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_ISESSIONMANAGER, "attachPubSubSession(.) - " + conInfoNL);
		ISession session = conInfoNL.getSession(); //new ISession(SessionTypes.ST_PEER_UNKNOWN, conInfoNL);//, _localSequencer.getNext());
		ISessionManager.setType(session, SessionTypes.ST_PUBSUB_INITIATED);
		if(withTempRepositoryObject)
			session._tempRepositoryObject = new TempSessionRecoveryDataRepository(session);
		return session;
	}
	
	public ISession attachRecoverySession(IConInfoNonListening<?> conInfoNL) {
		LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_ISESSIONMANAGER, "attachRecoverySession(.) - " + conInfoNL);
		ISession session = conInfoNL.getSession(); //new ISession(SessionTypes.ST_PEER_UNKNOWN, conInfoNL);//, _localSequencer.getNext());
		ISessionManager.setType(session, SessionTypes.ST_RECOVERY_INITIATED);
		session._tempRepositoryObject = new TempSessionRecoveryDataRepository(session);
		return session;
	}

	public static ISession attachSession(IBrokerShadow brokerShadow, ISession session, IConInfoNonListening<?> conInfoNL) {
		LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_ISESSIONMANAGER, "attachSession(.) - " + conInfoNL);
//		if(session == null)
//			session = ISessionManager.getUnknownPeerSession(brokerShadow);
		
		session.setIConInfo(conInfoNL);
		
		return session;
	}		
	
	public static ISession createBaseSession(IBrokerShadow brokerShadow, SessionTypes type) {
		if(brokerShadow.isMP())
			return new ISessionMP(brokerShadow, SessionObjectTypes.ISESSION_MP, type);

		return new ISession(brokerShadow, SessionObjectTypes.ISESSION, type);
	}
	
	public static ISession getPubSubSession(IBrokerShadow brokerShadow, InetSocketAddress remote, boolean withTempRepositoryObject) {
		ISession session = createBaseSession(brokerShadow, SessionTypes.ST_PUBSUB_INITIATED);//, _localSequencer.getNext());
		session.setRemoteCC(remote);
		if(withTempRepositoryObject)
			session._tempRepositoryObject = new TempSessionRecoveryDataRepository(session);
		return session;
	}

	public static ISession getUnknownPeerSession(IBrokerShadow brokerShadow) {
		ISession session = createBaseSession(brokerShadow, SessionTypes.ST_PEER_UNKNOWN);
		session._tempRepositoryObject = new TempSessionRecoveryDataRepository(session);
		return session;
	}
	
	public static ISession getRecoverySession(IBrokerShadow brokerShadow, InetSocketAddress remote) {
		ISession session = createBaseSession(brokerShadow, SessionTypes.ST_RECOVERY_INITIATED);
		session.setRemoteCC(remote);
		session._tempRepositoryObject = new TempSessionRecoveryDataRepository(session);
		return session;
	}

	public boolean send(ISession session, IRawPacket raw) {
		if(raw == null)
			return true;
		
		if(raw.getType() == PacketableTypes.TMULTICAST) {
			TMulticast tm = ((TMulticast)raw.getObject());
			TMulticastTypes tmType = tm.getType();
			switch(tmType) {
			// Only types that are bandwidth regulated appear here.
			case T_MULTICAST_PUBLICATION:
			case T_MULTICAST_PUBLICATION_MP:
			case T_MULTICAST_PUBLICATION_NC:
				if(!_connectionManager.checkOutputBWAvailability())
					return true;
				_connectionManager.getBWEnforcer().addOutgoingMessage(raw.getSize());
				break;
				
			default:
				break;
			}
		}
		
		IBrokerShadow brokerShadow = session._brokerShadow;
		ISession realSession = session.getRealSession(raw);
		if(realSession == null)
			throw new NullPointerException("Session: " + session + " " + raw.getString(brokerShadow));
		
		TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(_localSequencer, raw, AnnotationEvent.ANNOTATION_EVENT_MESSAGE_OUT_QUEUED, "Message is queued for send");

		raw.setEncoding((short)0);
		if(realSession._remote!=null)
			raw.setReceiver(realSession._remote);
		raw.setSender(_localAddress);
		raw.setSeq(_orderedSeqeuence++);
		raw.setSession(0);
		
		if(Broker.DEBUG) {
			IPacketable packetable = PacketFactory.unwrapObject(brokerShadow, raw);
			LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_ISESSIONMANAGER, "SENDING(" + realSession + "): " + packetable);
			raw.rewind();
		}
		
		synchronized(realSession._pendingMsgs)
		{
			IConInfoNonListening<?> conInfoNL = realSession.getConInfoNL();
			if(conInfoNL == null || !conInfoNL.isConnected())
				return false;
			
			if(Broker.CORRELATE) {
				if(raw.getType() == PacketableTypes.TMULTICAST) {
					TMulticast tm = (TMulticast) PacketFactory.unwrapObject(brokerShadow, raw);
					TrafficCorrelator.getInstance().sessionOutEnqueue(session, tm);
				}
			}
			realSession._pendingMsgs.add(raw);
		}
		
		return resend(realSession);
	}
	
	public boolean resend(ISession session) {
		List<IRawPacket> pendingMessages = session._pendingMsgs;
		if(pendingMessages == null)
			return true;
		
		synchronized(pendingMessages) {
			if(pendingMessages != session._pendingMsgs)
				return true;
			
			while ( !session._pendingMsgs.isEmpty()) {
				IRawPacket raw = session._pendingMsgs.get(0);
				if(!_nioBinding.send(raw, session.getConInfoNL())) {
					return false;
				}

				if(Broker.CORRELATE) {
					if(raw.getType() == PacketableTypes.TMULTICAST) {
						IBrokerShadow brokerShadow = session._brokerShadow;
						TMulticast tm = (TMulticast) PacketFactory.unwrapObject(brokerShadow, raw);
						TrafficCorrelator.getInstance().sessionOutDequeue(session, tm);
					}
				}
				session._pendingMsgs.remove(0);
			}
			return true;
		}
	}

	public void dropSession(ISession session, boolean sendEndMsg) {
		if(Broker.CORRELATE)
			TrafficCorrelator.getInstance().sessionDropped(session);
		
		IConInfoNonListening<?> conInfoNL;
		synchronized (session)
		{
			if(session._type == SessionTypes.ST_END)
				return;
			
			setType(session, SessionTypes.ST_END);
			conInfoNL = session.getConInfoNL();
		}

		if(!Broker.RELEASE || Broker.DEBUG)
			LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_ISESSIONMANAGER, "Dropping session with" + (sendEndMsg?" ":"out ") + "SEND_DROP: " + session);
		
		if(sendEndMsg) {
			if(session.getSessionConnectionType() != SessionConnectionType.S_CON_T_DROPPED)
				throw new IllegalStateException("This must have been CON_T_DROPPED, it is '" + session.getSessionConnectionType() + "'.");
			TSessionInitiation sessionEnd = new TSessionInitiation(_localSequencer, session._remote, SessionInitiationTypes.SINIT_DROP, null);
			send(session, PacketFactory.wrapObject(_localSequencer, sessionEnd));
		}
		
		
		if(conInfoNL != null) {
			DestroyConnectionTask destroyConnectionTask = new DestroyConnectionTask(conInfoNL);
			_connectionManager.scheduleTaskWithTimer(destroyConnectionTask, DROP_CONNECTION_DELAY);
		}
	}
	
	public void renewSessionsConnection(ISession session) {
		synchronized(session) {
			if(Broker.CORRELATE)
				TrafficCorrelator.getInstance().renewingSession(session);
			
			LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_ISESSIONMANAGER, "Starting to renew connection to '" + session._remote + "' (Attempt #"+ session.getRetryString() + ").");
			if(session._type == SessionTypes.ST_END) {
				LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_ISESSIONMANAGER, "Session already ended.");				
				return;
			}
			
			IConInfoNonListening<?> conInfoNL = session.getConInfoNL();
			
			if(conInfoNL.isConnected()) {
				LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_ISESSIONMANAGER, "Session already connected.");
				return;
			}
			else if(conInfoNL.isConnecting()) {
				LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_ISESSIONMANAGER, "Killing previous connecting connection.");
				_nioBinding.destroyConnection(session.getConInfoNL());
			}
			
			switch(session._type) {
				case ST_PUBSUB_INITIATED:
				case ST_RECOVERY_INITIATED:
					if(session.testRetry()) {
						_nioBinding.renewConnection(session);//, _connectionManager, _connectionManager, _connectionManager, session._remoteCC);
					}
					else{
						setType(session, SessionTypes.ST_END);
						_connectionManager.failed(session);
						return;
					}					
					break;
				
				case ST_UNINITIATED:
					break;
					
				default:
					throw new IllegalStateException("ISession::renewConnection - ERROR, session is '" + session._type + "'.");
			}
		}
	}

	public abstract ISession update(IBrokerShadow brokerShadow, ISession session, TSessionInitiation sInit);
	
	public void connectionBecameConnecting(ISession session) {
		return;
	}
	
	public void connectionBecameConnected(IBrokerShadow brokerShadow, ISession session) {
		synchronized(session) {
			connectionBecameConnectedPrivately(brokerShadow, session);
		}
	}
	
	protected void connectionBecameConnectedPrivately(IBrokerShadow brokerShadow, ISession session) {
		LoggerFactory.getLogger().debug(this, "SessionBecameCONNECTED '" + session.getRemoteAddress() + "'");
		session.resetRetryCountToSuccess();
		InetSocketAddress remote = session._remote;

		switch(session._type) {
		case ST_END:
			dropSession(session, false);
			return;

		case ST_UNINITIATED:
		case ST_PUBSUB_INITIATED:
			setType(session, SessionTypes.ST_PUBSUB_CONNECTED);
			Sequence lastSequence = _connectionManager.getLastReceivedSequence(remote);
			sendPubSubInit(brokerShadow, session, remote, lastSequence);
			return;
		
		case ST_RECOVERY_INITIATED:
			setType(session, SessionTypes.ST_RECOVERY_CONNECTED);
			sendRecoveryInit(_localSequencer, session, remote);
			return;
		
		case ST_PEER_UNKNOWN:
			return;
		
		case ST__PUBSUB_PEER_PUBSUB:
		case ST__PUBSUB_PEER_RECOVERY:
		case ST__RECOVERY_PEER_PUBSUB:
		case ST__RECOVERY_PEER_RECOVERY:
			throw new IllegalStateException ("ISession::connectionBecameConnected() - ERROR, a connected connection ('"+ session._type + "') cannot become reconnected.");				
		
		default:
			throw new IllegalStateException ("ISession::connectionBecameConnected() - ERROR, a connected connection ('"+ session._type + "') cannot become reconnected.");
		}
	}
	
	// session must have already been locked.
	public void connectionBecameDisconnected(ISession session) {
		synchronized(session)
		{
			LoggerFactory.getLogger().debug(this, "SessionBecameDISCONNECTED '" + session.getRemoteAddress() + "'");
			session.flushTempRepositoryData();
			session.flushPendingMessages();
			
			switch(session._type) {
			case ST_END:
				return;
			
			case ST_RECOVERY_INITIATED: 
			case ST_RECOVERY_CONNECTED: 
			case ST__RECOVERY_PEER_PUBSUB:
			case ST__RECOVERY_PEER_RECOVERY:
				setType(session, SessionTypes.ST_RECOVERY_INITIATED);
				session.resetRetryCountToPending();
				SessionRenewTask sessionRenewTask1 = new SessionRenewTask(_connectionManager, session);
				_connectionManager.scheduleTaskWithTimer(sessionRenewTask1, RETRY_DELAY);
				return;

			case ST_UNINITIATED:
				return;
				
			case ST_PUBSUB_CONNECTED: 
				
			case ST_PUBSUB_INITIATED:
			case ST__PUBSUB_PEER_PUBSUB:
			case ST__PUBSUB_PEER_RECOVERY:
				setType(session, SessionTypes.ST_PUBSUB_INITIATED);
				session.resetRetryCountToPending();
				SessionRenewTask sessionRenewTask2 = new SessionRenewTask(_connectionManager, session);
				_connectionManager.scheduleTaskWithTimer(sessionRenewTask2, RETRY_DELAY);
				return;
			
			case ST_PEER_UNKNOWN:
				setType(session, SessionTypes.ST_END);
				//TODO: no need for the below, as session is UNKNOWN and is not in allSessions at all.
				_connectionManager.failed(session);
				return;
				
			default:
				break;
			}
		}
	}
	
	protected void sendPubSubInit(
			IBrokerShadow brokerShadow, ISession session,
			InetSocketAddress remote, Sequence lastReceivedSequence) {
		TSessionInitiation sInit;
		if(brokerShadow.isBFT())
			sInit = new TSessionInitiation(
					_localSequencer, session._remote,
					SessionInitiationTypes.SINIT_PUBSUB, lastReceivedSequence);
		else
			sInit = new TSessionInitiation(
					_localSequencer, session._remote,
					SessionInitiationTypes.SINIT_PUBSUB, lastReceivedSequence);
		
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, sInit);
		send(session, raw);
	}
	
	protected void sendRecoveryInit(LocalSequencer localSequencer, ISession session, InetSocketAddress remote) {
		Sequence lastSequence = _connectionManager.getLastReceivedSequence(remote);
		TSessionInitiation sInit = new TSessionInitiation(_localSequencer, session._remote, SessionInitiationTypes.SINIT_REOCVERY, lastSequence);
		
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, sInit);
		send(session, raw);		
	}
	
	public synchronized static void setType(ISession session, SessionTypes newType) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(LoggingSource.LOG_SRC_ISESSIONMANAGER, "Setting sessionType '" + session + "' from '" + session.getSessionType() + "' to '" + newType + "'.");

		if(Broker.CORRELATE)
			TrafficCorrelator.getInstance().sessionType(session, newType);
		
		session._type = newType;
	}

	protected final void sessionDroppedByPeer(ISession session) {
		dropSession(session, false);
	}

	public void updateSessionTypeAfterLocalRecoveryIsFinished(IBrokerShadow brokerShadow, ISession session, Sequence lastReceivedSequence) {
		switch(session._type) {
		case ST__RECOVERY_PEER_PUBSUB:
			setType(session, SessionTypes.ST__PUBSUB_PEER_PUBSUB);
			break;
		
		case ST__RECOVERY_PEER_RECOVERY:
			setType(session, SessionTypes.ST__PUBSUB_PEER_RECOVERY);
			break; 
		
		case ST_PEER_UNKNOWN:
			setType(session, SessionTypes.ST_PEER_UNKNOWN);
			break; 
		
		case ST_END:
			setType(session, SessionTypes.ST_END);
			break; 
			
		case ST_UNINITIATED:
		case ST__PUBSUB_PEER_PUBSUB:
		case ST__PUBSUB_PEER_RECOVERY:
		case ST_PUBSUB_INITIATED:
			throw new IllegalStateException("Session '" + session._remote + "' could not be in '" + session._type + "' while the previous connection manager was recovering.");

		case ST_RECOVERY_CONNECTED:
			setType(session, SessionTypes.ST_PUBSUB_CONNECTED);
			break; 
			
		case ST_RECOVERY_INITIATED:
			setType(session, SessionTypes.ST_PUBSUB_INITIATED);
			break;
		
		default:
			throw new IllegalStateException("Unknown sessionType '" + session._type + "' for session '" + session._remote + "'.");
		}
		
		switch(session._type) {
		case ST__PUBSUB_PEER_PUBSUB:
		case ST__PUBSUB_PEER_RECOVERY:
		case ST_PUBSUB_CONNECTED:
			sendPubSubInit(brokerShadow, session, session.getRemoteAddress(), lastReceivedSequence);
			break;
			
		default:
			break;
		}
	}
	
	@Deprecated
	public static ISession createDummyISession(IBrokerShadow brokerShadow, InetSocketAddress remote) {
		ISession session = ISessionManager.createBaseSession(brokerShadow, SessionTypes.ST_DUMMY);
		session.setRemoteCC(remote);
		return session;
	}

	public static ISessionLocal createBaseLocalSession(IBrokerShadow brokerShadow, SessionTypes sType) {
		return new ISessionLocal(brokerShadow, SessionObjectTypes.ISESSION, sType);
	}
	
	public static IEventOccurance createNewSessionRetryEventOccurance(Object obj, String str) {
		IEventOccurance eventOccurance = EventOccuranceFactory.createNewTimedEventOccurance(MAX_RETRY_DELAY, obj, str);
		eventOccurance.success();
		
		return eventOccurance;
	}
	
	public static Set<ISession> getSessionObjectsSet(IBrokerShadow brokerShadow, String sessionsString, SessionConnectionType sessionConnectionType) {
		Set<ISession> sessions = new HashSet<ISession>();
		StringTokenizer actSessionStringTokenizer = new StringTokenizer(sessionsString, ",");
		while(actSessionStringTokenizer.hasMoreElements()) {
			String address = (String) actSessionStringTokenizer.nextElement();
			String[] addressPort = address.split(":");
			ISession session = ISessionManager.createDummyISession(brokerShadow, new InetSocketAddress(addressPort[0], new Integer(addressPort[1])));
			session.setSessionConnectionType(sessionConnectionType);
			sessions.add(session);
		}
		return sessions;
	}

	public void setNIOBinding(INIOBinding nioBinding) {
		if(_nioBinding != null)
			if(nioBinding != _nioBinding)
				throw new IllegalArgumentException();
		
		_nioBinding = nioBinding;
	}

	public LocalSequencer getLocalSequencer() {
		return _localSequencer;
	}
}
