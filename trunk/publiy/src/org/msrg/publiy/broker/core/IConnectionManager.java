package org.msrg.publiy.broker.core;

import java.net.InetSocketAddress;
import java.util.TimerTask;

import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;

import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerTypes;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.client.subscriber.ISubscriptionListener;
import org.msrg.publiy.communication.core.listener.INIO_A_Listener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.packet.types.TCommand;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.component.IComponent;
import org.msrg.publiy.component.IComponentListener;

public interface IConnectionManager extends IConnectionManagerQueriable, IComponent, IComponentListener , 
								INIO_A_Listener, INIO_R_Listener, INIO_W_Listener, ITMConfirmationListener
			{

	public IBrokerShadow getBrokerShadow();
	public InetSocketAddress getLocalAddress();
	public LocalSequencer getLocalSequencer();
	public BrokerOpState getBrokerOpState();
	public ConnectionManagerTypes getType();
	
	public void sendTMulticast(TMulticast tm, ITMConfirmationListener tmConfirmationListener);
	public void sendTMulticast(TMulticast[] tms, ITMConfirmationListener tmConfirmationListener);

	void registerSession(ISession newSession);
	void failed(ISession fSession);
	void registerUnjoinedSessionAsJoined(InetSocketAddress remote);
	boolean upgradeUnjoinedSessionToActive(InetSocketAddress completedJoinAddress);

	public ISession getDefaultOutgoingSession(InetSocketAddress remote);

	ISessionManager getSessionManager();
	
	public String getBrokerRecoveryFileName();
//	public String getBrokerPSOverlayDumpFileName();
	public String getBrokerSubDumpFileName();
	public String getBrokerSessionsDumpFileName();

	public void scheduleTaskWithTimer(TimerTask task, long  delay);
	public void scheduleTaskWithTimer(TimerTask task, long delay, long period);

	public IConnectionManager getNextConnctionManager();
	public abstract InOutBWEnforcer getBWEnforcer();
	
	public void loadPrepareSubscriptionsFile(String subscriptionFile, BrokerIdentityManager idManager);
	public boolean loadPrepareSubscription(SubscriptionEntry subEntry);
	public TMulticast_Subscribe issueSubscription(Subscription subscription, ISubscriptionListener subscriptionListener);
	public boolean issueTCommandDisseminateMessage(TCommand tCommand);

	public void renewSessionsConnection(ISession _session);
	public ISession[] getAllLocallyActiveSessions();

	public void insertForceConfAckEvent();
	public void insertCandidatesEvent();
	public void sendDackOnAllSessions();
	public void sendTPingOnAllSessions();
	public void purgeMQ();
		
	public IOverlayManager getOverlayManager();
	public ISubscriptionManager getSubscriptionManager();
	public void enableDisableMQProceed(boolean enable);
	
	public void setLastReceivedSequence2(ISession sesion, Sequence seq, boolean doProceed, boolean initializeMQNode);
	public Sequence getLastReceivedSequence(InetSocketAddress remote);

	boolean checkOutputBWAvailability();
	
}
