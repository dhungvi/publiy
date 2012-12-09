package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;

import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class MessageQueueNodeMP_Conf extends MessageQueueNode_Conf {

	MessageQueueNodeMP_Conf(MessageQueueMP mq, TMulticast tm,
			ITMConfirmationListener confirmationListener, Sequence seq,
			InetSocketAddress confirmedTo) {
		super(mq, tm, confirmationListener, seq, confirmedTo);
	}

	protected String getPrefix(){
		return "MQN_CNF_MP_" + (_confirmed ? "C" : "NC");
	}
}
