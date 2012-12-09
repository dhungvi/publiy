package org.msrg.publiy.broker.networkcoding.connectionManager.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.component.ComponentStatus;



import org.msrg.publiy.networkcodes.PSCodedPiece;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPiece;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReq;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqBreak;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqBreakDeclined;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqBreakSendId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqSendContentSendId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqSendId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReply;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReq;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.multipath.loadweights.LoadWeightRepository;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.log.casuallogger.coding.CasualPListLogger;
import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent_AddContentFlow;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent_CodedContentReady;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent_ContentInversed;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent_DecodedContentReady;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent_ProcessIncompleteBreakedWatchList;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent_ProcessIncompleteUnbreakedWatchList;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent_ProcessServeQueue;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent_RequestNetworkCodingIdReq;
import org.msrg.publiy.broker.core.connectionManager.ConnectionEvent_publishContent;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerJoin;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerNC;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerRecovery;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerTypes;
import org.msrg.publiy.broker.core.connectionManager.TimerTask_ProcessIncompleteBreakedWatchList;
import org.msrg.publiy.broker.core.connectionManager.TimerTask_ProcessIncompleteUnbreakedWatchList;
import org.msrg.publiy.broker.core.connectionManager.TimerTask_ProcessNextFlow;
import org.msrg.publiy.broker.core.connectionManager.TimerTask_ProcessServeQueue;
import org.msrg.publiy.broker.core.connectionManager.TimerTask_RequestNetworkCodingIdReq;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.contentManager.ContentBreakPolicy;
import org.msrg.publiy.broker.core.contentManager.ContentManager;
import org.msrg.publiy.broker.core.contentManager.IContentListener;
import org.msrg.publiy.broker.core.contentManager.IContentManager;
import org.msrg.publiy.broker.core.contentManager.SourcePSContent;
import org.msrg.publiy.broker.core.contentManager.servingPolicy.ContentServingPolicy;
import org.msrg.publiy.broker.core.flowManager.FlowManager;
import org.msrg.publiy.broker.core.flowManager.FlowPriority;
import org.msrg.publiy.broker.core.flowManager.FlowSelectionPolicy;
import org.msrg.publiy.broker.core.flowManager.IFlow;
import org.msrg.publiy.broker.core.flowManager.IFlowManager;
import org.msrg.publiy.broker.core.plistManager.PListManager;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.core.sequence.SequenceCounter;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.IPSCodedBatch;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;

/**
 * @author Reza
 *
 */
public class ConnectionManagerNC_Client extends ConnectionManagerNC implements IContentListener {

	public static final int TIMER_DELAY_PROCESS_INCOMPLETE_BREAKED_WATCH_LIST = 10 * 1000;
	public static final int TIMER_DELAY_PROCESS_INCOMPLETE_UNBREAKED_WATCH_LIST =
		3 * TIMER_DELAY_PROCESS_INCOMPLETE_BREAKED_WATCH_LIST;
	public static final int TIMER_DELAY_PROCESS_NEXT_FLOW = 50;
	public static final int TIMER_DELAY_PROCESS_SERVE_QUEUE = 500;
	public static final int SOURCE_PLISTS_REQUESTS = 2;
	public static final int NONSOURCE_PLISTS_REQUESTS = 2;
	public static final int MAX_SERVE_TIME = 100 * 1000;
	public static final int MIN_PIECE_SERVE_COUNT = (int)(Broker.COLS * 1.1);
	
	protected final Map<Sequence, SequenceCounter> _sequencePListCounter =
		new HashMap<Sequence, SequenceCounter>();
	protected final Map<Sequence, SequenceCounter> _sequenceServeCounter =
		new HashMap<Sequence, SequenceCounter>();
	protected final IContentManager _contentManager;
	protected final IFlowManager _flowManager;
	protected final Set<Content> _incompleteBreakedWatchSet =
		new HashSet<Content>();
	protected final Set<Sequence> _remotelyBreakedSequences =
		new HashSet<Sequence>();
	protected final Set<Sequence> _locallyBreakedContents =
		new HashSet<Sequence>();
	protected final Map<Sequence, List<InetSocketAddress>> _previousWatchListRequestee =
		new HashMap<Sequence, List<InetSocketAddress>>();
	protected final ContentBreakPolicy _contentBreakPolicy;
	
	protected final List<Content> _serveQueue =
		new LinkedList<Content>();
	protected final Set<Content> _serveQueueSet =
		new HashSet<Content>();

	protected final Map<Sequence, Long> _beingServedTimer =
		new HashMap<Sequence, Long>();

	protected Set<Sequence> _pendingPListRequests =
		new HashSet<Sequence>();
	Set<Content> _pendingMetadataRequestsSet =
		new HashSet<Content>();
	
	protected ConnectionManagerNC_Client(InetSocketAddress localUDPAddress,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue,
			TRecovery_Join[] trjs,
			LoadWeightRepository loadWeightRepo)
			throws IOException {
		super("ConMan-NC-CLNT",
				ConnectionManagerTypes.CONNECTION_MANAGER_NC_CLIENT,
				localUDPAddress,
				broker,
				brokerShadow,
				overlayManager,
				subscriptionManager,
				messageQueue,
				trjs,
				loadWeightRepo);
		
		_flowManager =
			new FlowManager(brokerShadow,
					Broker.BW_IN,
					Broker.BW_OUT,
					Broker.BW_OUT);
		
		_contentManager = new ContentManager(this, brokerShadow);
		if(_brokerShadow!=null)
			_contentBreakPolicy = _brokerShadow.getContentBreakPolicy();
		else
			_contentBreakPolicy = BrokerShadow.getDefaultContentBreakPolicy();
		
		scheduleProcessServeQueue();
		scheduleProcessNextFlowEvent();
		scheduleProcessIncompleteBreakedWatchList();
		scheduleProcessIncompleteUnBreakedWatchList();
	}

	public ConnectionManagerNC_Client(ConnectionManagerJoin conManJoin) {
		super(conManJoin);
		
		_flowManager =
			new FlowManager(_brokerShadow,
					Broker.BW_IN,
					Broker.BW_OUT,
					Broker.BW_OUT);
		
		_contentManager = new ContentManager(this, _brokerShadow);
		if(_brokerShadow!=null)
			_contentBreakPolicy = _brokerShadow.getContentBreakPolicy();
		else
			_contentBreakPolicy = BrokerShadow.getDefaultContentBreakPolicy();
		
		scheduleProcessServeQueue();
		scheduleProcessNextFlowEvent();
		scheduleProcessIncompleteBreakedWatchList();
		scheduleProcessIncompleteUnBreakedWatchList();
	}

