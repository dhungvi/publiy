package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

import org.msrg.publiy.broker.BrokerIdentityManager;

public interface IBFTVerifiable {
	
	public InetSocketAddress getSourceAddress();
	
	public IBFTDigestable getDigestable();
	
	public SequencePair addSequencePair(SequencePair sp);
	public SequencePair addSequencePair(IBFTIssuer issuer, InetSocketAddress vAddr, long e, int o);
	public void addSequencePairs(Collection<SequencePair> verifiersSequencePairs);

	public SequencePair getSequencePair(InetSocketAddress iAddr, InetSocketAddress vAddr);
	public SequencePair getSequencePair(IBFTIssuerProxy iProxy, IBFTVerifier verifier);
	public List<SequencePair> getSequencePairs(InetSocketAddress iAddr);
	public List<SequencePair> getSequencePairs(IBFTIssuerProxy iProxy);
	public List<SequencePair> getSequencePairs(IBFTVerifier verifier);
	public List<SequencePair> getSequencePairs();

	public int getValidSequencePairsCount();
	public int getInvalidSequencePairsCount();

	public String toString(BrokerIdentityManager idMan);
	
}
