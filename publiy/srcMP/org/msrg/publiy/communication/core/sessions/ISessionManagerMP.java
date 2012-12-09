package org.msrg.publiy.communication.core.sessions;

import java.net.InetSocketAddress;
import java.util.SortedSet;
import java.util.TreeSet;

import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.RTTLogger;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.niobinding.INIOBinding;
import org.msrg.publiy.communication.core.packet.types.SessionInitiationTypes;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;

public class ISessionManagerMP extends ISessionManagerPS {

	public ISessionManagerMP(LocalSequencer localSequencer, INIOBinding nioBinding,
			IConnectionManager connectionManager) {
		super(localSequencer, nioBinding, connectionManager);
	}

	@Override
	public ISession update(IBrokerShadow brokerShadow, ISession session, TSessionInitiation sInit) {
		synchronized(session) {
			if(Broker.DEBUG)
				LoggerFactory.getLogger().debug(this, "Updating session '" + session.getRemoteAddress() + "' with: " + sInit);
			
			return updatePrivately(brokerShadow, session, sInit);
		}
	}
	
	public boolean isFast(InetSocketAddress remote) {
		RTTLogger rttLogger = _nioBinding.getBrokerShadow().getRTTLogger();
		
		double avgRtt = rttLogger.getAverageRTT(remote);
		if(avgRtt == -1 || avgRtt > Broker.FAST_CONNECTION_RTT)
			return false;
		else
			return true;
	}
	
	@Override
	protected ISession updatePrivately(IBrokerShadow brokerShadow, ISession session, TSessionInitiation sInit) {
		SessionInitiationTypes msgType = sInit.getSessionType();
		InetSocketAddress msgSrcAddr = sInit.getSourceAddress();
		Sequence msgSrcSeq = sInit.getSourceSequence();	
		
		if(session._type == SessionTypes.ST_END)
			return session;
		
		if(session._lastRemoteSequence!=null && !msgSrcSeq.succeeds(session._lastRemoteSequence))
			return session;
		else // Updating the last remote sequence number on the local session.
			session._lastRemoteSequence = msgSrcSeq;
		
		if(msgType == SessionInitiationTypes.SINIT_DROP) {
			sessionDroppedByPeer(session);
			return session;
		}
		
		//BrokerState is BRKR_PUBSUB_PS
		switch(session._type) {
		case ST_PEER_UNKNOWN:
			session.setRemoteCC(msgSrcAddr);
			switch(msgType) {
			case SINIT_PUBSUB:
				setType(session, SessionTypes.ST__PUBSUB_PEER_PUBSUB);
				sendPubSubInit(brokerShadow, session, msgSrcAddr, _connectionManager.getLastReceivedSequence(session._remote));
				
				Sequence lastSequence = null;
				switch(session._sessionConnectionType) {
				case S_CON_T_SOFT:
				case S_CON_T_SOFTING:
				case S_CON_T_CANDIDATE:
					lastSequence = _localSequencer.getNext();
				default:
					lastSequence = sInit.getLastReceiveSequence();
				}
				
//				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true);
				session.setLastReceivedSequence(lastSequence);
				_connectionManager.registerSession(session);
				break;
			
			case SINIT_REOCVERY:
				setType(session, SessionTypes.ST__PUBSUB_PEER_RECOVERY);
				sendPubSubInit(brokerShadow, session, msgSrcAddr, _connectionManager.getLastReceivedSequence(session._remote));
				
				_connectionManager.registerSession(session);
				break;
				
			default:
				throw new IllegalStateException("ISession::update - ERROR, Unknown peer must initate with SINIT_PUBSUB or SINIT_RECOVERY: " + session);
			}
			break;

		case ST_UNINITIATED:
		case ST_PUBSUB_INITIATED:
			throw new IllegalStateException("ISession::update(msg) - missing 'connected' event: " + session);
		
		case ST_PUBSUB_CONNECTED:
			switch(msgType) {
			case SINIT_PUBSUB:
				setType(session, SessionTypes.ST__PUBSUB_PEER_PUBSUB);
				
//				Sequence lastSequence = sInit.getLastReceiveSequence();
//				Sequence lastSequence = LocalSequencer.getLocalSequencer().getNext();
				Sequence lastSequence = null;
				switch(session._sessionConnectionType) {
				case S_CON_T_SOFT:
				case S_CON_T_SOFTING:
				case S_CON_T_CANDIDATE:
					lastSequence = session._localSequencer.getNext();
				default:
					lastSequence = sInit.getLastReceiveSequence();
				}
//				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true);
				session.setLastReceivedSequence(lastSequence);
				_connectionManager.registerSession(session);
				break;
			
			case SINIT_REOCVERY:
				setType(session, SessionTypes.ST__PUBSUB_PEER_RECOVERY);
				_connectionManager.registerSession(session);
				break;

			default:
				throw new IllegalStateException("ISession::update - ERROR, did not expect '" + msgType + "' while ST_PUBSUB_CONNECTED: " + session);
			}
			break;
		
		case ST__PUBSUB_PEER_RECOVERY:
			switch(msgType) {
			case SINIT_REOCVERY:
				throw new IllegalStateException("ISession::update(msg) - ERROR, SINIT_RECOVERY received while session alreay in P_RECOVERY mode: " + session);
			
			case SINIT_PUBSUB:
				setType(session, SessionTypes.ST__PUBSUB_PEER_PUBSUB);
//				Sequence lastSequence = sInit.getLastReceiveSequence();
//				Sequence lastSequence = LocalSequencer.getLocalSequencer().getNext();
				Sequence lastSequence = null;
				switch(session._sessionConnectionType) {
				case S_CON_T_SOFT:
				case S_CON_T_SOFTING:
				case S_CON_T_CANDIDATE:
					lastSequence = _localSequencer.getNext();
				default:
					lastSequence = sInit.getLastReceiveSequence();
				}
//				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true); // true?
				session.setLastReceivedSequence(lastSequence);
				_connectionManager.registerSession(session);
				break;
			
			default:
				throw new IllegalStateException("ISession::update - ERROR, did not expect '" + msgType + "' while ST__PUBSUB_PEER_RECOVERY: " + session);
			}
			break;
		
		default:
			throw new IllegalStateException("ISession::update - ERROR, we cannot be in '" + session._type + "': we received a '" + msgType + "' message: " + session);
		}

		return session;
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_SESSION_MAN_MP;
	}

