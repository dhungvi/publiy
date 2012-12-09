package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class BFTMessageQueueNode_Conf extends MessageQueueNode_Conf {

	BFTMessageQueueNode_Conf(MessageQueue mQ, TMulticast tm,
			ITMConfirmationListener confirmationListener, Sequence seq,
			InetSocketAddress confirmedTo) {
		super(mQ, tm, confirmationListener, seq, confirmedTo);
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
		
//		IOverlayManager overlayManager = _mQ.getOverlayManager();
//		Path<INode> path = overlayManager.getPathFrom(remote);
//		TMulticast_Conf tmc = ((TMulticast_Conf)_tm).getCloneAndShift(path.getLength(), _sequence);
		
		TMulticast_Conf tmc = ((TMulticast_Conf)_tm).getCloneAndShift(1, _sequence);
		if(tmc != null)
			_checkedOutSet.add(remote);
		
		IRawPacket rawConf = PacketFactory.wrapObject(_mQ._localSequencer, tmc);
		if(rawConf != null)
			logMessageWithMessageLogger(remote, tmc, true);

		return rawConf;
	}
}
