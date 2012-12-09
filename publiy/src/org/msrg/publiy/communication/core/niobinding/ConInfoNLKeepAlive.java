package org.msrg.publiy.communication.core.niobinding;

import java.nio.channels.SocketChannel;
import java.util.Collection;

import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.niobinding.keepalive.FDReceiveTask;
import org.msrg.publiy.communication.core.niobinding.keepalive.FDSendTask;
import org.msrg.publiy.communication.core.niobinding.keepalive.FDTimer;
import org.msrg.publiy.communication.core.niobinding.keepalive.IConInfoNLKeepAlive;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;

class ConInfoNLKeepAlive extends TCPConInfoNonListening implements IConInfoNLKeepAlive {
	
	public static final long KA_RECIEVE_DEADLINE = 3000;
	public static final long KA_SEND_INTERVAL = 1000;
	
	protected FDSendTask _fdSendTask;
	protected FDReceiveTask _fdReceiveTask;
	protected FDTimer _fdTimer;
	protected final Object _lock = new Object();
	
	@Override
	boolean addIncomingDataToList(IRawPacket raw) {
		switch(raw.getType()) {
		case THEARTBEAT:
			updateLastReceiveTime(raw);
			return true;
			
		default:
			return super.addIncomingDataToList(raw);
		}
	}
	
	public ConInfoNLKeepAlive(IBrokerShadow brokerShadow, ISession session, ChannelInfoNonListening<SocketChannel> chNL, INIOListener listener,
			INIO_R_Listener listener2, Collection<INIO_W_Listener> listeners, FDTimer fdTimer) {
		super(brokerShadow, session, chNL, listener, listener2, listeners);
		_fdTimer = fdTimer;
		updateLastReceiveTime(null);
	}
	
	@Override
	public void makeConnected() {
		super.makeConnected();
		updateLastReceiveTime(null);
		updateNextSendTime();
	}
	
	@Override
	public void makeCancelled() {
		synchronized(_lock) {
			if(_fdSendTask != null)
				_fdSendTask.cancel();
			if(_fdReceiveTask != null)
				_fdReceiveTask.cancel(); 
			super.makeCancelled();
		}
	}
	
	@Override
	public void makeCancelledSilently() {
		synchronized(_lock) {
			if(_fdSendTask != null)
				_fdSendTask.cancel();
			if(_fdReceiveTask != null)
				_fdReceiveTask.cancel(); 
			super.makeCancelledSilently();
		}
	}

	@Override
	public void updateLastReceiveTime(IRawPacket rawHeartBeat) {
		synchronized(_lock) {
			if(_fdReceiveTask != null)
				_fdReceiveTask.cancel();
			_fdReceiveTask = new FDReceiveTask(this);
			_fdTimer.schedule(_fdReceiveTask, KA_RECIEVE_DEADLINE);
		}
	}
	
	@Override
	public void updateNextSendTime() {
		synchronized(_lock) {
			if(_fdSendTask != null)
				_fdSendTask.cancel();
			_fdSendTask = new FDSendTask(_localSequencer, this);
			_fdTimer.schedule(_fdSendTask, KA_SEND_INTERVAL);
		}
	}
}
