package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.msrg.publiy.broker.BrokerIdentityManager;

import org.msrg.publiy.utils.BFTWriters;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.BFTDSALogger;

public class SimpleBFTVerifiable implements IBFTVerifiable {

	protected byte[] _data;
	protected final List<SequencePair> _splist = new LinkedList<SequencePair>();
	protected final IBFTDigestable _digestable;
	
	public SimpleBFTVerifiable(IBFTDigestable digestable) {
		_digestable = digestable;
	}
	
	@Override
	public IBFTDigestable getDigestable() {
		return _digestable;
	}

	@Override
	public List<SequencePair> getSequencePairs(IBFTIssuerProxy issuer) {
		return getSequencePairs(issuer.getIssuerAddress());
	}

	@Override
	public SequencePair addSequencePair(SequencePair sp) {
		if(sp == null)
			return null;
		
		if(_data!=null)
			throw new IllegalStateException();
		
		if(sp.canValidateDigest())
			if(!sp.isValid(_digestable.getDigest()))
				throw new IllegalStateException(sp + " vs. " + this);
		
		InetSocketAddress iAddr = sp.getIssuerAddress();
		InetSocketAddress vAddr = sp.getVerifierAddress();
		// Remove any existing sp
		ListIterator<SequencePair> it = _splist.listIterator();
		while(it.hasNext()) {
			SequencePair spIt = it.next();
			if(spIt.getVerifierAddress().equals(vAddr) && spIt.getIssuerAddress().equals(iAddr)) {
				LoggerFactory.getLogger().warn(LoggingSource.LOG_SRC_DUMMY_SOURCE, "Overwritting existing sequence pairs.");
//				it.remove();
				throw new IllegalStateException(spIt + " vs. " + sp);
			}
		}
		_splist.add(sp);
		return sp;
	}

	@Override
	public SequencePair addSequencePair(IBFTIssuer issuer, InetSocketAddress vAddr, long e, int o) {
		return addSequencePair(new SequencePair(issuer, e, o, vAddr, _digestable.getDigest()));
	}

	@Override
	public SequencePair getSequencePair(InetSocketAddress iAddr, InetSocketAddress vAddr) {
		for(SequencePair sp : _splist)
			if(sp._vAddr.equals(vAddr) && sp.getIssuerAddress().equals(iAddr))
				return sp;
		return null;
	}

	@Override
	public List<SequencePair> getSequencePairs(InetSocketAddress iAddr) {
		List<SequencePair> retlist = new LinkedList<SequencePair>();
		for(SequencePair sp : _splist)
			if(sp.getIssuerAddress().equals(iAddr))
				retlist.add(sp);

		return retlist;
	}

	@Override
	public List<SequencePair> getSequencePairs() {
		return _splist;
	}
	
	// Encoding format: ##digestable##[spsize,sp]*
	public byte[] encode() {
		if(_data!=null)
			return _data;
		
		byte[] digest = _digestable.getDigest();
		int datasize = 2 + digest.length + 2;
		for(SequencePair sp : _splist)
			datasize += sp.getContentSize() + Byte.SIZE/8;
		
		byte[] data = new byte[datasize];
		int offset = 0;
		data[offset++] = (byte)(0xFF & digest.length>>8);
		data[offset++] = (byte)(0xFF & digest.length>>0);
		for(int i=0 ; i<digest.length ; i++)
			data[offset++] = digest[i];

		int splistsize = _splist.size();
		data[offset++] = (byte)(0xFF & splistsize>>8);
		data[offset++] = (byte)(0xFF & splistsize>>0);
		for(SequencePair sp : _splist) {
			byte[] spdata = sp.encode();
			data[offset++] = (byte) spdata.length;
			for(int i=0 ; i<spdata.length ; i++)
				data[i+offset] = spdata[i];
			offset += spdata.length;
		}
		
		return _data = data;
	}
	
