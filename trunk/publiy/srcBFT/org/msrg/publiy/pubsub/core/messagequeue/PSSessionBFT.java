package org.msrg.publiy.pubsub.core.messagequeue;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.pubsub.core.overlaymanager.IBFTOverlayManager;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;
import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationDumpSpecifier;
import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationMessageDumpSpecifier;

import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionLocal;

public class PSSessionBFT extends PSSession {
	
	final IBFTMessageQueue _bftMessageQueue;

	protected PSSessionBFT(ISession session, BFTMessageQueue bftMessageQueue, IBFTOverlayManager overlayManager) {
		super(session, bftMessageQueue, overlayManager);
		
		_bftMessageQueue = bftMessageQueue;
	}
	
	protected PSSessionBFT(ISessionLocal localSession, BFTMessageQueue bftMessageQueue) {
		super(localSession, bftMessageQueue);
		
		_bftMessageQueue = bftMessageQueue;
	}

	@Override
	void resetSession(ISession session, boolean initializeMQNode) {
		switch(session.getSessionConnectionType()) {
		case S_CON_T_ACTIVE:
		case S_CON_T_UNJOINED:
		case S_CON_T_CANDIDATE:
		case S_CON_T_DELTA_VIOLATED:
		case S_CON_T_SOFT:
		case S_CON_T_BYPASSING_AND_INACTIVATING:
		case S_CON_T_BYPASSING_AND_ACTIVE:
			break;
			
		default:
			throw new IllegalStateException("Session '" + session + "' cannot be associated with a PSSession since it is '" + session.getSessionConnectionType() + "'.");
		}

		try {
			resetSessionPrivately(session, initializeMQNode);
		} catch(IllegalStateException sEx) {
			if(Broker.CORRELATE) {
				TrafficCorrelationDumpSpecifier dumpSpecifier =
					TrafficCorrelationMessageDumpSpecifier.getAllCorrelationDump("FATAL:" + sEx, false);
				TrafficCorrelator.getInstance().dump(dumpSpecifier, true);
			}
			throw sEx;
		}
	}
}
