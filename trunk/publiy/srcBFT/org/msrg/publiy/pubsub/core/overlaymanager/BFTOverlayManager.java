package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT_Dack;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.utils.BFTWriters;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.casuallogger.BFTDSALogger;
import org.msrg.publiy.utils.log.casuallogger.BFTDackLogger;
import org.msrg.publiy.utils.log.casuallogger.BFTStatisticsLogger;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;

import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBFTSuspectable;
import org.msrg.publiy.broker.IBFTSuspectedRepo;
import org.msrg.publiy.broker.core.connectionManager.BFTConnectionManager;
import org.msrg.publiy.broker.core.sequence.IBFTDackIssuerProxy;
import org.msrg.publiy.broker.core.sequence.IBFTDigestable;
import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.IBFTVerifiable;
import org.msrg.publiy.broker.core.sequence.IBFTVerifierProxy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.SequencePair;
import org.msrg.publiy.broker.core.sequence.SequencePairVerifyResult;
import org.msrg.publiy.broker.security.keys.KeyManager;

public class BFTOverlayManager extends OverlayManager implements IBFTOverlayManager {
	
	protected static boolean LOOK_FOR_MATCHING_INCONSITENCIES = false;
	
	protected final KeyManager _keyman;
	protected final IBFTSuspectedRepo _suspectedRepo;
	protected final BFTDackLogger _dackLogger;
	protected final int _nr;
	protected final int _minVerificationRequied;
	protected final BFTDSALogger _bftDSALogger;
	protected final StatisticsLogger _statLogger;
	
	public BFTOverlayManager(IBFTBrokerShadow brokerShadow) {
		super(brokerShadow);
		
		_keyman = brokerShadow.getKeyManager();
		_suspectedRepo = brokerShadow.getBFTSuspectedRepo();
		_dackLogger = ((IBFTBrokerShadow)_brokerShadow).getBFTDackLogger();
		_nr = _brokerShadow.getNeighborhoodRadius();
		_minVerificationRequied = ((IBFTBrokerShadow)_brokerShadow).getMinVerificationRequied();
		_bftDSALogger = brokerShadow.getBFTDSALogger();
		_statLogger = brokerShadow.getStatisticsLogger();
	}

	@Override
	protected OverlayNode getNewOverlayNode(InetSocketAddress joiningAddress, NodeTypes nodeType, INode joinPointNode) {
		return new BFTOverlayNode(_localSequencer, joiningAddress, nodeType, joinPointNode, this);
	}
	
	@Override
	protected NodeCache createNodeCache(INode node) {
		return new NodeCache(this, node, _brokerShadow.getNeighborhoodRadius());
	}
	
	@Override
	public KeyManager getKeyManager() {
		return _keyman;
	}

	@Override
	public IBFTVerifiable refineVerifiable(IBFTVerifiable verifiable) {
		throw new UnsupportedOperationException("To be implemented.");
	}

	@Override
	public boolean verifyVerifiable(IBFTVerifiable verifiable) {
		throw new UnsupportedOperationException("To be implemented.");
	}

	@Override
	public IBFTSuspectedRepo getSuspectedRepo() {
		return _suspectedRepo;
	}

	@Override
	public InetSocketAddress getVerifierAddress() {
		return getLocalAddress();
	}

	@Override
	public InetSocketAddress getIssuerAddress() {
		return getLocalAddress();
	}

	@Override
	public PrivateKey getPrivateKey() {
		return _keyman.getPrivateKey(getIssuerAddress());
	}

	@Override
	public InetSocketAddress getDackIsserAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InetSocketAddress getDackVerifierAddress() {
		return getLocalAddress();
	}

