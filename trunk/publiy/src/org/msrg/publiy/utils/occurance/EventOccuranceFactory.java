package org.msrg.publiy.utils.occurance;

public class EventOccuranceFactory {

	public static IEventOccurance createNewTimedEventOccurance(long maxTotalDelay, Object obj, String str){
		IEventOccurance eventOccurance = new TimedEventOccurance(maxTotalDelay, obj, str);
		return eventOccurance;
	}
	
	public static IEventOccurance createNewCounterEventOccurance(int maxCounter, Object obj){
		IEventOccurance eventOccurance = new CountedEventOccurance(maxCounter, obj);
		return eventOccurance;
	}
}
