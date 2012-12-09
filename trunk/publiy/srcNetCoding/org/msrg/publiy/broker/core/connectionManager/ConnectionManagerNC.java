package org.msrg.publiy.broker.core.connectionManager;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.plistManager.IPListManager;
import org.msrg.publiy.broker.core.plistManager.PListManager;
import org.msrg.publiy.broker.core.plistManager.PListTypes;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPiece;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqBreak;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReply;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReq;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReq;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.multipath.loadweights.LoadWeightRepository;

import org.msrg.publiy.communication.core.niobinding.IConInfoListening;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.sessions.ISession;

public class ConnectionManagerNC extends AbstractConnectionManagerNC {

	protected final IPListManager _plistManager;
	
	protected ConnectionManagerNC(
			InetSocketAddress localUDPAddress,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue,
			TRecovery_Join[] trjs,
			LoadWeightRepository loadWeightRepo) throws IOException {
		this("ConMan-NC-BRK", ConnectionManagerTypes.CONNECTION_MANAGER_NC_BROKER, localUDPAddress, broker, brokerShadow, overlayManager, subscriptionManager, messageQueue, trjs, loadWeightRepo);
	}

	protected ConnectionManagerNC(
			String name,
			ConnectionManagerTypes type,
			InetSocketAddress localUDPAddress,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue,
			TRecovery_Join[] trjs,
			LoadWeightRepository loadWeightRepo) throws IOException {
		super(name, type, localUDPAddress, broker, brokerShadow, overlayManager, subscriptionManager, messageQueue, trjs, loadWeightRepo);
		
		_plistManager = creatPListManager();
	}
	
	protected PListManager creatPListManager() {
		return new PListManager(this);
	}
	
	public ConnectionManagerNC(ConnectionManagerRecovery conManRecovery) {
		super(conManRecovery);
		
		_plistManager = creatPListManager();
	}
	
	public ConnectionManagerNC(ConnectionManagerJoin conManJoin) {
		super(conManJoin);
		
		_plistManager = creatPListManager();
	}

	@Override
	protected void handleTNetworkCoding_CodedPiece(TNetworkCoding_CodedPiece psCodedPiece) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected void handleTNetworkCoding_CodedPieceIdReq(TNetworkCoding_CodedPieceIdReq psCodedPieceIdReq) {
		InetSocketAddress sender = psCodedPieceIdReq._sender;
		if(_localAddress.equals(sender))
			throw new IllegalStateException(sender.toString());
		
		switch(psCodedPieceIdReq._reqType) {
		case PLIST_REQ:
			TNetworkCoding_PListReq pListReq =
				(TNetworkCoding_PListReq) psCodedPieceIdReq;
			_plistManager.handlePListClientRequest(pListReq);
			break;
			
		case BREAK:
			TNetworkCoding_CodedPieceIdReqBreak tCodedPieceBreak =
				(TNetworkCoding_CodedPieceIdReqBreak) psCodedPieceIdReq;
			_plistManager.handlePListBreak(tCodedPieceBreak);
			break;
			
		default:
			throw new UnsupportedOperationException("Unknown type: " + psCodedPieceIdReq._reqType);
		}
	}
	

	protected void handleTNetworkCoding_PList(TNetworkCoding_PListReply pList) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void newIncomingConnection(IConInfoListening<?> conInfoL,
			IConInfoNonListening<?> newConInfoNL) {
		switch(conInfoL.getChannelType()) {
		case TCP:
			super.newIncomingConnection(conInfoL, newConInfoNL);
			return;
			
		case UDP:
		default:
			throw new IllegalStateException();
		}
	}

	@Override
	protected void handleConnectionUpdateEvent(IConInfoNonListening<?> conInfoNL) {
		switch(conInfoNL.getChannelType()) {
		case TCP:
			super.handleConnectionUpdateEvent(conInfoNL);
			return;
			
		case UDP:
			new Exception("WTF: " + conInfoNL);
			break;
			
		default:
			throw new IllegalStateException();
		}
	}
	
	@Override
	protected void handleMulticastMessage(ISession session, TMulticast tm) {
		switch(tm.getType()) {
		case T_MULTICAST_PUBLICATION_NC:
		case T_MULTICAST_PUBLICATION_MP:
		case T_MULTICAST_PUBLICATION:
			handleTMulticastPublishMPMessage(session, (TMulticast_Publish_MP) tm);
			
		default:
			super.handleMulticastMessage(session, tm);
		}
	}

	protected void handleTMulticastPublishMPMessage(ISession session, TMulticast_Publish_MP tm) {
		Publication pub = tm.getPublication();
		PListTypes plistType = PListManager.getPListType(pub);
		if(plistType == null)
			return;
		
		switch(plistType) {
		case PLIST_REPLY:
			_plistManager.handlePListBrokerReply(pub);
			break;
			
		case PLIST_REQ:
			_plistManager.handlePListBrokerRequest(pub);
			break;
			
		default:
			throw new UnsupportedOperationException("Unknown PListType: " + plistType);
		}
	}
	
	@Override
	public InetSocketAddress getUDPLocalAddress() {
		return _localUDPAddress;
	}

	@Override
	public void addContentFlows(Content content) {
		throw new UnsupportedOperationException();
	}
	

	@Override
	public void publishContent(PSSourceCodedBatch psSourceCodedBatch) {
		throw new UnsupportedOperationException();
	}
	
	protected void processNextFlowEvent() {
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent = new ConnectionEvent_ProcessNextFlow(localSequencer);
		addConnectionEvent(connEvent);
	}

	@Override
	protected void applyBreakPolicy(Content content, boolean contentRecentlyChanged) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected void checkBWAvailability() {
		return;
	}
	
	@Override
	public String getThreadName() {
		return "ConMan-NC";
	}

	@Override
	public void sendPListReplyPublication(Publication plistReplyPublication) {
		TMulticast_Publish_MP tmp =
			new TMulticast_Publish_MP(plistReplyPublication, _localAddress, 0, (byte)0, getPublicationForwardingStrategy(), _localSequencer.getNext());
		sendTMulticast(tmp, null);
	}

	@Override
	protected void handleTNetworkCoding_CodedPieceId(TNetworkCoding_CodedPieceId psCodedPieceId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isServingContent(Content content) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendCodedPieceId(Content content, InetSocketAddress remote) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void sendCodedPieceIdReqBreakDeclined(
			Sequence sourceSequence, InetSocketAddress requester, int outstandingPieces) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void sendCodedPieceIdReqBreak(
			Sequence sourceSequence, InetSocketAddress remote, int breakingSize) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean serveContent(Content content) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}

	@Override
	public void sendPListReply(
			TNetworkCoding_PListReply pList, InetSocketAddress requester) {
		sendAsUDP(pList, requester);
	}

	@Override
	public void sendTMPListRequest(TMulticast_Publish_MP tmp) {
		sendTMulticast(tmp, null);
	}

	@Override
	protected void handleConnectionEvent_loadPrepareSubscriptionsFile(
			ConnectionEvent_loadPrepareSubscriptionsFile connEvent) {
		issueSpecialSubscriptions();
		super.handleConnectionEvent_loadPrepareSubscriptionsFile(connEvent);
	}
	
	protected void issueSpecialSubscriptions() {
		Subscription sub = PListManager.getPListReplySubscription(_localAddress);
		issueSubscription(sub, _plistManager);
	}
}
