package org.msrg.publiy.communication.core.sessions;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.niobinding.INIOBinding;
import org.msrg.publiy.communication.core.packet.types.SessionInitiationTypes;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;

public class ISessionManagerPS extends ISessionManager {

	public ISessionManagerPS(LocalSequencer localSequencer, INIOBinding nioBinding,
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
		Sequence lastSequence = sInit.getLastReceiveSequence();
		switch(session._type) {
		case ST_PEER_UNKNOWN:
			session.setRemoteCC(msgSrcAddr);
			switch(msgType) {
			case SINIT_PUBSUB:
				setType(session, SessionTypes.ST__PUBSUB_PEER_PUBSUB);
				sendPubSubInit(brokerShadow, session, msgSrcAddr, _connectionManager.getLastReceivedSequence(session._remote));
				
				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true);
				_connectionManager.registerSession(session);
				break;
			
			case SINIT_REOCVERY:
				setType(session, SessionTypes.ST__PUBSUB_PEER_RECOVERY);
				sendPubSubInit(brokerShadow, session, msgSrcAddr, _connectionManager.getLastReceivedSequence(session._remote));
				
				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true);
				_connectionManager.registerSession(session);
				//TODO: take care of the below in registration.
//				_connectionManager.sendAllToRecoveringAndEndRecovery(session);
				break;
				
			default:
				throw new IllegalStateException("ISession::update - ERROR, Unknown peer must initate with SINIT_PUBSUB or SINIT_RECOVERY");
			}
			break;

		case ST_UNINITIATED:
		case ST_PUBSUB_INITIATED:
			throw new IllegalStateException("ISession::update(msg) - missing 'connected' event.");
		
		case ST_PUBSUB_CONNECTED:
			switch(msgType) {
			case SINIT_PUBSUB:
				setType(session, SessionTypes.ST__PUBSUB_PEER_PUBSUB);
				
				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true);
				_connectionManager.registerSession(session);
				break;
			
			case SINIT_REOCVERY:
				setType(session, SessionTypes.ST__PUBSUB_PEER_RECOVERY);
				
				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true);
				_connectionManager.registerSession(session);
				//TODO: take care of the below in registration.
//				_connectionManager.sendAllToRecoveringAndEndRecovery(session);
				break;

			default:
				throw new IllegalStateException("ISession::update - ERROR, did not expect '" + msgType + "' while ST_PUBSUB_CONNECTED.");
			}
			break;

		case ST__PUBSUB_PEER_RECOVERY:
			switch(msgType) {
			case SINIT_REOCVERY:
				throw new IllegalStateException("ISession::update(msg) - ERROR, SINIT_RECOVERY received while session alreay in P_RECOVERY mode.");
			
			case SINIT_PUBSUB:
				setType(session, SessionTypes.ST__PUBSUB_PEER_PUBSUB);
				
				_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true); // true?
//				_connectionManager.deregisterRecoverySession(session);
//				_connectionManager.registerSession(session);
				break;
			
			default:
				throw new IllegalStateException("ISession::update - ERROR, did not expect '" + msgType + "' while ST__PUBSUB_PEER_RECOVERY.");
			}
			break;
		
		default:
			throw new IllegalStateException("ISession::update - ERROR, we cannot be in '" + session._type + "': we received a '" + msgType + "' message.");
		}

		return session;
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_SESSION_MAN_PS;
	}
}