	// Encoding format: ##digestable##[spsize,sp]*
	public static SimpleBFTVerifiable decode(BFTDSALogger dsaLogger, IBFTIssuerProxyRepo iProxyRepo, byte[] data, int offset) {
		int digestsize = (data[offset++] << 8) + data[offset++];
		byte[] digest = new byte[digestsize];
		for(int i=0 ; i<digestsize ; i++)
			digest[i] = data[offset++];
		IBFTDigestable digestable = new VerySimpleBFTDigestable(digest);
		SimpleBFTVerifiable bftVerifiable = new SimpleBFTVerifiable(digestable);
		
		int splistsize = data[offset++] << 8 | (data[offset++] & 0xFF) << 0;
		for(int i=0 ; i<splistsize ; i++) {
			byte spsize = data[offset++];
			try {
				SequencePair sp = SequencePair.decode(dsaLogger, iProxyRepo, data, offset);
				offset += spsize;
				bftVerifiable.addSequencePair(sp);
			} catch (IllegalStateException x) {
				System.out.println("XXX " + i + " vs. " + splistsize + " vs. " + data.length);
				throw x;
			}
		}
		bftVerifiable._data = data;
		return bftVerifiable;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(getClass().isAssignableFrom(obj.getClass())) {
			SimpleBFTVerifiable bftverifiable = (SimpleBFTVerifiable) obj;
			if(_data!=null && bftverifiable!=null) {
				if(_data.length!=bftverifiable._data.length)
					return false;
				for(int i=0 ; i<_data.length ; i++)
					if(_data[i]!=bftverifiable._data[i])
						return false;
				return true;
			}
		}
		
		if(!IBFTVerifiable.class.isAssignableFrom(obj.getClass()))
			return false;
		
		IBFTVerifiable bftVerifiable = (IBFTVerifiable) obj;
		
		byte[] digest = _digestable.getDigest();
		byte[] digestObj = bftVerifiable.getDigestable().getDigest();
		if(digest.length != digestObj.length)
			return false;
		for(int i=0 ; i<digest.length ; i++)
			if(digest[i] != digestObj[i])
				return false;
		
		List<SequencePair> splist = bftVerifiable.getSequencePairs();
		if(_splist.size() != splist.size())
			return false;
		for(SequencePair sp : _splist)
			if(!splist.contains(sp))
				return false;

		return true;
	}

	@Override
	public SequencePair getSequencePair(IBFTIssuerProxy iProxy, IBFTVerifier verifier) {
		return getSequencePair(iProxy.getIssuerAddress(), verifier.getVerifierAddress());
	}

	@Override
	public void addSequencePairs(Collection<SequencePair> sequencePairs) {
		for(SequencePair sequencePair : sequencePairs)
			addSequencePair(sequencePair);
	}

	@Override
	public int getValidSequencePairsCount() {
		int counter = 0;
		for(SequencePair sequencePair : _splist)
			if(sequencePair.canValidateDigest())
				if(sequencePair.isValid(_digestable.getDigest()))
					counter++;
		return counter;
	}

	@Override
	public int getInvalidSequencePairsCount() {
		int counter = 0;
		for(SequencePair sequencePair : _splist)
			if(sequencePair.canValidateDigest())
				if(!sequencePair.isValid(_digestable.getDigest()))
					counter++;
		return counter;
	}

	@Override
	public InetSocketAddress getSourceAddress() {
		return _digestable.getSource();
	}

	@Override
	public List<SequencePair> getSequencePairs(IBFTVerifier verifier) {
		InetSocketAddress vAddr = verifier.getVerifierAddress();
		List<SequencePair> retlist = new LinkedList<SequencePair>();
		for(SequencePair sp : _splist)
			if(sp.getVerifierAddress().equals(vAddr))
				retlist.add(sp);

		return retlist;
	}
	
	@Override
	public String toString() {
		return _digestable + BFTWriters.write(_splist);
	}

	@Override
	public String toString(BrokerIdentityManager idMan) {
		return _digestable + BFTWriters.write(idMan, _splist);
	}
}