	public ConnectionManagerNC_Client(ConnectionManagerRecovery conManRecovery) {
		super(conManRecovery);
		
		_flowManager =
			new FlowManager(_brokerShadow,
					Broker.BW_IN,
					Broker.BW_OUT,
					Broker.BW_OUT);
		
		_contentManager = new ContentManager(this, _brokerShadow);
		if(_brokerShadow!=null)
			_contentBreakPolicy = _brokerShadow.getContentBreakPolicy();
		else
			_contentBreakPolicy = BrokerShadow.getDefaultContentBreakPolicy();

		scheduleProcessServeQueue();
		scheduleProcessNextFlowEvent();
		scheduleProcessIncompleteBreakedWatchList();
		scheduleProcessIncompleteUnBreakedWatchList();
	}
	
	@Override
	protected boolean doComponentStateTransitions(){
		ComponentStatus oldStatus = getComponentState();
		boolean ret = super.doComponentStateTransitions();
		ComponentStatus newStatus = getComponentState();
		BrokerInternalTimer.inform(
				"TRANSITIONING[" + (ret?"T":"F") + "]: FROM: " + oldStatus + " TO: " + newStatus);
		
		return ret;
	}

	@Override
	protected void handleTNetworkCoding_CodedPiece(TNetworkCoding_CodedPiece psCodedPiece) {
		InetSocketAddress remote = psCodedPiece._sender;
		Content content = _contentManager.addContent(remote, psCodedPiece);
		Sequence sourceSequence = psCodedPiece._sourceSeq;
		boolean fromMainSeed = psCodedPiece._fromMainSeed;
		
		if(content == null) {
			_pendingPListRequests.add(sourceSequence);
			sendPListRequest(sourceSequence, -1, -1);
			return;
		}

		boolean isInversed = content.isInversed();
		
		if(isInversed)
			_brokerShadow.getContentLogger().contentCodedPieceReceivedAfterInverse(
					sourceSequence, remote, fromMainSeed);
		else
			_brokerShadow.getContentLogger().contentCodedPieceReceived(
					sourceSequence, remote, fromMainSeed);
		
		if(fromMainSeed)
			content.addHaveNode(remote);
		
		if(isInversed) {
			sendCodedPieceIdReqBreak(sourceSequence, remote, -1);
			sendCodedPieceIdReqBreak(sourceSequence, getClientsJoiningBrokerAddress(), -1);
			return;
		}

		tryContentDecode(content);
		applyContentServingPolicy(content);
		applyBreakPolicy(content, true);
		maintainIncompleteWatchList(content);
	}
	
	protected boolean maintainIncompleteWatchList(Content content) {
		if(content == null)
			return false;
		
		Sequence sourceSequence = content.getSourceSequence();
		
		if(content.isInversed())
		{
			if(_incompleteBreakedWatchSet.remove(content)) {
				_brokerShadow.getContentLogger().contentUnWatched(sourceSequence);
				List<InetSocketAddress> prevRequestees =
					_previousWatchListRequestee.get(sourceSequence);
				if(prevRequestees != null)
					for(InetSocketAddress prevRequestee : prevRequestees)
						sendCodedPieceIdReqBreak(
								sourceSequence, prevRequestee, -1);
			}
		} else if(_locallyBreakedContents.contains(content.getSourceSequence()) &&
				!_incompleteBreakedWatchSet.contains(content))
		{
				_incompleteBreakedWatchSet.add(content);
				_brokerShadow.getContentLogger().contentWatched(sourceSequence);
		}
		
		return _incompleteBreakedWatchSet.contains(content);
	}
	
	protected boolean tryContentDecode(Content content) {
		if(content.isInversed())
			return false;
		
		if(content.canPotentiallyBeSolved()) {
			_contentManager.decode(content.getSourceSequence(), this);
			return true;
		}
		
		return false;
	}

	@Override
	protected void handleTNetworkCoding_CodedPieceId(TNetworkCoding_CodedPieceId psCodedPieceId) {
		Content content = _contentManager.addContent(psCodedPieceId);
		_pendingMetadataRequestsSet.remove(content);
		if(content.isSolved())
			content.loadDefaultSourcePSContent();
		
		applyContentServingPolicy(content);
	}

