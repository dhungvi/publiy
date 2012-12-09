package org.msrg.publiy.communication.core.niobinding;

import java.nio.channels.DatagramChannel;
import java.util.Collection;

import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.sessions.ISession;

public class UDPConInfoNonListening extends ConInfoNonListening<DatagramChannel> {

	protected UDPConInfoNonListening(IBrokerShadow brokerShadow, ISession session,
			ChannelInfoNonListening<DatagramChannel> chNL,
			INIOListener listener, INIO_R_Listener rListener,
			Collection<INIO_W_Listener> wListeners) {
		super(brokerShadow, session, chNL, listener, rListener, wListeners);
	}

	@Override
	public boolean isConnected() {
		return _state != ConnectionInfoNonListeningStatus.NL_CANCELLED && _state != ConnectionInfoNonListeningStatus.NL_CONNECTING;
	}

	@Override
	public boolean isConnecting() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ChannelTypes getChannelType() {
		return ChannelTypes.UDP;
	}

	@Override
	protected void makeConnected() {
		synchronized (_session) {
			if ( this._state == ConnectionInfoNonListeningStatus.NL_CONNECTED )
				return;
			setState(ConnectionInfoNonListeningStatus.NL_CONNECTED);
		}
		notifyAllListeners();
	}
}
