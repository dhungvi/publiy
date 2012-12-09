package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.sequence.IBFTVerifierProxy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.SequencePair;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.overlaymanager.IBFTOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT_Dack;
import org.msrg.publiy.utils.HistoryType;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.casuallogger.BFTStatisticsLogger;
import org.msrg.publiy.utils.log.casuallogger.MessageProfilerLogger;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;
import org.msrg.publiy.utils.log.casuallogger.exectime.ExecutionTimeType;
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeEntity;

public class BFTMessageQueueNode extends MessageQueueNode {

	protected final Map<IBFTVerifierProxy, SequencePair> _downstreamVerifersSequencePairs;
	
	protected BFTMessageQueueNode(
			LocalSequencer localSequencer,
			BFTMessageQueue mq, TMulticast tm,
			ITMConfirmationListener confirmationListener,
			IBFTOverlayManager bftOverlayManager) {
		super(mq, tm, confirmationListener, localSequencer.getNext());
		
		_downstreamVerifersSequencePairs =
				computeDownstreamVerifersSequencePairs(tm, _matchingSet, bftOverlayManager);
	}
	
	protected final Map<IBFTVerifierProxy, SequencePair> computeDownstreamVerifersSequencePairs(
			TMulticast tm, Set<InetSocketAddress> matchingSet, IBFTOverlayManager bftOverlayManager) {
		if(matchingSet == null)
			return null;
		
		Map<IBFTVerifierProxy, SequencePair> ret;
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_BFT_DACK:
		{
			
			TMulticast_Publish_BFT_Dack bftDack = (TMulticast_Publish_BFT_Dack) tm;
			if(_mQ._localAddress.equals(tm.getSourceAddress()))
				ret = bftOverlayManager.computeIntermediateVerifersSequencePairs(tm.getSenderAddress(),
					false, true, bftDack, matchingSet);
			else
				// Do not issue local sequencepairs to dack messages not generated locally
				ret = new HashMap<IBFTVerifierProxy, SequencePair>();
		}
		break;
		
		case T_MULTICAST_PUBLICATION_BFT:
		{
			ret = bftOverlayManager.computeIntermediateVerifersSequencePairs(tm.getSenderAddress(),
					true, false, ((TMulticast_Publish_BFT) tm), matchingSet);
		}
		break;
		
		default:
			ret = null;
		}
		
