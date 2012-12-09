package org.msrg.publiy.communication.core.niobinding;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.msrg.publiy.communication.core.listener.INIO_A_Listener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;

public class UDPConInfoListening extends ConInfoListening<DatagramChannel> {

	private boolean _isReaderReady = false;
	
	UDPConInfoListening(ChannelInfoListening<DatagramChannel> chInfoL,
			INIO_A_Listener aListener, INIO_R_Listener dRListener,
			INIO_W_Listener dWListener) {
		super(chInfoL, aListener, dRListener, dWListener);
	}

	public void makeCancelled() {
		setState(ConnectionInfoListeningStatus.L_CANCELLED);
		try {
			_chInfoL._channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		notifyAllListeners();
	}
	
	protected void makeConnected() {
		NIOBinding myNIOBinding = (NIOBinding) getNIOBinding();
		if ( _isReaderReady == false ){
			_isReaderReady = true;
			myNIOBinding.updateReaderCount(1);
			
			SelectionKey key = _chInfoL.getKey();
			_chInfoL.getNIOBinding().interestedInReads(key);
		}
		notifyAllListeners();
	}

	@Override
	public ChannelTypes getChannelType() {
		return ChannelTypes.UDP;
	}
}
