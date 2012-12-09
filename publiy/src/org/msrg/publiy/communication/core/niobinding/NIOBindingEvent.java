package org.msrg.publiy.communication.core.niobinding;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collection;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.component.ComponentStatus;

abstract class NIOBindingEvent {

	NIOBindingEventType _type;
	final Sequence _sequence;
	
	protected NIOBindingEvent(LocalSequencer localSequencer, NIOBindingEventType type){
		_type = type;
		_sequence = localSequencer.getNext();
	}
	
	public abstract String toString();

}

class NIOBindingEvent_makeIncoming extends NIOBindingEvent{
	
//	final INIO_A_Listener _nioAListener;
//	final INIO_R_Listener _dRListener;
//	final INIO_W_Listener _dWListener;
//	final InetSocketAddress _localListeningAddress;
//	NIOBindingEvent_makeIncoming(INIO_A_Listener nioAListener, INIO_R_Listener dRListener, INIO_W_Listener dWListener, InetSocketAddress localListeningAddress){
//	_nioAListener = nioAListener;
//	_dRListener = dRListener;
//	_dWListener = dWListener;
//	_localListeningAddress = localListeningAddress;

	final ChannelInfoListening<?> _chInfo;
	NIOBindingEvent_makeIncoming(LocalSequencer localSequencer, ChannelInfoListening<?> chInfo){
		super(localSequencer, NIOBindingEventType.NIO_MAKE_INCOMING);
		_chInfo = chInfo;
	}
	
	@Override
	public String toString(){
		return "NIOBindingEvent_makeIncoming:::" + _chInfo;
	}
}
class NIOBindingEvent_summarizeChannels extends NIOBindingEvent{

	protected NIOBindingEvent_summarizeChannels(LocalSequencer localSequencer) {
		super(localSequencer, NIOBindingEventType.NIO_SUMMARIZE_CHANNELS);
	}

	@Override
	public String toString() {
		return "NIOBindingEvent_summarizeChannels";
	}
	
}

class NIOBindingEvent_makeOutgoing extends NIOBindingEvent{
	
//	final ISession _session; 
//	final INIOListener _listener;
//	final INIO_R_Listener _listener2;
//	final Collection<INIO_W_Listener> _listeners;
//	final InetSocketAddress _remoteListeningAddress;
//	NIOBindingEvent_makeOutgoing(ISession session, INIOListener listener, INIO_R_Listener listener2,
//	Collection<INIO_W_Listener> listeners, InetSocketAddress remoteListeningAddress){
//	super(NIOBindingEventType.NIO_MAKE_OUTGOING);
//		_session = session;
//		_listener = listener;
//		_listener2 = listener2;
//		_listeners = listeners;
//		_remoteListeningAddress = remoteListeningAddress;
//	}
//
//	NIOBindingEvent_makeOutgoing(ISession session, INIOListener listener, INIO_R_Listener listener2,
//		INIO_W_Listener wListener, InetSocketAddress remoteListeningAddress){
//		this ( session, listener, listener2, new HashSet<INIO_W_Listener>(), remoteListeningAddress);
//		_listeners.add(wListener);
//	}

	final ChannelInfoNonListening<SocketChannel> _chInfo;
	
	NIOBindingEvent_makeOutgoing(LocalSequencer localSequencer, ChannelInfoNonListening<SocketChannel> chInfo){
		super(localSequencer, NIOBindingEventType.NIO_MAKE_OUTGOING);
		_chInfo = chInfo;
	}
	
	public String toString(){
		return "NIOBindingEvent_makeOutgoing:::" + _chInfo;
	}
}

class NIOBindingEvent_stateChanged extends NIOBindingEvent{
	
	final ComponentStatus _requestedState;
	NIOBindingEvent_stateChanged(LocalSequencer localSequencer, ComponentStatus newState){
		super(localSequencer, NIOBindingEventType.NIO_STATE_CHANGED);
		_requestedState = newState;
	}

