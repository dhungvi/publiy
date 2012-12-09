package org.msrg.publiy.pubsub.multipath.loadweights;

import org.msrg.publiy.utils.ListBucketElement;
import org.msrg.publiy.utils.SystemTime;

enum ProfilerBucketIndex{
	PUBLICATIONS	(0);
//	SUBSCRIPTIONS	(1),
//	JOINS			(2),
//	CONFS			(3);
	
	public final int _index;
	ProfilerBucketIndex(int index){
		_index = index;
	}
}

public class ProfilerBucket implements ListBucketElement {
	private boolean _invalid = false;
	private long _lastUpdateTime = -1;
	private final long _creationTime = SystemTime.currentTimeMillis();
	private final int _numberOfProfiledValues;
	private final int[] _updateCounters;
	private final int[] _values;
	
	
	public ProfilerBucket(int numberOfProfiledValues) {
		_numberOfProfiledValues = numberOfProfiledValues;
		_updateCounters = new int[_numberOfProfiledValues];
		_values = new int[_numberOfProfiledValues];
	}
	
	@Override
	public void invalidate(){
		_invalid = true;
	}
	
	@Override
	public boolean isValid(long validityPeriod) {
		return !_invalid && (_creationTime + validityPeriod >  SystemTime.currentTimeMillis());
	}
	
	@Override
	public synchronized void addToBucket(int i, int val){
		if ( _invalid )
			throw new IllegalStateException("Bucket is invalid");
		
		_updateCounters[i] += 1;
		_values[i] += val;
		_lastUpdateTime = SystemTime.currentTimeMillis();
	}
	
	@Override
	public long getCreationTime(){
		return _creationTime;
	}
	
	@Override
	public long getElementTime() {
		return _lastUpdateTime;
	}

	@Override
	public double getValue(int i) {
		if ( i>=0 && i<getValuesCount() )
			return _values[i];
		else
			throw new IllegalArgumentException("i: " + i);
	}

	@Override
	public int getUpdateCounter(int i){
		return _updateCounters[i];
	}
	
	@Override
	public int getValuesCount() {
		return ProfilerBucketIndex.values().length;
	}

	public String toString(){
		String str = "(";
		for ( int i=0 ; i<_values.length ; i++ )
			str += _values[i] + ((i==_values.length-1)?"":",");
		
		return str + ")";
	}
}
