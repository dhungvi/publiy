package org.msrg.publiy.communication.core.niobinding;

import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;

public interface IConInfoListening<T extends SelectableChannel> extends IConInfo<T> {
	
	public InetSocketAddress getListeningAddress();
	public ConnectionInfoListeningStatus getStatus();
	
//	public INIO_A_Listener getAcceptingListener();
	
}