	public String toString(){
		return "NIOBindingEvent_stateChanged:::" + _requestedState;
	}
}

class NIOBindingEvent_renewConnection extends NIOBindingEvent{
	
	final ISession _session;
	
	NIOBindingEvent_renewConnection(LocalSequencer localSequencer, ISession session){
		super(localSequencer, NIOBindingEventType.NIO_RENEW);
		_session = session;
	}

	@Override
	public String toString(){
		return "NIOBindingEvent_renewConnection:::" + _session;
	}
}

class NIOBindingEvent_destroyConnection extends NIOBindingEvent{
	
	final IConInfo<?> _conInfo;
	final ConnectionType _conInfoType;
	
	enum ConnectionType{
		ConnectionTypeListening,
		ConnectionTypeNonListening
	}
	
	NIOBindingEvent_destroyConnection(LocalSequencer localSequencer, IConInfoListening<?> conInfo) {
		super(localSequencer, NIOBindingEventType.NIO_DESTROY);
		_conInfoType = ConnectionType.ConnectionTypeListening;
		_conInfo = conInfo;
	}
	
	NIOBindingEvent_destroyConnection(LocalSequencer localSequencer, IConInfoNonListening<?> conInfo) {
		super(localSequencer, NIOBindingEventType.NIO_DESTROY);
		_conInfoType = ConnectionType.ConnectionTypeNonListening;
		_conInfo = conInfo;
	}

	@Override
	public String toString(){
		return "NIOBindingEvent_destroyConnection:::(" + _conInfoType + ")" + _conInfo;
	}
	
}

class NIOBindingEvent_sendPacket extends NIOBindingEvent{
	
	final IRawPacket _raw;
	final IConInfoNonListening<SocketChannel> _conInfo;
	
	NIOBindingEvent_sendPacket(LocalSequencer localSequencer, IRawPacket raw, IConInfoNonListening<SocketChannel> conInfo){
		super(localSequencer, NIOBindingEventType.NIO_SEND);
		_raw = raw;
		_conInfo = conInfo;
	}
	
	@Override
	public String toString(){
		return "NIOBindingEvent_sendPacket:::" + _raw + " on " + _conInfo; 
	}
}

class NIOBindingEvent_killAllConnections extends NIOBindingEvent{
	
	NIOBindingEvent_killAllConnections(LocalSequencer localSequencer) {
		super(localSequencer, NIOBindingEventType.NIO_KILL_ALL);
	}
	
	@Override
	public String toString() {
		return "NIOBindingEvent_killAllConnections"; 
	}
}

class NIOBindingEvent_registerUDPConInfoNonListening extends NIOBindingEvent {
	
	private UDPConInfoNonListening _conInfoNL;
	final ISession _session;
	final INIOListener _listener;
	final INIO_R_Listener _rListener; 
	final Collection<INIO_W_Listener> _wListeners;
	final InetSocketAddress _remoteListeningAddress;
	private boolean _isSet = false;
	
	NIOBindingEvent_registerUDPConInfoNonListening(
			LocalSequencer localSequencer, ISession session, INIOListener listener, INIO_R_Listener rListener, 
			Collection<INIO_W_Listener> wListeners, InetSocketAddress remoteListeningAddress) {
		super(localSequencer, NIOBindingEventType.NIO_REGISTER_UDP_CONINFO_NONLISTENING);
		
		_session = session;
		_listener = listener;
		_rListener = rListener; 
		_wListeners = wListeners;
		_remoteListeningAddress = remoteListeningAddress;
	}

	@Override
	public String toString() {
		return "NIOBindingEvent_killAllConnections"; 
	}
	
	public void setConInfoNL(UDPConInfoNonListening conInfoNL) {
		synchronized (this) {
			_isSet = true;
			_conInfoNL = conInfoNL;
			notifyAll();
		}
	}
	
	public boolean isSet() {
		return _isSet;
	}

	public UDPConInfoNonListening getConInfoNL() {
		return _conInfoNL;
	}
}