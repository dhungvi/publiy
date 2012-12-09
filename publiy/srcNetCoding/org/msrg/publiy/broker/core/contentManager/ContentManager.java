package org.msrg.publiy.broker.core.contentManager;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



import org.msrg.raccoon.CodedBatch;
import org.msrg.raccoon.ReceivedCodedBatch;
import org.msrg.raccoon.engine.ICodingEngine;
import org.msrg.raccoon.engine.ICodingListener;
import org.msrg.raccoon.engine.task.result.CodedSlice_CodingResult;
import org.msrg.raccoon.engine.task.result.CodingResult;
import org.msrg.raccoon.engine.task.result.Equals_CodingResult;


import org.msrg.publiy.networkcodes.PSCodedPiece;
import org.msrg.publiy.networkcodes.engine.ClientCodingEngineImp;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReq;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqBreak;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqBreakSendId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqSendContent;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqSendContentSendId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReply;
import org.msrg.publiy.utils.log.casuallogger.coding.CasualContentLogger;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.connectionManager.IConnectionManagerNC;
import org.msrg.publiy.broker.core.contentManager.activities.ContentManagerActivity;
import org.msrg.publiy.broker.core.contentManager.activities.ContentManagerActivity_CodeAndSend;
import org.msrg.publiy.broker.core.contentManager.activities.ContentManagerActivity_DecodePSBulkMatrix;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;
import org.msrg.publiy.component.ComponentStatus;
import org.msrg.publiy.component.IComponent;
import org.msrg.publiy.component.IComponentListener;

public class ContentManager implements IContentManager, ICodingListener, IComponent {
	
	public static final int MAX_CONCURRENT_ACTIVITIES;
	public static final int CODING_ENGINE_THREADS;
	
	static {
		String codingEngineThreadsStr = System.getProperty("ContentManager.CodingEngineThreads", "4");
		CODING_ENGINE_THREADS = new Integer(codingEngineThreadsStr).intValue();
		MAX_CONCURRENT_ACTIVITIES = Broker.MAX_ACTIVE_FLOWS;
	}
	
	protected final Map<CodingResult, ContentManagerActivity> _contentManagementActivities =
		new HashMap<CodingResult, ContentManagerActivity>();
	protected final Map<Sequence, Content> _allContents =
		new HashMap<Sequence, Content>();
	protected final Set<Sequence> _unknownContents =
		new HashSet<Sequence>();
	protected final Set<Sequence> _discardedContents =
		new HashSet<Sequence>();
	protected Map<Sequence, Set<InetSocketAddress>> _contentReplyingBrokers =
		new HashMap<Sequence, Set<InetSocketAddress>>();
	
	protected final IConnectionManagerNC _connectionManagerNC;
	protected final ICodingEngine _codingEngine;
	protected final CasualContentLogger _contentLogger;
	
	protected ComponentStatus _status = ComponentStatus.COMPONENT_STATUS_UNINITIALIZED;
	protected final IBrokerShadow _brokerShadow;
	
	public ContentManager(IConnectionManagerNC connectionManagerNC, IBrokerShadow brokerShadow) {
		_connectionManagerNC = connectionManagerNC;
		_codingEngine = new ClientCodingEngineImp(brokerShadow, CODING_ENGINE_THREADS);
		_brokerShadow = brokerShadow;
		_contentLogger = _brokerShadow.getContentLogger();
	}
	
	@Override
	public Content addContent(InetSocketAddress senderRemote, PSCodedPiece psCodedPiece) {
		Content content =
			addContent(senderRemote, psCodedPiece._sourceSeq, null, psCodedPiece.getRows(), psCodedPiece.getCols());
		
		content.addCodedPiece(senderRemote, psCodedPiece);
		return content;
	}

	public Content addContent(TNetworkCoding_CodedPieceId tCodedPieceId) {
		Content content = addContent(
				tCodedPieceId._sender, tCodedPieceId._sequence,
				tCodedPieceId._publication, tCodedPieceId._rows, tCodedPieceId._cols);
		
		if(content != null)
			content.getContentLifecycle().getMetadataArrivedTimes();
		
		return content;
	}
	