	@Override
	protected void handleTNetworkCoding_CodedPieceIdReq(TNetworkCoding_CodedPieceIdReq psCodedPieceIdReq) {
		Sequence contentSequence = psCodedPieceIdReq.getSourceSequence();
		Content content = _contentManager.getContent(contentSequence);

		InetSocketAddress sender = psCodedPieceIdReq._sender;
		if(_localAddress.equals(sender))
			throw new IllegalStateException(sender.toString() + " vs. " + psCodedPieceIdReq.toString());
		
		switch(psCodedPieceIdReq._reqType) {
		case BREAK_DECLINE:
		{
			if(content == null)
				return;

			TNetworkCoding_CodedPieceIdReqBreakDeclined tBreakDeclined =
				(TNetworkCoding_CodedPieceIdReqBreakDeclined) psCodedPieceIdReq;
			InetSocketAddress declinee = tBreakDeclined._sender;
			int outstandingPieces = tBreakDeclined.getOustandingPieces();
			_brokerShadow.getContentLogger().contentBreakDeclineReceived(
					contentSequence, sender, outstandingPieces);
			
			int currentOutstandingPieces =
				content.getRequiredCodedPieceCount() -
						content.getAvailableCodedPieceCount();
			outstandingPieces =
				outstandingPieces < currentOutstandingPieces
						? outstandingPieces : currentOutstandingPieces;
			
			if(outstandingPieces >
				content.getRequiredCodedPieceCount() -
						_contentBreakPolicy._minReceivedSlicesForBreak)
				return;
			if(outstandingPieces <= 1)
				return;
			List<InetSocketAddress> prevRequestees =
				_previousWatchListRequestee.get(contentSequence);
			if(prevRequestees == null)
				return;
			if(!prevRequestees.contains(declinee))
				return;
			InetSocketAddress newRequestee = content.getOneRandomHaveNode();
			if(newRequestee == null)
				return;
			if(prevRequestees.contains(newRequestee))
				return;
			prevRequestees.add(newRequestee);
			sendCodedPieceIdReqBreak(
					contentSequence, newRequestee, outstandingPieces-1);
		}
		break;
		
		case BREAK:
		{
			if(content == null)
				return;

			TNetworkCoding_CodedPieceIdReqBreak tBreak =
				(TNetworkCoding_CodedPieceIdReqBreak) psCodedPieceIdReq;
			int piecesToSendBeforeBreak = tBreak._breakingSize;
			_brokerShadow.getContentLogger().contentBreakReceived(
					contentSequence, sender, piecesToSendBeforeBreak);
			
			if(piecesToSendBeforeBreak <= content.getRequiredCodedPieceCount() - _contentBreakPolicy._minReceivedSlicesForBreak) {
				_remotelyBreakedSequences.add(content.getSourceSequence());
			} else {
				IFlow flow = _flowManager.getFlow(sender, contentSequence);
				if(flow != null)
					flow.setPiecesToSendBeforeBreak(piecesToSendBeforeBreak);
				return;
			}
			
			if(piecesToSendBeforeBreak == -1)
				content.addHaveNode(sender);
			else if(piecesToSendBeforeBreak == 0)
				content.removeNeedNode(sender);
			else
				content.addNeedNode(sender);
			
			if(content.hasContent(sender)) {
				IFlow flow = _flowManager.getFlow(sender, contentSequence);
				if(flow != null && flow.isActive()) {
					flow.fullstop();
					flow.setPiecesToSendBeforeBreak(piecesToSendBeforeBreak);
					flow.deactivate();
				}
				
				return;
			}
			
			FlowPriority flowPriority =
				_flowManager.getFlowPriority(content, sender, piecesToSendBeforeBreak);
			IFlow flow =
				_flowManager.addFlow(
						flowPriority, sender, content, piecesToSendBeforeBreak);
			if(piecesToSendBeforeBreak > 0)
				if(flow == null)
					throw new IllegalStateException("" + piecesToSendBeforeBreak);
				else if(!flow.isActive())
					throw new IllegalStateException(
							sender + " vs. " + piecesToSendBeforeBreak);
			
			if(piecesToSendBeforeBreak == 0) {
				if(flow != null) {
					if(flow.isActive())
						throw new IllegalStateException("" + piecesToSendBeforeBreak);
					flow.stop();
				}
			}
			
			if(!content.isInversed() && piecesToSendBeforeBreak > 0 && piecesToSendBeforeBreak < content.getRows() - _contentBreakPolicy._minReceivedSlicesForBreak) {
				int morePiecesWeCanSend = content.getAvailableCodedPieceCount() - flow.getPiecesSent();
				int outstandingPieces = piecesToSendBeforeBreak - morePiecesWeCanSend;
				if(outstandingPieces > 0 || morePiecesWeCanSend <= 0)
					sendCodedPieceIdReqBreakDeclined(
							contentSequence, sender,
							outstandingPieces < morePiecesWeCanSend ? outstandingPieces : morePiecesWeCanSend);
			}
			
			serveContent(content);
		}
		break;
			
		case BREAK_SEND_ID:
		{
			if(content == null)
				return;

			content.addHaveNode(sender);
			_flowManager.deactivate(sender, contentSequence);
			sendCodedPieceId(contentSequence, sender);
		}
		break;
			
		case SEND_CONTENT:
		{
		}
		break;
			
		case SEND_CONTENT_ID:
			sendCodedPieceId(contentSequence, sender);
			break;
			
		case SEND_ID:
			sendCodedPieceId(contentSequence, sender);
			break;
			
		case PLIST_REPLY:
			TNetworkCoding_PListReply pList =
				(TNetworkCoding_PListReply) psCodedPieceIdReq;
			handleTNetworkCoding_PList(pList);
			break;
			
		case PLIST_REQ:
			TNetworkCoding_PListReq pListReq =
				(TNetworkCoding_PListReq) psCodedPieceIdReq;
			throw new UnsupportedOperationException("" + pListReq);
			
		default:
			throw new UnsupportedOperationException(
					"Unknown request type: " + psCodedPieceIdReq._reqType);
		}
	}

	@Override
	protected void handleTNetworkCoding_PList(
			TNetworkCoding_PListReply pList) {
		BrokerInternalTimer.inform("THERE: " + pList);
		// Add content flows...
		Sequence contentSequence = pList._sequence;
		InetSocketAddress remoteBroker = pList._sender;
		_brokerShadow.getContentLogger().contentPlistReceived(
				contentSequence, remoteBroker, pList._launch);

		updateSequencePListCounter(
				_sequencePListCounter, contentSequence, _localAddress);
		_pendingPListRequests.remove(contentSequence);
		
		Set<InetSocketAddress> needNodes = pList._remotes;
		needNodes.remove(_localAddress);
		Content content = _contentManager.addContent(pList);

		if(pList._launch) {
			_flowManager.addLaunchContent(contentSequence, pList._remotes);
		}
		
		addContentFlows(content);
		_brokerShadow.getContentLogger().contentServed(contentSequence);
		
		CasualPListLogger plistLogger = _brokerShadow.getPlistLogger();
		if(plistLogger != null)
			if(pList._launch)
				plistLogger.plistBrokerReplyReceived(
						pList._sender, contentSequence, null, needNodes);
			else
				plistLogger.plistBrokerReplyReceived(
						pList._sender, contentSequence, needNodes, null);
	}
	
	protected static boolean checkSequencePListCounter(
			Map<Sequence, SequenceCounter> sequencePListCounter,
			Sequence contentSequence, boolean isSource) {
		SequenceCounter sequenceCounter = sequencePListCounter.get(contentSequence);
		if(sequenceCounter == null) {
			sequenceCounter = new SequenceCounter(contentSequence);
			sequencePListCounter.put(contentSequence, sequenceCounter);	
			return true;
		}

		return sequenceCounter.increaseAndCompareLess(
				isSource ? SOURCE_PLISTS_REQUESTS : NONSOURCE_PLISTS_REQUESTS, true);
	}

	protected static void updateSequencePListCounter(
			Map<Sequence, SequenceCounter> sequencePListCounter,
			Sequence contentSequence, InetSocketAddress localAddress) {
		if(!contentSequence._address.equals(localAddress))
			return;
		
		SequenceCounter sequenceCounter = sequencePListCounter.get(contentSequence);
		if(sequenceCounter == null)
			sequenceCounter = new SequenceCounter(contentSequence);
		
		sequenceCounter.increaseAndCompareLess(-1, -1, false);
		sequencePListCounter.put(contentSequence, sequenceCounter);
	}
	
