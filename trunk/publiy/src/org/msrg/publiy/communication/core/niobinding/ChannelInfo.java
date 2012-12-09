package org.msrg.publiy.communication.core.niobinding;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

abstract class ChannelInfo<T extends SelectableChannel> {

	protected final ChannelTypes _type;
	protected final T _channel;
	protected NIOBinding _nioBinding;
	protected SelectionKey _key;
	protected final LocalSequencer _localSequencer;

	ChannelInfo(ChannelTypes type, NIOBinding nioBinding, T ch){
		_nioBinding = nioBinding;
		_channel = ch;
		_type = type;
		_localSequencer = _nioBinding.getLocalSequencer();
	}
	
	T getChannel(){
		return _channel;
	}

	public NIOBinding getNIOBinding() {
		return _nioBinding;
	}
	
	abstract int getInterest();
	
	abstract void destroyChannelSilently();
	
	void destroyChannel(){
		if (!Broker.RELEASE || Broker.DEBUG)
			LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CHN_NL, "Channel destroyed: " + this);
		try{
			if ( _key !=null ){
				_key.cancel();
				_key.attach(null);
				_key = null;
			}
			_channel.close();
		}catch(IOException iox){
			iox.printStackTrace();
		}
	}
	
	final void setKey(SelectionKey key){
		if ( key==null )
			throw new IllegalArgumentException("Key is null for channel: " + this);
		_key = key;
	}
	
	SelectionKey getKey(){
		return _key;
	}

	abstract IConInfo<T> getConnnectionInfo();
}