	@Override
	public Content addContent(SourcePSContent srcPSContent) {
		Sequence sequence = srcPSContent.getSourceSequence();
		Content existingContent = _allContents.get(sequence);
		if(existingContent != null)
			throw new IllegalStateException("Content " + srcPSContent.getSourceSequence() + " already exists.");
		
		if(_discardedContents.contains(sequence))
			throw new IllegalStateException("Content " + srcPSContent.getSourceSequence() + " has already been discarded.");
		
		_allContents.put(sequence, srcPSContent);
		
		return srcPSContent;
	}
	
	@Override
	public Content addContent(PSSourceCodedBatch psSourceCodedBatch) {
		Content content = _allContents.get(psSourceCodedBatch._sequence);
		if(content == null) {
			content = createEmptyContent(psSourceCodedBatch);
			Sequence contentSequence = psSourceCodedBatch._sequence;
			_allContents.put(contentSequence, content);
			if(_contentLogger != null)
				_contentLogger.contentPublished(contentSequence);
			
//			Publication publication = psSourceCodedBatch.getPublication();
//			Set<InetSocketAddress> matchingSet = _connectionManagerNC.matchingSet(publication);
//			BrokerInternalTimer.inform("MatchingSet2: " + null + ", " + publication);
//			content.addNeedNodes(matchingSet);
		}
		
		content.verify(psSourceCodedBatch._sequence);
		_discardedContents.remove(psSourceCodedBatch._sequence);
		
		return content;
	}

	@Override
	public Content addContent(TNetworkCoding_PListReply plistreply) {
		Sequence contentSequence = plistreply.getSourceSequence();
		InetSocketAddress replyingBroker = plistreply._sender;
		if(replyingBroker == null)
			throw new NullPointerException();
			
		Content content = _allContents.get(contentSequence);
		if(content == null) {
			int rows = plistreply._rows;
			int cols = plistreply._cols;
			
			content = createEmptyContent(contentSequence, rows, cols);
			_allContents.put(contentSequence, content);
			_unknownContents.add(contentSequence);
		}
		content.addNeedNodes(plistreply._remotes);
		if(_contentLogger!=null)
			_contentLogger.contentStarted(contentSequence);
		
		updateContentReplyingBroker(contentSequence, replyingBroker);
		return content;
	}
	
	@Override
	public Set<Sequence> getAllContentSequences() {
		return _allContents.keySet();
	}
	
	@Override
	public Collection<Content> getAllContents() {
		return _allContents.values();
	}
	
	protected void updateContentReplyingBroker(
			Sequence contentSequence, InetSocketAddress replyingBroker) {
		
		Set<InetSocketAddress> replyingBrokers =
			_contentReplyingBrokers.get(replyingBroker);
		if(replyingBrokers == null) {
			replyingBrokers = new HashSet<InetSocketAddress>();
			_contentReplyingBrokers.put(contentSequence, replyingBrokers);
		}
		
		replyingBrokers.add(replyingBroker);
	}
	
	@Override
	public Set<InetSocketAddress> getModifiableReplyingBrokers(Sequence sourceSequence) {
		return _contentReplyingBrokers.get(sourceSequence);
	}
	
	@Override
	public Content addContent(
			InetSocketAddress senderRemote,
			Sequence contentSequence,
			Publication publication,
			int rows, int cols) {
		Content content = _allContents.get(contentSequence);
		if(content == null) {
			content = createEmptyContent(contentSequence, rows, cols);
			_allContents.put(contentSequence, content);
			_unknownContents.add(contentSequence);
			if(_contentLogger != null)
				_contentLogger.contentStarted(contentSequence);
		}
		
		if(content.loadPublicationForFirstTime(senderRemote, publication)) {
//			Set<InetSocketAddress> matchingSet = _connectionManagerNC.matchingSet(publication);
//			BrokerInternalTimer.inform("MatchingSet1: " + matchingSet);
//			content.addNeedNodes(matchingSet);
			_unknownContents.remove(contentSequence);
			if(_connectionManagerNC != null)
				_connectionManagerNC.applyContentServingPolicy(content);
		}
		
		content.verifyAll(contentSequence, publication, rows, cols);
		_discardedContents.remove(contentSequence);
		
		return content;
	}

	@Override
	public void discardContent(Sequence sequence) {
		if(_allContents.remove(sequence) != null)
			_discardedContents.add(sequence);
		_unknownContents.remove(sequence);
	}
	
