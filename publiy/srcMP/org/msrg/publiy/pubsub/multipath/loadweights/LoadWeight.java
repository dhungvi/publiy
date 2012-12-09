package org.msrg.publiy.pubsub.multipath.loadweights;

import java.net.InetSocketAddress;

import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.communication.core.packet.types.TLoadWeight;

public class LoadWeight {

	public final long VALIDITY_PERIOD_MS = 10000;
	
	public final InetSocketAddress _remote;
	public final double _normalizedLoadWeight;
	public final long _creationTimestamp;
	public final int _currentSentLoad;
	
	public LoadWeight(TLoadWeight tLoadWeight, int currentSentLoad) {
		_remote = tLoadWeight.getInetSocketAddress();
		_normalizedLoadWeight = tLoadWeight.getNormalizedLoadWeight();
		_creationTimestamp = SystemTime.currentTimeMillis();
		_currentSentLoad = currentSentLoad;
	}
	
	public double getNormalizedLoadWeight() {
		return _normalizedLoadWeight;
	}
	
	public InetSocketAddress getRemoteAddress() {
		return _remote;
	}
	
	public int getCurrentLoad() {
		return _currentSentLoad;
	}
	
	public int getCappedLoad() {
		return (int) (_currentSentLoad * _normalizedLoadWeight);
	}
	
	public boolean isValid() {
		return _creationTimestamp + VALIDITY_PERIOD_MS > SystemTime.currentTimeMillis();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj==null)
			return false;
		
		if (!obj.getClass().isInstance(this))
			return false;
		
		else
			return ((LoadWeight)obj)._remote.equals(_remote);
	}
	
	public String toStringLong() {
		return "LW " + _remote + ": [" + _currentSentLoad + ", " + _normalizedLoadWeight + "]";
	}

	@Override
	public String toString() {
		return "LW[" + _currentSentLoad + ", " + _normalizedLoadWeight + "]";
	}
}
