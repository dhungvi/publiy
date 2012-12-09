package org.msrg.publiy.broker.core.contentManager;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Set;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;

import org.msrg.publiy.networkcodes.PSCodedPiece;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReq;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReply;

public interface IContentManager {
	
	public Content getContent(Sequence contentSequenceId);
	public TNetworkCoding_CodedPieceId getCodedPieceId(Sequence contentSequenceId, InetSocketAddress remote);
	public TNetworkCoding_CodedPieceIdReq getAppropriateCodedPieceReq(Sequence contentSequenceId);
	

	public Content addContent(TNetworkCoding_PListReply plistreply);
	public Content addContent(SourcePSContent srcPSContent);
	public Content addContent(InetSocketAddress senderRemote, PSCodedPiece psCodedPiece);
	public Content addContent(TNetworkCoding_CodedPieceId tCodedPieceId);
	public Content addContent(PSSourceCodedBatch psSourceCodedBatch);
	public Content addContent(
			InetSocketAddress senderRemote, Sequence sourceSequence, Publication publication, int rows, int cols);
	
	
	public Set<Sequence> getContentsMissingMetadata();
	public void discardContent(Sequence sequence);
	public boolean canTakeOnMoreActivities();
	
	
	public boolean encodeAndSend(Sequence sequence, InetSocketAddress remote, IContentListener contentListener);
	public boolean decode(Sequence sequence, IContentListener contentListener);
	
	
	public void prepareToStart();
	public void startComponent();
	public Set<InetSocketAddress> getModifiableReplyingBrokers(Sequence sourceSequence);
	public Set<Sequence> getAllContentSequences();
	public Collection<Content> getAllContents();
}
