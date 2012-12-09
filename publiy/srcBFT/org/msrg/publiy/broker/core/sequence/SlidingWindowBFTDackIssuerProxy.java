package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.core.connectionManager.BFTConnectionManager;

public class SlidingWindowBFTDackIssuerProxy extends SimpleBFTDackIssuerProxy {

	protected final static long WINDOW_LENGTH_MS = BFTConnectionManager.BFT_DACK_RECEIVE_INTERVAL;
	
	protected final List<Long> _dackReceiptTimes = new LinkedList<Long>();
	
	public SlidingWindowBFTDackIssuerProxy(InetSocketAddress dackIAddr,
			InetSocketAddress dackVAddr, long dackReceiptTimeout) {
		super(dackIAddr, dackVAddr, dackReceiptTimeout);
	}

	@Override
	protected void updateLastDackReceivedTime(long currTime) {
		super.updateLastDackReceivedTime(currTime);
		_dackReceiptTimes.add(currTime);
		
		for(long oldestDackReceiptTime = _dackReceiptTimes.get(0) ; currTime - oldestDackReceiptTime >= WINDOW_LENGTH_MS && _dackReceiptTimes.size() > 0 ; _dackReceiptTimes.remove(0))
			;
	}
	
	
	@Override
	public boolean isDackReciptTooLate() {
		if(_dackReceiptTimes.size() == 0)
			return false;

		long currTime = SystemTime.currentTimeMillis();
		for(long oldestDackReceiptTime = _dackReceiptTimes.get(0) ; currTime - oldestDackReceiptTime >= WINDOW_LENGTH_MS && _dackReceiptTimes.size() > 0 ; _dackReceiptTimes.remove(0))
			;
		
		return _dackReceiptTimes.size() == 0;
	}		

}
