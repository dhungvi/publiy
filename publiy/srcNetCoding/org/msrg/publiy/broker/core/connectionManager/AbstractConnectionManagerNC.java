package org.msrg.publiy.broker.core.connectionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.contentManager.servingPolicy.ContentServingPolicy;
import org.msrg.publiy.broker.core.flowManager.FlowSelectionPolicy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPiece;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReq;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.multipath.loadweights.LoadWeightRepository;
import org.msrg.publiy.utils.SortableNodeAddress;
import org.msrg.publiy.utils.SortableNodeAddressSet;

import org.msrg.publiy.communication.core.niobinding.CommunicationTransportLogger;
import org.msrg.publiy.communication.core.niobinding.IConInfo;
import org.msrg.publiy.communication.core.niobinding.IConInfoListening;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.niobinding.UDPConInfoListening;
import org.msrg.publiy.communication.core.niobinding.UDPConInfoNonListening;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

public abstract class AbstractConnectionManagerNC extends ConnectionManagerMP implements IConnectionManagerNC {

	protected final Map<InetSocketAddress, UDPConInfoNonListening> _udpConnectionsMap =
		new HashMap<InetSocketAddress, UDPConInfoNonListening>();
	protected UDPConInfoListening _udpListeningSocket;
	protected final InetSocketAddress _localUDPAddress;
	
	protected final SortableNodeAddressSet _launchSortedNodeAddress =
		new SortableNodeAddressSet();
	protected final SortableNodeAddressSet _nonlaunchSortedNodeAddress =
		new SortableNodeAddressSet();
	
	protected AbstractConnectionManagerNC(
			String connectionManagerName,
			ConnectionManagerTypes type,
			InetSocketAddress localUDPAddress,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue,
			TRecovery_Join[] trjs,
			LoadWeightRepository loadWeightRepo) throws IOException{
		super(connectionManagerName, type, broker, brokerShadow, overlayManager, subscriptionManager, messageQueue, trjs, loadWeightRepo);
		
		_localUDPAddress = localUDPAddress;
	}
	
	public AbstractConnectionManagerNC(ConnectionManagerJoin conManJoin) {
		super(conManJoin);
		
		_localUDPAddress = _brokerShadow.getLocalUDPAddress();
	}

	public AbstractConnectionManagerNC(ConnectionManagerRecovery conManRecovery) {
		super(conManRecovery);
		
		_localUDPAddress = _brokerShadow.getLocalUDPAddress();
	}

	@Override
	protected void prepareListeningConnection() {
		super.prepareListeningConnection();
		
		if (_udpListeningSocket == null)
			_udpListeningSocket =
				_nioBinding.makeIncomingDatagramConnection(this, this, this, _localUDPAddress);
	}

	@Override
	protected boolean handleSpecialMessage(ISession session, IRawPacket raw){
		if(session != null)
			throw new IllegalStateException();
		
		switch(raw.getType()) {
		case TCODEDPIECE_ID:
		{
			TNetworkCoding_CodedPieceId tCodedPieceId =
				(TNetworkCoding_CodedPieceId) PacketFactory.unwrapObject(_brokerShadow, raw);
			handleTNetworkCoding_CodedPieceId(tCodedPieceId);
			return true;
		}
		
		case TCODEDPIECE:
		{
			TNetworkCoding_CodedPiece tCodedPiece =
				(TNetworkCoding_CodedPiece) PacketFactory.unwrapObject(_brokerShadow, raw);
			handleTNetworkCoding_CodedPiece(tCodedPiece);
			return true;
		}
		
		case TCODEDPIECE_ID_REQ:
		{
			TNetworkCoding_CodedPieceIdReq tcodedPiceIdReq =
				(TNetworkCoding_CodedPieceIdReq) PacketFactory.unwrapObject(_brokerShadow, raw);
			handleTNetworkCoding_CodedPieceIdReq(tcodedPiceIdReq);
			return true;
		}
		
		default:
			return super.handleSpecialMessage(session, raw);
		}
	}

	@Override
	public void becomeMyAcceptingListener(IConInfoListening<?> conInfo) {
		switch(conInfo.getChannelType()) {
		case TCP:
			super.becomeMyAcceptingListener(conInfo);
			return;
			
		case UDP:
			if ( !conInfo.getListeningAddress().equals(_localUDPAddress) )
				throw new IllegalStateException ("ConnectionManager::becomeMyAcceptingListener(.) - ERROR, this conInfoListening is not known");
			_udpListeningSocket = (UDPConInfoListening) conInfo;
			return;
		}
	}

	@Override
	public void conInfoUpdated(IConInfo<?> conInfo) {
		switch(conInfo.getChannelType()) {
		case TCP:
			super.conInfoUpdated(conInfo);
			return;
			
		case UDP:
			return;
		}
		
		IConInfoNonListening<?> conInfoNL = (IConInfoNonListening<?>) conInfo;
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent = new ConnectionEvent_updatedConnection(localSequencer, conInfoNL);
		addConnectionEvent(connEvent);
	}
	
	protected abstract void handleTNetworkCoding_CodedPiece(TNetworkCoding_CodedPiece psCodedPiece);
	protected abstract void handleTNetworkCoding_CodedPieceId(TNetworkCoding_CodedPieceId psCodedPieceId);
	protected abstract void handleTNetworkCoding_CodedPieceIdReq(TNetworkCoding_CodedPieceIdReq psCodedPieceIdReq);
	
