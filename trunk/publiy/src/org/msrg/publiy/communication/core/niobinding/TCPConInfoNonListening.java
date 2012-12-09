package org.msrg.publiy.communication.core.niobinding;

import java.nio.channels.SocketChannel;
import java.util.Collection;

import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.sessions.ISession;

public class TCPConInfoNonListening extends ConInfoNonListening<SocketChannel> {

	protected TCPConInfoNonListening(IBrokerShadow brokerShadow, ISession session,
			ChannelInfoNonListening<SocketChannel> chNL, INIOListener listener,
			INIO_R_Listener rListener, Collection<INIO_W_Listener> wListeners) {
		super(brokerShadow, session, chNL, listener, rListener, wListeners);
	}

	@Override
	public boolean isConnected() {
		return _chInfoNL._channel.isConnected();
	}

	@Override
	public boolean isConnecting() {
		return _chInfoNL._channel.isConnectionPending();
	}

	@Override
	public ChannelTypes getChannelType() {
		return ChannelTypes.TCP;
	}
}
