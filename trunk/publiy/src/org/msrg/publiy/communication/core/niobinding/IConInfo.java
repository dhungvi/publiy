package org.msrg.publiy.communication.core.niobinding;

import java.nio.channels.SelectableChannel;

import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.sessions.ISession;

public interface IConInfo<T extends SelectableChannel> {

	public ChannelTypes getChannelType();
	public boolean isListening();
	public boolean isConnected();
	public boolean isConnecting();
	public boolean isCancelled();
	
	void makeCancelled(); 
	
	public INIOBinding getNIOBinding();
	public Sequence getLastUpdateSequence();
	public void registerInterestedListener(INIOListener Listener);
	
	public ISession getSession();
	
	public void switchListeners(INIOListener newListener, INIOListener oldListener);
	ChannelInfo<T> getChannelInfo();
}
