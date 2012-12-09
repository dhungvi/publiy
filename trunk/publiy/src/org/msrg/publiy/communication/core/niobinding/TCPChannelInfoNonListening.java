package org.msrg.publiy.communication.core.niobinding;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collection;

import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.sessions.ISession;

public class TCPChannelInfoNonListening extends ChannelInfoNonListening<SocketChannel> {

	protected TCPChannelInfoNonListening(NIOBinding nioBinding,
			ISession session, SocketChannel ch, INIOListener listener,
			INIO_R_Listener rListener, Collection<INIO_W_Listener> wListeners,
			InetSocketAddress remoteAddress) {
		super(ChannelTypes.TCP, nioBinding, session, ch, listener, rListener, wListeners,
				remoteAddress);
	}

}
