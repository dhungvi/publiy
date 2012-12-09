package org.msrg.publiy.pubsub.core.messagequeue;

public class MQCleanupInfo {

	public final int _size;
	public final int _recentlyDiscarded;
	
	MQCleanupInfo(int size, int recentlyDiscarded){
		_size = size;
		_recentlyDiscarded = recentlyDiscarded;
	}
	
	@Override
	public String toString(){
		return "MQInfo[" + _recentlyDiscarded + ", " + _size + "]";
	}
	
}
