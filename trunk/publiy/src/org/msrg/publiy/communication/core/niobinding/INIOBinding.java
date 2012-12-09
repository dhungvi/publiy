package org.msrg.publiy.communication.core.niobinding;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;

import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_A_Listener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.niobinding.keepalive.FDTimer;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.component.IComponent;

public interface INIOBinding extends IComponent {
	public IConInfoListening<ServerSocketChannel> makeIncomingConnection(
			INIO_A_Listener nioAListener, INIO_R_Listener dRListener, INIO_W_Listener dWListener,
			InetSocketAddress localListeningAddress) throws IllegalArgumentException;
	
	public IConInfoNonListening<SocketChannel> makeOutgoingConnection(
			ISession session, INIOListener listener, INIO_R_Listener listener2,
			Collection<INIO_W_Listener> listeners, InetSocketAddress remoteListeningAddress);
	public IConInfoNonListening<SocketChannel> makeOutgoingConnection(
			ISession session, INIOListener listener, INIO_R_Listener listener2,
			INIO_W_Listener wListener, InetSocketAddress remoteListeningAddress);


	public UDPConInfoListening makeIncomingDatagramConnection(
			INIO_A_Listener nioAListener, INIO_R_Listener dRListener, INIO_W_Listener dWListener,
			InetSocketAddress localListeningAddress) throws IllegalArgumentException;

	public UDPConInfoNonListening makeOutgoingDatagramConnection(
			ISession session, INIOListener listener, INIO_R_Listener listener2,
			Collection<INIO_W_Listener> listeners, InetSocketAddress remoteListeningAddress);
	public UDPConInfoNonListening makeOutgoingDatagramConnection(
			ISession session, INIOListener listener, INIO_R_Listener listener2,
			INIO_W_Listener wListener, InetSocketAddress remoteListeningAddress);
	
	public void renewConnection(ISession session);
	
	public void destroyConnection(IConInfoListening<?> conInfo);
	public void destroyConnection(IConInfoNonListening<?> conInfo);
	
	// If outgoing queue is full, returns false; otherwise true.
	public boolean send(IRawPacket raw, IConInfoNonListening<?> conInfo) throws IllegalArgumentException;
	
	public FDTimer getFDTimer();
	
	public void switchListeners(INIOListener newListener, INIOListener oldListener);
	
	public void dumpChannelSummary();
	
	public IBrokerShadow getBrokerShadow();
}
