package org.msrg.publiy.utils.occurance;

public abstract class IEventOccurance {

	protected EventStatus _eventStatus;
	protected final Object _lock = new Object();
	
	protected IEventOccurance(){
		_eventStatus = EventStatus.PENDING;
	}
	
	public EventStatus getStatus(){
		return _eventStatus;
	}
	
	public final synchronized void pending(){
		synchronized(_lock){
			pendingPrivate(_eventStatus == EventStatus.SUCCESS);
		}
	}
	
	public final synchronized void pending(boolean reset){
		synchronized(_lock){
			pendingPrivate(reset);
		}
	}
	
	private final synchronized void pendingPrivate(boolean reset){
		if ( reset )
			reset();
		
		transitEventState((byte)(EventStatus.SUCCESS.getByte() | EventStatus.PENDING.getByte())
						, EventStatus.PENDING);
	}
	
	public final void success(){
		synchronized(_lock){
			transitEventState(EventStatus.PENDING.getByte(), EventStatus.SUCCESS);
			reset();
		}
	}
	
	public final void fail(){
		synchronized(_lock){
			transitEventState(EventStatus.PENDING.getByte(), EventStatus.FAILED);
			reset();
		}
	}
	
	public final boolean isFailed(){
		synchronized(_lock){
			return _eventStatus == EventStatus.FAILED;
		}
	}
	
	public final boolean isPending(){
		synchronized(_lock){
			return _eventStatus == EventStatus.PENDING;
		}
	}
	
	private final void transitEventState(byte fromBitVector, EventStatus to){
		if( (_eventStatus.getByte()&fromBitVector) == 0 )
			throw new IllegalStateException("Already started: " + _eventStatus);
		else
			_eventStatus = to;
	}
	
	public abstract boolean test();
	public abstract void reset();
}
