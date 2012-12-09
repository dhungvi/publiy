package org.msrg.publiy.communication.core.niobinding;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

import org.msrg.publiy.communication.core.listener.INIO_A_Listener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;

public class TCPChannelInfoListening extends ChannelInfoListening<ServerSocketChannel> {

	TCPChannelInfoListening(NIOBinding nioBinding,
			ServerSocketChannel ch, INIO_A_Listener aListener,
			INIO_R_Listener dRListener, INIO_W_Listener dWListener,
			InetSocketAddress listeningAddress) {
		super(ChannelTypes.TCP, nioBinding, ch, aListener, dRListener, dWListener, listeningAddress);
	}
}
