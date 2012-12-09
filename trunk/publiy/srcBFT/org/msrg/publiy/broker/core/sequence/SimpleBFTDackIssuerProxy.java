package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

import org.msrg.publiy.utils.SystemTime;

public class SimpleBFTDackIssuerProxy implements IBFTDackIssuerProxy {

	protected final InetSocketAddress _dackIAddr;
	protected final InetSocketAddress _dackVAddr;
	protected long _dackReceiptTimeout;
	protected int _lastReceivedOrder;
	protected int _lastDiscardedOrder;
	protected long _lastDackRecievedTime;
	
	public SimpleBFTDackIssuerProxy(InetSocketAddress dackIAddr, InetSocketAddress dackVAddr, long dackReceiptTimeout) {
		_dackIAddr = dackIAddr;
		_dackVAddr = dackVAddr;
		_lastReceivedOrder = _lastDiscardedOrder = 0;
		_lastDackRecievedTime = -1;
		setDACKReceiptTimeout(dackReceiptTimeout);
	}
	
	@Override
	public void setDACKReceiptTimeout(long dackReceiptTimeout) {
		_dackReceiptTimeout = dackReceiptTimeout;
	}

	@Override
	public InetSocketAddress getDackIsserAddress() {
		return _dackIAddr;
	}

	@Override
	public InetSocketAddress getDackVerifierAddress() {
		return _dackVAddr;
	}

	@Override
	public int getLastProxyReceivedOrderFromMe() {
		return _lastReceivedOrder;
	}

	@Override
	public int getLastProxyDiscardedOrderFromMe() {
		return _lastDiscardedOrder;
	}

	@Override
	public boolean setLastProxyReceivedOrderFromMe(SequencePair receivedSP) {
		checkSequencePair(receivedSP);
		
		if(receivedSP._lastReceivedOrder >= _lastReceivedOrder) {
			updateLastDackReceivedTime(SystemTime.currentTimeMillis());
			_lastReceivedOrder = receivedSP._lastReceivedOrder;
			return true;
		}
		
		return false;
	}

	private void checkSequencePair(SequencePair dackSP) {
		if(!dackSP.getIssuerAddress().equals(getDackIsserAddress()))
			throw new IllegalArgumentException("Dack issuer mismatch: " + dackSP.getIssuerAddress() + " vs. " + getDackIsserAddress());
		if(!dackSP.getVerifierAddress().equals(getDackVerifierAddress()))
			throw new IllegalArgumentException("Dack verifier mismatch: " + dackSP.getVerifierAddress() + " vs. " + getDackVerifierAddress());
	}

	@Override
	public boolean setLastProxyDiscardedOrderFromMe(SequencePair discardedSP) {
		checkSequencePair(discardedSP);
		
		if(discardedSP._lastDiscardedOrder >= _lastDiscardedOrder) {
			updateLastDackReceivedTime(SystemTime.currentTimeMillis());
			_lastDiscardedOrder = discardedSP._lastDiscardedOrder;
			return true;
		}
		
		return false;
	}

	@Override
	public long getLastDackReceivedTime() {
		return _lastDackRecievedTime;
	}
	
	@Override
	public boolean isDackReciptTooLate() {
		// Only consider dack as late if you have received one before.
		// This is to avoid false positives in the beginning of the execution.
		if(_lastDackRecievedTime < 0)
			return false;
		
		return SystemTime.currentTimeMillis() - _lastDackRecievedTime >= _dackReceiptTimeout;
	}
	
	protected void updateLastDackReceivedTime(long currTime) {
		_lastDackRecievedTime = currTime;
	}

	@Override
	public boolean hasDiscardedSequencePair(SequencePair sp) {
		if(_lastDackRecievedTime == -1)
			return false;
//			return sp._epoch == 0 && sp._order == 0;
		
		return _lastDiscardedOrder >= sp._order;
	}

	@Override
	public boolean hasReceivedSequencePair(SequencePair sp) {
		if(_lastDackRecievedTime == -1)
			return false;
//		return sp._epoch == 0 && sp._order == 0;
		
		return _lastReceivedOrder >= sp._order;
	}
}