	protected void sendCodedPieceId(Sequence contentSequence, InetSocketAddress requester) {
		Content content = _contentManager.getContent(contentSequence);
		if(content == null)
			return;
		
		TNetworkCoding_CodedPieceId tCodedPieceId =
			_contentManager.getCodedPieceId(contentSequence, requester);
		sendAsUDP(tCodedPieceId, requester);
	}

	@Override
	public void addContentFlows(Content content) {
		if(content == null)
			throw new NullPointerException();
		
		ConnectionEvent connEvent =
			new ConnectionEvent_AddContentFlow(_localSequencer, content);
		addConnectionEvent(connEvent);
	}

	protected final void handleConnectionEvent_addContentFlow(
			ConnectionEvent_AddContentFlow connEvent) {
		Content content = connEvent._content;
		if(content == null)
			return;
		
		Publication publication = content.getPublication();
		if(publication == null)
			requestNetworkCodingIdReq(content);
		
		Sequence contentSequence = content.getSourceSequence();
		Set<InetSocketAddress> needNodes = content.getNeedNodes();
		
		for(InetSocketAddress needNode : needNodes)
		{
			if(needNode.equals(getLocalAddress()))
				continue;
			
			if(needNode.equals(content.getSource()))
				continue;
			
			if(content.hasContent(needNode))
				continue;

			FlowPriority flowPriority = _flowManager.getFlowPriority(content, needNode);
			IFlow flow = _flowManager.getFlow(needNode, contentSequence);
			if(flow != null && flow.isActive()) {
				flow.upgradeFlowPriority(flowPriority);
				continue;
			}

			flow = _flowManager.addFlow(flowPriority, needNode, content);
			if(flow == null)
				throw new NullPointerException();
			
			InetSocketAddress matchingUDPRemote = Broker.getUDPListeningSocket(needNode);
			getOrCreateUDPConnection(matchingUDPRemote);
			
			sendCodedPieceId(flow.getContent(), flow.getRemoteAddress());
		}
		
		handleConnectionEvent_processNextNetcodingFlow();
	}
	
	protected Map<Sequence, Long> _lastPListRequestSentForContentSequence =
		new HashMap<Sequence, Long>();
	protected boolean canSendPListRequestForContentSequence(Sequence contentSequence) {
		return canSendPListRequestForContentSequence(
				_lastPListRequestSentForContentSequence, contentSequence);
	}
	
	protected static boolean canSendPListRequestForContentSequence(
			Map<Sequence, Long> lastPListRequestSentForContentSequence,
			Sequence contentSequence) {
		Long lastTime = lastPListRequestSentForContentSequence.get(contentSequence);
		long currTime = SystemTime.currentTimeMillis();
		
		if(lastTime == null){
			lastPListRequestSentForContentSequence.put(contentSequence, currTime);
			return true;
		} else if(currTime - lastTime<Broker.PLIST_REQUEST_MIN_INTERVAL) {
			return false;
		} else {
			lastPListRequestSentForContentSequence.put(contentSequence, currTime);
			return true;
		}
	}
	
	protected void handleConnectionEvent_SendPListRequest(ConnectionEvent_SendPListRequest connEvent) {
		Sequence contentSequence = connEvent._contentSequence;
		if(!_pendingPListRequests.contains(contentSequence))
			return;

		boolean fromSource = connEvent._fromSource;
		int rows = connEvent._rows;
		int cols = connEvent._cols;
		
		if(//(!fromSource || _flowManager.canAddNewFlows()) && 
				canSendPListRequestForContentSequence(contentSequence)) {
			Content content = _contentManager.getContent(contentSequence);
			Publication publication = content.getPublication();
			if(publication == null && fromSource)
				throw new NullPointerException();
			
			TNetworkCoding_PListReq tPListReq =
				new TNetworkCoding_PListReq(
						_localAddress, contentSequence,
						rows, cols,
						publication, fromSource);
			InetSocketAddress clientsJoinpointBroker =
				getClientsJoiningBrokerAddress();
			if(clientsJoinpointBroker == null)
				throw new NullPointerException();
			sendAsUDP(tPListReq, clientsJoinpointBroker);
			_brokerShadow.getContentLogger().contentPlistRequested(
					contentSequence, clientsJoinpointBroker);
		} else {
//			BrokerInternalTimer.inform(
//					"CANNOT SEND PlIST REQUEST[" + _flowManager.getActiveFlowsCount() + "," +
//					canSendPListRequestForContentSequence(contentSequence) + "]: " + connEvent);
		}
		
		scheduleNextSendPListRequest(contentSequence, rows, cols, fromSource, true);
	}
	
	protected void scheduleNextSendMetadataRequest(Content content) {
		if(!_pendingMetadataRequestsSet.contains(content))
			return;
		
		TimerTask_RequestNetworkCodingIdReq sendMetadataRequest_TimerTask =
			new TimerTask_RequestNetworkCodingIdReq(this, content);
		scheduleTaskWithTimer(sendMetadataRequest_TimerTask, Broker.SEND_METADATA_REQ_TIMER_INTERVAL);
	}
	
	protected void scheduleNextSendPListRequest(
			Sequence contentSequence, int rows, int cols, boolean fromSource, boolean canAddNewFlow) {
		SendPListRequest_TimerTask sendPListRequest_TimerTask =
			new SendPListRequest_TimerTask(this, contentSequence, rows, cols, fromSource);
		if(canAddNewFlow)
			scheduleTaskWithTimer(sendPListRequest_TimerTask, Broker.SEND_PLIST_REQ_SHORT_TIMER_INTERVAL);
		else
			scheduleTaskWithTimer(sendPListRequest_TimerTask, Broker.SEND_PLIST_REQ_LONG_TIMER_INTERVAL);
	}
	
	@Override
	protected void handleTMulticastPublishMPMessage(ISession session, TMulticast_Publish_MP tmp) {
		throw new UnsupportedOperationException("This is a client: " + tmp);
	}
	
