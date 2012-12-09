package org.msrg.publiy.broker.core;

import java.io.IOException;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.pubsub.core.IBFTSubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IBFTMessageQueue;
import org.msrg.publiy.pubsub.core.overlaymanager.IBFTOverlayManager;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_UnSubscribe;

import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBFTSuspectedRepo;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerJoin;

public class BFTConnectionManagerJoin extends ConnectionManagerJoin implements IBFTConnectionManagerJoin {

	protected final IBFTBrokerShadow _bftBrokerShadow;
	protected final IBFTSuspectedRepo _bftSuspicionRepo;
	
	public BFTConnectionManagerJoin(IBroker broker,
			IBFTBrokerShadow bftBrokerShadow,
			IBFTOverlayManager overlayManager,
			IBFTSubscriptionManager subscriptionManager,
			IBFTMessageQueue bftMessageQueue) throws IOException {
		super(broker, bftBrokerShadow, overlayManager, subscriptionManager, bftMessageQueue);
		
		_bftBrokerShadow = bftBrokerShadow;
		_bftSuspicionRepo = _bftBrokerShadow.getBFTSuspectedRepo();
	}

	@Override
	public void unsubscribe(TMulticast_UnSubscribe tmus) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected boolean handleSpecialMessage(ISession session, IRawPacket raw) {
		return super.handleSpecialMessage(session, raw);
	}

	@Override
	public void nodeSuspected(BFTSuspecionReason reason) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public IBFTSuspectedRepo getSuspicionRepo() {
		return _bftSuspicionRepo;
	}
	
	@Override
	public IBFTBrokerShadow getBrokerShadow() {
		return _bftBrokerShadow;
	}
}
