package org.msrg.publiy.pubsub.core;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;

public interface ITMConfirmationListener {
	
	public void tmConfirmed(TMulticast tm);

}