	@Override
	protected boolean handleConnectionEvent_Special(ConnectionEvent connEvent) {
		switch(connEvent._eventType) {
		case CONNECTION_EVENT_PROCESS_NEXT_NETCODING_FLOW:
			handleConnectionEvent_processNextNetcodingFlow();
			return true;
			
		case CONNECTION_EVENT_ADD_CONTENT_FLOW:
			handleConnectionEvent_addContentFlow(
					(ConnectionEvent_AddContentFlow)connEvent);
			return true;
			
		case CONNECTION_EVENT_PROCESS_SERVE_QUEUE:
			handleConnectionEvent_processServeQueue();
			return true;
			
		case CONNECTION_EVENT_PROCESS_INCOMPLETE_BREAKED_WATCH_LIST:
			handleConnectionEvent_processIncompleteBreakedWatchList();
			return true;
			
		case CONNECTION_EVENT_PROCESS_INCOMPLETE_UNBREAKED_WATCH_LIST:
			handleConnectionEvent_processIncompleteUnBreakedWatchList();
			return true;
			
		case CONNECTION_EVENT_CODED_CONTENT_READY:
			handleConnectionEvent_CodedContentReady(
					(ConnectionEvent_CodedContentReady) connEvent);
			return true;
			
		case CONNECTION_EVENT_DECODED_CONTENT_READY:
			handleConnectionEvent_DecodedContentReady(
					(ConnectionEvent_DecodedContentReady) connEvent);
			return true;
			
		case CONNECTION_EVENT_PUBLISH_CONTENT:
			handleConnectionEvent_publishContent(
					(ConnectionEvent_publishContent) connEvent);
			return true;
			
		case CONNECTION_EVENT_CONTENT_INVERSED:
			handleConnectionEvent_ContentInversed(
					(ConnectionEvent_ContentInversed) connEvent);
			return true;
			
		case CONNECTION_EVENT_SEND_PLIST_REQUESTS:
			handleConnectionEvent_SendPListRequest(
					(ConnectionEvent_SendPListRequest)connEvent);
			return true;
			
		case CONNECTION_EVENT_SEND_CONTENTID_REQ:
			handleConnectionEvent_RequestNetworkCodingIdReq(
					(ConnectionEvent_RequestNetworkCodingIdReq) connEvent);
			return true;
			
		default:
			return super.handleConnectionEvent_Special(connEvent);
		}
	}
	
	int i=0;
	protected void handleConnectionEvent_processNextNetcodingFlow() {
		FlowSelectionPolicy flPolicy = getFlowSectionPolicy();
		int packetSize = getDefaultFlowPacketSize();
	
		while(_contentManager.canTakeOnMoreActivities()) {
			IFlow flow = _flowManager.getNextOutgoingFlow(flPolicy, packetSize);
			if(flow == null)
				return;
			
			if(!flow.isActive())
				throw new IllegalStateException();
			
			InetSocketAddress remote = flow.getRemoteAddress();
			Sequence sequence = flow.getContentSequence();
			_contentManager.encodeAndSend(sequence, remote, this);
		}
	}

	protected void handleConnectionEvent_processServeQueue() {
		BrokerInternalTimer.inform("handleConnectionEvent_processServeQueue:" + _serveQueue.size());
		int counter = 0;
		for(Iterator<Content> it = _serveQueue.iterator() ; it.hasNext() ; ) {
			Content content = it.next();
			Sequence contentSequence = content.getSourceSequence();
			Long startTime = _beingServedTimer.get(contentSequence);
			if(startTime == null) {
				if(counter < Broker.MAX_ACTIVE_FLOWS) {
					counter++;
					actuallyServeContent(content);
				}
//				else
//					throw new IllegalStateException(counter + " vs. " + content + " vs. " + _serveQueue);
				continue;
			}
			
			long timeFromStart = SystemTime.currentTimeMillis() - startTime;
			if(_remotelyBreakedSequences.contains(contentSequence)) {
				_brokerShadow.getContentLogger().contentTookTooLong(contentSequence, timeFromStart);
				it.remove();
			} else if(content.getPiecesSent() >= MIN_PIECE_SERVE_COUNT) {
				_brokerShadow.getContentLogger().contentTookTooLong(contentSequence, timeFromStart);
				it.remove();
			} else if(timeFromStart > MAX_SERVE_TIME) {
				_brokerShadow.getContentLogger().contentTookTooLong(contentSequence, timeFromStart);
				it.remove();
			} else {
				counter++;
				if(counter <= Broker.MAX_ACTIVE_FLOWS)
					actuallyServeContent(content);
				else
					break;
			}
		}
	}
	
	protected void handleConnectionEvent_processIncompleteUnBreakedWatchList() {
		Collection<Content> allContents = _contentManager.getAllContents();
		for(Content content : allContents) {
			if(content.isInversed())
				continue;
			
			if(_incompleteBreakedWatchSet.contains(content))
				continue;
		
			int availablePieces = content.getAvailableCodedPieceCount();
			if(availablePieces >= _contentBreakPolicy._minReceivedSlicesForBreak)
				continue;
			
			List<InetSocketAddress> sendingRemotes =
				content.getSendingRemotes(0, false);
			if(sendingRemotes == null)
				continue;
			
			Sequence contentSequence = content.getSourceSequence();
			InetSocketAddress sourceAddress = contentSequence._address;
			int outstandingPiecesCount =
				content.getRequiredCodedPieceCount() - availablePieces;
			for(InetSocketAddress sendingRemote : sendingRemotes) {
				if(sendingRemote.equals(sourceAddress))
					if(_rand.nextBoolean())
						continue;
				
				sendCodedPieceIdReqBreak(
						contentSequence, sendingRemote,
						outstandingPiecesCount);
			}
		}
	}
	