	@Override
	public final void applyContentServingPolicy(Content content) {
		if(content == null)
			return;
		
		if(isServingContent(content))
			return;
		
		serveContent(content);
	}
	
	protected abstract void applyBreakPolicy(Content content, boolean contentRecentlyChanged);
	
	@Override
	public ContentServingPolicy getContentServingPolicy() {
		return _brokerShadow.getContentServingPolicy();
	}

	@Override
	public FlowSelectionPolicy getFlowSectionPolicy() {
		return FlowSelectionPolicy.FL_FAIR_ROUNDROBIN_ROUNDROBIN;
	}

	@Override
	public int getDefaultFlowPacketSize() {
		return _brokerShadow.getDefaultFlowPacketSize();
	}
	
	protected void sendAsUDP(IPacketable packet, InetSocketAddress remote) {
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, packet);
		if(raw == null)
			return;
		
		InetSocketAddress udpRemote = Broker.getUDPListeningSocket(remote);
		UDPConInfoNonListening udpConnection = getOrCreateUDPConnection(udpRemote);
		raw.setSender(_localAddress);
		raw.setReceiver(remote);
		
		raw = CommunicationTransportLogger.logOutgoing(raw);
		if(raw != null)
			_nioBinding.send(raw, udpConnection);
	}
	
	protected UDPConInfoNonListening getOrCreateUDPConnection(InetSocketAddress udpRemote) {
		UDPConInfoNonListening udpConnection = _udpConnectionsMap.get(udpRemote);
		if(udpConnection == null) {
			ISession session = ISession.createDummySession(_brokerShadow);
			udpConnection = _nioBinding.makeOutgoingDatagramConnection(session, this, this, this, udpRemote);
			_udpConnectionsMap.put(udpRemote, udpConnection);
		}
		
		return udpConnection;
	}

//	protected Map<SortableNodeAddress, SortableNodeAddress> _sortedMatchingRemotesSet =
//		new HashMap<SortableNodeAddress, SortableNodeAddress>();
	
	@Override
	public final SortableNodeAddressSet getSortableNonLaunchNodesSet() {
		return _nonlaunchSortedNodeAddress;
	}
	
	@Override
	public final SortableNodeAddressSet getSortableLaunchNodesSet() {
		return _launchSortedNodeAddress;
	}
	
	@Override
	public final SortedSet<SortableNodeAddress> convertToSortableLaunchNodeAddress(Set<InetSocketAddress> remotes) {
		return _launchSortedNodeAddress.convertToSortableSet(remotes);
	}
	
	@Override
	public final SortedSet<SortableNodeAddress> convertToSortableNonLaunchNodeAddress(Set<InetSocketAddress> remotes) {
		return _nonlaunchSortedNodeAddress.convertToSortableSet(remotes);
	}

	@Override
	public final void incrementLaunchSortableNodeAddress(Collection<InetSocketAddress> remotes, double val) {
		_launchSortedNodeAddress.incrementSortableNodeAddress(remotes, val);
	}
	
	@Override
	public final void incrementNonLaunchSortableNodeAddress(
			Collection<InetSocketAddress> remotes, double val) {
		_nonlaunchSortedNodeAddress.incrementSortableNodeAddress(remotes, val);
	}
	
	@Override
	public Set<InetSocketAddress> getMatchingSet(Publication publication, boolean local) {
		Set<InetSocketAddress> matchingRemotes = null;
		if(local)
			matchingRemotes = _subscriptionManager.getLocalMatchingSet(publication);
		else
			matchingRemotes = _subscriptionManager.getMatchingSet(publication);
		
		return matchingRemotes;
	}
	
//	@Override
//	public SortedSet<SortableNodeAddress> sortedMatchingSet(Publication publication, boolean local) {
//		Set<InetSocketAddress> matchingRemotes = null;
//		if(local)
//			matchingRemotes = _subscriptionManager.getLocalMatchingSet(publication);
//		else
//			matchingRemotes = _subscriptionManager.getMatchingSet(publication);
//		
//		return getSortedRemotesPrivately(matchingRemotes);
//	}
	
//	protected SortedSet<SortableNodeAddress> getSortedRemotesPrivately(
//			Set<InetSocketAddress> matchingRemotes) {
//		if(matchingRemotes == null)
//			return null;
//		
//		SortedSet<SortableNodeAddress> sortedMatchingRemotes =
//			new TreeSet<SortableNodeAddress>();
//		
//		for(InetSocketAddress remote: matchingRemotes) {
//			SortableNodeAddress sortableRemote =
//				SortableNodeAddress.incrementSortableNodeAddress(remote, 1);
//			
//			_sortedMatchingRemotesSet.put(sortableRemote, sortableRemote);
//			sortedMatchingRemotes.add(sortableRemote);
//		}
//		
//		if(matchingRemotes.size() != sortedMatchingRemotes.size())
//			throw new IllegalStateException(matchingRemotes + " vs. " + sortedMatchingRemotes);
//		
//		return sortedMatchingRemotes;
//
////		IWorkingSubscriptionManager wokringSubscriptionManager = getWorkingSubscriptionManager();
////		WorkingRemoteSet wRemoteSet = wokringSubscriptionManager.getMatchingWokringSet(publication);
////		return wRemoteSet.getMatchingRemotes();
//	}
}
