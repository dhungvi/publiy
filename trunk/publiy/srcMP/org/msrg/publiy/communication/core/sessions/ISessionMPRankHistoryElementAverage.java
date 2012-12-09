package org.msrg.publiy.communication.core.sessions;

import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerMP;
import org.msrg.publiy.utils.ElementTotal;
import org.msrg.publiy.utils.LimittedSizeList;

public class ISessionMPRankHistoryElementAverage extends ElementTotal<LimittedSizeList<ISessionMPRank>> implements Comparable<ISessionMPRankHistoryElementAverage>{
	private static int COUNTER = 0;
	private int _COUNTER = COUNTER++;
	
	private final ElementTotal<LimittedSizeList<ISessionMPRank>> _elementAverage;
	private final boolean _valid;
	private final int _rank;
	
	public ISessionMPRankHistoryElementAverage(ElementTotal<LimittedSizeList<ISessionMPRank>> elementTotal, int rank){
		super(elementTotal._collection, elementTotal._earliestTime, elementTotal._latestTime, elementTotal._total, elementTotal._totaledValuesCount);
		_elementAverage = elementTotal;
		if ( _totaledValuesCount >= ConnectionManagerMP.RANKS_HISTORY )
			_valid = true;
		else
			_valid = false;
		
		_rank = rank;
	}
	
	public int getRank(){
		return _rank;
	}
	
	private int compareCOUNTER(ISessionMPRankHistoryElementAverage o){
		if ( _COUNTER > o._COUNTER )
			return -1;
		else if ( _COUNTER < o._COUNTER )
			return 1;
		else
			throw new IllegalArgumentException(this + " vs. " + o);
	}
	
	@Override
	public int compareTo(ISessionMPRankHistoryElementAverage o) {
		if ( !_valid ){
			if ( !o._valid )
				return compareCOUNTER(o);
			else
				return -1;
		}
		else if ( !o._valid )
			return o.compareTo(this);
			
		if ( _total < o._total )
			return 1;
		else if ( _total > o._total )
			return -1;
		else
			return compareCOUNTER(o);
	}

	public ISessionMP getSession(){
		LimittedSizeList<ISessionMPRank> rank = _elementAverage._collection;
		ISessionMP session = rank.getLastElement().getISessionMP();
		return session;
	}
}