	@Override
	public boolean dackReceived(byte[] dackDigest, SequencePair sp) {
		InetSocketAddress dackVAddr = sp.getVerifierAddress();
		if(!dackVAddr.equals(_localAddress))
			throw new IllegalArgumentException(dackVAddr + " vs. " + _localAddress);

		InetSocketAddress dackIAddr = sp.getIssuerAddress();
		IBFTOverlayNode bftOverlayNode = (IBFTOverlayNode) _nodes.get(dackIAddr);
		if(bftOverlayNode.dackReceived(dackDigest, sp)) {
			if(_dackLogger != null)
				_dackLogger.dackSequencePairRegistered(sp);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Map<IBFTVerifierProxy, SequencePair> computeIntermediateVerifersSequencePairs(
			InetSocketAddress receivedFrom,
			boolean incrementSequencePairs, boolean includeLastReceivedDiscardedOrders,
			IBFTVerifiable verifiable, Set<InetSocketAddress> matchingSet) {
		if(matchingSet == null)
			return null;
		
		IBFTDigestable digestable = verifiable.getDigestable();
		
		Map<IBFTVerifierProxy, SequencePair> issuedSequencePairs =
				new HashMap<IBFTVerifierProxy, SequencePair>();
		Set<InetSocketAddress> intermediateVerifiers =
				new HashSet<InetSocketAddress>();
		
		Path<INode> pathFromSender = getPathFrom(receivedFrom);
		if(pathFromSender == null)
			throw new IllegalStateException(receivedFrom + " vs. " + matchingSet);
		
		InetSocketAddress immediateNeigborToSender = pathFromSender.getLast().getAddress();
		for(InetSocketAddress matchingRemote : matchingSet) {
			if(_localAddress.equals(matchingRemote))
				continue;
			
			Path<INode> pathFromAnchor = getPathFrom(matchingRemote);
			if(pathFromAnchor.passes(immediateNeigborToSender))
				throw new IllegalStateException(matchingRemote + " vs. " + receivedFrom);
//				continue;
			
			for(InetSocketAddress remote : pathFromAnchor.getAddresses()) {
//			for(int i=0 ; i<pathFromAnchor._length ; i++) {
//				IBFTOverlayNode bftOverlayNode = (IBFTOverlayNode) pathFromAnchor._nodes[i];
//				InetSocketAddress remote = bftOverlayNode.getAddress();
				if(!intermediateVerifiers.add(remote))
					continue;
				
				IBFTOverlayNode bftOverlayNode = (IBFTOverlayNode) _nodes.get(remote);
				SequencePair issuedSequencePair = incrementSequencePairs ?
						bftOverlayNode.issueNextSequencePair(digestable, includeLastReceivedDiscardedOrders ? (IBFTIssuerProxy) bftOverlayNode : null) :
							bftOverlayNode.issueCurrentSequencePair(digestable, includeLastReceivedDiscardedOrders ? (IBFTIssuerProxy) bftOverlayNode : null);
				if(issuedSequencePairs.put(bftOverlayNode, issuedSequencePair) != null)
					throw new IllegalStateException(issuedSequencePair.toString(
							_brokerShadow.getBrokerIdentityManager()) + " vs. " + Writers.write(_brokerShadow.getBrokerIdentityManager(), matchingSet));
			}
		}
		
		if(!LOOK_FOR_MATCHING_INCONSITENCIES)
			return issuedSequencePairs;
		
		// Now consider the sequencepairs issued by upstream nodes for downstream nodes (not in our matching sets).
		List<SequencePair> upstreamTargetSequencePairs = verifiable.getSequencePairs();
		for(SequencePair upstreamTargetSequencePair : upstreamTargetSequencePairs) {
			InetSocketAddress upstreamTargetVerifier = upstreamTargetSequencePair.getVerifierAddress();
			if(_localAddress.equals(upstreamTargetVerifier))
				continue;
			
			Path<INode> pathFromUpstreamTargetVerifier = getPathFrom(upstreamTargetVerifier);
			if(pathFromUpstreamTargetVerifier.passes(immediateNeigborToSender))
				throw new IllegalStateException(upstreamTargetVerifier + " vs. " + receivedFrom);
			
			for(InetSocketAddress remote : pathFromUpstreamTargetVerifier.getAddresses()) {
				if(!intermediateVerifiers.add(remote))
					continue;
				
				IBFTOverlayNode bftOverlayNode = (IBFTOverlayNode) _nodes.get(remote);
				SequencePair issuedSequencePair = incrementSequencePairs ?
						bftOverlayNode.issueNextSequencePair(digestable, includeLastReceivedDiscardedOrders ? (IBFTIssuerProxy) bftOverlayNode : null) :
							bftOverlayNode.issueCurrentSequencePair(digestable, includeLastReceivedDiscardedOrders ? (IBFTIssuerProxy) bftOverlayNode : null);
				if(issuedSequencePairs.put(bftOverlayNode, issuedSequencePair) != null)
					throw new IllegalStateException(issuedSequencePair.toString(
							_brokerShadow.getBrokerIdentityManager()) + " vs. " + Writers.write(_brokerShadow.getBrokerIdentityManager(), matchingSet));
				if(_statLogger != null)
					((BFTStatisticsLogger)_statLogger).matchingSetInconsistency(verifiable, upstreamTargetVerifier);
			}
		}
		
		return issuedSequencePairs;
	}

	@Override
	protected LocalOverlayNode createLocalOverlayNode(LocalSequencer localSequencer, InetSocketAddress localAddress) {
		return new BFTLocalOverlayNode(localSequencer, localAddress, this);
	}
	
	@Override
	public int getNeighborhoodRadius() {
		return _neighborhoodRadius;
	}

	@Override
	public Collection<SequencePair> getDownstreamSequencePairsOf(
			InetSocketAddress remote, Map<IBFTVerifierProxy, SequencePair> downstreamVerifersSequencePairs) {
		if(downstreamVerifersSequencePairs == null)
			return new HashSet<SequencePair>();
		
		Set<SequencePair> downstreamSequencePairs = new HashSet<SequencePair>();
		for(Entry<IBFTVerifierProxy, SequencePair> entry : downstreamVerifersSequencePairs.entrySet()) {
			IBFTVerifierProxy vProxy = entry.getKey();
			InetSocketAddress verifier = vProxy.getVerifierAddress();
			Path<INode> pathFromVerifier = getPathFrom(verifier);
			if(pathFromVerifier.passes(remote)) {
				SequencePair sequencePair = entry.getValue();
				downstreamSequencePairs.add(sequencePair);
			}
		}
		
		return downstreamSequencePairs;
	}

	@Override
	public void trimSequencePairs(IBFTVerifiable verifiable, InetSocketAddress remote) {
		BrokerIdentityManager idMan = _brokerShadow.getBrokerIdentityManager();
		List<SequencePair> allSequencePairs = verifiable.getSequencePairs();
		Iterator<SequencePair> allSequencePairsIt  = allSequencePairs.iterator();
		while(allSequencePairsIt.hasNext()) {
			SequencePair sequencePair = allSequencePairsIt.next();
			InetSocketAddress issuer = sequencePair.getIssuerAddress();
			Path<INode> pathFromIssuer = getPathFrom(issuer);
			if(pathFromIssuer.passes(remote))
				throw new IllegalStateException(remote + " vs. " + issuer);
			
			InetSocketAddress verifier = sequencePair.getVerifierAddress();
			Path<INode> pathFromVerifier = getPathFrom(verifier);
			if(pathFromVerifier == null) {
				LoggerFactory.getLogger().warn(this, "Triming sequence pairs did not find verifier in OM: " + sequencePair.toString(idMan));
				continue;
			}
//				throw new NullPointerException(verifier + " vs. " + _localAddress);

			if(!pathFromVerifier.passes(remote)) {
				allSequencePairsIt.remove();
//				BrokerInternalTimer.inform("Trimmed: " + Writers.write(remote, idMan) + " vs. " + sequencePair.toString(idMan));
			}
		}
	}

	@Override
	public Set<InetSocketAddress> getMatchingSetForBFTDack(TMulticast_Publish_BFT_Dack bftDack) {
		Set<InetSocketAddress> retSet = new HashSet<InetSocketAddress>();
		
		int nr = getNeighborhoodRadius();
		InetSocketAddress source = bftDack.getSourceAddress();
		Path<INode> pathFromSource = getPathFrom(source);
		int sourceDistance = source.equals(_localAddress) ? 0 : pathFromSource.getLength();
		INode immediateNeighborTowardsSourceNodeId = pathFromSource.getLast();
		InetSocketAddress immediateNeighborTowardsSource =
				immediateNeighborTowardsSourceNodeId == null ? null :
					immediateNeighborTowardsSourceNodeId.getAddress();
		for(INode node : _nodes.values()) {
			Path<INode> pathFromNode = getPathFrom(node.getAddress());
			if(!pathFromNode.passes(immediateNeighborTowardsSource) && 
					pathFromNode.getLength() + sourceDistance <= nr)
				retSet.add(node.getAddress());
		}
		
		retSet.remove(_localAddress);
		return retSet;
	}

	@Override
	public void dackReceived(IBFTVerifiable dack) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, "Dack received: " + dack.getDigestable() + " vs. " + BFTWriters.write(dack.getSequencePairs()));
		InetSocketAddress source = dack.getSourceAddress();
		if(_localAddress.equals(source))
			return;
		
		byte[] dackDigest = dack.getDigestable().getDigest();
		List<SequencePair> sequencePairsForMe = dack.getSequencePairs(this);
		for(SequencePair sp : sequencePairsForMe) {
			// No need to process locally issued sequence pairs in a dack!
			if(!_localAddress.equals(sp.getIssuerAddress()))
				dackReceived(dackDigest, sp);
		}
	}

