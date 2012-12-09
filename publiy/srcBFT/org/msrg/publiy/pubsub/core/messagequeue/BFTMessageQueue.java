package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionLocal;

import org.msrg.publiy.pubsub.core.IBFTSubscriptionManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.overlaymanager.IBFTOverlayManager;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT_Dack;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.BFTInvalidMessageLogger;
import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBFTSuspectedRepo;
import org.msrg.publiy.broker.core.IBFTConnectionManager;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class BFTMessageQueue extends MessageQueue implements IBFTMessageQueue {
	
	/* Auxiliary variables of type BFT*
		even though the superclass maintains these variables, these are re-defined here to
		 avoid casting these types a zillion times for every single use. */
	protected final IBFTConnectionManager _bftConnectionManager;
	protected final IBFTOverlayManager _bftOverlayManager;
	protected final IBFTSubscriptionManager _bftSubscriptionManager;
	protected final IBFTBrokerShadow _bftBrokerShadow;
	protected final IBFTSuspectedRepo _bftSuspicionRepo;
	protected final BFTInvalidMessageLogger _bftInvalidMessageLogger;
	
	public BFTMessageQueue(IBFTBrokerShadow bftBrokerShadow,
			IBFTConnectionManager bftConnectionManager,
			IBFTOverlayManager bftOverlayManager,
			IBFTSubscriptionManager bftSubscriptionManager) {
		super(bftBrokerShadow, bftConnectionManager, bftOverlayManager, bftSubscriptionManager, true);

		_bftBrokerShadow = bftBrokerShadow;
		_bftConnectionManager = bftConnectionManager;
		_bftOverlayManager = bftOverlayManager;
		_bftSubscriptionManager = bftSubscriptionManager;
		_bftSuspicionRepo = _bftBrokerShadow.getBFTSuspectedRepo();
		_bftInvalidMessageLogger = _bftBrokerShadow.getBFTIInvalidMessageLogger();
	}
	
	public BFTMessageQueue(IBFTBrokerShadow bftBrokerShadow, IBFTConnectionManager bftConnectionManager) {
		this(bftBrokerShadow, bftConnectionManager, (IBFTOverlayManager) bftConnectionManager.getOverlayManager(), (IBFTSubscriptionManager)bftConnectionManager.getSubscriptionManager());
	}

	public BFTMessageQueue(IBFTBrokerShadow bftBrokerShadow, IBFTConnectionManager bftConnectionManager, IMessageQueue oldMq) {
		super(bftConnectionManager, oldMq);
		
		_bftBrokerShadow = bftBrokerShadow;
		_bftConnectionManager = bftConnectionManager;
		_bftOverlayManager = (IBFTOverlayManager) bftConnectionManager.getOverlayManager();
		_bftSubscriptionManager = (IBFTSubscriptionManager) bftConnectionManager.getSubscriptionManager();
		_bftSuspicionRepo = _bftBrokerShadow.getBFTSuspectedRepo();
		_bftInvalidMessageLogger = _bftBrokerShadow.getBFTIInvalidMessageLogger();
	}
	
	@Override
	protected MessageQueueNode_Conf createNewMessageQueueNode_Conf(
			TMulticast_Conf tmc, Sequence confirmationFromSequence, InetSocketAddress to) {
		switch(tmc.getOriginalTMulticastType()) {
		case T_MULTICAST_PUBLICATION_BFT:
			throw new UnsupportedOperationException();
		
		default:
			return new BFTMessageQueueNode_Conf(this, tmc, null, confirmationFromSequence, to);
		}
	}

	@Override
	protected void sendConfirmations(IMessageQueueNode node) {
		switch(node.getType()) {
		case T_MULTICAST_PUBLICATION_BFT:
			return;
			
		default:
			super.sendConfirmations(node);
		}
	}

	@Override
	protected void informLocalSubscribers(TMulticast tm) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_BFT:
			getSubscriptionManager().informLocalSubscribers((TMulticast_Publish)tm);
			break;
			
		default:
			super.informLocalSubscribers(tm);
			break;
		}
	}

	boolean isValid(TMulticast_Publish_BFT tm) {
		boolean isDack = tm.getType() == TMulticastTypes.T_MULTICAST_PUBLICATION_BFT_DACK;
		List<BFTSuspecionReason> reasons = new LinkedList<BFTSuspecionReason>();
		boolean verified = _bftOverlayManager.verifySuccession(
				_bftSuspicionRepo, isDack, (TMulticast_Publish_BFT)tm, reasons);
		if(!verified)
			if(_bftInvalidMessageLogger != null)
				_bftInvalidMessageLogger.invalidMessage(tm);
		
		return verified;
	}
	
	@Override
	public void addNewMessage(TMulticast tm) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_BFT:
		{
			if(isValid((TMulticast_Publish_BFT) tm))
				super.addNewMessage(tm);
		}
		break;
		
		case T_MULTICAST_PUBLICATION_BFT_DACK:
		{
			TMulticast_Publish_BFT_Dack bftDack = (TMulticast_Publish_BFT_Dack) tm;
			if(!isValid(bftDack)) {
				BrokerInternalTimer.inform("Dack is invalid: " + bftDack);
				break;
			}
			
			if(!_localAddress.equals(tm.getSourceAddress()))
					_bftOverlayManager.dackReceived(bftDack);

			super.addNewMessage(tm);
		}
		break;
		
		// continue to default
		default:
			super.addNewMessage(tm);
		}
	}
	
	@Override
	public void addNewMessage(TMulticast tm, ITMConfirmationListener confirmationListener) {
		super.addNewMessage(tm, confirmationListener);
	}
	
	@Override
	protected void duplicateMessageReceived(TMulticast tm) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_BFT:
		case T_MULTICAST_PUBLICATION_BFT_DACK:
			return;
			
		default:
			super.duplicateMessageReceived(tm);
		}
	}
	
	@Override
	protected MessageQueueNode createNewMessageQueueNode(TMulticast tm, ITMConfirmationListener confirmationListener) {
		MessageQueueNode mqn = new BFTMessageQueueNode(_localSequencer, this, tm, confirmationListener, _bftOverlayManager);
		return mqn;
	}
	
	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_MQ_BFT;
	}
	
	@Override
	public ISessionLocal addPSSessionLocalForRecoveryWithENDEDIsession() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected PSSession createNewPSSession(ISession newSession) {
		return new PSSessionBFT(newSession, this, _bftOverlayManager);
	}

	@Override
	public MQCleanupInfo purge() {
		checkAllForConfirmations();
		return proceedHeadAndJumpConfirmed();
	}
	
	@Override
	protected MQCleanupInfo proceedHeadAndJumpConfirmed() {
		proceedHead();
		
		IMessageQueueNode prev = getRealHead();
		if(prev == null)
			return new MQCleanupInfo(0, 0);
		int discardedCount=0;
		int size=0;
				
		IMessageQueueNode next = prev.getNext();
		
		while(next != null && next != _tail) {
			if(next.isConfirmed()) {
				discardedCount++;
				next = next.getNext();
				prev.setNext(next);
			} else {
				size++;
				prev = next;
				next = next.getNext();
			}
		}
		
		if(Broker.DEBUG) 
			LoggerFactory.getLogger().debug(this, "ProceedJump: " + _mQSet.size() + "=" + getHeadToToeSize());
		
		return new MQCleanupInfo(size, discardedCount);
	}
	
	@Override
	protected IMessageQueueNode getBehindHead(Sequence lastSequence, ISession session, boolean passThrough) {
		return null;
	}
}
