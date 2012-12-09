package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Set;

import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;

class FastConfMessageQueueNode extends MessageQueueNode {
	
	FastConfMessageQueueNode(MessageQueue mQ, TMulticast tm, ITMConfirmationListener confirmationListener, Sequence seq) {
		super(mQ, tm, confirmationListener, seq);
	}
	
	@Override
	protected Set<InetSocketAddress> computeMatchingSetForPublicationMessages(MessageQueue mq, TMulticast tm) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_MP:
		case T_MULTICAST_PUBLICATION:
			if(!tm.getType()._canFastConfirm)
				throw new IllegalStateException();
			Publication publication = ((TMulticast_Publish)tm).getPublication();
			return mq.getSubscriptionManager().getMatchingSet(publication);
			
		default:
			return null;
		}
	}

	@Override
	protected boolean checkMQNodeForConfirmation() {
		Set<InetSocketAddress> setToCheck = _matchingSet;
		if(setToCheck == null)
			throw new IllegalStateException();
		
		if(setToCheck.size() != 0)
		{
			Iterator<InetSocketAddress> recipientsIt = setToCheck.iterator();
			while ( recipientsIt.hasNext())
			{
				InetSocketAddress recipient = recipientsIt.next();
				boolean safe = _mQ.getOverlayManager().safeToDiscard(recipient, _sequence);
				if(safe)
					recipientsIt.remove();
			}
		}
		
		return _matchingSet.size() == 0 && _mQ.getActiveSessionsCount() == _visitedSessions;
	}
	
	@Override
	public boolean isConfirmed() {
		checkForConfirmation();

		return super.isConfirmed();
	}

	@Override
	public TMulticast_Conf getConfirmation(InetSocketAddress remote, InetSocketAddress from, Sequence confirmationFromSequence) {
		throw new UnsupportedOperationException("FastConfMessageQueueNode does not produce confimrations");
	}
	
	@Override
	public void confirmationReceieved(TMulticast_Conf tmc) {
		throw new UnsupportedOperationException("A FastConfMessageQueueNode cannot receive a TMulticast_Conf: " + tmc);
	}
	
	@Override
	public boolean canFastConfirm() {
		return true;
	}
	
	@Override
	public String toString() {
		return "F" + super.toString();
	}
	
	@Override
	public String toStringLong(boolean testForConfirmation) {
		if(canFastConfirm())
			return "F" + super.toStringLong(testForConfirmation);
		
		return super.toStringLong(testForConfirmation);
	}
}