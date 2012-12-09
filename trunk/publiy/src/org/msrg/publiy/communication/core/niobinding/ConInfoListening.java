package org.msrg.publiy.communication.core.niobinding;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_A_Listener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.sessions.ISession;

public abstract class ConInfoListening<T extends SelectableChannel> implements IConInfoListening<T> {
	protected final ChannelInfoListening<T> _chInfoL;
	protected INIO_A_Listener _aListener;
	protected INIO_R_Listener _defaultRListener;
	protected INIO_W_Listener _defaultWListener;
	protected final LocalSequencer _localSequencer;
	protected boolean _isAcceptingReady = false;
	
	protected ConnectionInfoListeningStatus _state;
	protected Sequence _lastUpdateSequence;

	INIO_R_Listener getDefault_R_Listener(){
		return _defaultRListener;
	}
	
	INIO_W_Listener getDefault_W_Listener(){
		return _defaultWListener;
	}

	@Override
	public Sequence getLastUpdateSequence(){
		return _lastUpdateSequence;
	}
	
	protected void setState(ConnectionInfoListeningStatus newState){
		_state = newState;
		_lastUpdateSequence = _localSequencer.getNext();
	}

	protected ConInfoListening(ChannelInfoListening<T> chInfoL, INIO_A_Listener aListener, INIO_R_Listener dRListener, INIO_W_Listener dWListener){
		_chInfoL = chInfoL;
		_localSequencer = _chInfoL._localSequencer;
		setState(ConnectionInfoListeningStatus.L_UNINITIALIZED);
		if ( aListener == null )
			throw new IllegalArgumentException("ConnectionInfoListening::ConnectionInfoListening(.) AcceptingLisener cannot be null");
		
		_aListener = aListener;
		_defaultRListener = dRListener;
		_defaultWListener = dWListener;
		
		_aListener.becomeMyAcceptingListener(this);
	}
	
	// Called by NIOBinding
	protected void makeListening(){
		setState(ConnectionInfoListeningStatus.L_LISTENING);
		NIOBinding myNIOBinding = _chInfoL.getNIOBinding();
		if ( _isAcceptingReady == false ){
			_isAcceptingReady = true;
			myNIOBinding.updateAcceptingCount(1);
		}
		

		notifyAllListeners();
	}

	protected void makeBound(){
		setState(ConnectionInfoListeningStatus.L_BOUND);
		notifyAllListeners();
	}

	public void makeCancelled() {
		setState(ConnectionInfoListeningStatus.L_CANCELLED);
		try {
			_chInfoL._channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		NIOBinding myNIOBinding = _chInfoL.getNIOBinding();
		_isAcceptingReady = false;
		myNIOBinding.updateAcceptingCount(-1);
		notifyAllListeners();
	}
	
	protected void notifyAllListeners() {
		_aListener.conInfoUpdated(this);
	}
	
	@Override
	public InetSocketAddress getListeningAddress() {
		InetSocketAddress listeningAddress = _chInfoL.getListeningAddress();
		if(listeningAddress == null)
			throw new NullPointerException("" + _chInfoL);
		
		return listeningAddress;
	}

	@Override
	public ConnectionInfoListeningStatus getStatus() {
		return _state;
	}

	@Override
	public INIOBinding getNIOBinding() {
		return _chInfoL.getNIOBinding();
	}

	public INIO_A_Listener getAcceptingListener(){
		return _aListener;
	}
	
	@Override
	public ChannelInfo<T> getChannelInfo(){
		return _chInfoL;
	}
	
	public String toString(){
		if ( _isAcceptingReady )
			return "ConInfoL: (" + _state + ") is accepting ready.";
		else return "ConInfoL: (" + _state + ") is not accepting ready.";
	}
	
	@Override
	public void registerInterestedListener(INIOListener Listener) {
		throw new UnsupportedOperationException("ConInfoListening::registerInterestedListener(.) is not supported for a Listening connection; use constructor to specify lisener.");
	}

	@Override
	public boolean isListening() {
		return _state == ConnectionInfoListeningStatus.L_LISTENING;
	}
	
	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	public boolean isConnecting() {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return _state == ConnectionInfoListeningStatus.L_CANCELLED;
	}


	@Override
	public int hashCode(){
		return _chInfoL.getListeningAddress().hashCode();
	}
	
	@Override
	public ISession getSession() {
		return null;
	}
	
	@Override
	public void switchListeners(INIOListener newListener, INIOListener oldListener) {
		if ( _aListener == oldListener )
			_aListener = (INIO_A_Listener) newListener;
		if ( _defaultRListener == oldListener )
			_defaultRListener = (INIO_R_Listener) newListener;
		if ( _defaultWListener == oldListener )
			_defaultWListener = (INIO_W_Listener) newListener;
	}
}
