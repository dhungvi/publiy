package org.msrg.publiy.pubsub.core;

import java.net.InetSocketAddress;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;

import org.msrg.publiy.broker.core.sequence.Sequence;

public interface ITimestamp {
	
	public boolean isDuplicate(Sequence seq);
	public boolean isDuplicate(TMulticast tm);
	
	public Sequence getLastReceivedSequence(InetSocketAddress remote);
	
}
