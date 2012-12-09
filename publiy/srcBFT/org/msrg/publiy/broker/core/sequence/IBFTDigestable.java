package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

import org.msrg.publiy.utils.security.dsa.DigestUtils;

public interface IBFTDigestable {

	public InetSocketAddress getSource();
	public BFTDigestableType getType();
	public byte[] getDigest();
	public byte[] computeDigest(DigestUtils du);
	public boolean isMutable();

}
