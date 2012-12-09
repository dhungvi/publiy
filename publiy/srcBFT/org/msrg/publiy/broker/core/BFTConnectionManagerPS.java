package org.msrg.publiy.broker.core;

import java.io.IOException;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;

import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBFTSuspectedRepo;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerJoin;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerPS;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerRecovery;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerTypes;

public class BFTConnectionManagerPS extends ConnectionManagerPS implements IBFTConnectionManager {

	protected BFTConnectionManagerPS(ConnectionManagerJoin conManJoin) {
		super(conManJoin);
		// TODO Auto-generated constructor stub
	}

	public BFTConnectionManagerPS(ConnectionManagerJoin conManJoin,
			String connManName, ConnectionManagerTypes connManType) {
		super(conManJoin, connManName, connManType);
		// TODO Auto-generated constructor stub
	}

	protected BFTConnectionManagerPS(ConnectionManagerRecovery conManRecovery) {
		super(conManRecovery);
		// TODO Auto-generated constructor stub
	}

	public BFTConnectionManagerPS(ConnectionManagerRecovery conManRecovery,
			String connManName, ConnectionManagerTypes connManType) {
		super(conManRecovery, connManName, connManType);
		// TODO Auto-generated constructor stub
	}

	protected BFTConnectionManagerPS(
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue)
			throws IOException {
		super(brokerShadow, overlayManager, subscriptionManager, messageQueue);
		// TODO Auto-generated constructor stub
	}

	protected BFTConnectionManagerPS(
			String connectionManagerName,
			ConnectionManagerTypes type,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue,
			TRecovery_Join[] trjs)
			throws IOException {
		super(connectionManagerName, type, broker, brokerShadow, overlayManager, subscriptionManager, messageQueue, trjs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public IBFTSuspectedRepo getSuspicionRepo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBFTBrokerShadow getBrokerShadow() {
		return null;
	}
}
