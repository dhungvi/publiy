package org.msrg.publiy.publishSubscribe;

import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.utils.security.dsa.DigestUtils;

import org.msrg.publiy.broker.core.sequence.BFTDigestableType;
import org.msrg.publiy.broker.core.sequence.IBFTDigestable;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.core.sequence.SimpleBFTDigestable;


public class BFTPublication extends Publication implements IBFTDigestable {

	/**
	 * Auto generated.
	 */
	private static final long serialVersionUID = -7068808883635897335L;
	
	protected final InetSocketAddress _sAddr;
	protected IBFTDigestable _digestable;

	public BFTPublication(Publication publication, InetSocketAddress sourceAddress) {
		this(sourceAddress);

		super._noSourceRoute = publication._noSourceRoute;
		super._stringPredicates = publication._stringPredicates;
		super._predicates = publication._predicates;
		super._serialID = publication._serialID;
	}

	public BFTPublication(InetSocketAddress sAddr) {
		super();
		
		_sAddr = sAddr;
	}
	
	@Override
	public BFTPublication addStringPredicate(String att, String value) {
		if(!isMutable())
			throw new IllegalStateException();
		
		return (BFTPublication) super.addStringPredicate(att, value);
	}

	@Override
	public BFTPublication addPredicate(String att, int value) {
		if(!isMutable())
			throw new IllegalStateException();
		
		return (BFTPublication) super.addPredicate(att, value);
	}

	@Override
	public boolean isMutable() {
		return _digestable == null;
	}
	
	@Override
	public String encode() {
		return encode(this);
	}
	
	public static String encode(BFTPublication publication) {
		if(publication == null)
			return "NULL";
		
		StringWriter writer = new StringWriter();
		writer.append(publication._sAddr.toString());
		writer.append('\t');
		
		SortedSet<String> sortedAttributeSet = new TreeSet<String>(publication._predicates.keySet());
		for(String attr : sortedAttributeSet) {
			List<Integer> values = publication._predicates.get(attr);
			for(Integer value : values)
				writer.append("\t" + attr + "\t" + value);
		}

		SortedSet<String> sortedStringAttributeSet = new TreeSet<String>(publication._stringPredicates.keySet());
		for(String attr : sortedStringAttributeSet) {
			List<String> values = publication._stringPredicates.get(attr);
			for(String value : values)
				writer.append("\t" + attr + "\t" + value);
		}
		
		return writer.toString();
	}
	
	public static BFTPublication decode(String str) {
		if(str.equals("NULL"))
			return null;
		
		StringTokenizer strT = new StringTokenizer(str);
		String ipPort = strT.nextToken();
		InetSocketAddress sourceAddr = Sequence.readAddressFromString(ipPort);
		BFTPublication publication = new BFTPublication(sourceAddr);
		while(strT.hasMoreTokens()) {
			String attr = strT.nextToken();
			String value = strT.nextToken();
			try{
				int intValue = new Integer(value).intValue();
				publication.addPredicate(attr, intValue);
			} catch (Exception ex) {
				publication.addStringPredicate(attr, value);
			}
		}
		return publication;
	}
	
	@Override
	public Publication getClone() {
		return duplicate(new BFTPublication(_sAddr));
	}
	
	@Override
	public Publication duplicate(Publication publication) {
		Publication retPublication = super.duplicate(publication);
		((BFTPublication)retPublication)._digestable = ((BFTPublication)publication)._digestable;
		return retPublication;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!this.getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		BFTPublication pubObj = (BFTPublication) obj;
		return pubObj._sAddr.equals(_sAddr) &&
				pubObj._predicates.equals(_predicates) &&
				pubObj._stringPredicates.equals(_stringPredicates);
	}
	
	@Override
	public InetSocketAddress getSource() {
		return _sAddr;
	}

	@Override
	public BFTDigestableType getType() {
		return BFTDigestableType.BTF_PUBLICATION;
	}

	@Override
	public byte[] getDigest() {
		return computeDigest(DigestUtils.getInstance());
	}
	
	public byte[] getBytes() {
		return (_sAddr + super.encode(this)).getBytes();
	}
	
	@Override
	public byte[] computeDigest(DigestUtils du) {
		if(_digestable == null) {
			_digestable = new SimpleBFTDigestable(_sAddr, getBytes());
		}
		
		return _digestable.computeDigest(du);
	}

	@Override
	public String toString() {
		return "BFTPub("+ _sAddr + ": " + _predicates + _stringPredicates + ")";
	}
}
