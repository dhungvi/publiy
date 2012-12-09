package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.Set;

import org.msrg.publiy.broker.BrokerOpState;

import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.communication.core.packet.types.TConf_Ack;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionLocal;
import org.msrg.publiy.pubsub.core.IMessageQueueQueriable;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.ITimestamp;
import org.msrg.publiy.pubsub.core.packets.multicast.TDack;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;

public interface IMessageQueue extends IMessageQueueQueriable {

	public BrokerOpState getBrokerOpState();
	public Sequence getLastReceivedSequence(InetSocketAddress remote);
	
	public void replaceSessions(Set<ISession> oldSessions, Set<ISession> newSessions);
	
	public void processTDack(TDack dack);
	public void addNewMessage(TMulticast tm);
	public void addNewMessage(TMulticast tm, ITMConfirmationListener confListener);
	public void receiveTConfAck(TConf_Ack tConfAck);
	
	public void applySpecial(TMulticast tm);
	public void applySummary(TRecovery tr);
	public void applyAllSummary(TRecovery_Join[] trjs);
	public void applyAllSummary(TRecovery_Subscription[] trss);
	public ISessionLocal addPSSessionLocalForRecoveryWithENDEDIsession();

	public boolean dumpOverlay(String filename);
	public void dumpAll(String prepend);

	public void allowConfirmations(boolean allow);
	public boolean getAllowedToConfirm();
	
//	void changeConnectionManager(IConnectionManager newConnectionManager);

	public int getPSSessionSize();
	public Set<ISession> getPSSessionsAsISessions();
	public void proceed(InetSocketAddress remote);
	public void proceedAll();
	
	public ITimestamp getArrivedTimestamp();
	public ITimestamp getDiscardedTimestamp();
	
	public MQCleanupInfo purge();
	public boolean isProceedEnabled();
	public void enableProceed();
	public void disableProceed();
	boolean dumpPSTop();
	public void substituteSession(ISession oldSession, ISession newSession);

	void resetSession(ISession session, Sequence seq, boolean initializeMQNode);
	void setConnectionManager(IConnectionManager connectionManager);
}
