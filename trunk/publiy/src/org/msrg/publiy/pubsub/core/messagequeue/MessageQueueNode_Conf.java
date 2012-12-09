package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.utils.log.LoggerFactory;

class MessageQueueNode_Conf extends MessageQueueNode {

	protected final InetSocketAddress _confirmedTo;
	
	MessageQueueNode_Conf(MessageQueue mQ, TMulticast tm, ITMConfirmationListener confirmationListener, Sequence seq, InetSocketAddress confirmedTo) {
		super(mQ, tm, confirmationListener, seq);
		
		if(tm.getType() != TMulticastTypes.T_MULTICAST_CONF)
			throw new IllegalArgumentException("MessageQueueNode_Conf can only accept TMulticast_Conf.");
		
		_confirmedTo = confirmedTo;
	}

	@Override
	public void cancelCheckoutVisited(InetSocketAddress remote) {
		if(_confirmed)
			return;
		
		// TODO: URGENT! have an eye on this..
		if(remote.equals(_confirmedTo) && false)
			_mQ.confirm(this);
		else 
			decrementVisitedCount(remote);
		
		_checkedOutSet.remove(remote);
		
		return;
	}
	
	@Override
	public void cancelCheckoutUnVisited(InetSocketAddress remote) {
		if(_confirmed)
			return;

		if(!Broker.RELEASE)
			if(_visitedSet.contains(remote))
				throw new IllegalStateException(this.toStringLong(true) + " vs. " + remote);
		
		return;
	}
	
	@Override
	public void passThrough(InetSocketAddress remote) {
		if(_confirmed)
			return;
		
		incrementVisitedCount(remote);
		return;
	}

	@Override
	public int hashCode() {
		return ((TMulticast_Conf)_tm).getConfirmationFromSequence().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if(Sequence.class.isInstance(o)) {
			Sequence oSequence = (Sequence)o;
			return ((TMulticast_Conf)_tm).getConfirmationFromSequence().equalsExact(oSequence);
		}

		if(MessageQueueNode_Conf.class.isInstance(o)) { 
			MessageQueueNode_Conf oMessageQueueNode_Conf = (MessageQueueNode_Conf) o;
			Sequence oSequence = ((TMulticast_Conf)oMessageQueueNode_Conf._tm).getConfirmationFromSequence();
			return equals(oSequence);
		}

		return false;
	}
	
	@Override
	public IRawPacket checkOutAndmorphPrivately(ISession session, boolean isGuided) {
		if(_confirmed)
			return null;
		
		InetSocketAddress remote = session.getRemoteAddress();
		incrementVisitedCount(remote);
		
		if(_confirmedTo == null)
			LoggerFactory.getLogger().debug(this, "NullPointer - " + this);

		if(!_confirmedTo.equals(remote))
			return null;
		
		IOverlayManager overlayManager = _mQ.getOverlayManager();
		Path<INode> path = overlayManager.getPathFrom(remote);
		
		TMulticast_Conf tmc = ((TMulticast_Conf)_tm).getCloneAndShift(path.getLength(), _sequence);
		if(tmc != null)
			_checkedOutSet.add(remote);
		
		IRawPacket rawConf = PacketFactory.wrapObject(_mQ._localSequencer, tmc);
		if(rawConf != null)
			logMessageWithMessageLogger(remote, tmc, true);

		return rawConf;
	}
	
	TMulticast_Conf getConfirmation(InetSocketAddress remote) {
		throw new UnsupportedOperationException("Cannot confirm a TMulticast_Conf.");
	}
	
	@Override
	public String toString() {
		String confStr = super.toString();
		return confStr + "_" + ":" + _tm + " _ " + _confirmedTo; 
	}
	
	@Override
	protected String getPrefix() {
		return "MQN_CNF_" + (_confirmed ? "C" : "NC");
	}
	
	@Override
	public void confirmationReceieved(TMulticast_Conf tmc) {
		throw new UnsupportedOperationException("There is no confirmation for a MessageQueueNode_Conf: " + this.toStringLong(false) + " vs. " + tmc.toStringTooLong());
	}
	
	@Override
	protected boolean checkMQNodeForConfirmation() {
		return false;
	}
}
