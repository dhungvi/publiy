package org.msrg.publiy.broker.core.connectionManager.connectionStateMachine;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.pubsub.core.IOverlayManager;

public class GlobalSessions {
	
	final private IOverlayManager _overlayManager;
	final private Map<InetSocketAddress, ISession> _allSessions;
	final private List<ISession> _unknownSessions;

	GlobalSessions(IOverlayManager overlayManager) {
		_overlayManager = overlayManager;
		
		_allSessions = new HashMap<InetSocketAddress, ISession>();
		_unknownSessions = new LinkedList<ISession>();
	}
}