	final Random _rand = new Random(SystemTime.currentTimeMillis());
	protected void handleConnectionEvent_processIncompleteBreakedWatchList() {
		int size = _incompleteBreakedWatchSet.size();
		_brokerShadow.getContentLogger().processingIncompleteWatchList(size);
		
		for(Content content : _incompleteBreakedWatchSet.toArray(new Content[0])) {
			if(!maintainIncompleteWatchList(content))
				continue;

			boolean isInversed = content.isInversed();
			if(isInversed)
				throw new IllegalStateException();
				
			int remainingBlocks =
				content.getRequiredCodedPieceCount() -
						content.getAvailableCodedPieceCount();
			if(remainingBlocks<=0)
				remainingBlocks = 1;
			
			Sequence sourceSequence = content.getSourceSequence();
			int requesteeCount = getRequesteeCount(remainingBlocks);
			Set<InetSocketAddress> requesteeNodes =
				new HashSet<InetSocketAddress>();
			content.getRandomHaveNodes(requesteeCount, requesteeNodes);
			if(!content.isSendingRemote(sourceSequence._address))
				if(_rand.nextInt(50) != 0)
					requesteeNodes.remove(sourceSequence._address);
			content.getRandomNeedNodes(requesteeCount, requesteeNodes);
			content.getRandomSendingNodes(requesteeCount, requesteeNodes);
			if(requesteeNodes.isEmpty())
				continue;
			
			requesteeCount = requesteeNodes.size();

			List<InetSocketAddress> prevRequestees =
				_previousWatchListRequestee.get(sourceSequence);
			if(prevRequestees == null) {
				prevRequestees = new LinkedList<InetSocketAddress>();
				_previousWatchListRequestee.put(
						sourceSequence,prevRequestees);
			}
			
			for(InetSocketAddress prevRequestee : prevRequestees)
				if(!requesteeNodes.contains(prevRequestee))
					sendCodedPieceIdReqBreak(
							sourceSequence, prevRequestee, 0);
			prevRequestees.clear();
			
			boolean firstnode = true;
			for(InetSocketAddress requesteeNode : requesteeNodes) {
				if(!requesteeNode.equals(_localAddress)) {
					int requestingCount =
							(remainingBlocks / requesteeCount)
							+
							(firstnode ? (remainingBlocks % requesteeCount) : 0);
					prevRequestees.add(requesteeNode);
					sendCodedPieceIdReqBreak(
							sourceSequence, requesteeNode, requestingCount);
					
					firstnode = false;
				}
			}
		}
	}
	
	private int getRequesteeCount(int remainingBlocks) {
		if(remainingBlocks <= 2)
			return 1;

		if(remainingBlocks <= 5)
			return 2;
		
		return remainingBlocks / 3;
	}

	public void processServeQueue() {
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent = new ConnectionEvent_ProcessServeQueue(localSequencer);
		addConnectionEvent(connEvent);
	}
	
	public void processIncompleteUnBreakedWatchList() {
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent = new ConnectionEvent_ProcessIncompleteUnbreakedWatchList(localSequencer);
		addConnectionEvent(connEvent);
	}
	
	public void processIncompleteBreakedWatchList() {
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent = new ConnectionEvent_ProcessIncompleteBreakedWatchList(localSequencer);
		addConnectionEvent(connEvent);
	}
	
	@Override
	public boolean serveContent(Content content) {
		ContentServingPolicy contentServingPolicy =
			getContentServingPolicy();
		if(!contentServingPolicy.canServe(content))
			return false;
		
//		if(!content.hasMetadata()) {
//			BrokerInternalTimer.inform("No meta data to serve: " + content);
//			return false;
//		}

		Sequence contentSequence = content.getSourceSequence();
//		if(!_beingServed.add(contentSequence) && _flowManager.getActiveFlowsCount(contentSequence) != 0) {
//			BrokerInternalTimer.inform("Serving: " + content);
//			return false;
//		}
		
		if(isSourceLocal(contentSequence)) {
			if(_serveQueueSet.add(content)) {
				_serveQueue.add(content);
			}
			return true;
		} else {
			actuallyServeContent(content);
			return true;
		}
	}
	
	protected boolean isSourceLocal(Sequence contentSequence) {
		return _localAddress.equals(contentSequence._address);
	}
	
	private void actuallyServeContent(Content content) {
		Sequence contentSequence = content.getSourceSequence();
		Long t = _beingServedTimer.get(contentSequence);
		if(t != null)
			return;

		t = SystemTime.currentTimeMillis();
		_beingServedTimer.put(contentSequence, t);
		
		_brokerShadow.getContentLogger().contentServed(contentSequence);
		int rows = content.getRows();
		int cols = content.getCols();
		_pendingPListRequests.add(contentSequence);
		sendPListRequest(contentSequence, rows, cols);
	}

	@Override
	public boolean isServingContent(Content content) {
		Long t = _beingServedTimer.get(content.getSourceSequence());
		return t != null && t != 0;
	}

	@Override
	public final void codedContentReady(
			PSCodedPiece psCodedPiece, InetSocketAddress remote) {
		ConnectionEvent_CodedContentReady connEvent =
			new ConnectionEvent_CodedContentReady(_localSequencer, psCodedPiece, remote);

		addConnectionEvent(connEvent);
	}
	
	protected final void handleConnectionEvent_CodedContentReady(ConnectionEvent_CodedContentReady connEvent) {
		PSCodedPiece psCodedPiece = connEvent._psCodedPiece;
		InetSocketAddress remote = connEvent._remote;
		
		Sequence sourceSequence = psCodedPiece.getSourceSequence();
		IFlow flow = _flowManager.getFlow(remote, sourceSequence);
//		_flowManager.outgoingFlowEncodingFinished(flow);
		
		if(flow == null) {
			_brokerShadow.getContentLogger().contentCodedPieceDiscarded(sourceSequence, remote, 1);
			return;
		}
		
		if(!flow.isActive()) {
			_brokerShadow.getContentLogger().contentCodedPieceDiscarded(sourceSequence, remote, 2);
			return;
		}

		if(!flow.checkRequestedPiecesToBeSent()) {
			flow.deferredSend();
			_brokerShadow.getContentLogger().contentCodedPieceDiscarded(sourceSequence, remote, 3);
			return;
		}
		
		Content content = _contentManager.getContent(sourceSequence);
		if(content.hasContent(remote))
			return;
		
		if(flow.getContent().isInversed())
			psCodedPiece.setFromMainSeed();
		TNetworkCoding_CodedPiece tCodedPiece =
			new TNetworkCoding_CodedPiece(
					_localAddress, null, sourceSequence,
					psCodedPiece.getCodedCoefficients(),
					psCodedPiece._codedContent,
					psCodedPiece._fromMainSeed);
		sendAsUDP(tCodedPiece, remote);
		boolean fromMainSeed = tCodedPiece._fromMainSeed;
		_brokerShadow.getContentLogger().contentCodedPieceSent(sourceSequence, remote, fromMainSeed);
	}

	@Override
	public void decodedContentReady(boolean success, IPSCodedBatch psDecodedBatch) {
//		if(!success)
//			return;
		
		ConnectionEvent_DecodedContentReady connEvent =
			new ConnectionEvent_DecodedContentReady(_localSequencer, success, psDecodedBatch);
		addConnectionEvent(connEvent);
	}
	
