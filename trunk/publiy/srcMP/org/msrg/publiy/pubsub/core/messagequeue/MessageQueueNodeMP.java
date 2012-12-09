package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.Set;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.IWorkingOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.overlaymanager.WorkingRemoteSet;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.pubsub.core.subscriptionmanager.multipath.IWorkingSubscriptionManager;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class MessageQueueNodeMP extends MessageQueueNode {

	protected WorkingRemoteSet _workingRemoteSet;
	protected Set<InetSocketAddress> _realRemoteSet;
	
	protected MessageQueueNodeMP(MessageQueueMP mq, TMulticast tm, ITMConfirmationListener confirmationListener, Sequence seq) {
		super(mq, tm, confirmationListener, seq);
		
		TMulticastTypes tmType = tm.getType();
		switch(tmType) {
		case T_MULTICAST_PUBLICATION_NC:
		case T_MULTICAST_PUBLICATION:
			throw new IllegalArgumentException(tmType + " vs. " + tm.toString());
		
		default:
			break;
		}
	}

	@Override
	protected Set<InetSocketAddress> computeMatchingSetForPublicationMessages(MessageQueue mq, TMulticast tm) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_MP:
			break;

		default:
			return super.computeMatchingSetForPublicationMessages(mq, tm);
		}
		
		Publication publication = ((TMulticast_Publish_MP)_tm).getPublication();
		MessageQueueMP mqMP = ((MessageQueueMP)mq);
		IWorkingSubscriptionManager workingSubManager = mqMP.getWorkingSubscriptionManager();
		if(_workingRemoteSet != null && _realRemoteSet != null) // && _workingRemoteSet.isValid(workingSubManager))
			return _realRemoteSet;
		
		if(Broker.CORRELATE)
			TrafficCorrelator.getInstance().workingSetComputed(_tm);
			
		InOutBWEnforcer bwEnforcer = mqMP.getBWEnforcer();
		WorkingRemoteSet matchingSet = workingSubManager.getMatchingWokringSet(publication);
		PubForwardingStrategy forwardingStrategy = getForwardingStrategy();
		IWorkingOverlayManager workingOverlayManager = ((MessageQueueMP)_mQ).getWorkingOverlayManager();
		_workingRemoteSet = workingOverlayManager.computeSoftLinksAddresses(bwEnforcer, forwardingStrategy, matchingSet);
		
		_realRemoteSet = _workingRemoteSet.getRealRemotes();
		_realRemoteSet.remove(_mQ._localAddress);
		
		for(InetSocketAddress remote : _workingRemoteSet.getMatchingRemotes())
			logMessageWithMessageLogger(remote, _tm, true);
		
		return _realRemoteSet;
	}
	
	protected PubForwardingStrategy getForwardingStrategy() {
		PubForwardingStrategy messageForwardingStrategy = ((TMulticast_Publish)_tm).getPubForwardingStrategy();;
		PubForwardingStrategy brokerForwardingStrategy = ((MessageQueueMP)_mQ).getPublicationForwardingStrategy();
		switch(brokerForwardingStrategy) {
		case PUB_FORWARDING_STRATEGY_0:
			return messageForwardingStrategy;
			
		case PUB_FORWARDING_STRATEGY_1:
		case PUB_FORWARDING_STRATEGY_2:
		case PUB_FORWARDING_STRATEGY_3:
		case PUB_FORWARDING_STRATEGY_4:
			return brokerForwardingStrategy;
			
		default:
			throw new UnsupportedOperationException(messageForwardingStrategy + " vs. " + brokerForwardingStrategy);
		}
	}
	
	protected ISession getISession(InetSocketAddress remote) {
		return _mQ.getPSSession(remote).getISession();
	}

	@Override
	protected IRawPacket morphG_TMulticastPublish(ISession session, boolean asGuided) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected IRawPacket morphTMulticastPublish(ISession session, boolean asGuided) {
		if(asGuided)
			throw new UnsupportedOperationException();
		else if(!canCheckOutTMulticast(session, TMulticastTypes.T_MULTICAST_PUBLICATION))
			return null;
		
		TMulticast_Publish_MP tmp = (TMulticast_Publish_MP) _tm;
		InetSocketAddress remote = session.getRemoteAddress();
		IOverlayManager overlayManager = _mQ.getOverlayManager();
		Path<INode> pathFromRemote = overlayManager.getPathFrom(remote);
		Set<InetSocketAddress> exclusionSet = tmp.getExclusionSet();
		for(InetSocketAddress excludedAddress : exclusionSet) {
			if(pathFromRemote.passes(excludedAddress))
				return null;
		}

		Set<InetSocketAddress> realRemotes = computeMatchingSetForPublicationMessages(_mQ, _tm);
		
		IWorkingOverlayManager workingOverlayManager = ((MessageQueueMP)_mQ).getWorkingOverlayManager();
		if(realRemotes == null)
			throw new IllegalStateException(realRemotes + " vs. " + workingOverlayManager);
		
		if(!realRemotes.contains(remote))
			return null;

		TMulticast_Publish tmpClone = tmp.getShiftedClone(pathFromRemote.getLength(), _sequence);
		tmpClone.setFrom(_mQ._localAddress);
		
		IRawPacket raw = PacketFactory.wrapObject(_mQ._localSequencer, tmpClone);
		return raw;
	}
	
	@Override
	protected String getPrefix() {
		return "MQN_MP_" + (_confirmed ? "C" : "NC");
	}

	@Override
	protected boolean checkMQNodeForConfirmation() {
		switch(_tm.getType()) {
		case T_MULTICAST_PUBLICATION:
		case T_MULTICAST_PUBLICATION_MP:
			int activeSessionsCount = _mQ.getActiveSessionsCount();
			return activeSessionsCount == _visitedSessions;
			
		default:
			return super.checkMQNodeForConfirmation();
		}
	}
}
