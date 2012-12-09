package org.msrg.publiy.communication.core.niobinding;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.communication.core.listener.INIO_A_Listener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;

abstract class ChannelInfoListening<T extends SelectableChannel> extends ChannelInfo<T> implements ILoggerSource {
	
	protected IConInfo<T> _conInfoL;
	public final InetSocketAddress _listeningAddress;
	public final ChannelTypes _channelType;
	
	ChannelInfoListening(ChannelTypes type, NIOBinding nioBinding, T ch, INIO_A_Listener aListener, INIO_R_Listener dRListener, INIO_W_Listener dWListener, InetSocketAddress listeningAddress) {
		super(type, nioBinding, ch);
		_listeningAddress = listeningAddress;
		if(_listeningAddress == null)
			throw new NullPointerException();
		
		switch(type)
		{
		case TCP:
			{
				if(!ServerSocketChannel.class.isAssignableFrom(ch.getClass()))
					throw new IllegalArgumentException();
				_channelType = ChannelTypes.TCP;
				_conInfoL =
					(ConInfoListening<T>) new TCPConInfoListening(
						(ChannelInfoListening<ServerSocketChannel>) this, aListener, dRListener, dWListener);
				break;
			}
			
		case UDP:
			{
				if(!DatagramChannel.class.isAssignableFrom(ch.getClass()))
					throw new IllegalArgumentException();
				_channelType = ChannelTypes.UDP;
				_conInfoL =
					(ConInfoListening<T>) new UDPConInfoListening(
						(ChannelInfoListening<DatagramChannel>) this, aListener, dRListener, dWListener);
				break;
			}
			
		default:
			throw new UnsupportedOperationException("Channel type not supported: " + ch);
		}
		
		if ( listeningAddress == null )
			throw new IllegalArgumentException("ChannelInfoListening::ChannelInfoListening(.) listeningAddress cannot be null");
	}

	@Override
	IConInfo<T> getConnnectionInfo(){
		return _conInfoL;
	}
	
	InetSocketAddress getListeningAddress(){
		return _listeningAddress;
	}
	
	InetSocketAddress getCCAddress(){
		return _nioBinding._ccSockAddress;
	}

	@Override
	void destroyChannel() {
		_conInfoL.makeCancelled();
		super.destroyChannel();
	}

	@Override
	void destroyChannelSilently() {
		throw new UnsupportedOperationException("ChannelInfoListening does not support destroyChannelSilently()");
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_CHN_L;
	}
	
	@Override
	int getInterest() {
		ConnectionInfoListeningStatus status = ((ConInfoListening<T>)_conInfoL).getStatus();
		
		int ret = 0;
		switch (status) {
		case L_LISTENING:
			ret += SelectionKey.OP_ACCEPT;
			break;
			
		default:
			LoggerFactory.getLogger().info(this, "DAMN: (" + status + ")" + _conInfoL.toString());
		}
		
		int op = _key.interestOps();
		if ( op != ret || true)
			LoggerFactory.getLogger().info(this, "DAMN: (" + op + " vs. " + ret + ") " + _conInfoL.toString());
		
		return ret;
	}
}
