package org.msrg.publiy.communication.core.niobinding;

import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;

import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.packet.IRawPacket;

public interface IConInfoNonListening<T extends SelectableChannel> extends IConInfo<T> {
	
	public int getCounterId();
	public InetSocketAddress getRemoteAddress();
	public ConnectionInfoNonListeningStatus getStatus();
	
	public IRawPacket getNextIncomingData();
	
	public void registerInterestedReadingListener(INIO_R_Listener rListener);
	public void registerInterestedWritingListener(INIO_W_Listener wListener);
	public void deregisterUnnterestedReadingListener(INIO_R_Listener rListener);
	public void deregisterUninterestedReadingListener(INIO_W_Listener wListener);
	
	int getIncomingQSize();
	int getOutgoingQSize();
	
	public Sequence getLastUpdateSequence();
	public Sequence getConnectedSequence();
	public Sequence getCancellationSequence();
	
	public String toStringShort();
	public String toStringLong();
	public IBrokerShadow getBrokerShadow();
	
}
