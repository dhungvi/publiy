package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.Set;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

import publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayNode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.overlaymanager.IWorkingOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.WorkingRemoteSet;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.pubsub.core.subscriptionmanager.multipath.IWorkingSubscriptionManager;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class MessageQueueNodeMP_New extends MessageQueueNode {

	protected MessageQueueNodeMP_New(MessageQueueMP mq, TMulticast tm, ITMConfirmationListener confirmationListener, Sequence seq) {
		super(mq, tm, confirmationListener, seq);
	}

	protected Set<InetSocketAddress> getPublicationsMatchingRecipientSet(Publication publication){
		IWorkingSubscriptionManager workingSubManager = ((MessageQueueMP)_mQ).getWorkingSubscriptionManager();
		if ( _recipientSet != null && ((WorkingRemoteSet)_recipientSet).isValid(workingSubManager))
			return _recipientSet;
		
		else
		{
			WorkingRemoteSet matchingSet = workingSubManager.getMatchingWokringSet(publication);
			matchingSet.remove(_mQ._localAddress);
			
			_recipientSet = matchingSet;
			return matchingSet;
		}
	}
	
	protected PubForwardingStrategy getForwardingStrategy(){
		PubForwardingStrategy messageForwardingStrategy = ((TMulticast_Publish)_tm).getPubForwardingStrategy();;
		PubForwardingStrategy brokerForwardingStrategy = ((MessageQueueMP)_mQ).getPublicationForwardingStrategy();
		switch(brokerForwardingStrategy){
		case PUB_FORWARDING_STRATEGY_0:
			return messageForwardingStrategy;
			
		case PUB_FORWARDING_STRATEGY_1:
		case PUB_FORWARDING_STRATEGY_2:
		case PUB_FORWARDING_STRATEGY_3:
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
//		if ( !session.canCheckOutTMulticast(TMulticastTypes.T_MULTICAST_PUBLICATION) )
//			return null;
//
//		TMulticast_Publish tmp = (TMulticast_Publish) _tm;
//		Set<InetSocketAddress> recipients = tmp.getClonedGuidedInfo();
//		if (recipients.contains(_mQ._localAddress))
//			return morphTMulticastPublish(session, false);
//		
//		IOverlayManager overlayManager = _mQ.getOverlayManager();
//		InetSocketAddress remote = session.getRemoteAddress();
//		
//		for(InetSocketAddress recipient : recipients) {
//			Path<OverlayNode> pathFromRemote = overlayManager.getPathFrom(remote);
//			if (pathFromRemote.passes(recipient))
//				return morphTMulticastPublish(session, false);
//		}
//
//		return null;
	}
	
	@Override
	protected IRawPacket morphTMulticastPublish(ISession session, boolean asGuided){
		if (asGuided)
			throw new UnsupportedOperationException();
		else if ( !session.canCheckOutTMulticast(TMulticastTypes.T_MULTICAST_PUBLICATION) )
			return null;
		
		TMulticast_Publish_MP tmp = (TMulticast_Publish_MP) _tm;
		InetSocketAddress remote = session.getRemoteAddress();
		IOverlayManager overlayManager = _mQ.getOverlayManager();
		Path<OverlayNode> pathFromRemote = overlayManager.getPathFrom(remote);
		Set<InetSocketAddress> exclusionSet = tmp.getExclusionSet();
		for (InetSocketAddress excludedAddress : exclusionSet) {
			if (pathFromRemote.passes(excludedAddress))
				return null;
		}

		PubForwardingStrategy forwardingStrategy = getForwardingStrategy();
		Publication publication = tmp.getPublication();
		WorkingRemoteSet matchingRemotes = (WorkingRemoteSet) getPublicationsMatchingRecipientSet(publication);
		
		IWorkingOverlayManager workingOverlayManager = ((MessageQueueMP)_mQ).getWorkingOverlayManager();
		WorkingRemoteSet workingSet = workingOverlayManager.getSoftLinksAddresses(forwardingStrategy, matchingRemotes);
		if ( workingSet == null )
			throw new IllegalStateException(matchingRemotes + " vs. " + workingOverlayManager);
		else if (!outRemotes.contains(remote)) {
			if (matchingRemotes.contains(remote))
				logMessageWithMessageLogger(session.getRemoteAddress(), _tm, true);
			return null;
		}

		TMulticast_Publish tmpClone = tmp.getShiftedClone(pathFromRemote.getLength(), _sequence);
		tmpClone.setFrom(_mQ._localAddress);
		
		for(InetSocketAddress matchingRemote : matchingRemotes) {
			if (matchingRemote.equals(remote))
				continue;
			else if(workingOverlayManager.isReal(matchingRemote))
				continue;
			
			Path<OverlayNode> pathFromMatchingRemote = overlayManager.getPathFrom(matchingRemote);
			if (pathFromMatchingRemote.passes(remote))
				tmp.addExclusion(matchingRemote);
		}
		
		IRawPacket raw = PacketFactory.wrapObject(tmpClone);
		return raw;
	}
	
	protected String getPrefix(){
		return "MQN_MP_" + (_confirmed ? "C" : "NC");
	}

	@Override
	protected boolean testMQNodeForConfirmation(){
		switch(_tm.getType()) {
		case T_MULTICAST_PUBLICATION:
		case T_MULTICAST_PUBLICATION_MP:
			int activeSessionsCount = _mQ.getActiveSessionsCount();
			return activeSessionsCount == _visitedSessions;
			
		default:
			return super.testMQNodeForConfirmation();
		}
	}
}
