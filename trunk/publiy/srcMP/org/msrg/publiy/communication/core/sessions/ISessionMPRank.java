package org.msrg.publiy.communication.core.sessions;

import org.msrg.publiy.utils.ListElement;
import org.msrg.publiy.utils.SystemTime;

public class ISessionMPRank implements ListElement {

	private final ISessionMP _sessionMP;
	private final long _elementTime;
	private final int _rank;
	private final double _value;
	
	ISessionMPRank(ISessionMP sessionMP, int rank, double value){
		_sessionMP = sessionMP;
		_elementTime = SystemTime.currentTimeMillis();
		_rank = rank;
		_value = value;
	}
	
	public ISessionMP getISessionMP(){
		return _sessionMP;
	}
	
	@Override
	public long getElementTime() {
		return _elementTime;
	}

	@Override
	public double getValue(int i) {
		switch(i){
		case 0:
			return _rank;
			
		default:
			throw new UnsupportedOperationException("i: " + i);	
		}
	}

	@Override
	public int getValuesCount() {
		return 1;
	}
	
	public String toStringLong() {
		return _sessionMP.toStringElaborate() + " ranked: " + _rank;
	}

	@Override
	public String toString() {
		return _sessionMP.toStringShort() + " ranked: " + _rank + "[" + _value + "]";
	}
}
