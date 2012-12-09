package org.msrg.publiy.communication.core.sessions;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.niobinding.INIOBinding;
import org.msrg.publiy.communication.core.packet.types.SessionInitiationTypes;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;

public class ISessionManagerRecovery extends ISessionManager {

	public ISessionManagerRecovery(LocalSequencer localSequencer, INIOBinding nioBinding,
			IConnectionManager connectionManager) {
		super(localSequencer, nioBinding, connectionManager);
	}

	@Override
	public ISession update(IBrokerShadow brokerShadow, ISession session, TSessionInitiation sInit) {
		synchronized(session) {
			LoggerFactory.getLogger().debug(this, "Updating session '" + session.getRemoteAddress() + "' with: " + sInit);
			return updatePrivately(brokerShadow, session, sInit);
		}
	}
	
	private ISession updatePrivately(IBrokerShadow brokerShadow, ISession session, TSessionInitiation sInit) {
		SessionInitiationTypes msgType = sInit.getSessionType();
		InetSocketAddress msgSrcAddr = sInit.getSourceAddress();
		Sequence msgSrcSeq = sInit.getSourceSequence();	
		
		//BrokerState is BRKR_PUBSUB_JOIN
		if(session._type == SessionTypes.ST_END)
			return session;
		
		if(session._lastRemoteSequence!=null && !msgSrcSeq.succeeds(session._lastRemoteSequence)) {
			LoggerFactory.getLogger().debug(this, "Update to ISession refused, since last update had a more recent source seq.");
			return session;
		}
		else // Updating the last remote sequence number on the local session.
			session._lastRemoteSequence = msgSrcSeq;

		if(msgType == SessionInitiationTypes.SINIT_DROP) {
			sessionDroppedByPeer(session);
			return session;
		}
		
		Sequence lastSequence = sInit.getLastReceiveSequence();
		switch(session._type) {
		case ST_PEER_UNKNOWN:
			session.setRemoteCC(msgSrcAddr);
			switch(msgType) {
			case SINIT_PUBSUB:
				setType(session, SessionTypes.ST__RECOVERY_PEER_PUBSUB);
				sendRecoveryInit(_localSequencer,session, msgSrcAddr);
				
				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true);
				_connectionManager.registerSession(session);
				break;
			
			case SINIT_REOCVERY:
				setType(session, SessionTypes.ST__RECOVERY_PEER_RECOVERY);
				sendRecoveryInit(_localSequencer, session, msgSrcAddr);
				
				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true);
				_connectionManager.registerSession(session);
				//TODO: take care of the below in session Registration.
//				_connectionManager.sendAllToRecoveringAndNotEndRecovery(session);
				break;
			
			default:
				throw new IllegalStateException("ISession::updateWhileRecovery - ERROR, received unexpected sInit msg.");
			}
			break;

		case ST_RECOVERY_INITIATED:
			throw new IllegalStateException("ISession::updateWhileRecovery(msg) - missing 'connected' event.");
		
		case ST_RECOVERY_CONNECTED:
			switch(msgType) {
			case SINIT_PUBSUB:
				setType(session, SessionTypes.ST__RECOVERY_PEER_PUBSUB);
				
				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true);
				_connectionManager.registerSession(session);
				break;
				
			case SINIT_REOCVERY:
				setType(session, SessionTypes.ST__RECOVERY_PEER_RECOVERY);
//				sendRecoveryInit(session, msgSrcAddr);
				
				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true);
				_connectionManager.registerSession(session);
				//TODO: take care of the below in session registration.
//				_connectionManager.sendAllToRecoveringAndNotEndRecovery(session);
				break;
				
			default:
				break;
			}
			break;

//		case ST__RECOVERY_PEER_PUBSUB:
//			if(msgType == SessionInitiationTypes.SINIT_REOCVERY)
//				throw new IllegalStateException("ISession::updateWhileRecovery(msg) - ERROR, SINIT_RECOVERY received while session alreay in P_PUBSUB mode.");
//			else if(msgType == SessionInitiationTypes.SINIT_PUBSUB)
//				throw new IllegalStateException("ISession::updateWhileRecovery(msg) - ERROR, SINIT_PUBSUB received while session alreay in P_PUBSUB mode.");
//			break;
		
		case ST__RECOVERY_PEER_RECOVERY:
			switch(msgType) {
			case SINIT_REOCVERY:
				throw new IllegalStateException("ISession::updateWhileRecovery(msg) - ERROR, SINIT_RECOVERY received while session alreay in P_RECOVERY mode.");
			
			case SINIT_PUBSUB:
				setType(session, SessionTypes.ST__RECOVERY_PEER_PUBSUB);
//				_connectionManager.deregisterRecoverySession(session);
				
				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true); // true?
				break;
				
			default:
				throw new IllegalStateException("No other type of sInit is accepted when a session '" + session._remote + "' is '" + session._type + "'.");
			}
			break;

		default:
			throw new IllegalStateException("ISession::updateWhileRecovery(msg) - ERROR, should not have reached this line: " + session._type + " _ " + sInit);
		}
		return session; 
	}

	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_SESSION_MAN_RECOVER;
	}
}