	public static ISessionMP getCandidatePubSubMPSession(IBrokerShadow brokerShadow, InetSocketAddress remote) {
		if(!brokerShadow.isMP())
			throw new IllegalStateException();
		
		ISessionMP sessionMP = (ISessionMP)ISessionManager.getPubSubSession(brokerShadow, remote, false);
		sessionMP.setSessionConnectionType(SessionConnectionType.S_CON_T_CANDIDATE);
		ISessionManager.setType(sessionMP, SessionTypes.ST_UNINITIATED);
		return sessionMP;
	}
	
	public static ISessionMP getPubSubMPSession(IBrokerShadow brokerShadow, InetSocketAddress remote) {
		if(!brokerShadow.isMP())
			throw new IllegalStateException();
		return (ISessionMP)ISessionManager.getPubSubSession(brokerShadow, remote, false);
	}
	
	public static ISessionMPRank[] getISessionMPRanks(ISessionMP[] sessions) {
		SortedSet<ISessionMP> sortedSessions = new TreeSet<ISessionMP>();
		for(int i=0; i<sessions.length ; i++)
			if(sessions[i] != null)
				if(sessions[i].isDistanceSet())
					sortedSessions.add(sessions[i]);
				else
					System.out.println("WARN: Session has no distance: " + sessions[i]);
		
		int rank = 0;
		ISessionMPRank[] sessionRanks = new ISessionMPRank[sortedSessions.size()];
		for(ISessionMP sessionMP : sortedSessions) {
			ISessionMPRank sessionRank = new ISessionMPRank(sessionMP, rank, sessionMP.getTotalOutPublications());
			sessionRanks[rank++] = sessionRank;
		}
		
		return sessionRanks;
	}

	@Override
	public void renewSessionsConnection(ISession session) {
		switch(session.getSessionConnectionType()) {
		case S_CON_T_SOFT:
			session.setSessionConnectionType(SessionConnectionType.S_CON_T_SOFTING);
			super.renewSessionsConnection(session);
			return;

		case S_CON_T_CANDIDATE:
			session.setIConInfo(null);
			return;

		case S_CON_T_SOFTING:
			super.renewSessionsConnection(session);
			return;

		case S_CON_T_ACTIVE:
			session.setSessionConnectionType(SessionConnectionType.S_CON_T_ACTIVATING);
			super.renewSessionsConnection(session);
			return;
			
		default:
			break;
		}
	}
	
	@Override
	protected void connectionBecameConnectedPrivately(IBrokerShadow brokerShadow, ISession session) {
//		SessionConnectionType sessionConnectionType = session.getSessionConnectionType();
//		switch(sessionConnectionType) {
//		case S_CON_T_SOFTING:
//			InetSocketAddress remote= session.getRemoteAddress();
//			setType(session, SessionTypes.ST_PUBSUB_CONNECTED);
//			Sequence lastSequence = _connectionManager.getLastReceivedSequence(remote);
//			sendPubSubInit(session, remote, lastSequence);
//			break;
//
//		case S_CON_T_CANDIDATE:
//		case S_CON_T_SOFT:
//			throw new IllegalStateException();
//		}
		super.connectionBecameConnectedPrivately(brokerShadow, session);
	}
}

