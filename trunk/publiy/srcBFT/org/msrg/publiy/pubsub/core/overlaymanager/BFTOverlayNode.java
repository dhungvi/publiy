package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.IBFTSuspectable;
import org.msrg.publiy.broker.IBFTSuspectedRepo;
import org.msrg.publiy.broker.core.sequence.IBFTDigestable;
import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.IBFTVerifiable;
import org.msrg.publiy.broker.core.sequence.IBFTVerifierProxy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.SequencePair;
import org.msrg.publiy.broker.core.sequence.SequencePairVerifyResult;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.SimpleBFTVerifierProxy;
import org.msrg.publiy.node.NodeTypes;


public class BFTOverlayNode extends OverlayNode implements IBFTOverlayNode {
	
	protected final IBFTIssuerProxy _iProxy;
	protected final IBFTVerifierProxy _vProxy;
	protected final IBFTSuspectedRepo _suspectedRepo;
	protected final IBFTOverlayManager _bftOverlayMan;
	protected final Set<InetSocketAddress> _affectedRemotes =
			new HashSet<InetSocketAddress>();
	
	protected BFTOverlayNode(LocalSequencer localSequencer, InetSocketAddress nodeAddr, NodeTypes nodeType, INode closerNode, IBFTOverlayManager bftOverlayMan) {
		super(localSequencer, nodeAddr, nodeType, closerNode);
		
		_bftOverlayMan = bftOverlayMan;
		_suspectedRepo = _bftOverlayMan.getSuspectedRepo();
		long dackReceiveTimeout = bftOverlayMan.getDackReceiveTimeout(nodeAddr);
		_vProxy = new SimpleBFTVerifierProxy(_bftOverlayMan.getBrokerShadow().getBrokerIdentityManager(), bftOverlayMan, nodeAddr, dackReceiveTimeout);
		_iProxy = new SimpleBFTIssuerProxy(bftOverlayMan.getBrokerShadow().getBrokerIdentityManager(), _bftOverlayMan, nodeAddr, bftOverlayMan.getKeyManager());
		((BFTBrokerShadow)_bftOverlayMan.getBrokerShadow()).getIssuerProxyRepo().registerIssuerProxy(this);
	}		
	
	protected BFTOverlayNode(LocalSequencer localSequencer, InetSocketAddress nodeAddr, NodeTypes nodeType, IBFTOverlayManager bftOverlayMan) {
		super(localSequencer, nodeAddr, nodeType);

		_bftOverlayMan = bftOverlayMan;
		_suspectedRepo = _bftOverlayMan.getSuspectedRepo();
		long dackReceiveTimeout = bftOverlayMan.getDackReceiveTimeout(nodeAddr);
		_vProxy = new SimpleBFTVerifierProxy(_bftOverlayMan.getBrokerShadow().getBrokerIdentityManager(), bftOverlayMan, nodeAddr, dackReceiveTimeout);
		_iProxy = new SimpleBFTIssuerProxy(bftOverlayMan.getBrokerShadow().getBrokerIdentityManager(), bftOverlayMan, nodeAddr, bftOverlayMan.getKeyManager());
	}

	@Override
	protected OverlayNode createNewOverlayNode(InetSocketAddress remote, NodeTypes nodeType, INode neighbor) {
		return new BFTOverlayNode(_localSequencer, remote, nodeType, neighbor, _bftOverlayMan);
	}
	
	@Override
	public PublicKey getPublicKey() {
		return _iProxy.getPublicKey();
	}
	
	@Override
	public SequencePairVerifyResult verifySuccession(
			boolean isDack,
			boolean updateIProxySequencePairs, 
			IBFTSuspectable suspectable,
			IBFTVerifiable verifiable,
			List<BFTSuspecionReason> reasons) {
		return _iProxy.verifySuccession(
				isDack, updateIProxySequencePairs, suspectable, verifiable, reasons);
	}

	@Override
	public InetSocketAddress getIssuerAddress() {
		return _iProxy.getIssuerAddress();
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
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(InetSocketAddress iAddr) {
		return _address.equals(iAddr);
	}

	@Override
	public SequencePair getLastLocallySeenSequencePairFromProxy() {
		return _iProxy.getLastLocallySeenSequencePairFromProxy();
	}

	@Override
	public InetSocketAddress getVerifierAddress() {
		return _vProxy.getVerifierAddress();
	}

	@Override
	public SequencePair issueCurrentSequencePair(
			IBFTDigestable digestable, IBFTIssuerProxy iProxy) {
		return _vProxy.issueCurrentSequencePair(digestable, iProxy);
	}

	@Override
	public SequencePair issueNextSequencePair(
			IBFTDigestable digestable, IBFTIssuerProxy iProxy) {
		return _vProxy.issueNextSequencePair(digestable, iProxy);
	}

	@Override
	public boolean isSuspected() {
		return _suspectedRepo.isSuspected(getIssuerAddress());
	}

	@Override
	public List<BFTSuspecionReason> getReasons() {
		return _suspectedRepo.getReasons(this);
	}

	@Override
	public InetSocketAddress getDackIsserAddress() {
		return _vProxy.getDackIsserAddress();
	}

	@Override
	public InetSocketAddress getDackVerifierAddress() {
		return _vProxy.getDackVerifierAddress();
	}

	@Override
	public int getLastProxyReceivedOrderFromMe() {
		return _vProxy.getLastProxyDiscardedOrderFromMe();
	}

	@Override
	public int getLastProxyDiscardedOrderFromMe() {
		return _vProxy.getLastProxyDiscardedOrderFromMe();
	}

	@Override
	public boolean setLastProxyReceivedOrderFromMe(SequencePair receivedSP) {
		return _vProxy.setLastProxyReceivedOrderFromMe(receivedSP);
	}

	@Override
	public boolean setLastProxyDiscardedOrderFromMe(SequencePair discardedSP) {
		return _vProxy.setLastProxyDiscardedOrderFromMe(discardedSP);
	}
	
	@Override
	public boolean dackReceived(byte[] dackDigest, SequencePair sp) {
		if(!sp.isValid(dackDigest))
			return false;
		
		boolean b1 = setLastProxyReceivedOrderFromMe(sp);
		boolean b2 = setLastProxyDiscardedOrderFromMe(sp);
		return b1 || b2;
	}

	@Override
	public void dackReceived(IBFTVerifiable dack) {
		SequencePair sp = dack.getSequencePair(this, _bftOverlayMan);
		dackReceived(dack.getDigestable().getDigest(), sp);
	}

	@Override
	public long getLastDackReceivedTime() {
		return _vProxy.getLastDackReceivedTime();
	}

	@Override
	public boolean isDackReciptTooLate() {
		return _vProxy.isDackReciptTooLate();
	}

	@Override
	public boolean hasDiscardedSequencePair(SequencePair sp) {
		return _vProxy.hasDiscardedSequencePair(sp);
	}

	@Override
	public boolean hasReceivedSequencePair(SequencePair sp) {
		return _vProxy.hasReceivedSequencePair(sp);
	}

	@Override
	public void setDACKReceiptTimeout(long dackReceiptTimeout) {
		_vProxy.setDACKReceiptTimeout(dackReceiptTimeout);		
	}

	@Override
	public Set<InetSocketAddress> getAffectedRemotes() {
		return _affectedRemotes;
	}

	@Override
	public boolean addAffectedRemote(InetSocketAddress remote) {
		return _affectedRemotes.add(remote);
	}

	@Override
	public boolean remoteIsAffected(InetSocketAddress remote) {
		return _affectedRemotes.contains(remote);
	}
}