	@Override
	public void checkDackSafety(TMulticast_Publish_BFT _tm, Set<InetSocketAddress> matchingSet) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<InetSocketAddress> whoseDacksAreNotReceived() {
		Set<InetSocketAddress> timedoutDackers = new HashSet<InetSocketAddress>();
		for(INode node : _nodes.values()) {
			IBFTDackIssuerProxy iDackProxy = (IBFTDackIssuerProxy) node;
			if(iDackProxy.isDackReciptTooLate())
				timedoutDackers.add(iDackProxy.getDackIsserAddress());
		}
		
		return timedoutDackers;
	}

	@Override
	public boolean verifySuccession(
			IBFTSuspectedRepo bftSuspicionRepo, boolean isDack, IBFTVerifiable verifiable, List<BFTSuspecionReason> reasons) {
		InetSocketAddress source = verifiable.getSourceAddress();
		if(_localAddress.equals(source)) {
			SequencePairVerifyResult verificationResult =
					((IBFTLocalOverlayNode)_localNode).verifySuccession(
							isDack, false, ((IBFTLocalOverlayNode) _localNode), verifiable, reasons);
			switch(verificationResult) {
			case SEQ_PAIR_INVALID:
			case SEQ_PAIR_MISSING:
				// We cannot really suspect the sender! (anyone could have sent this message)
//				throw new IllegalStateException(verifiable.toString());
				return false;

			case SEQ_PAIR_PRECEDES:
			case SEQ_PAIR_EQUALS:
			case SEQ_PAIR_JUMPS:
				// Someone may have sent it back to us, not a bid deal.
//				throw new IllegalStateException(verifiable.toString());
				return false;
				
			case SEQ_PAIR_SUCCEEDS:
				// All is good.
				// Also proceed, so we increment sequence pairs...
				break;
			}
		}
		
		boolean sourceValid = false;
		int succeedingSPs = 0;
		for(Iterator<SequencePair> spIt = verifiable.getSequencePairs(this).iterator() ;
				spIt.hasNext() ; ) {
			SequencePair sp = spIt.next();
			
			InetSocketAddress iAddr = sp.getIssuerAddress();
			IBFTOverlayNode bftNode = (IBFTOverlayNode) _nodes.get(iAddr);
			SequencePairVerifyResult verificationResults =
					bftNode.verifySuccession(isDack, false, bftNode, verifiable, reasons);
			
			if(iAddr.equals(source))
				sourceValid |= (verificationResults == SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS);

			succeedingSPs += (verificationResults == SequencePairVerifyResult.SEQ_PAIR_SUCCEEDS) ? 1 : 0;
		}

		boolean msgValid = sourceValid | (succeedingSPs >= _minVerificationRequied);
		if(!msgValid) {
			// Message is not valid but we cannot really suspect anyone!
			return false;
		}
		
		// Message is valid. Now let's suspect people!
		for(SequencePair sp : verifiable.getSequencePairs(this)) {
			InetSocketAddress iAddr = sp.getIssuerAddress();
			IBFTOverlayNode bftNode = (IBFTOverlayNode) _nodes.get(iAddr);
			switch(bftNode.verifySuccession(isDack, true, bftNode, verifiable, reasons)) {
			case SEQ_PAIR_MISSING:
			case SEQ_PAIR_INVALID:
				// This is not a sufficient reason for suspicion
				break;
				
			case SEQ_PAIR_SUCCEEDS:
				// All is good!
				break;
				
			case SEQ_PAIR_EQUALS:
			case SEQ_PAIR_PRECEDES:
			case SEQ_PAIR_JUMPS:
				// This is a misbehavior (since order of message had been asserted earlier)
				bftSuspicionRepo.suspect(reasons.get(reasons.size() - 1));
				break;
			}
		}
		
		return true;
	}
	
