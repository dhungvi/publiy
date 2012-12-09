package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

public interface IBFTDackIssuerProxy {

	public InetSocketAddress getDackIsserAddress();
	public InetSocketAddress getDackVerifierAddress();
	public int getLastProxyReceivedOrderFromMe();
	public int getLastProxyDiscardedOrderFromMe();
	public boolean setLastProxyReceivedOrderFromMe(SequencePair receivedSP);
	public boolean setLastProxyDiscardedOrderFromMe(SequencePair discardedSP);
	
	public void setDACKReceiptTimeout(long dackReceiptTimeout);
	public long getLastDackReceivedTime();
	public boolean isDackReciptTooLate();
	
	public boolean hasDiscardedSequencePair(SequencePair sp);
	public boolean hasReceivedSequencePair(SequencePair sp);
	
}
