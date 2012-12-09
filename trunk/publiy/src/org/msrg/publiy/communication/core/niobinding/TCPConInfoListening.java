package org.msrg.publiy.communication.core.niobinding;

import java.nio.channels.ServerSocketChannel;

import org.msrg.publiy.communication.core.listener.INIO_A_Listener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;

public class TCPConInfoListening extends ConInfoListening<ServerSocketChannel> {

	TCPConInfoListening(ChannelInfoListening<ServerSocketChannel> chInfoL,
			INIO_A_Listener aListener, INIO_R_Listener dRListener,
			INIO_W_Listener dWListener) {
		super(chInfoL, aListener, dRListener, dWListener);
	}

	@Override
	public ChannelTypes getChannelType() {
		return ChannelTypes.TCP;
	}
}
