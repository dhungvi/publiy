package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.List;

import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.IBFTSuspectable;

public interface IBFTIssuerProxy {
	
	public SequencePair getLastLocallySeenSequencePairFromProxy();
	public SequencePairVerifyResult verifySuccession(
			boolean isDack,
			boolean updateIProxySequencePairs,
			IBFTSuspectable suspectable,
			IBFTVerifiable verifiable,
			List<BFTSuspecionReason> reasons);
	public InetSocketAddress getIssuerAddress();
	public int hashCode();
	public boolean equals(Object obj);

	public PublicKey getPublicKey();

	public boolean equals(InetSocketAddress iAddr);
}
