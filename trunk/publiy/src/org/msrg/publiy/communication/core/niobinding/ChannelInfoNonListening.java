package org.msrg.publiy.communication.core.niobinding;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.TrafficLogger;

import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

public class ChannelInfoNonListening<T extends SelectableChannel> extends ChannelInfo<T> implements ILoggerSource {
	
	public final ChannelTypes _channelType;

	private ConInfoNonListening<T> _conInfoNL;
	private InetSocketAddress _remoteAddress;
	private IRawPacket _outRaw;
	private ByteBuffer[] _inBuffs;
	private ByteBuffer[] _outBuffs;
	protected final TrafficLogger _tLogger;

	ChannelInfoNonListening(
			ChannelTypes channelType, NIOBinding nioBinding, ISession session, T ch, INIOListener listener, INIO_R_Listener rListener, Collection<INIO_W_Listener> wListeners, InetSocketAddress remoteAddress) {
		super(channelType, nioBinding, ch);
		
		IBrokerShadow brokerShadow = nioBinding.getBrokerShadow();
		_tLogger = brokerShadow.getTrafficLogger();
		if ( remoteAddress == null )
			throw new IllegalArgumentException("ChannelInfoNonListening::ChannelInfoNonListening(.) remoteAddress cannot be null");

		_remoteAddress = remoteAddress;
		_inBuffs = new ByteBuffer[2];
		_channelType = channelType;
		
		switch(_channelType) {
		case UDP:
			if(!DatagramChannel.class.isAssignableFrom(ch.getClass()))
				throw new IllegalArgumentException("" + ch);

			_conInfoNL = (ConInfoNonListening<T>) new UDPConInfoNonListening(brokerShadow,
					session, (ChannelInfoNonListening<DatagramChannel>) this, listener, rListener, wListeners);
			break;
			
		case TCP:
			if(!SocketChannel.class.isAssignableFrom(ch.getClass()))
				throw new IllegalArgumentException("" + ch);
			
			_conInfoNL = (ConInfoNonListening<T>) new ConInfoNLKeepAlive(brokerShadow,
				session, (ChannelInfoNonListening<SocketChannel>) this, listener, rListener, wListeners, _nioBinding.getFDTimer());
			break;
			
		default:
			throw new UnsupportedOperationException("Unknown type: " + _channelType);
		}
	}
	
	@Override
	ConInfoNonListening<T> getConnnectionInfo(){
		return _conInfoNL;
	}
	
	InetSocketAddress getRemoteAddress(){
		return _remoteAddress;
	}
	
	InetSocketAddress getCCAddress(){
		return _nioBinding._ccSockAddress;
	}
	
	ByteBuffer getInBody(){
		if ( _inBuffs[0] == null )
			return null;
		if ( _inBuffs[1] == null ){
			int len = PacketFactory.getBodyLen(_inBuffs[0]);
			if ( len != -1 ) //transfer of header has been complete so we can allocate the body!
				_inBuffs[1] = ByteBuffer.allocate(len);
		}
		return _inBuffs[1];
	}

	ByteBuffer getInHeader(){
		if ( _inBuffs[0] == null )
			_inBuffs[0] = PacketFactory.getUnPreparedHeader();
		return _inBuffs[0];
	}
	
	void removeInBuffers(){
		_inBuffs[0] = null;
		_inBuffs[1] = null;
	}
	
	void emptyOutBuffers(){
		if ( _outBuffs == null )
			return;

		if ( Broker.DEBUG ) 
			LoggerFactory.getLogger().debug(this, "NIO-S(" + _conInfoNL.getSession() + "/" + _conInfoNL.getOutgoingQSize() + "): " + _outRaw.getQuickString());

		if(_tLogger != null)
			_tLogger.logOutgoing(_conInfoNL.getSession(), _outRaw);
		_outBuffs = null;
		_outRaw = null;
	}
	
	IRawPacket getOutRawPacket(){
		return _outRaw;
	}
	
	ByteBuffer[] getOutBuffs(SelectionKey key){
		if ( _outBuffs == null ){
			IRawPacket raw = getConnnectionInfo().removeOutgoingDataFromList(key);

			if ( raw!=null ){
				_outRaw = raw;
				_outBuffs = raw.getBuffs();
				// If out is null, makeUninterestedInW(.) is already called by the removeOutgoingDataFromList();
				if ( _outBuffs == null ){
					LoggerFactory.getLogger().error(this, "NIOBinding::processWritable(.) - ERROR OUT is null: " + getConnnectionInfo() + " - " + (_outBuffs==null));
					System.exit(-200);
				}
				
				IPacketListener packetListener = raw.getPacketListener();
				if(packetListener != null)
					packetListener.packetBeingSent(raw.getObject());
			}
		}

		return _outBuffs;
	}

	@Override
	void destroyChannel() {
		_conInfoNL.makeCancelled();
		super.destroyChannel();
	}
	
	@Override
	void destroyChannelSilently() {
		_conInfoNL.makeCancelledSilently();
		super.destroyChannel();
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_CHN_NL;
	}

	@Override
	int getInterest() {
		ConnectionInfoNonListeningStatus status = _conInfoNL.getStatus();
		
		int ret = 0;
		switch (status) {
		case NL_CONNECTING:
			ret = SelectionKey.OP_CONNECT;
			break;
			
		case NL_CONNECTED:
			if (_conInfoNL.updateIsReaderReady())
				ret += SelectionKey.OP_READ;
			
			if (_conInfoNL.updateIsWriterReady())
				ret += SelectionKey.OP_WRITE;
			break;
			
		default:
			return 0;
		}

		int op = _key.interestOps();
		if ( op != ret )
			LoggerFactory.getLogger().info(this, "DAMN: (" + op + " vs. " + ret + ") " + _conInfoNL.toStringLong());

		return ret;
	}
}
