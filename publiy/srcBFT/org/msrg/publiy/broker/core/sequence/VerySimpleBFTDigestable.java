package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

import org.msrg.publiy.utils.security.dsa.DigestUtils;

public class VerySimpleBFTDigestable extends SimpleBFTDigestable {

	public VerySimpleBFTDigestable(byte[] digest) {
		super(null, null);
		_digest = digest;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!IBFTDigestable.class.isAssignableFrom(obj.getClass()))
			return false;
		
		byte[] digest = ((IBFTDigestable)obj).computeDigest(DigestUtils.getInstance());
		if(_digest.length != digest.length)
			return false;
		for(int i=0 ; i<_digest.length ; i++)
			if(_digest[i] != digest[i])
				return false;
		return true;
	}
	
	@Override
	public InetSocketAddress getSource() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public BFTDigestableType getType() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public byte[] getDigest() {
		return _digest;
	}
	
	@Override
	public byte[] computeDigest(DigestUtils du) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isMutable() {
		return true;
	}
}