	@Override
	public void issueLocalMessageSequencePair(TMulticast_Publish_BFT tmBFT) {
		SequencePair sp = ((IBFTLocalOverlayNode)_localNode).issueNextSequencePair(tmBFT.getDigestable(), null);
		if(!sp.getVerifierAddress().equals(_localAddress))
			throw new IllegalStateException(sp + " vs. " + _localAddress);
		
		if(!sp.getIssuerAddress().equals(_localAddress))
			throw new IllegalStateException(sp + " vs. " + _localAddress);

		tmBFT.addSequencePair(sp);
	}

	@Override
	public BFTDSALogger getBFTDSALogger() {
		return _bftDSALogger;
	}
	
	@Override
	public void handleMessage(TMulticast_Join tmj) {
		super.handleMessage(tmj);

		InetSocketAddress joiningnodeAddress = tmj.getJoiningNode();
		IBFTOverlayNode joiningNode = (IBFTOverlayNode) _nodes.get(joiningnodeAddress);
		if(joiningNode == null)
			return;
		
		long dackReceiptTimeout = getDackReceiveTimeout(joiningnodeAddress);
		joiningNode.setDACKReceiptTimeout(dackReceiptTimeout);
	}
	
	@Override
	public void applySummary(TRecovery_Join trj) {
		super.applySummary(trj);
		
		InetSocketAddress joiningnodeAddress = trj.getJoiningNode();
		IBFTOverlayNode joiningNode = (IBFTOverlayNode) _nodes.get(joiningnodeAddress);
		if(joiningNode == null)
			return;
		
		long dackReceiptTimeout = getDackReceiveTimeout(joiningnodeAddress);
		joiningNode.setDACKReceiptTimeout(dackReceiptTimeout);
	}
	
	@Override
	public final long getDackReceiveTimeout(InetSocketAddress remote) {
		Path<INode> pathFromRemote = getPathFrom(remote);
		if(pathFromRemote == null)
			return BFTConnectionManager.BFT_DACK_RECEIVE_INTERVAL;
		
		int distance = pathFromRemote.getLength();
		return BFTConnectionManager.BFT_DACK_RECEIVE_INTERVAL * distance;
	}

	@Override
	public IBFTSuspectable getSuspectable(InetSocketAddress suspect) {
		return (IBFTSuspectable) _nodes.get(suspect);
	}
}
