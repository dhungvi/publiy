package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT_Dack;
import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.IBFTSuspectable;
import org.msrg.publiy.broker.IBFTSuspectedRepo;
import org.msrg.publiy.broker.core.sequence.IBFTDackVerifier;
import org.msrg.publiy.broker.core.sequence.IBFTIssuer;
import org.msrg.publiy.broker.core.sequence.IBFTVerifiable;
import org.msrg.publiy.broker.core.sequence.IBFTVerifier;
import org.msrg.publiy.broker.core.sequence.IBFTVerifierProxy;
import org.msrg.publiy.broker.core.sequence.SequencePair;
import org.msrg.publiy.broker.security.keys.KeyManager;

public interface IBFTOverlayManager extends IBFTVerifier, IBFTIssuer, IBFTDackVerifier, IOverlayManager {

	public KeyManager getKeyManager();
	public IBFTVerifiable refineVerifiable(IBFTVerifiable verifiable);
	public boolean verifyVerifiable(IBFTVerifiable verifiable);
	public IBFTSuspectedRepo getSuspectedRepo();
	public Map<IBFTVerifierProxy, SequencePair> computeIntermediateVerifersSequencePairs(
			InetSocketAddress receivedFrom,
			boolean incrementSequencePairs, boolean includeLastReceivedDiscardedOrders,
			IBFTVerifiable verifiable, Set<InetSocketAddress> matchingSet);
	public Collection<SequencePair> getDownstreamSequencePairsOf(InetSocketAddress remote, Map<IBFTVerifierProxy, SequencePair> downstreamVerifersSequencePairs);
	public void trimSequencePairs(IBFTVerifiable verifiable, InetSocketAddress remote);
	public Set<InetSocketAddress> getMatchingSetForBFTDack(TMulticast_Publish_BFT_Dack bftHB);
	public void checkDackSafety(TMulticast_Publish_BFT _tm, Set<InetSocketAddress> matchingSet);
	public Set<InetSocketAddress> whoseDacksAreNotReceived();
	public boolean verifySuccession(
			IBFTSuspectedRepo bftSuspicionRepo, boolean isDack, IBFTVerifiable verifiable, List<BFTSuspecionReason> reasons);
	public void issueLocalMessageSequencePair(TMulticast_Publish_BFT tmBFT);
	public long getDackReceiveTimeout(InetSocketAddress nodeAddr);
	public IBFTSuspectable getSuspectable(InetSocketAddress suspect);

}