	protected void handleConnectionEvent_RequestNetworkCodingIdReq(
			ConnectionEvent_RequestNetworkCodingIdReq connEvent) {
		Content content = connEvent._content;
		Sequence contentSequence = content.getSourceSequence();
		if(!_pendingMetadataRequestsSet.contains(content))
			return;
		
		InetSocketAddress remote = content.getRemoteToRequestMetadataFrom();
		sendCodedPieceIdReqSendId(contentSequence, remote);
//		sendCodedPieceIdReqBreakSendId(contentSequence, remote);
		scheduleNextSendMetadataRequest(content);
	}
	
	public final void requestNetworkCodingIdReq(Content content) {
		if(content == null)
			throw new NullPointerException();
		
		if(!_pendingMetadataRequestsSet.add(content))
			return;
		
		sendMetadataRequest(content);
	}
	
	public void sendMetadataRequest(Content content) {
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent =
			new ConnectionEvent_RequestNetworkCodingIdReq(
					localSequencer, content);
		addConnectionEvent(connEvent);
	}
	
	protected final void handleConnectionEvent_DecodedContentReady(
			ConnectionEvent_DecodedContentReady connEvent) {
		IPSCodedBatch psDecodedBatch = connEvent._psDecodedBatch;
		Sequence sourceSequence = psDecodedBatch.getSourceSequence();
		Content content = _contentManager.getContent(sourceSequence);
		if(!content.decodeComplete()) {
			if(!connEvent._success)
				_brokerShadow.getContentLogger().contentFailed(sourceSequence);
			return;
		}
		
//		if(!content.decodeComplete()) // return if this is not the first time {
//			return;

		_brokerShadow.getContentLogger().contentDecoded(sourceSequence);
		
		if(SourcePSContent.verifyIntegrityDefaultSourcePSContent(content))
			BrokerInternalTimer.inform(
					"NO ERRORS!!!\t"
					+ _localAddress.getPort()
					+ "\t" + sourceSequence.toStringVeryVeryShort()
					+ "\t" + getPublishTime(psDecodedBatch) + " " + SystemTime.currentTimeMillis());
		else
			throw new IllegalStateException("Mismatch between source coded batch and decoded coded batch.");
		
		if(content.hasMetadata())
			content.loadDefaultSourcePSContent();
		else
			requestNetworkCodingIdReq(content);
		
		breakToBrokers(sourceSequence);
		breakToAllSenders(content);
		applyContentServingPolicy(content);
	}
	
	protected void breakToAllSenders(Content content) {
		List<InetSocketAddress> sendingRemotes = content.getSendingRemotesToAll(true);
		if(sendingRemotes == null)
			return;
		for(InetSocketAddress sendingRemote : sendingRemotes)
			sendCodedPieceIdReqBreak(content.getSourceSequence(), sendingRemote, -1);
	}
	
	protected void breakToBrokers(Sequence sourceSequence){
		InetSocketAddress homeBroker = getClientsJoiningBrokerAddress();
		sendCodedPieceIdReqBreak(sourceSequence, homeBroker, -1);

		_locallyBreakedContents.add(sourceSequence);
		Set<InetSocketAddress> plistReplyingBrokers =
			_contentManager.getModifiableReplyingBrokers(sourceSequence);
		if(plistReplyingBrokers != null) {
			for(InetSocketAddress plistReplyingBroker : plistReplyingBrokers)
				sendCodedPieceIdReqBreak(sourceSequence, plistReplyingBroker, -1);
			plistReplyingBrokers.clear();
		}
	}
	
//	protected void sendFullBreakToAll(Content content) {
//		Sequence sourceSequence = content.getSourceSequence();
//		List<InetSocketAddress> sendingRemotes =
//			content.getSendingRemotesToAll();
//		if(sendingRemotes != null) {
//			for(InetSocketAddress sendingRemote : sendingRemotes)
//				if(sendingRemote.equals(_localAddress))
//					throw new IllegalStateException();
//				else
//					sendCodedPieceIdReqBreak(sourceSequence, sendingRemote, 0);
//		}
//		
//		Set<InetSocketAddress> plistReplyingBrokers =
//			_contentManager.getReplyingBrokers(sourceSequence);
//		if(plistReplyingBrokers != null)
//			for(InetSocketAddress plistReplyingBroker : plistReplyingBrokers)
//				sendCodedPieceIdReqBreak(sourceSequence, plistReplyingBroker, 0);
//		
//		InetSocketAddress homeBroker = getClientsJoiningBrokerAddress();
//		sendCodedPieceIdReqBreak(sourceSequence, homeBroker, 0);
//	}
	
	@Override
	public void sendCodedPieceId(Content content, InetSocketAddress remote) {
		Publication publication = content.getPublication();
		Sequence sourceSequence = content.getSourceSequence();
		
		TNetworkCoding_CodedPieceId tCodedPieceId =
			new TNetworkCoding_CodedPieceId(_localAddress, sourceSequence,
					publication, content.getRows(), content.getCols());
		
		sendAsUDP(tCodedPieceId, remote);
	}

	protected void handleConnectionEvent_publishContent(ConnectionEvent_publishContent connEvent) {
		Content content = _contentManager.addContent(connEvent._psSourceCodedBatch);
		if(content == null)
			return;
		
		serveContent(content);
	}
	
	protected final void sendPListRequest(Sequence contentSequence, int rows, int cols) {
		boolean fromSource = contentSequence._address.equals(_localAddress);
		
		if(!checkSequencePListCounter(_sequencePListCounter, contentSequence, fromSource)) {
			return;
		}
		
		ConnectionEvent_SendPListRequest connEventSEndPListRequest =
			new ConnectionEvent_SendPListRequest(
					_localSequencer, contentSequence, fromSource, rows, cols);
		addConnectionEvent(connEventSEndPListRequest);
	}

	@Override
	public void startComponent() {
		super.startComponent();
		_contentManager.prepareToStart();
		_contentManager.startComponent();
	}
	
