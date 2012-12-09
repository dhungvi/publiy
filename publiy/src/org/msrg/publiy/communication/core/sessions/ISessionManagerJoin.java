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

public class ISessionManagerJoin extends ISessionManager {

	public ISessionManagerJoin(LocalSequencer localSequencer, INIOBinding nioBinding,
			IConnectionManager connectionManager) {
		super(localSequencer, nioBinding, connectionManager);
	}

	@Override
	public ISession update(IBrokerShadow brokerShadow, ISession session, TSessionInitiation sInit){
		synchronized(session){
			LoggerFactory.getLogger().debug(this, "Updating session '" + session.getRemoteAddress() + "' with: " + sInit );
			return updatePrivately(brokerShadow, session, sInit);
		}
	}
	
	private ISession updatePrivately(IBrokerShadow brokerShadow, ISession session, TSessionInitiation sInit){
		SessionInitiationTypes msgType = sInit.getSessionType();
		Sequence msgSrcSeq = sInit.getSourceSequence();
		InetSocketAddress msgSrcAddr = sInit.getSourceAddress();

		//BrokerState is BRKR_PUBSUB_JOIN
		if ( session._type == SessionTypes.ST_END )
			return session;
		
		if ( msgType == SessionInitiationTypes.SINIT_DROP ){
			sessionDroppedByPeer(session);
			return session;
		}
		
		else if ( session._lastRemoteSequence!=null && !msgSrcSeq.succeeds(session._lastRemoteSequence) ){
//				System.out.println("Update to ISession refused, since last update had a more recent source seq.");
			return session;
		}
		else // Updating the last remote sequence number on the local session.
			session._lastRemoteSequence = msgSrcSeq;

		Sequence lastSequence = sInit.getLastReceiveSequence();
		switch(session._type){
			case ST_END:
				return session;
				
			case ST_PEER_UNKNOWN:
				session.setRemoteCC(msgSrcAddr);
				switch(msgType){
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
					break;
					
				default:
					throw new IllegalStateException("ISession::update - ERROR, Unknown peer must initate with SINIT_PUBSUB or SINIT_RECOVERY");
				}
				break;
				
			case ST_UNINITIATED:
			case ST_PUBSUB_INITIATED:
				throw new IllegalStateException("ISession::updateWhileJoin(msg) - missing 'connected' event.");
		
			case ST_PUBSUB_CONNECTED:
				switch(msgType){
				case SINIT_PUBSUB:
					setType(session, SessionTypes.ST__PUBSUB_PEER_PUBSUB);

					_connectionManager.setLastReceivedSequence2(session, lastSequence, false, true);
					_connectionManager.registerSession(session);
					break;

				case SINIT_REOCVERY:
					throw new IllegalStateException("ISession::updateWhileJoin - ERROR, cannot connect to a recoverying node.");

				default:
					throw new IllegalStateException("ISession::updateWhileJoin - ERROR, received unexpected sInit msg.");
				}
				break;
		
//			case ST__PUBSUB_PEER_PUBSUB:
//				switch(msgType){
//				case SINIT_REOCVERY:
//					throw new IllegalStateException("ISession::updateWhileJoin(msg) - ERROR, SINIT_RECOVERY received while session alreay in P_PUBSUB mode.");
//					
//				case SINIT_PUBSUB:
//					throw new IllegalStateException("ISession::updateWhileJoin(msg) - ERROR, SINIT_PUBSUB received while session alreay in P_PUBSUB mode.");
//
//				default:
//					throw new IllegalStateException("ISession::updateWhileJoin - ERROR, '" + msgType + "' received unexpected sInit msg: " + sInit);
//				}
				
			default:
				throw new IllegalStateException("ISession::updateWhileJoin - ERROR, '" + msgType + "' received unexpected sInit msg: " + sInit);
			//	throw new IllegalStateException("ISession::updateWhileJoin(msg) - ERROR, should not have reached this line '" + sInit + " _ " + this);
		}

		return session; 
	}

	public LoggingSource getLogSource(){
		return LoggingSource.LOG_SRC_SESSION_MAN_JOIN;
	}

}
