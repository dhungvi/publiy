package org.msrg.publiy.communication.core.niobinding;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Collection;

import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.sessions.ISession;

public class UDPChannelInfoNonListening extends ChannelInfoNonListening<DatagramChannel> {

	UDPChannelInfoNonListening(NIOBinding nioBinding,
			ISession session, DatagramChannel ch, INIOListener listener,
			INIO_R_Listener rListener, Collection<INIO_W_Listener> wListeners,
			InetSocketAddress remoteAddress) {
		super(ChannelTypes.UDP, nioBinding, session, ch, listener, rListener, wListeners,
				remoteAddress);
	}

}
