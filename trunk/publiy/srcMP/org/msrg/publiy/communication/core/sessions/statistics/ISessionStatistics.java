package org.msrg.publiy.communication.core.sessions.statistics;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.communication.core.sessions.ISessionMP;

public abstract class ISessionStatistics implements Comparable<ISessionStatistics>{

	public final ISessionMP _session;
	public final long _time;
	private final int _validityPeriod;
	private final double _statistics;
	private int _rank = -1;
	
	ISessionStatistics(ISessionMP session, int validityPeriod, double statistics){
		if ( session == null )
			throw new NullPointerException();
		
		_session = session;
		_time = SystemTime.currentTimeMillis();
		_validityPeriod = validityPeriod;
		_statistics = statistics;
	}
	
	public void setRank(int rank){
		if ( _rank == -1 )
			throw new IllegalStateException("Rank already set: " + _rank + " vs. " + rank);
		
		if ( rank < 0 )
			throw new IllegalArgumentException("Rank cannot be negative: " + rank);
		
		_rank = rank;
	}

	public int getRank(){
		if ( _rank < 0 )
			throw new IllegalStateException("Rank has not been set yet");
		
		return _rank;
	}
	
	public ISessionMP getSession(){
		return _session;
	}
	
	public double getStatistics(){
		return _statistics;
	}
	
	public boolean isValid(long now){
		return now < _time + _validityPeriod;
	}
	
	public boolean isValid(){
		return isValid(SystemTime.currentTimeMillis());
	}
	
	@Override
	public int compareTo(ISessionStatistics o){
		if ( o._statistics < this._statistics )
			return -1;
		else if ( o._statistics > this._statistics )
			return 1;
		else
			return _session.compareISessionMPCounters(o._session);
	}

	@Override
	public String toString(){
		return "<" + Writers.write(_session.getRemoteAddress()) + ":" + _statistics + ">";
	}
}
