package org.msrg.publiy.utils.occurance;

public class CountedEventOccurance extends IEventOccurance {

	private final int _maxSuccessCount;
	private int _counter;
	
	CountedEventOccurance(int maxSuccessCount, Object obj){
		_maxSuccessCount = maxSuccessCount;
		_counter = 0;
	}
	
	public boolean test(){
		synchronized(_lock){
			if ( isFailed() )
				throw new IllegalStateException("" + getStatus());

			_counter++;
			
			return _maxSuccessCount >= _counter;
		}
	}
	
	public void reset(){
		synchronized(_lock){
			_counter = 0;
		}
	}
}
