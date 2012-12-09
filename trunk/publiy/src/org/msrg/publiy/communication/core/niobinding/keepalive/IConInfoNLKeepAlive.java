package org.msrg.publiy.communication.core.niobinding.keepalive;

import java.nio.channels.SocketChannel;

import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.packet.IRawPacket;

public interface IConInfoNLKeepAlive extends IConInfoNonListening<SocketChannel> {
	
	void updateLastReceiveTime(IRawPacket rawHeartBeat);
	void updateNextSendTime();
	
}
