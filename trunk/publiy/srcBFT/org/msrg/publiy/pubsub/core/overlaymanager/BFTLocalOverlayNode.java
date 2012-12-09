package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.utils.log.casuallogger.BFTDSALogger;


import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBFTSuspectable;
import org.msrg.publiy.broker.core.sequence.IBFTDigestable;
import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.IBFTVerifiable;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.SequencePair;
import org.msrg.publiy.broker.core.sequence.SequencePairVerifyResult;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxy;
import org.msrg.publiy.broker.security.keys.KeyManager;

public class BFTLocalOverlayNode extends LocalOverlayNode implements IBFTLocalOverlayNode {

	protected final IBFTOverlayManager _bftOverlayManager;
	protected final InetSocketAddress _localAddress;
	protected final IBFTIssuerProxy _iProxy;
	protected final IBFTBrokerShadow _bftBrokerShadow;
	protected final PrivateKey _privateKey;
	protected final PublicKey _publicKey;
//	protected final Set<InetSocketAddress> _affectedRemotes =
//			new HashSet<InetSocketAddress>();
	
	protected long _epoch = 0;
	protected int _order = 0;
	
	protected BFTLocalOverlayNode(LocalSequencer localSequencer, InetSocketAddress address, IBFTOverlayManager bftOverlayManager) {
		super(localSequencer, address);
	
		_bftOverlayManager = bftOverlayManager;
		_bftBrokerShadow = (IBFTBrokerShadow) _bftOverlayManager.getBrokerShadow();
		
		_localAddress = _bftBrokerShadow.getLocalAddress();
		KeyManager keyMan = _bftBrokerShadow.getKeyManager();
		_privateKey = keyMan.getPrivateKey(_localAddress);
		_publicKey = keyMan.getPublicKey(_localAddress);
//		_iProxy = _bftBrokerShadow.getIssuerProxyRepo().getIssuerProxy(_localAddress);
//		if(_iProxy == null)
//			throw new IllegalStateException();
		_iProxy = new SimpleBFTIssuerProxy(bftOverlayManager.getBrokerShadow().getBrokerIdentityManager(), _bftOverlayManager, _localAddress, keyMan);
		((BFTBrokerShadow)_bftBrokerShadow).getIssuerProxyRepo().registerIssuerProxy(this);
	}
	
	@Override
	protected OverlayNode createNewOverlayNode(InetSocketAddress remote, NodeTypes nodeType, INode neighbor) {
		return new BFTOverlayNode(_localSequencer, remote, nodeType, neighbor, _bftOverlayManager);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;

		if(InetSocketAddress.class.isInstance(obj)) {
			InetSocketAddress objAddress = (InetSocketAddress) obj;
			return this._address.equals(objAddress);
		}
			
		if(IBFTOverlayNode.class.isAssignableFrom(obj.getClass())) {
			IBFTOverlayNode bftOverlayNode = (IBFTOverlayNode) obj;
			return bftOverlayNode.getAddress().equals(this._address);
		}

		return false;
	}

	@Override
	public InetSocketAddress getIssuerAddress() {
		return _localAddress;
	}

	@Override
	public PrivateKey getPrivateKey() {
		return _privateKey;
	}

	@Override
	public SequencePair getLastLocallySeenSequencePairFromProxy() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PublicKey getPublicKey() {
		return _publicKey;
	}

	@Override
	public boolean equals(InetSocketAddress iAddr) {
		return iAddr.equals(_localAddress);
	}

	@Override
	public InetSocketAddress getVerifierAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SequencePair issueNextSequencePair(IBFTDigestable digestable, IBFTIssuerProxy iProxy) {
		return new SequencePair(this, _epoch, ++_order, _localAddress, digestable);
	}

	@Override
	public SequencePair issueCurrentSequencePair(IBFTDigestable digestable, IBFTIssuerProxy iProxy) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isSuspected() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<BFTSuspecionReason> getReasons() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InetSocketAddress getDackIsserAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InetSocketAddress getDackVerifierAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getLastProxyReceivedOrderFromMe() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getLastProxyDiscardedOrderFromMe() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setLastProxyReceivedOrderFromMe(SequencePair receivedSP) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public boolean setLastProxyDiscardedOrderFromMe(SequencePair discardedSP) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public boolean dackReceived(byte[] dackDigest, SequencePair sp) {
		throw new UnsupportedOperationException();
//		return true;
	}

	@Override
	public void dackReceived(IBFTVerifiable dack) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getLastDackReceivedTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDackReciptTooLate() {
		return false;
	}

	@Override
	public boolean hasDiscardedSequencePair(SequencePair sp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasReceivedSequencePair(SequencePair sp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SequencePairVerifyResult verifySuccession(
			boolean isDack, boolean updateIProxySequencePairs, IBFTSuspectable suspectable,
			IBFTVerifiable verifiable, List<BFTSuspecionReason> reasons) {
		SequencePair locallyGeneratedSequencePair = verifiable.getSequencePair(_localAddress, _localAddress);
		if(locallyGeneratedSequencePair.isValid(verifiable.getDigestable().getDigest()))
			return SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS;
		else
			return SequencePairVerifyResult.SEQ_PAIR_INVALID;
//		return _iProxy.verifySuccession(
//				isDack, updateIProxySequencePairs, suspectable, verifiable, reasons);
	}

	@Override
	public BFTDSALogger getBFTDSALogger() {
		return _bftOverlayManager.getBFTDSALogger();
	}

	@Override
	public void setDACKReceiptTimeout(long dackReceiptTimeout) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<InetSocketAddress> getAffectedRemotes() {
//		return _affectedRemotes;
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAffectedRemote(InetSocketAddress remote) {
//		return _affectedRemotes.add(remote);
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remoteIsAffected(InetSocketAddress remote) {
		throw new UnsupportedOperationException();
	}
}
