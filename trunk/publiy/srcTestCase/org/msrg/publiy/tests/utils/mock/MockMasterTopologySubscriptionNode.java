package org.msrg.publiy.tests.utils.mock;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManager;

import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;

public class MockMasterTopologySubscriptionNode {

	protected final IBrokerShadow _brokerShadow;
	protected final InetSocketAddress _nodeAddress;
	protected final IOverlayManager _overlayManager;
	protected final ISubscriptionManager _masterSubscriptionManager;
	protected final Set<ISession> _allSessions = new HashSet<ISession>();
	
	MockMasterTopologySubscriptionNode(IBrokerShadow brokerShadow, TRecovery_Join[] trjs, TRecovery_Subscription[] trss) {
		_brokerShadow = brokerShadow;
		_nodeAddress = brokerShadow.getLocalAddress();
		_overlayManager = new OverlayManager(_brokerShadow);
		_overlayManager.applyAllJoinSummary(trjs);
		
		InetSocketAddress[] neighborAddresses = _overlayManager.getNeighbors(_nodeAddress);
		String activeSessionsStr = "";
		for (int i=0 ; i<neighborAddresses.length ; i++)
			activeSessionsStr += neighborAddresses[i].toString().substring(1) + (i==neighborAddresses.length-1?"":",");
		
		setActiveSessions(activeSessionsStr);
		
		_masterSubscriptionManager = new SubscriptionManager(_brokerShadow, null, true);
		_masterSubscriptionManager.applyAllSubscriptionSummary(trss);
	}
	
	public InetSocketAddress[] getSessions(SessionConnectionType type) {
		Set<InetSocketAddress> sessionAddresses = new HashSet<InetSocketAddress>();
		for (ISession session : _allSessions)
			if (session.getSessionConnectionType() == type)
				sessionAddresses.add(session.getRemoteAddress());
		
		return sessionAddresses.toArray(new InetSocketAddress[0]);
	}
	
	private void removeAllSessions(SessionConnectionType type) {
		Iterator<ISession> sessionIt = _allSessions.iterator();
		while (sessionIt.hasNext()) {
			ISession session = sessionIt.next();
			if (session.getSessionConnectionType() == type )
				sessionIt.remove();
		}
	}
	
	public void setActiveSessions(String sessionsStr) {
		removeAllSessions(SessionConnectionType.S_CON_T_ACTIVE);
		_allSessions.addAll(ISessionManager.getSessionObjectsSet(_brokerShadow, sessionsStr, SessionConnectionType.S_CON_T_ACTIVE));
	}
	
	public void setCandidateSessions(String sessionsStr) {
		removeAllSessions(SessionConnectionType.S_CON_T_CANDIDATE);
		_allSessions.addAll(ISessionManager.getSessionObjectsSet(_brokerShadow, sessionsStr, SessionConnectionType.S_CON_T_CANDIDATE));
	}

	public void setSoftSessions(String sessionsStr) {
		removeAllSessions(SessionConnectionType.S_CON_T_SOFT);
		_allSessions.addAll(ISessionManager.getSessionObjectsSet(_brokerShadow, sessionsStr, SessionConnectionType.S_CON_T_SOFT));
	}
	
	public String toStringSessions(SessionConnectionType type) {
		InetSocketAddress[] addresses = getSessions(type);
		String str = "";
		for(InetSocketAddress address : addresses)
			str += address.toString().substring(1) + ",";
		
		return str;
	}

	@Override
	public String toString() {
		return _nodeAddress.toString() + "[" + toStringSessions(SessionConnectionType.S_CON_T_ACTIVE) + "] [" + _masterSubscriptionManager.getSubscriptionEntries().size() + "]";
	}
}
