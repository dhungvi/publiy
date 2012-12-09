package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

import org.msrg.publiy.utils.security.dsa.DigestUtils;

public class SimpleBFTDigestable implements IBFTDigestable {

	protected byte[] _digest;
	protected final byte[] _data;
	protected final InetSocketAddress _sAddr;
	
	public SimpleBFTDigestable(InetSocketAddress sAddr, byte[] data) {
		_sAddr = sAddr;
		_data = data;
	}
	
	@Override
	public InetSocketAddress getSource() {
		return _sAddr;
	}

	@Override
	public BFTDigestableType getType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] getDigest() {
		return computeDigest(DigestUtils.getInstance());
	}

	@Override
	public synchronized byte[] computeDigest(DigestUtils du) {
		if(_digest == null)
			return _digest = du.getDigest(_data);
		else
			return _digest;
	}

	@Override
	public boolean isMutable() {
		return _digest == null;
	}
}