package org.msrg.publiy.broker.core.contentManager;

import java.io.Serializable;
import java.net.InetSocketAddress;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimerReading;

public class ContentLifecycleEvent implements Serializable {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -6012735653655563799L;
	public final InetSocketAddress _senderRemote;
	public final BrokerInternalTimerReading _arrivalTime;
	
	ContentLifecycleEvent(InetSocketAddress senderRemote) {
		_senderRemote = senderRemote;
		_arrivalTime = BrokerInternalTimer.read();
	}
	
	public BrokerInternalTimerReading getArrivalTime() {
		return _arrivalTime;
	}
	
	public InetSocketAddress getSenderRemote() {
		return _senderRemote;
	}
	
	@Override
	public String toString() {
		return "ARRIVAL[F:" + (_senderRemote == null?'-':_senderRemote.getPort()) + '@' + _arrivalTime + "]";
	}
}
