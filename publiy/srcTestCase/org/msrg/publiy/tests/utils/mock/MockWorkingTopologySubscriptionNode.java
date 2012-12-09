package org.msrg.publiy.tests.utils.mock;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.pubsub.core.multipath.WorkingManagersBundle;

import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;

class MockWorkingTopologySubscriptionNode {

	final WorkingManagersBundle _workingBundle;
	final MockMasterTopologySubscriptionNode _mockMasterNode;
	private final Set<ISession> _allSessions;
	
	MockWorkingTopologySubscriptionNode(
			MockMasterTopologySubscriptionNode mockMasterNode,
			InetSocketAddress[] softRemotes, InetSocketAddress[] candidateRemotes) {
		_mockMasterNode = mockMasterNode;
		
		_allSessions = new HashSet<ISession>(_mockMasterNode._allSessions);
		IBrokerShadow brokerShadow = mockMasterNode._brokerShadow;
		
		if (softRemotes != null )
			for(InetSocketAddress softRemote : softRemotes){
				ISession session = ISessionManager.createDummyISession(brokerShadow, softRemote);
				session.setSessionConnectionType(SessionConnectionType.S_CON_T_SOFT);
				_allSessions.add(session);
			}
		
		if (candidateRemotes != null )
			for(InetSocketAddress candidateRemote : candidateRemotes){
				ISession session = ISessionManager.createDummyISession(brokerShadow, candidateRemote);
				session.setSessionConnectionType(SessionConnectionType.S_CON_T_CANDIDATE);
				_allSessions.add(session);
		}
		
		_workingBundle = WorkingManagersBundle.createWorkingManagersBundle(
				_mockMasterNode._overlayManager,
				_allSessions.toArray(new ISession[0]),
				_mockMasterNode._masterSubscriptionManager);
	}
}
