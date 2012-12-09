package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

public interface IBFTDackVerifier {

	public InetSocketAddress getDackIsserAddress();
	public InetSocketAddress getDackVerifierAddress();

//	public boolean isDiscardedByVerifier(SequencePair sp);
//	public boolean isReceivedByVerifier(SequencePair sp);
	
	public boolean dackReceived(byte[] dackDigest, SequencePair sp);
	public void dackReceived(IBFTVerifiable dack);
	
}