		return ret;
	}

	@Override
	protected IExecutionTimeEntity getCheckOutExecutionTimerEntity(TMulticastTypes tmType, IBrokerShadow brokerShadow) {
		switch (tmType) {
		case T_MULTICAST_PUBLICATION_BFT:
			return (brokerShadow == null) ? null : brokerShadow.getExecutionTypeEntity(ExecutionTimeType.EXEC_TIME_CHECKOUT_PUB);
			
		default:
			return super.getCheckOutExecutionTimerEntity(tmType, brokerShadow);
		}
	}

	@Override
	protected String getPrefix() {
		return "MQN-BFT_" + (_confirmed ? "C" : "NC");
	}
	
	@Override
	public boolean canFastConfirm() {
		return false;
	}

	protected IRawPacket morphBFTDack(TMulticast_Publish_BFT_Dack dack, ISession session) {
		InetSocketAddress remote = session.getRemoteAddress();
		if(!_matchingSet.contains(remote))
			return null;
		
		InetSocketAddress from = dack.getFrom();
		TMulticast_Publish_BFT tmpClone = dack.getNonShiftedClone(_sequence);
		IBFTOverlayManager bftOverlayManager = ((IBFTOverlayManager)_mQ._overlayManager);
		tmpClone.addSequencePairs(_downstreamVerifersSequencePairs.values());
		bftOverlayManager.trimSequencePairs(tmpClone, remote);
		InetSocketAddress newFrom =
				bftOverlayManager.getNewFromForMorphedMessage(remote, from);
		if(newFrom == null)
			throw new IllegalStateException("NewFrom should not be null.");
		tmpClone.setFrom(newFrom);
		
		StatisticsLogger bftStatisticsLogger = _mQ._brokerShadow.getStatisticsLogger();
		if(bftStatisticsLogger != null) {
			int localSequencePairsCount = tmpClone.getSequencePairs(_mQ._localAddress).size();
			((BFTStatisticsLogger) bftStatisticsLogger).addOutputBFTDackPublicationInformation(tmpClone.getSequencePairs().size() - localSequencePairsCount, localSequencePairsCount);
		}
		
		return PacketFactory.wrapObject(_mQ._localSequencer, tmpClone);
	}
	
	@Override
	protected synchronized IRawPacket morph(ISession session, boolean asGuided) {
		IRawPacket raw;
		switch(_tm.getType()) {
		case T_MULTICAST_PUBLICATION_BFT_DACK:
		{
			TMulticast_Publish_BFT_Dack dack = (TMulticast_Publish_BFT_Dack) _tm;
			raw = morphBFTDack(dack, session);
		}
		break;
		
		case T_MULTICAST_PUBLICATION_BFT:
		{
			if(_tm.isGuided())
				raw = morphG_TMulticastPublish(session, asGuided);
			else
				raw = morphTMulticastPublish(session, asGuided);
		}
		break;

		default:
			raw = super.morph(session, asGuided);
		}
		
		IRawPacket sessionApprovedRaw =  session.hasCheckedout(_tm, raw);
		BFTMessageQueue bftMessageQueue = ((BFTMessageQueue)_mQ);
		IRawPacket bftManipulatorRaw =
				bftMessageQueue._bftBrokerShadow.applyAllBFTMessageManipulators(
						sessionApprovedRaw, session, bftMessageQueue._bftOverlayManager);
		if(bftManipulatorRaw != sessionApprovedRaw)
			LoggerFactory.getLogger().info(this, "Message (", _tm.toString(), ") to " + session.getRemoteAddress(), " has been manipulated: " + ((bftManipulatorRaw==null) ? " DROPPED" : "" + bftManipulatorRaw.getObject()));
		return bftManipulatorRaw;
	}
	
	@Override
	public String toString() {
		if(Broker.RELEASE)
			return "";
		else
			return super.toString();
	}
	
	@Override
	public String toStringLong(boolean testForConfirmation) {
		if(Broker.RELEASE)
			return "";
		else
			return super.toStringLong(testForConfirmation);
	}

	@Override
	protected Set<InetSocketAddress> computeMatchingSetForPublicationMessages(MessageQueue mq, TMulticast tm) {
		IBFTOverlayManager bftOverlayManager = (IBFTOverlayManager) mq.getOverlayManager();
		StatisticsLogger bftStatisticsLogger = _mQ._brokerShadow.getStatisticsLogger();
		Set<InetSocketAddress> ret = null;
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_BFT_DACK:
			TMulticast_Publish_BFT_Dack bftDack = (TMulticast_Publish_BFT_Dack) tm;
			ret = bftOverlayManager.getMatchingSetForBFTDack(bftDack);
			if(bftStatisticsLogger != null)
				((BFTStatisticsLogger) bftStatisticsLogger).addInputBFTDackPublicationInformation(ret.size(), bftDack.getSequencePairs().size());
			break;
			
			
		case T_MULTICAST_PUBLICATION_BFT:
			TMulticast_Publish_BFT bftTM = (TMulticast_Publish_BFT) tm;
			Publication publication = bftTM.getPublication();
			ret = mq.getSubscriptionManager().getMatchingSet(publication);
			InetSocketAddress from = bftTM.getFrom();
			Path<INode> pathFromFrom = bftOverlayManager.getPathFrom(from);
			INode immediateNeighborOnPathFromFromNode = pathFromFrom.getLast();
			InetSocketAddress immediateNeighborOnPathFromFrom = immediateNeighborOnPathFromFromNode.getAddress();

			Iterator<InetSocketAddress> matchingRemoteIt = ret.iterator();
			while(matchingRemoteIt.hasNext()) {
				InetSocketAddress matchingRemote = matchingRemoteIt.next();
				Path<INode> pathFromMatchingRemote = bftOverlayManager.getPathFrom(matchingRemote);
				if(pathFromMatchingRemote == null)
					throw new IllegalStateException(matchingRemote + " vs. " + _mQ._localAddress);
//					matchingRemoteIt.remove();
				
				else if(pathFromMatchingRemote.passes(immediateNeighborOnPathFromFrom))
					matchingRemoteIt.remove();
			}
			
			if(bftStatisticsLogger != null)
				((BFTStatisticsLogger) bftStatisticsLogger).addInputBFTPublicationInformation(ret.size(), bftTM.getSequencePairs().size());
			break;
			
		default:
			ret = super.computeMatchingSetForPublicationMessages(mq, tm);
		}
		
		return ret;
	}
	
	@Override
	protected void logMessageWithMessageLogger(InetSocketAddress remote, TMulticast tm, boolean outgoing) {
		MessageProfilerLogger messageProfilerLogger = _mQ._brokerShadow.getMessageProfilerLogger();
		if(messageProfilerLogger == null)
			return;
		
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_BFT:
			if(outgoing)
				messageProfilerLogger.logMessage(HistoryType.HIST_T_PUB_OUT_BUTLAST, remote, tm.getContentSize());
			else
				messageProfilerLogger.logMessage(HistoryType.HIST_T_PUB_IN_BUTLAST, remote, tm.getContentSize());
			break;
			
		default:
			super.logMessageWithMessageLogger(remote, tm, outgoing);
			break;
		}
	}

	@Override
	protected boolean checkMQNodeForConfirmation() {
		switch(_tm.getType()) {
		case T_MULTICAST_PUBLICATION_BFT:
		{
			Iterator<Entry<IBFTVerifierProxy, SequencePair>> entryIt =
					_downstreamVerifersSequencePairs.entrySet().iterator();
			while(entryIt.hasNext()) {
				Entry<IBFTVerifierProxy, SequencePair> entry = entryIt.next();
				IBFTVerifierProxy verifierProxy = entry.getKey();
				SequencePair sp = entry.getValue();
				if(verifierProxy.hasReceivedSequencePair(sp))
					entryIt.remove();
				else if(verifierProxy.hasDiscardedSequencePair(sp))
					entryIt.remove();
			}
			
			if(_downstreamVerifersSequencePairs.size() == 0)
				return true;
			
			// TODO:
//			for(Iterator<InetSocketAddress> it = _matchingSet.iterator() ; it.hasNext() ; ) {
//				InetSocketAddress matchingRemote = it.next();
//				if(!_downstreamVerifersSequencePairs.containsKey(matchingRemote))
//					it.remove();
//			}
			return _matchingSet.size() == 0;
		}
		
		case T_MULTICAST_PUBLICATION_BFT_DACK:
		{
			int activeSessionsCount = _mQ.getActiveSessionsCount();
			return activeSessionsCount == _visitedSessions;
		}
		
		default:
			return super.checkMQNodeForConfirmation();
		}
	}

	@Override
	protected IRawPacket morphTMulticastPublish(ISession session, boolean asGuided) {
		InetSocketAddress remote = session.getRemoteAddress();
		IBFTOverlayManager bftOverlayManager = (IBFTOverlayManager) _mQ.getOverlayManager();
		if(!canCheckOutTMulticast(session, _tm.getType()))
			return null;

		TMulticast_Publish_BFT tmp = (TMulticast_Publish_BFT) _tm;
		InetSocketAddress from = tmp.getFrom();

		Collection<SequencePair> verifiersSequencePairs =
				bftOverlayManager.getDownstreamSequencePairsOf(remote, _downstreamVerifersSequencePairs);
		if(verifiersSequencePairs.size() == 0)
			return null;

		InetSocketAddress newFrom = bftOverlayManager.getNewFromForMorphedMessage(remote, from);
		if(newFrom == null)
			throw new IllegalStateException("NewFrom should not be null.");

		TMulticast_Publish_BFT tmpClone = tmp.getNonShiftedClone(_sequence);
		tmpClone.addSequencePairs(verifiersSequencePairs);
		bftOverlayManager.trimSequencePairs(tmpClone, remote);
		
		tmpClone.setFrom(newFrom);
		if(asGuided)
			throw new UnsupportedOperationException();

		logMessageWithMessageLogger(remote, _tm, true);
		int localSequencePairsCount = tmpClone.getSequencePairs(_mQ._localAddress).size();
		StatisticsLogger bftStatisticsLogger = _mQ._brokerShadow.getStatisticsLogger();
		if(bftStatisticsLogger != null)
			((BFTStatisticsLogger) bftStatisticsLogger).addOutputBFTPublicationInformation(tmpClone.getSequencePairs().size() - localSequencePairsCount, localSequencePairsCount);

		IRawPacket raw = PacketFactory.wrapObject(_mQ._localSequencer, tmpClone);
		return raw;
	}

	@Override
	protected IRawPacket morphG_TMulticastSubscribe(ISession session, boolean asGuided) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean canCheckOutTMulticast(ISession session, TMulticastTypes tmType) {
		switch(tmType) {
		case T_MULTICAST_PUBLICATION_BFT_DACK:
		case T_MULTICAST_PUBLICATION_BFT:
			switch(session.getSessionConnectionType()) {
			case S_CON_T_ACTIVE:
			case S_CON_T_BYPASSING_AND_ACTIVE:
				return true;

			case S_CON_T_BYPASSING_AND_INACTIVE:
			case S_CON_T_BYPASSING_AND_INACTIVATING:
				throw new IllegalStateException("An activating session should not move along the MQ: " + this);
				
			case S_CON_T_CANDIDATE:
				throw new IllegalStateException("BFT mode should not create any candidate session: " + this);
				
			default:
				return false;
			}
			
		default:
			return super.canCheckOutTMulticast(session, tmType);
		}
	}
}
