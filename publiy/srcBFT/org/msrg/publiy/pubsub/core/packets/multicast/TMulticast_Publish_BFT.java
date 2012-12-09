package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.sequence.IBFTDigestable;
import org.msrg.publiy.broker.core.sequence.IBFTIssuer;
import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.IBFTVerifiable;
import org.msrg.publiy.broker.core.sequence.IBFTVerifier;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.core.sequence.SequencePair;
import org.msrg.publiy.broker.core.sequence.SimpleBFTVerifiable;


import org.msrg.publiy.publishSubscribe.BFTPublication;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.utils.BFTWriters;

public class TMulticast_Publish_BFT extends TMulticast_Publish implements IBFTVerifiable {
	
	protected final IBFTVerifiable _verifiable;

	protected TMulticast_Publish_BFT(TMulticastTypes tmType, BFTPublication publication, InetSocketAddress from, Sequence sourceSequence) {
		super(tmType, publication, from, sourceSequence);
		
		_verifiable = new SimpleBFTVerifiable(publication);		
	}

	public TMulticast_Publish_BFT(BFTPublication publication, InetSocketAddress from, Sequence sourceSequence) {
		this(TMulticastTypes.T_MULTICAST_PUBLICATION_BFT, publication, from, sourceSequence);
	}

	protected TMulticast_Publish_BFT(IBrokerShadow brokerShadow, ByteBuffer bdy, int contentSize, int subContentSize, String annotations) {
		super(bdy, contentSize, subContentSize, annotations);
		
		IBFTBrokerShadow bftBrokerShadow = (IBFTBrokerShadow)brokerShadow;
		_verifiable = SimpleBFTVerifiable.decode(bftBrokerShadow.getBFTDSALogger(), (bftBrokerShadow).getIssuerProxyRepo(), bdy.array(), bdy.position());
	}

