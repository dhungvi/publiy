package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.broker.BrokerIdentityManager;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class ConnectionEvent_loadPrepareSubscriptionsFile extends
		ConnectionEvent {

	public final String _subscriptionFile;
	public final BrokerIdentityManager _idManager;
	
	protected ConnectionEvent_loadPrepareSubscriptionsFile(
			LocalSequencer localSequence, String subscriptionFile, BrokerIdentityManager idManager) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_LOAD_PREPARED_SUBSCRIPTIONS_FILE);
		
		_subscriptionFile = subscriptionFile;
		_idManager = idManager;
	}

	@Override
	public String toString() {
		return "CONNECTION_EVENT_LOAD_PREPARED_SUBSCRIPTIONS_FILE: " + _subscriptionFile;
	}

}
