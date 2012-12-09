package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.Set;

import org.msrg.publiy.communication.core.packet.types.TConf_Ack;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.overlaymanager.IWorkingOverlayManager;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.pubsub.core.subscriptionmanager.multipath.IWorkingSubscriptionManager;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.connectionManager.IConnectionManagerMP;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class MessageQueueMP extends MessageQueue implements IMessageQueueMP {

	protected IConnectionManagerMP _connectionManagerMP;
	protected PubForwardingStrategy _forwardingStrategy;
	
	public MessageQueueMP(IBrokerShadow brokerShadow, IConnectionManagerMP connectionManager, IOverlayManager overlayManager, ISubscriptionManager subscriptionManager, boolean allowedToConfirm) {
		super(brokerShadow, connectionManager, overlayManager, subscriptionManager, allowedToConfirm);

		setConnectionManager(connectionManager);
	}
	
	public MessageQueueMP(IConnectionManagerMP connectionManager, MessageQueue oldMQ) {
		super(connectionManager, oldMQ);
		
		setConnectionManager(connectionManager);
	}
	
	@Override
	public void setConnectionManager(IConnectionManager connectionManager) {
		super.setConnectionManager(connectionManager);
		_connectionManagerMP = (IConnectionManagerMP) connectionManager;
		_forwardingStrategy = _connectionManagerMP == null ? PubForwardingStrategy.PUB_FORWARDING_STRATEGY_UNKNOWN : _connectionManagerMP.getPublicationForwardingStrategy();
	}
	
	// Used in the tester code
	@Deprecated
	protected MessageQueueMP(IBrokerShadow brokerShadow, PubForwardingStrategy forwardingStrategy) {
		super(brokerShadow, null, null, null, false);
		_forwardingStrategy = forwardingStrategy;
	}
	
	@Override
	protected MessageQueueNode createNewMessageQueueNode(TMulticast tm, ITMConfirmationListener confirmationListener) {
		return new MessageQueueNodeMP(this, tm, confirmationListener, _localSequencer.getNext());
	}

	@Override
	protected MessageQueueNode_Conf createNewMessageQueueNode_Conf(TMulticast_Conf tmc, Sequence confirmationFromSequence, InetSocketAddress to) {
		return new MessageQueueNodeMP_Conf(this, tmc, null, confirmationFromSequence, to);
	}

	@Override
	public void receiveTConfAck(TConf_Ack tConfAck) {
		super.receiveTConfAck(tConfAck);
	}
	
	@Override
	protected void informLocalSubscribers(TMulticast tm) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_MP:
			getSubscriptionManager().informLocalSubscribers((TMulticast_Publish)tm);
			break;
			
		default:
			super.informLocalSubscribers(tm);
			break;
		}
	}

	@Override
	protected boolean isDuplicate(TMulticast tm) {
//		if (!Broker.RELEASE) {
//			Sequence seq = tm.getSourceSequence();
//			boolean isDuplicate = _arrivalTimestamp.isDuplicate(seq);
//			if(isDuplicate)
//				if (Broker.CORRELATE)
//					TrafficCorrelator.getInstance().duplicate(_arrivalTimestamp, tm);
//			
//			return isDuplicate;
//		}
		
		Sequence senderSequence = tm.getSenderSequence();
		if (senderSequence == null)
			senderSequence = tm.getSourceSequence();
		boolean isDuplicate = _arrivalTimestamp.isDuplicate(senderSequence);
		
		if(isDuplicate)
			new IllegalStateException("DUPLICATE DETECTED: " + tm.toStringTooLong()).printStackTrace();
		
		if(isDuplicate)
			if (Broker.CORRELATE)
				TrafficCorrelator.getInstance().duplicate(_arrivalTimestamp, tm);
		
		return isDuplicate;
	}
	
	public IWorkingOverlayManager getWorkingOverlayManager() {
		return _connectionManagerMP.getWorkingOverlayManager();
	}
	
	public IWorkingSubscriptionManager getWorkingSubscriptionManager() {
		return _connectionManagerMP.getWorkingSubscriptionManager();
	}
	
	@Override
	protected void sendConfirmations(IMessageQueueNode node) {
		switch(node.getType()) {
		case T_MULTICAST_PUBLICATION:
		case T_MULTICAST_PUBLICATION_MP:
			return;
			
		default:
			super.sendConfirmations(node);
		}
	}
	
	@Override
	protected void confirm(IMessageQueueNode node) {
		super.confirm(node);
		
		TMulticastTypes type = node.getType();
		switch(type) {
		case T_MULTICAST_DEPART:
		case T_MULTICAST_JOIN:
			_connectionManagerMP.updateWorkingOverlayManager(getOverlayManager(), node.getMessage());
			break;
			
		case T_MULTICAST_SUBSCRIPTION:
		case T_MULTICAST_UNSUBSCRIPTION:
			_connectionManagerMP.updateWorkingSubscriptionManager(node.getMessage());
			break;
			
		default:
			break;
		}
	}
	
	@Override
	protected void considerSubscriptionSummary(TRecovery_Subscription trs) {
		super.considerSubscriptionSummary(trs);
		getWorkingSubscriptionManager().applySummary(trs);
	}
	
	@Override
	protected void considerJoinSummary(TRecovery_Join trj) {
		super.considerJoinSummary(trj);
		getWorkingOverlayManager().applySummary(trj);
	}

	@Override
	protected void replaceSessionsPrivately(Set<ISession> oldSessions, Set<ISession> newSessions) {
		if(!Broker.RELEASE)
			LoggerFactory.getLogger().info(this, "Replacing PSSessionsss:",
					"__________ removing: " + oldSessions, 
					"__________ adding: " +  newSessions, 
					"__________ ALL: " + _psSessions);
		
		int added = addSessions(newSessions);
		int removed = removeSessions(oldSessions);
		
		dumpPSTop();

		checkAllForConfirmations();
		added++; removed++;
		if(!Broker.RELEASE)
			LoggerFactory.getLogger().info(this, "Replaced PSSessionsss:", "__________ ALL: " + _psSessions);
	}

	@Override
	public PubForwardingStrategy getPublicationForwardingStrategy() {
		return _forwardingStrategy;
	}
	
	public InOutBWEnforcer getBWEnforcer() {
		return _connectionManagerMP == null ? null : _connectionManagerMP.getBWEnforcer();
	}
}