	@Override
	public void sendCodedPieceIdReqBreakDeclined(
			Sequence sourceSequence, InetSocketAddress remote, int outstandingPieces) {
		if(remote == null || sourceSequence == null)
			return;
		
		TNetworkCoding_CodedPieceIdReqBreakDeclined tBreak =
			new TNetworkCoding_CodedPieceIdReqBreakDeclined(
					_localAddress, sourceSequence, outstandingPieces);
		
		sendAsUDP(tBreak, remote);
		_brokerShadow.getContentLogger().contentBreakDeclineSent(sourceSequence, remote, outstandingPieces);
	}

	@Override
	public void sendCodedPieceIdReqBreak(
			Sequence sourceSequence, InetSocketAddress remote, int breakingSize) {
		if(remote == null || sourceSequence == null)
			return;
		
		TNetworkCoding_CodedPieceIdReqBreak tBreak =
			new TNetworkCoding_CodedPieceIdReqBreak(_localAddress, sourceSequence, breakingSize);
		
		sendAsUDP(tBreak, remote);
		_brokerShadow.getContentLogger().contentBreakRequested(sourceSequence, remote, breakingSize);
	}
	
	public void sendCodedPieceIdReqBreakSendId(Sequence sourceSequence, InetSocketAddress remote) {
		TNetworkCoding_CodedPieceIdReqBreakSendId tBreak =
			new TNetworkCoding_CodedPieceIdReqBreakSendId(_localAddress, sourceSequence);
		
		sendAsUDP(tBreak, remote);
	}
	
	public void sendCodedPieceIdReqSendId(Sequence sourceSequence, InetSocketAddress remote) {
		if(remote == null)
			return;
		
		TNetworkCoding_CodedPieceIdReqSendId tBreak =
			new TNetworkCoding_CodedPieceIdReqSendId(
					_localAddress, sourceSequence);
		
		sendAsUDP(tBreak, remote);
	}
	
	public void sendCodedPieceIdReqSendContentSendId(Sequence sourceSequence, InetSocketAddress remote) {
		TNetworkCoding_CodedPieceIdReqSendContentSendId tBreak =
			new TNetworkCoding_CodedPieceIdReqSendContentSendId(_localAddress, sourceSequence);
		sendAsUDP(tBreak, remote);
	}
	
	protected void handleConnectionEvent_ContentInversed(ConnectionEvent_ContentInversed connEvent) {
		Sequence sourceSequence = connEvent._contentSequence;
		Content content = _contentManager.getContent(sourceSequence);
//		sendCodedPieceIdReqBreak(sourceSequence, getClientsJoiningBrokerAddress(), 0);
		serveContent(content);
		maintainIncompleteWatchList(content);
	}

	protected void scheduleProcessNextFlowEvent() {
		scheduleTaskWithTimer(
				new TimerTask_ProcessNextFlow(this),
				5000, TIMER_DELAY_PROCESS_NEXT_FLOW);
	}
	
	protected void scheduleProcessServeQueue() {
		scheduleTaskWithTimer(
				new TimerTask_ProcessServeQueue(this),
				5000, TIMER_DELAY_PROCESS_SERVE_QUEUE);
	}
	
	protected void scheduleProcessIncompleteUnBreakedWatchList() {
		scheduleTaskWithTimer(
				new TimerTask_ProcessIncompleteUnbreakedWatchList(this),
				4000, TIMER_DELAY_PROCESS_INCOMPLETE_UNBREAKED_WATCH_LIST);
	}

	protected void scheduleProcessIncompleteBreakedWatchList() {
		scheduleTaskWithTimer(
				new TimerTask_ProcessIncompleteBreakedWatchList(this),
				5000, TIMER_DELAY_PROCESS_INCOMPLETE_BREAKED_WATCH_LIST);
	}

	@Override
	protected void connectionManagerJustStarted(){
		super.connectionManagerJustStarted();
//		scheduleProcessServeQueue();
//		scheduleProcessNextFlowEvent();
//		scheduleProcessIncompleteWatchList();
	}

	@Override
	public void publishContent(PSSourceCodedBatch psSourceCodedBatch) {
		ConnectionEvent connEvent = new ConnectionEvent_publishContent(_localSequencer, psSourceCodedBatch);
		addConnectionEvent(connEvent);
	}

	@Override
	protected void applyBreakPolicy(Content content, boolean contentRecentlyChanged) {
		Sequence sourceSequence = content.getSourceSequence();
		
		boolean isInversed = content.isInversed();
		if(!isInversed) {
			List<InetSocketAddress> breakRemotes =
				_contentBreakPolicy.sendBreak(content, contentRecentlyChanged);
			if(breakRemotes != null && breakRemotes.size() > 0) {
				_locallyBreakedContents.add(sourceSequence);
				for(InetSocketAddress remote : breakRemotes)
					if(remote.equals(_localAddress))
						continue;
					else
						sendCodedPieceIdReqBreak(sourceSequence, remote, 0);
			}
		}
	}
	
	@Override
	protected PListManager creatPListManager() {
		return null;
	}
	
	@Override
	public void sendPListReplyPublication(Publication plistReplyPublication) {
		throw new UnsupportedOperationException();	
	}
	
//	@Override
//	public SortedSet<SortableNodeAddress> sortedMatchingSet(Publication publication, boolean local) {
//		return null;
//	}
	
	@Override
	public void decodedContentInversed(Sequence sourceSequence) {
		ConnectionEvent_ContentInversed connEvent =
			new ConnectionEvent_ContentInversed(_localSequencer, sourceSequence);
		
		addConnectionEvent(connEvent);
	}

	protected InetSocketAddress getClientsJoiningBrokerAddress() {
		return _broker.getClientsJoiningBrokerAddress();
	}

	@Override
	protected void issueSpecialSubscriptions() {
		return;
	}
	
	public static String getPublishTime(IPSCodedBatch codedBatch) {
		Publication publication = codedBatch.getPublication();
		if(publication == null)
			return "NAN";
		
		List<String> publishTimeValList = publication.getStringPredicate("PUBLISH-TIME");
		if(publishTimeValList == null || publishTimeValList.isEmpty())
			return "NAN";
		else
			return publishTimeValList.get(0);
	}
	
//	protected boolean canServeSequence(Sequence sequence, boolean increase) {
//		SequenceCounter sequenceCounter = _sequenceServeCounter.get(sequence);
//		if(sequenceCounter == null) {
//			_sequenceServeCounter.put(sequence, new SequenceCounter(sequence));
//			return true;
//		} else {
//			return sequenceCounter.increaseAndCompareLess(Broker.MAX_SEREVE_SEQUENCE, increase);
//		}
//	}
}