	private Content createEmptyContent(PSSourceCodedBatch psSourceCodedBatch) {
		return new SourcePSContent(psSourceCodedBatch);
	}
	
	private Content createEmptyContent(Sequence sequence, int rows, int cols) {
		return new ReceivedPSContent(_brokerShadow, sequence, null, rows * cols, rows);
	}
	
	@Override
	public final void codingFailed(CodingResult result) {
		synchronized(_contentManagementActivities) {
			_contentManagementActivities.remove(result);
		}
		
		new UnknownError("Some errors occured: " + result).printStackTrace();
	}

	@Override
	public void codingFinished(CodingResult result) {
		ContentManagerActivity activity;
		synchronized(_contentManagementActivities) {
			activity = _contentManagementActivities.remove(result);
		}
		
		if(activity == null)
			throw new IllegalStateException();
		
		switch(activity._codingActivityType) {
		case CONTENTMANAGER_ACTIVITY_DECODE:
		{
			ContentManagerActivity_DecodePSBulkMatrix decodeActivity =
				(ContentManagerActivity_DecodePSBulkMatrix) activity;
			
			Equals_CodingResult decodedResult = (Equals_CodingResult) result;
			activity._contentListener.decodedContentReady(
					decodedResult.getResult(), decodeActivity._psCodedBatch);
			break;
		}
			
		case CONTENTMANAGER_ACTIVITY_ENCODE_AND_SEND:
		{
			ContentManagerActivity_CodeAndSend codeActivity =
				(ContentManagerActivity_CodeAndSend) activity;
			
			CodedSlice_CodingResult codedResult = (CodedSlice_CodingResult) result;
			if(codedResult.isFailed())
				throw new IllegalStateException();
			if((PSCodedPiece)codedResult.getResult() == null)
				throw new NullPointerException();
			
			activity._contentListener.codedContentReady(
					(PSCodedPiece)codedResult.getResult(), codeActivity._remote);
			break;
		}
			
		default:
			throw new UnsupportedOperationException("Unknown type: " + activity._codingActivityType);
		}
	}

	@Override
	public void codingStarted(CodingResult result) {
		return;
	}

