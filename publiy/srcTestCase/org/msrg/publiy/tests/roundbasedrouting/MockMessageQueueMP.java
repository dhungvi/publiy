package org.msrg.publiy.tests.roundbasedrouting;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.PubForwardingStrategy;


import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueueMP;
import org.msrg.publiy.pubsub.core.multipath.WorkingManagersBundle;
import org.msrg.publiy.pubsub.core.overlaymanager.IWorkingOverlayManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.multipath.IWorkingSubscriptionManager;

class MockMessageQueueMP extends MessageQueueMP {
	WorkingManagersBundle _workingManagersBundle;
	ISession[] _sessions;
	public MockMessageQueueMP(IBrokerShadow brokerShadow, PubForwardingStrategy strategy, InetSocketAddress localAddress, ISession[] sessions, WorkingManagersBundle workingManagersBundle) {
		super(brokerShadow, strategy);
		_workingManagersBundle = workingManagersBundle;
		_localAddress = localAddress;
		_sessions = sessions;
	}

	protected ISession getISession(InetSocketAddress remote) {
		for (int i=0 ; i<=_sessions.length ; i++)
			if(_sessions[i].getRemoteAddress().equals(remote))
				return _sessions[i];
		return null;
	}
	
	@Override
	public IWorkingOverlayManager getWorkingOverlayManager(){
		return _workingManagersBundle._workingOverlayManager;
	}
	
	@Override
	public IWorkingSubscriptionManager getWorkingSubscriptionManager(){
		return _workingManagersBundle._workingSubscriptionManager;
	}
	
	@Override
	public IOverlayManager getOverlayManager() {
		return _workingManagersBundle.getMasterOverlayManager();
	}
	
	@Override
	public ISubscriptionManager getSubscriptionManager() {
		return _workingManagersBundle.getMasterSubscriptionManager();
	}
}
