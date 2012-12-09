package org.msrg.publiy.tests.roundbasedrouting;

import java.net.InetSocketAddress;

import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueueNodeMP;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.sessions.ISession;

class MockMessageQueueNodeMP extends MessageQueueNodeMP {

	MockMessageQueueNodeMP(MockMessageQueueMP mq, TMulticast tm,
			ITMConfirmationListener confirmationListener, Sequence seq) {
		super(mq, tm, confirmationListener, seq);
	}
	
	@Override
	protected ISession getISession(InetSocketAddress remote) {
		return ((MockMessageQueueMP)_mQ).getISession(remote);
	}
}