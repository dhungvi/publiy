package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

public interface IBFTVerifierProxy extends IBFTDackIssuerProxy {

	public InetSocketAddress getVerifierAddress();
	public boolean equals(InetSocketAddress vaddr);
	public SequencePair issueNextSequencePair(IBFTDigestable digestable, IBFTIssuerProxy iProxy);
	public SequencePair issueCurrentSequencePair(IBFTDigestable digestable, IBFTIssuerProxy iProxy);
	
}