	@Override
	public void addNewComponentListener(IComponentListener comListener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void awakeFromPause() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getComponentName() {
		return "ContentMan";
	}

	@Override
	public ComponentStatus getComponentState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void pauseComponent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void prepareToStart() {
		if(_status != ComponentStatus.COMPONENT_STATUS_UNINITIALIZED)
			throw new IllegalStateException("State is invalid: " + _status);
		
		_codingEngine.init();
//		_codingEngine.registerCodingListener(this);
		
		_status = ComponentStatus.COMPONENT_STATUS_INITIALIZED;
	}

	@Override
	public void removeComponentListener(IComponentListener comListener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void startComponent() {
		if(_status != ComponentStatus.COMPONENT_STATUS_INITIALIZED)
			throw new IllegalStateException("State is invalid: " + _status);

		_status = ComponentStatus.COMPONENT_STATUS_STARTING;
		_codingEngine.startComponent();
		_status = ComponentStatus.COMPONENT_STATUS_RUNNING;
	}

	@Override
	public void stopComponent() {
		throw new UnsupportedOperationException();
	}

//	protected void codeNewCodedPieceAndSend(InetSocketAddress receiver, Sequence contentSequenceId) {
//		Content content = getContent(contentSequenceId);
//		if(content==null)
//			return;
//		
////		x;
//	}
	
	@Override
	public Content getContent(Sequence contentSequenceId) {
		return _allContents.get(contentSequenceId);
	}

	@Override
	public boolean decode(Sequence sequence, IContentListener contentListener) {
		Content content = getContent(sequence);
		if(content == null)
			return false;

		if(content._codeBatch.isSolved())
			return false;
		
		if(content._codeBatch.getRequiredCodedPieceCount() > content._codeBatch.getAvailableCodedPieceCount())
			return false;
		
		synchronized(_contentManagementActivities) {
			ContentManagerActivity activity =
				new ContentManagerActivity_DecodePSBulkMatrix(
						contentListener, content._codeBatch);
			
			CodingResult codingResult =
				_codingEngine.decode(this, (ReceivedCodedBatch) content._codeBatch);
			_contentManagementActivities.put(codingResult, activity);
			
			return true;
		}
	}

	@Override
	public boolean encodeAndSend(
			Sequence sequence, InetSocketAddress remote, IContentListener contentListener) {
		Content content = getContent(sequence);
		if(content == null)
			return false;
		
		synchronized(_contentManagementActivities) {
			ContentManagerActivity activity =
				new ContentManagerActivity_CodeAndSend(contentListener, content, remote);
			
			CodingResult codingResult =
				_codingEngine.encode(this, (CodedBatch) content._codeBatch);
			_contentManagementActivities.put(codingResult, activity);
			
			return true;
		}
	}

	@Override
	public TNetworkCoding_CodedPieceIdReq getAppropriateCodedPieceReq(Sequence contentSequenceId) {
		Content content = getContent(contentSequenceId);
		Publication publication = content.getPublication();
		InetSocketAddress replyAddress = _connectionManagerNC.getUDPLocalAddress();
		
		TNetworkCoding_CodedPieceIdReq codedPieceIdReq = null;
		boolean isSolved = content.isSolved();
		if(isSolved)
		{
			if(publication == null) {
				codedPieceIdReq =
					new TNetworkCoding_CodedPieceIdReqBreakSendId(replyAddress, contentSequenceId);
			} else {
				codedPieceIdReq =
					new TNetworkCoding_CodedPieceIdReqBreak(replyAddress, contentSequenceId, 0);
			}
		}
		else
		{
			if(publication == null) {
				codedPieceIdReq =
					new TNetworkCoding_CodedPieceIdReqSendContentSendId(replyAddress, contentSequenceId);
			} else {
				codedPieceIdReq =
					new TNetworkCoding_CodedPieceIdReqSendContent(replyAddress, contentSequenceId);
			}
		}
		
		return codedPieceIdReq;
	}

	@Override
	public TNetworkCoding_CodedPieceId getCodedPieceId(Sequence contentSequenceId, InetSocketAddress remote) {
		Content content = getContent(contentSequenceId);
		if(content == null)
			return null;
		
		if(!content.getSourceSequence().equalsExact(contentSequenceId))
			throw new IllegalStateException();
		
		Publication publication = content.getPublication();
		if(publication == null)
			return null;

		int rows = content.getRows();
		int cols = content.getCols();
		InetSocketAddress replyAddress = _connectionManagerNC.getUDPLocalAddress();
		
		ContentLifecycle contentLC = content._contentLifecycle;
		contentLC.metadataSent(remote);
		
		return new TNetworkCoding_CodedPieceId(
				replyAddress, contentSequenceId, publication, rows, cols);
	}
	
	@Override
	public Set<Sequence> getContentsMissingMetadata() {
		return _unknownContents;
	}

	@Override
	public void codingPreliminaryStageCompleted(CodingResult result) {
		ContentManagerActivity activity;
		synchronized(_contentManagementActivities) {
			activity = _contentManagementActivities.get(result);
		}
		
		if(activity == null)
			return;
		
		switch(activity._codingActivityType) {
		case CONTENTMANAGER_ACTIVITY_DECODE:
		{
			ContentManagerActivity_DecodePSBulkMatrix decodeActivity =
				(ContentManagerActivity_DecodePSBulkMatrix) activity;
			
			activity._contentListener.decodedContentInversed(decodeActivity._psCodedBatch.getSourceSequence());
			break;
		}
		
		default:
			throw new UnsupportedOperationException();
		}		
	}
	
	@Override
	public String toString() {
		synchronized(_contentManagementActivities){
			return "ContentMan-" + _connectionManagerNC.getLocalAddress().getPort() + ":" + _contentManagementActivities;
		}
	}

	@Override
	public boolean canTakeOnMoreActivities() {
		synchronized(_contentManagementActivities){
			return _contentManagementActivities.size() < MAX_CONCURRENT_ACTIVITIES;
		}
	}

	public int getCurrentActivitiesSize() {
		synchronized(_contentManagementActivities){
			return _contentManagementActivities.size();
		}
	}
	
	protected CasualContentLogger getContentLogger() {
		return _contentLogger;
	}
}