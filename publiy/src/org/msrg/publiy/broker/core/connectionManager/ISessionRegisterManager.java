package org.msrg.publiy.broker.core.connectionManager;

import java.net.InetSocketAddress;
import java.util.Map;

import org.msrg.publiy.broker.info.ISessionInfo;

import org.msrg.publiy.communication.core.sessions.ISession;

public interface ISessionRegisterManager {
	
	public void registerSession(ConnectionManager connManager, ISession newSession);
	public void failed(ConnectionManager connManager, ISession fSession);
	public void registerUnjoinedSessionAsJoined(ConnectionManager connManager, InetSocketAddress remote);
	public boolean upgradeUnjoinedSessionToActive(ConnectionManager connManager, InetSocketAddress completedJoinAddress);
	public void addAllSessionsForFirstRevision(ConnectionManager connManager);
	
	public ISessionInfo[] getSessions();
	public ISessionInfo[] getInactiveSessions();
	public ISessionInfo[] getActiveSessions();
	public Map<InetSocketAddress, ISession> getAllSessions();
	public ISession[] getAllLocallyActiveSessions();
	public ISession[] getAllSessionsArray();

	boolean dumpAllSessions(String sessionsDumpFileName);

}
