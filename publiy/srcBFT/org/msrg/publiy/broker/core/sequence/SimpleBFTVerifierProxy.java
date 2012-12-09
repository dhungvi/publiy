package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.BrokerIdentityManager;


public class SimpleBFTVerifierProxy implements IBFTVerifierProxy {

	protected final BrokerIdentityManager _idMan;
	protected final IBFTIssuer _issuer;
	protected final InetSocketAddress _vAddr;
	protected SequencePair _lastSequencePairIssued;
	protected final IBFTDackIssuerProxy _dackIssuerProxy;
	
	public SimpleBFTVerifierProxy(BrokerIdentityManager idMan, IBFTIssuer isser, InetSocketAddress vAddr, long dackReceiptTimeout) {
		_issuer = isser;
		_vAddr = vAddr;
		_lastSequencePairIssued = null;
		_idMan = idMan;
		_dackIssuerProxy = new SimpleBFTDackIssuerProxy(vAddr, _issuer.getIssuerAddress(), dackReceiptTimeout);
//		_dackIssuerProxy = new SlidingWindowBFTDackIssuerProxy(vAddr, _issuer.getIssuerAddress(), dackReceiptTimeout);
	}
	
	@Override
	public InetSocketAddress getVerifierAddress() {
		return _vAddr;
	}

	@Override
	public boolean equals(InetSocketAddress vAddr) {
		return _vAddr.equals(vAddr);
	}

	@Override
	public SequencePair issueNextSequencePair(IBFTDigestable digestable, IBFTIssuerProxy iProxy) {
		int order = _lastSequencePairIssued == null ? 1 : _lastSequencePairIssued._order + 1;
		long epoch = _lastSequencePairIssued == null ? 0 : _lastSequencePairIssued._epoch;
		SequencePair lastLocallySeenSequencePair = iProxy == null ? null : iProxy.getLastLocallySeenSequencePairFromProxy();
		SequencePair nextSequencePair = lastLocallySeenSequencePair == null ?
				new SequencePair(_issuer, epoch, order, _vAddr, digestable) : 
					new SequencePair(_issuer, epoch, order, _vAddr, digestable, lastLocallySeenSequencePair._order, lastLocallySeenSequencePair._order);
		
		_lastSequencePairIssued = nextSequencePair;
//		BrokerInternalTimer.inform("Issued: " + _lastSequencePairIssued.toString(_idMan) + " vs. " + digestable);
		
		return _lastSequencePairIssued;
	}

	@Override
	public SequencePair issueCurrentSequencePair(IBFTDigestable digestable, IBFTIssuerProxy iProxy) {
		int order = _lastSequencePairIssued == null ? 0 : _lastSequencePairIssued._order;
		long epoch = _lastSequencePairIssued == null ? 0 : _lastSequencePairIssued._epoch;
		SequencePair lastLocallySeenSequencePair = iProxy == null ? null : iProxy.getLastLocallySeenSequencePairFromProxy();
		SequencePair newSequencePair = lastLocallySeenSequencePair == null ?
				new SequencePair(_issuer, epoch, order, _vAddr, digestable) : 
					new SequencePair(_issuer, epoch, order, _vAddr, digestable, lastLocallySeenSequencePair._order, lastLocallySeenSequencePair._order);
		
		return newSequencePair;
	}

	@Override
	public InetSocketAddress getDackIsserAddress() {
		return _dackIssuerProxy.getDackIsserAddress();
	}

	@Override
	public InetSocketAddress getDackVerifierAddress() {
		return _dackIssuerProxy.getDackVerifierAddress();
	}

	@Override
	public int getLastProxyReceivedOrderFromMe() {
		return _dackIssuerProxy.getLastProxyReceivedOrderFromMe();
	}

	@Override
	public int getLastProxyDiscardedOrderFromMe() {
		return _dackIssuerProxy.getLastProxyDiscardedOrderFromMe();
	}

	@Override
	public boolean setLastProxyReceivedOrderFromMe(SequencePair receivedSP) {
		return _dackIssuerProxy.setLastProxyReceivedOrderFromMe(receivedSP);
	}

	@Override
	public boolean setLastProxyDiscardedOrderFromMe(SequencePair discardedSP) {
		return _dackIssuerProxy.setLastProxyDiscardedOrderFromMe(discardedSP);		
	}

	@Override
	public long getLastDackReceivedTime() {
		return _dackIssuerProxy.getLastDackReceivedTime();
	}

	@Override
	public boolean isDackReciptTooLate() {
		return _dackIssuerProxy.isDackReciptTooLate();
	}

	@Override
	public boolean hasDiscardedSequencePair(SequencePair sp) {
		return _dackIssuerProxy.hasDiscardedSequencePair(sp);
	}

	@Override
	public boolean hasReceivedSequencePair(SequencePair sp) {
		return _dackIssuerProxy.hasReceivedSequencePair(sp);
	}

	@Override
	public void setDACKReceiptTimeout(long dackReceiptTimeout) {
		_dackIssuerProxy.setDACKReceiptTimeout(dackReceiptTimeout);
	}
}