package org.msrg.publiy.broker.core;

import java.net.InetSocketAddress;

import org.msrg.publiy.communication.core.sessions.ISession;

public interface IConnectionManagerDebug extends IConnectionManager {
	
	public ISession[] getAllSessions();
	public ISession getSessionPrivately(InetSocketAddress remote);
	public int getConnectionEventsCount();
	
}