	@Override
	public TMulticastTypes getType() {
		return TMulticastTypes.T_MULTICAST_PUBLICATION_BFT;
	}

	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_PUBLICATION_BFT;
	}

	public TMulticast_Publish_BFT(BFTPublication publication, InetSocketAddress from, byte[] payload, Sequence sourceSequence) {
		this(TMulticastTypes.T_MULTICAST_PUBLICATION_BFT, publication, from, payload, sourceSequence);
	}
	
	protected TMulticast_Publish_BFT(TMulticastTypes type, BFTPublication publication, InetSocketAddress from, byte[] payload, Sequence sourceSequence) {
		super(type, publication, from, payload, sourceSequence);
		
		_verifiable = new SimpleBFTVerifiable(publication);
	}

	@Override
	public BFTPublication readPublicationFromByteArray(byte[] bArray) {
		return BFTPublication.decode(new String(bArray));
	}

	@Override
	protected TMulticast_Publish_BFT createNewTMulticastPublish(Publication publication, InetSocketAddress from, byte[] workload, Sequence sourceSequence) {
		return new TMulticast_Publish_BFT((BFTPublication) publication, from, workload, sourceSequence);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		putObjectInBuffer(localSequencer, buff, true);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, boolean addLocalSeq) {
		super.putObjectInBuffer(localSequencer, buff, addLocalSeq);
		buff.put(((SimpleBFTVerifiable)_verifiable).encode());
	}
	
	@Override
	public final PubForwardingStrategy getPubForwardingStrategy() {
		return PubForwardingStrategy.PUB_FORWARDING_STRATEGY_0;
	}
	
	@Override
	public int getTMSpecificContentSize() {
		return super.getTMSpecificContentSize() + ((SimpleBFTVerifiable)_verifiable).encode().length;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(Sequence.class.isInstance(obj)) {
			Sequence oSequence = (Sequence) obj;
			return oSequence.equalsExact(_sourceSequence);
		}

		if(!super.equals(obj))
			return false;
		
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		TMulticast_Publish_BFT tmPubObj = (TMulticast_Publish_BFT) obj;
		if(!_publication.equals(tmPubObj._publication))
			return false;
		
		Set<SequencePair> l1 = new HashSet<SequencePair>(getSequencePairs());
		Set<SequencePair> l2 = new HashSet<SequencePair>(tmPubObj.getSequencePairs());
		return l1.equals(l2); //SequencePair.equals(l1, l2);
	}
	
	////////////////////////////////////
	
	@Override
	public IBFTDigestable getDigestable() {
		return (IBFTDigestable) getPublication();
	}

	@Override
	public List<SequencePair> getSequencePairs(IBFTIssuerProxy i) {
		return _verifiable.getSequencePairs(i);
	}

	@Override
	public SequencePair addSequencePair(IBFTIssuer issuer, InetSocketAddress vAddr, long e, int o) {
		return _verifiable.addSequencePair(issuer, vAddr, e, o);
	}

	@Override
	public SequencePair getSequencePair(InetSocketAddress iAddr, InetSocketAddress vAddr) {
		return _verifiable.getSequencePair(iAddr, vAddr);
	}

	@Override
	public List<SequencePair> getSequencePairs(InetSocketAddress iAddr) {
		return _verifiable.getSequencePairs(iAddr);
	}

	@Override
	public BFTPublication getPublication() {
		return (BFTPublication) _publication;
	}

	@Override
	public List<SequencePair> getSequencePairs() {
		return _verifiable.getSequencePairs();
	}
	
	@Override
	protected int getSeqVecLength() {
		return 1;
	}

	@Override
	public List<SequencePair> getSequencePairs(IBFTVerifier verifier) {
		return _verifiable.getSequencePairs(verifier);
	}
	
	@Override
	public String toString() {
		return super.toString() + BFTWriters.write(_verifiable.getSequencePairs());
	}

	public String toString(BrokerIdentityManager idMan) {
		return super.toString() + _verifiable.toString(idMan);
	}

	@Override
	public SequencePair addSequencePair(SequencePair sp) {
		return _verifiable.addSequencePair(sp);
	}

	@Override
	public void addSequencePairs(Collection<SequencePair> verifiersSequencePairs) {
		for(SequencePair sp : verifiersSequencePairs)
			addSequencePair(sp);
	}

	@Override
	public TMulticast_Publish getShiftedClone(int shift, Sequence seq) {
		throw new UnsupportedOperationException("Please use getNonShiftedClone(seq) instead.");
	}
	
	public final TMulticast_Publish_BFT getNonShiftedClone(Sequence seq) {
		if(seq == null)
			throw new NullPointerException();
		
		TMulticast_Publish_BFT clone =
				createNewTMulticastPublish(this._publication, this._from, this._workload, this._sourceSequence);
		duplicate(this, clone);
		
		clone._sequencesVector[0] = seq;
		return clone;
	}
	
	protected static void duplicate(TMulticast tmSource, TMulticast tmCopy) {
		TMulticast_Publish.duplicate(tmCopy, tmSource);
		((TMulticast_Publish_BFT)tmCopy)._verifiable.addSequencePairs(
				((TMulticast_Publish_BFT)tmSource)._verifiable.getSequencePairs());
		
		if(((TMulticast_Publish_BFT)tmCopy)._verifiable.getSequencePairs().size() != ((TMulticast_Publish_BFT)tmSource)._verifiable.getSequencePairs().size())
			throw new IllegalStateException();
	}

	@Override
	public int getValidSequencePairsCount() {
		return _verifiable.getValidSequencePairsCount();
	}

	@Override
	public int getInvalidSequencePairsCount() {
		return _verifiable.getInvalidSequencePairsCount();
	}

	@Override
	public SequencePair getSequencePair(IBFTIssuerProxy iProxy, IBFTVerifier verifier) {
		return _verifiable.getSequencePair(iProxy, verifier);
	}
	
	@Override
	public void shiftSequenceVector(int shift) {
		throw new UnsupportedOperationException();
	}
}
