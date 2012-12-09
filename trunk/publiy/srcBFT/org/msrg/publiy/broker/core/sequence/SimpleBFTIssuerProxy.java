package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.List;

import org.msrg.publiy.utils.Writers;

import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBFTSuspectable;
import org.msrg.publiy.broker.security.keys.KeyManager;

public class SimpleBFTIssuerProxy implements IBFTIssuerProxy {
	
	protected final BrokerIdentityManager _idMan;
	protected final PublicKey _iPubKey;
	protected final InetSocketAddress _iAddr;
	protected final IBFTVerifier _verifier;
	protected SequencePair _lastSeenSequencePair;

	public SimpleBFTIssuerProxy(BrokerIdentityManager idMan, IBFTVerifier verifier, InetSocketAddress iAddr, KeyManager keyman) {
		this(idMan, verifier, iAddr, keyman.getPublicKey(iAddr));
	}

	public SimpleBFTIssuerProxy(BrokerIdentityManager idMan, IBFTVerifier verifier, InetSocketAddress iAddr, PublicKey iPubKey) {
		_verifier = verifier;
		_iAddr = iAddr;
		_iPubKey = iPubKey;
		_lastSeenSequencePair = null;
		
		_idMan = idMan;
	}
	
	@Override
	public SequencePairVerifyResult verifySuccession(
			boolean isDack,
			boolean updateIProxySequencePairs,
			IBFTSuspectable suspectable,
			IBFTVerifiable verifiable,
			List<BFTSuspecionReason> reasons) {
		byte[] digest = verifiable.getDigestable().getDigest();
		SequencePair sp = verifiable.getSequencePair(this, _verifier);
		if(sp == null) {
			if(reasons != null)
				reasons.add(new BFTSuspecionReason(false, false,
						suspectable, null, "Sequence pair is missing: " + getIssuerAddress() +
						" vs. " + _verifier.getVerifierAddress() + " vs. " + verifiable + " vs. " + verifiable));
			return SequencePairVerifyResult.SEQ_PAIR_MISSING;
		}

		if(!sp.isValid(digest)) {
			if(reasons != null)
				reasons.add(new BFTSuspecionReason(false, false,
						suspectable, null, "Sequence pair is invalid: " + getIssuerAddress() +
						" vs. " + Writers.write(_verifier.getVerifierAddress(), _idMan) + " vs. " + verifiable.toString(_idMan) + " vs. " + verifiable.toString(_idMan)));
			return SequencePairVerifyResult.SEQ_PAIR_INVALID;
		}
		
		int order = sp._order;
		if(_lastSeenSequencePair == null) {
			if(updateIProxySequencePairs && !isDack)
				_lastSeenSequencePair = sp;
			return SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS;
//			if(order==0) {
//				if(updateIProxySequencePairs && !isDack)
//					_lastSeenSequencePair = sp;
//				return SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS;
//			} else if(order==1) {
//				if(updateIProxySequencePairs && !isDack)
//					_lastSeenSequencePair = sp;
//				return SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS;
//			} else {
//				if(reasons != null)
//					reasons.add(new BFTSuspecionReason(false, true,
//							suspectable, suspectable == null ? null : suspectable.getAddress(),
//									"Sequence pair Jumps: " + _lastSeenSequencePair + " vs. " + sp));
//				return SequencePairVerifyResult.SEQ_PAIR_JUMPS;
//			}
		}

		int lastOrder = _lastSeenSequencePair._order;
		if(isDack && lastOrder == order) {
			if(updateIProxySequencePairs && !isDack)
				_lastSeenSequencePair = sp;
			return SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS;
		} else if(lastOrder + 1 == order) {
			if(updateIProxySequencePairs && !isDack)
				_lastSeenSequencePair = sp;
			return SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS;
		} else if(lastOrder == order) {
			if(reasons != null)
				reasons.add(new BFTSuspecionReason(false, true,
						suspectable, suspectable == null ? null : suspectable.getAddress(),
								"Sequence pair is duplicate: " + _lastSeenSequencePair.toString(_idMan) + " vs. " + sp.toString(_idMan) + " vs. " + verifiable.toString(_idMan)));
			return SequencePairVerifyResult.SEQ_PAIR_EQUALS;
		} else if(lastOrder > order) {
			if(reasons != null)
				reasons.add(new BFTSuspecionReason(false, true,
						suspectable, suspectable == null ? null : suspectable.getAddress(),
								"Sequence pair moves back: " + _lastSeenSequencePair.toString(_idMan) + " vs. " + sp.toString(_idMan) + " vs. " + verifiable.toString(_idMan)));
			return SequencePairVerifyResult.SEQ_PAIR_PRECEDES;
		} else {
			if(reasons != null)
				reasons.add(new BFTSuspecionReason(false, true,
						suspectable, suspectable == null ? null : suspectable.getAddress(),
								"Sequence pair Jumps: " + _lastSeenSequencePair.toString(_idMan) + " vs. " + sp.toString(_idMan) + " vs. " + verifiable.toString(_idMan)));
			return SequencePairVerifyResult.SEQ_PAIR_JUMPS;
		}
	}

	@Override
	public InetSocketAddress getIssuerAddress() {
		return _iAddr;
	}

	@Override
	public PublicKey getPublicKey() {
		return _iPubKey;
	}

	@Override
	public boolean equals(InetSocketAddress iAddr) {
		return _iAddr.equals(iAddr);
	}

	@Override
	public SequencePair getLastLocallySeenSequencePairFromProxy() {
		return _lastSeenSequencePair;
	}
}
