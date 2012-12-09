package org.msrg.publiy.utils;

public class ElementTotal<T>{
	public final long _earliestTime;
	public final long _latestTime;
	
	public final double _total;
	public final int _totaledValuesCount;
	public final T _collection;
	
	protected ElementTotal(T collection, long earliest, long latestTime, double total, int averagedValuesCount){
		_earliestTime = earliest;
		_latestTime = latestTime;
		_total = total;
		_totaledValuesCount = averagedValuesCount;
		_collection = collection;
	}
	
	public final double getAverage() {
		if (_totaledValuesCount == 0)
			return -1;
		else
			return _total / _totaledValuesCount;
	}
	
	public T getCollection(){
		return _collection;
	}
	
	@Override
	public String toString(){
		return "" + _total;
	}
}
