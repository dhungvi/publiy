package org.msrg.publiy.communication.core.niobinding;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_A_Listener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;

public class UDPChannelInfoListening extends ChannelInfoListening<DatagramChannel> implements IConInfoNonListening<DatagramChannel> {

	private static final int MAX_UDP_PACKET_SIZE = (Broker.COLS + Broker.ROWS + 10) * 2;
	private ByteBuffer _inBuffs;
	private final List<IRawPacket> _incomgingQ = new LinkedList<IRawPacket>();
	private final INIO_R_Listener _rListener;
	private final IBrokerShadow _brokerShadow;
	
	protected UDPChannelInfoListening(NIOBinding nioBinding, DatagramChannel ch,
			INIO_A_Listener aListener, INIO_R_Listener dRListener,
			INIO_W_Listener dWListener, InetSocketAddress listeningAddress) {
		super(ChannelTypes.UDP, nioBinding, ch, aListener, dRListener, dWListener, listeningAddress);
		
		_rListener = dRListener;
		_brokerShadow = nioBinding._brokerShadow;
	}
	
	protected void removeInBuffers(){
		_inBuffs = null;
	}
	
	ByteBuffer getInBody(){
		if ( _inBuffs == null )
			_inBuffs = ByteBuffer.allocate(MAX_UDP_PACKET_SIZE);
		
		return _inBuffs;
	}

	void addIncomingDataToList(IRawPacket raw) {
		synchronized(_incomgingQ) {
			switch(raw.getPacketPriority()) {
			case High:
				_incomgingQ.add(0, raw);
				break;
				
			case Normal:
			case Low:
				_incomgingQ.add(raw);
				break;
			}
			
			if(_incomgingQ.size() == 1)
				notifyReaders();
		}
	}
	
	protected void notifyReaders(){
		_rListener.conInfoGotFirstDataItem(this);
	}

	@Override
	public void deregisterUninterestedReadingListener(INIO_W_Listener wListener) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void deregisterUnnterestedReadingListener(INIO_R_Listener rListener) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public Sequence getCancellationSequence() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Sequence getConnectedSequence() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getCounterId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getIncomingQSize() {
		synchronized(_incomgingQ){
			return _incomgingQ.size();
		}
	}

	@Override
	public Sequence getLastUpdateSequence() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IRawPacket getNextIncomingData() {
		synchronized(_incomgingQ) {
			if(_incomgingQ.isEmpty())
				return null;
			
			IRawPacket raw = _incomgingQ.remove(0);
			raw.setReceiver(_nioBinding.getLocalAddress());
			CommunicationTransportLogger.logIncoming(_brokerShadow, raw);
			
			return raw;
		}
	}

	@Override
	public int getOutgoingQSize() {
		return 0;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return null;
	}

	@Override
	public ConnectionInfoNonListeningStatus getStatus() {
		return ConnectionInfoNonListeningStatus.NL_CONNECTED;
	}

	@Override
	public void registerInterestedReadingListener(INIO_R_Listener rListener) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void registerInterestedWritingListener(INIO_W_Listener wListener) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public String toStringLong() {
		return super.toString();
	}

	@Override
	public String toStringShort() {
		return super.toString();
	}

	@Override
	public ChannelInfo<DatagramChannel> getChannelInfo() {
		return this;
	}

	@Override
	public ISession getSession() {
		return null;
	}

	@Override
	public boolean isCancelled() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isConnected() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isConnecting() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isListening() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void makeCancelled() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerInterestedListener(INIOListener Listener) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void switchListeners(INIOListener newListener,
			INIOListener oldListener) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ChannelTypes getChannelType() {
		return ChannelTypes.UDP;
	}

	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}
}