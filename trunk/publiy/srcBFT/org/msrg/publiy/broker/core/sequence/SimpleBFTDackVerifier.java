package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

public class SimpleBFTDackVerifier implements IBFTDackVerifier {

	protected final InetSocketAddress _dackVerifier;
	protected final InetSocketAddress _dackIssuerAddr;
//	protected SequencePair _lastDacked;
	protected int _lastReceivedOrder;
	protected int _lastDiscardedOrder;
	
	public SimpleBFTDackVerifier(InetSocketAddress dackIssuerAddr, InetSocketAddress dackVerifierAddr) {
		_dackIssuerAddr = dackIssuerAddr;
		_dackVerifier = dackVerifierAddr;
		_lastReceivedOrder = _lastDiscardedOrder = 0;
	}

//	@Override
//	public boolean isReceivedByVerifier(SequencePair sp) {
//		checkSequencePair(sp);
//		
//		if(sp._lastReceivedOrder == 0)
//			return true;
//		
//		if(_lastReceivedOrder >= sp._lastReceivedOrder)
//			return true;
//		else
//			return false;
//	}
//	
//	@Override
//	public boolean isDiscardedByVerifier(SequencePair sp) {
//		checkSequencePair(sp);
//		
//		if(sp._lastDiscardedOrder == 0)
//			return true;
//		
//		if(_lastDiscardedOrder >= sp._lastDiscardedOrder)
//			return true;
//		else
//			return false;
//	}

	/* Note: The role of isser/verfier is inversed. */
	protected void checkSequencePair(byte[] digest, SequencePair sp) {
		if(!sp.getVerifierAddress().equals(getDackVerifierAddress()))
			throw new IllegalArgumentException("Issuer address mismatch: " + getDackVerifierAddress() + " v. " + sp.getVerifierAddress());
		if(!sp.getIssuerAddress().equals(getDackIsserAddress()))
			throw new IllegalArgumentException("Verifier address mismatch: " + getDackIsserAddress() + " v. " + sp.getIssuerAddress());
		if(!sp.isValid(digest))
			throw new IllegalArgumentException("Sequence pair is not valid: " + sp);
	}

	@Override
	public boolean dackReceived(byte[] dackDigest, SequencePair sp) {
		if(sp == null)
			return false;
		
		checkSequencePair(dackDigest, sp);
		
		if((_lastReceivedOrder <= sp._lastReceivedOrder) ^ (_lastDiscardedOrder <= sp._lastDiscardedOrder))
			throw new IllegalStateException("This cannot happen: " + _lastReceivedOrder + "/" + sp._lastReceivedOrder + " vs. " + _lastDiscardedOrder + " v. " + sp._lastDiscardedOrder);
		
		boolean ret = false;
		if(_lastReceivedOrder <= sp._lastReceivedOrder) {
			_lastReceivedOrder = sp._lastReceivedOrder;
			ret = true;
		}
		
		if(_lastDiscardedOrder <= sp._lastDiscardedOrder) {
			_lastDiscardedOrder = sp._lastDiscardedOrder;
			ret = true;
		}
		
		return ret;
	}

	@Override
	public InetSocketAddress getDackIsserAddress() {
		return _dackIssuerAddr;
	}

	@Override
	public InetSocketAddress getDackVerifierAddress() {
		return _dackVerifier;
	}

	@Override
	public void dackReceived(IBFTVerifiable dack) {
		throw new UnsupportedOperationException("Can only process dackReceived(SequencePair sp). Sorr!");
	}
}
