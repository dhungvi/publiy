package org.msrg.publiy.broker.core.connectionManager;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueue;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPiece;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReq;
import org.msrg.publiy.utils.SortableNodeAddressSet;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_A_Listener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.niobinding.CommunicationTransportLogger;
import org.msrg.publiy.communication.core.niobinding.INIOBinding;
import org.msrg.publiy.communication.core.niobinding.NIOBinding;
import org.msrg.publiy.communication.core.niobinding.NIOBindingFactory;
import org.msrg.publiy.communication.core.niobinding.NIOBindingImp_SelectorBugWorkaround;
import org.msrg.publiy.communication.core.niobinding.UDPChannelInfoListening;
import org.msrg.publiy.communication.core.niobinding.UDPConInfoNonListening;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.contentManager.IContentManager;
import org.msrg.publiy.broker.core.contentManager.SourcePSContent;
import org.msrg.publiy.broker.core.contentManager.servingPolicy.ContentServingPolicy;
import org.msrg.publiy.broker.core.contentManager.servingPolicy.ContentServingPolicy_ServeHalfReceived;
import org.msrg.publiy.broker.core.plistManager.IPListManager;
import org.msrg.publiy.broker.core.plistManager.PList;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.IPSCodedBatch;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;
import org.msrg.publiy.broker.networkcoding.connectionManager.client.ConnectionManagerNC_Client;
import junit.framework.TestCase;

public class ConnectionManagerNCTest extends TestCase {

	final int delta = 3;
	final double _packetLoss = 0.0;
	final boolean USE_REAL_NIO = false;
	
	final int _startPort = 8800;
	final int BROKERS_COUNT = new Integer(System.getProperty("NC.BROKERS", "2")).intValue();
	final int _rows = new Integer(System.getProperty("NC.ROWS", "100")).intValue();
	final int _cols = new Integer(System.getProperty("NC.COLS", "10000")).intValue();
	
	final InetSocketAddress _homeBrokerAddress =
		Sequence.getIncrementalLocalAddresses(_startPort, _startPort, 2)[0];
	final InetSocketAddress _sourceAddress =
		Sequence.getIncrementalLocalAddresses(_startPort+2, _startPort+2, 2)[0];
	final InetSocketAddress[] _localAddresses =
		Sequence.getIncrementalLocalAddresses(_startPort+4, _startPort+4+2*BROKERS_COUNT-1, 2);
	final Publication _publication = new Publication().addPredicate("attr1", 50);
	final Sequence _contentSequence = Sequence.getRandomSequence(_sourceAddress);
	final SourcePSContent _sourcePSContent =
		SourcePSContent.createDefaultSourcePSContent(_contentSequence, _publication, _rows, _cols);
	ConnectionManagerNC_ForTest _conManNC_homeBroker;
	final Map<InetSocketAddress, ConnectionManagerNC> _connectionManagerNCs =
		new HashMap<InetSocketAddress, ConnectionManagerNC>();
	final ContentServingPolicy _contentServingPolicy = new ContentServingPolicy_ServeHalfReceived(0.3);
//	final ContentServingPolicy _contentServingPolicy = new ContentServingPolicy_ServeFullReceived();
	CommunicationDriver _commDriver;
	final Set<InetSocketAddress> _defaultMatchingSet = new HashSet<InetSocketAddress>();
	boolean _end = false;
	final int _WAIT_TO_END_SEC = 3600;
	final Map<Sequence, Set<InetSocketAddress>> _expectedDeliveries =
		new HashMap<Sequence, Set<InetSocketAddress>>();
	int _received = 0;
	
	@Override
	public void setUp() throws IOException {
		LocalSequencer.init(null, _sourceAddress);
		
		LoggerFactory.modifyLogger(
				null, LoggingSource.values(),
				null, LoggingSource.values(),
				null, LoggingSource.values(),
				null, LoggingSource.values(),
				null, LoggingSource.values());
		BrokerInternalTimer.start();
		if(USE_REAL_NIO)
			_commDriver = null;
		else
			_commDriver = new CommunicationDriver(null, this, _packetLoss);

		if(!USE_REAL_NIO)
			for(int i=0 ; i<BROKERS_COUNT ; i++)
				new Thread(_commDriver).start();

		{
			IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _homeBrokerAddress).setNC(true).setDelta(delta);
			IOverlayManager overlayManager = brokerShadow.createOverlayManager();
			ISubscriptionManager subscriptionManager = brokerShadow.createSubscriptionManager(overlayManager, null, false);
			IMessageQueue messageQueue = new MessageQueue(brokerShadow, null, overlayManager, subscriptionManager, true);

			_conManNC_homeBroker =
				new ConnectionManagerNC_ForTest(
						this,
						_contentServingPolicy,
						Broker.getUDPListeningSocket(_homeBrokerAddress), null,
						brokerShadow,
						overlayManager,
						subscriptionManager,
						messageQueue);
			_conManNC_homeBroker.prepareToStart();
			_conManNC_homeBroker.startComponent();
			_connectionManagerNCs.put(_homeBrokerAddress, _conManNC_homeBroker);
		}
		
		{
			IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _sourceAddress).setNC(true).setDelta(delta);
			IOverlayManager overlayManager = brokerShadow.createOverlayManager();
			ISubscriptionManager subscriptionManager = brokerShadow.createSubscriptionManager(overlayManager, null, false);
			IMessageQueue messageQueue = new MessageQueue(brokerShadow, null, overlayManager, subscriptionManager, true);

			ConnectionManagerNC_Client_ForTest conManNC =
				new ConnectionManagerNC_Client_ForTest(
						this,
						_contentServingPolicy,
						Broker.getUDPListeningSocket(_sourceAddress),
						null,
						brokerShadow,
						overlayManager,
						subscriptionManager,
						messageQueue);
			conManNC.prepareToStart();
			conManNC.startComponent();
			_connectionManagerNCs.put(_sourceAddress, conManNC);
			_clientsJoiningBrokerAddressMap.put(_sourceAddress, _homeBrokerAddress);
		}
		
		SortableNodeAddressSet _sortableNodesSet = new SortableNodeAddressSet();
		
		for(InetSocketAddress localAddress : _localAddresses) {
			IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setNC(true).setDelta(delta);
			IOverlayManager overlayManager = brokerShadow.createOverlayManager();
			ISubscriptionManager subscriptionManager = brokerShadow.createSubscriptionManager(overlayManager, null, false);
			IMessageQueue messageQueue = new MessageQueue(brokerShadow, null, overlayManager, subscriptionManager, true);
			
			ConnectionManagerNC_Client_ForTest conManNC_Client =
				new ConnectionManagerNC_Client_ForTest(
					this,
					_contentServingPolicy,
					Broker.getUDPListeningSocket(localAddress),
					null, brokerShadow, overlayManager, subscriptionManager, messageQueue);
			conManNC_Client.prepareToStart();
			conManNC_Client.startComponent();
			_connectionManagerNCs.put(localAddress, conManNC_Client);
			_clientsJoiningBrokerAddressMap.put(localAddress, _homeBrokerAddress);
		}
		
		for(InetSocketAddress localAddress : _localAddresses)
			_defaultMatchingSet.add(_sortableNodesSet.getSortableNodeAddress(localAddress));
		
		synchronized(_expectedDeliveries){
			_expectedDeliveries.put(_contentSequence, new HashSet<InetSocketAddress>(_defaultMatchingSet));
		}
		
		BrokerInternalTimer.inform("PARAMS: " + _contentServingPolicy);
	}
	
	InetSocketAddress _localUDPAddress = Sequence.getRandomLocalAddress();
	private Map<InetSocketAddress, InetSocketAddress> _clientsJoiningBrokerAddressMap =
		new HashMap<InetSocketAddress, InetSocketAddress>();
	
	public void test() {
		_connectionManagerNCs.get(_sourceAddress).publishContent(
				(PSSourceCodedBatch)_sourcePSContent.getPSCodedBatch());
		
		waitToEnd(_WAIT_TO_END_SEC);
		System.out.println("Yay! ");
	}

	protected String getContentManagersStr() {
		StringWriter writer = new StringWriter();
		for(IConnectionManagerNC conManNC : _connectionManagerNCs.values()) {
			if(ConnectionManagerNC_Client_ForTest.class.isAssignableFrom(conManNC.getClass()))
				writer.append(((ConnectionManagerNC_Client_ForTest)conManNC).getContentManager().toString());
			else if(ConnectionManagerNC_ForTest.class.isAssignableFrom(conManNC.getClass()))
				writer.append(((ConnectionManagerNC_ForTest)conManNC).getPListManager().toString());
			else
				throw new UnsupportedOperationException("Unknown type: " + conManNC);
			
			writer.append('\t');
		}
		return writer.toString();
	}
	
	protected void waitToEnd(int waitSec) {
		for(int i=0 ; i<waitSec * 10 ; i++)
		{
			if(i % 100 == 0)
			{
				BrokerInternalTimer.inform("Waiting... " + _received);
				BrokerInternalTimer.inform("Total communication: " + CommunicationTransportLogger.toStringStatic());
				BrokerInternalTimer.inform("Content managers: " + getContentManagersStr());
			} else if(i % 20 == 0) {
				BrokerInternalTimer.inform("Waiting... " + _received);
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException itx) {itx.printStackTrace();}
			
			if(_end)
				break;
		}

		if(_end) {
			try { Thread.sleep(15000); } catch (InterruptedException itx) {itx.printStackTrace();}

			PList plist = _conManNC_homeBroker.getPListManager().getPList(_contentSequence);
			assertTrue(plist.getLocalClientsSize() == 0);
			assertTrue(plist.getCrossClientsSize() == 0);
			
			BrokerInternalTimer.inform("OK!");
		} else
			fail();
		
		BrokerInternalTimer.inform("Total BW_OUT: " + Broker.BW_OUT);
		BrokerInternalTimer.inform("Total Rows: " + _rows + " x Cols: " + _cols);
		BrokerInternalTimer.inform("Total Brokers: " + BROKERS_COUNT);

		if(!USE_REAL_NIO) {
			BrokerInternalTimer.inform("Total communication: " + _commDriver);
			BrokerInternalTimer.inform("Total bytes sent: " + _commDriver._totalBytesSent);
			BrokerInternalTimer.inform("Total bytes lost: " + _commDriver._totalBytesLost + " (" + _packetLoss + "%)");
			BrokerInternalTimer.inform("Total bytes delivered: " + _commDriver._totalBytesDelivered);
		} else {
			BrokerInternalTimer.inform("Total communication: " + CommunicationTransportLogger.toStringStatic());
		}
	}
	
	public void deliver(IRawPacket raw, InetSocketAddress receiver) {
		ConnectionManagerNC conManNC = _connectionManagerNCs.get(receiver);
		conManNC.processMessage(null, raw);
	}
	
	public Set<InetSocketAddress> getDefaultMatchingSet() {
		return _defaultMatchingSet;
	}

	public void contentDelivered(InetSocketAddress localAddress, Sequence sourceSequence) {
		synchronized(_expectedDeliveries) {
			Set<InetSocketAddress> expectedReceivers = _expectedDeliveries.get(sourceSequence);
			if(expectedReceivers == null)
				return;
			if(expectedReceivers.remove(localAddress))
				_received++;
			if(expectedReceivers.size() == 1)
				_expectedDeliveries.remove(sourceSequence);
			
			if(_expectedDeliveries.size() == 0)
				_end = true;
		}
	}

	public InetSocketAddress getClientsJoiningBrokerAddress(InetSocketAddress localAddress) {
		return _clientsJoiningBrokerAddressMap.get(localAddress);
	}
}


class ConnectionManagerNC_ForTest extends ConnectionManagerNC {
	protected final ConnectionManagerNCTest _tester;
	protected final ContentServingPolicy _contentServingPolicy;
	protected final BrokerShadow _brokerShadow;

	protected ConnectionManagerNC_ForTest(
			ConnectionManagerNCTest tester,
			ContentServingPolicy contentServingPolicy,
			InetSocketAddress localUDPAddress,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue)
			throws IOException {
		super(localUDPAddress, broker, brokerShadow, overlayManager, subscriptionManager, messageQueue, null, null);
		
		_brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, brokerShadow.getLocalAddress()).setDelta(brokerShadow.getDelta()).setNC(true).setLocalUDPAddress(localUDPAddress);
		_contentServingPolicy = contentServingPolicy;
		_tester = tester;
	}
	
	@Override
	protected void handleConnectionEvent_NewLocalOutgoing(ConnectionEvent_NewLocalOutgoing connEvent){
		return;
	}
	
	public IPListManager getPListManager() {
		return _plistManager;
	}
	
	@Override
	protected INIOBinding createNIOBinding(IBrokerShadow brokerShadow) throws IOException {
		InetSocketAddress ccInetAddress = brokerShadow.getLocalAddress();
		if ( ccInetAddress == null )
			return null;
		
		INIOBinding nioBinding = new NIOBinding_ForTest(brokerShadow);
		NIOBindingFactory.registerNIOBinding(nioBinding, brokerShadow);
		return nioBinding;
	}
	
	@Override
	public ContentServingPolicy getContentServingPolicy() {
		return _contentServingPolicy;
	}

	@Override
	public Set<InetSocketAddress> getMatchingSet(Publication publication, boolean local) {
		return _tester.getDefaultMatchingSet();
	}
	
	@Override
	protected void connectionManagerJustStarted(){
	}
	
	@Override
	protected IMessageQueue createMessageQueue(IBrokerShadow brokerShadow, IOverlayManager overlayManager, ISubscriptionManager subscriptionManager, boolean allowToConfirm){
		return null;
	}
	
	@Override
	protected UDPConInfoNonListening getOrCreateUDPConnection(InetSocketAddress udpRemote) {
		return super.getOrCreateUDPConnection(udpRemote);
	}
	
	@Override
	public InOutBWEnforcer getBWEnforcer() {
		return null;
	}
	
	@Override
	protected void sendAsUDP(IPacketable packet, InetSocketAddress remote) {
		if(_tester.USE_REAL_NIO) {
			super.sendAsUDP(packet, remote);
		} else {
			InetSocketAddress udpRemote = Broker.getUDPListeningSocket(remote);
			InetSocketAddress sender = getLocalAddress();
			IRawPacket raw = PacketFactory.wrapObject(_localSequencer, packet);
			raw.setReceiver(udpRemote);
			raw.setSender(sender);
			_tester._commDriver.send(raw);
		}
	}
	
	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}
}

class ConnectionManagerNC_Client_ForTest extends ConnectionManagerNC_Client {
	
	protected final ConnectionManagerNCTest _tester;
	protected final ContentServingPolicy _contentServingPolicy;
	protected final BrokerShadow _brokerShadow;
	
	protected ConnectionManagerNC_Client_ForTest(
			ConnectionManagerNCTest tester,
			ContentServingPolicy contentServingPolicy,
			InetSocketAddress localUDPAddress,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager,
			IMessageQueue messageQueue)
			throws IOException {
		super(localUDPAddress, broker, brokerShadow, overlayManager, subscriptionManager, messageQueue, null, null);
		
		_brokerShadow = new BrokerShadow(NodeTypes.NODE_NC_PUBLISHER, brokerShadow.getLocalAddress()).setDelta(brokerShadow.getDelta()).setNC(true).setLocalUDPAddress(localUDPAddress);
		_contentServingPolicy = contentServingPolicy;
		_tester = tester;
	}
	
	public IContentManager getContentManager() {
		return _contentManager;
	}
	
	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}

	@Override
	protected void handleTNetworkCoding_CodedPiece(TNetworkCoding_CodedPiece psCodedPiece) {
		BrokerInternalTimer.inform("Processing " + _localAddress.getPort() + ": " + psCodedPiece);
		super.handleTNetworkCoding_CodedPiece(psCodedPiece);
	}
	
	@Override
	protected void handleTNetworkCoding_CodedPieceId(TNetworkCoding_CodedPieceId psCodedPieceId) {
		BrokerInternalTimer.inform("Processing " + _localAddress.getPort() + ": " + psCodedPieceId);
		super.handleTNetworkCoding_CodedPieceId(psCodedPieceId);
	}
	
	@Override
	protected void handleTNetworkCoding_CodedPieceIdReq(TNetworkCoding_CodedPieceIdReq psCodedPieceIdReq) {
		BrokerInternalTimer.inform("Processing " + _localAddress.getPort() + ": " + psCodedPieceIdReq);
		super.handleTNetworkCoding_CodedPieceIdReq(psCodedPieceIdReq);
	}
	
	@Override
	public boolean serveContent(Content content) {
		boolean result = super.serveContent(content);
		if(result)
			BrokerInternalTimer.inform("CONTENT_BEING_ADDED @" + _localAddress.getPort() + ": " + content.getAvailableCodedPieceCount());
		
		return result;
	}
	
	@Override
	protected INIOBinding createNIOBinding(IBrokerShadow brokerShadow) throws IOException {
		InetSocketAddress ccInetAddress = brokerShadow.getLocalAddress();
		if ( ccInetAddress == null )
			return null;
		
		INIOBinding nioBinding = new NIOBinding_ForTest(brokerShadow);
		NIOBindingFactory.registerNIOBinding(nioBinding, brokerShadow);
		return nioBinding;
	}
	
	@Override
	public void decodedContentInversed(Sequence sourceSequence) {
		super.decodedContentInversed(sourceSequence);
	}

	@Override
	public void decodedContentReady(boolean success, IPSCodedBatch psDecodedBatch) {
		BrokerInternalTimer.inform("decodedContentReady @" + _localAddress.getPort() + ": " + success + "[" + psDecodedBatch.getSourceSequence() + "]");
		Sequence sourceSequence = psDecodedBatch.getSourceSequence();

		super.decodedContentReady(success, psDecodedBatch);
		
		if(!success)
			return;

		_tester.contentDelivered(_localAddress, sourceSequence);
	}
	
	@Override
	protected boolean tryContentDecode(Content content) {
		boolean result = super.tryContentDecode(content);
		BrokerInternalTimer.inform("Trying to decode content @" + _localAddress.getPort() + ": " + result);
		
		return result;
	}
	
	@Override
	public ContentServingPolicy getContentServingPolicy() {
		return _contentServingPolicy;
	}

	@Override
	public Set<InetSocketAddress> getMatchingSet(Publication publication, boolean local) {
		return null;
	}
	
	@Override
	protected void connectionManagerJustStarted(){
		scheduleProcessServeQueue();
		scheduleProcessNextFlowEvent();
		scheduleProcessIncompleteBreakedWatchList();
		scheduleProcessIncompleteUnBreakedWatchList();
	}
	
	@Override
	public int getDefaultFlowPacketSize() {
		return _brokerShadow.getDefaultFlowPacketSize();
	}
	
	@Override
	protected IMessageQueue createMessageQueue(IBrokerShadow brokerShadow, IOverlayManager overlayManager, ISubscriptionManager subscriptionManager, boolean allowToConfirm){
		return null;
	}
	
	@Override
	protected UDPConInfoNonListening getOrCreateUDPConnection(InetSocketAddress udpRemote) {
		return super.getOrCreateUDPConnection(udpRemote);
	}
	
	@Override
	public InOutBWEnforcer getBWEnforcer() {
		return null;
	}
	
	@Override
	protected void sendAsUDP(IPacketable packet, InetSocketAddress remote) {
		if(_tester.USE_REAL_NIO) {
			super.sendAsUDP(packet, remote);
		} else {
			InetSocketAddress udpRemote = Broker.getUDPListeningSocket(remote);
			InetSocketAddress sender = getLocalAddress();
			IRawPacket raw = PacketFactory.wrapObject(_localSequencer, packet);
			raw.setReceiver(udpRemote);
			raw.setSender(sender);
			_tester._commDriver.send(raw);
		}
	}
	
	@Override
	protected void handleConnectionEvent_processIncompleteBreakedWatchList() {
//		BrokerInternalTimer.inform("handleConnectionEvent_processIncompleteWatchList: " + _localAddress.getPort());
		super.handleConnectionEvent_processIncompleteBreakedWatchList();
	}
	
	@Override
	protected InetSocketAddress getClientsJoiningBrokerAddress() {
		return _tester.getClientsJoiningBrokerAddress(_localAddress);
	}
}


class CommunicationDriver implements Runnable {

	final double _packetLoss;
	final ConnectionManagerNCTest _tester;
	final List<IRawPacket> _rawPackets = new LinkedList<IRawPacket>();
	final Random _rand = new Random();
	final Object _LOCK = new Object();
	int droppedPackets = 0;
	int _totalBytesSent = 0;
	int _totalBytesLost = 0;
	int _totalBytesDelivered = 0;
	final IBrokerShadow _brokerShadow;
	
	CommunicationDriver(IBrokerShadow brokerShadow, ConnectionManagerNCTest tester, double packetLoss) {
		_tester = tester;
		_packetLoss = packetLoss;
		_brokerShadow = brokerShadow;
	}
	
	public void send(IRawPacket raw) {
		synchronized(_LOCK) {
			_totalBytesSent += raw.getBody().capacity();
			if(_rand.nextDouble() <= _packetLoss) {
				_totalBytesLost += raw.getBody().capacity();
				return;
			}
			
			switch(raw.getType()) {
			case TCODEDPIECE_ID_REQ: 
				_rawPackets.add(raw);
				break;
				
			case TCODEDPIECE:
				CommunicationTransportLogger.logOutgoing(raw);
				//Continue to next case.
			case TCODEDPIECE_ID:
				_rawPackets.add(raw);
				break;
				
			default:
				throw new UnsupportedOperationException();
			}
			
			_LOCK.notifyAll();
		}
	}

	@Override
	public String toString() {
		return CommunicationTransportLogger.toStringStatic();
	}
	
	@Override
	public void run() {
		while(true) {
			IRawPacket raw = null;
			InetSocketAddress receiver = null;
			
			synchronized(_LOCK) {
				while(_rawPackets.isEmpty())
					try {
						_LOCK.wait(1);
					} catch (InterruptedException e) { e.printStackTrace(); }
				raw = _rawPackets.remove(0);
				
				_totalBytesDelivered += raw.getBody().capacity();
				receiver = Broker.getTCPListeningSocket(raw.getReceiver());
				
				switch(raw.getType()) {
				case TCODEDPIECE:
					CommunicationTransportLogger.logIncoming(_brokerShadow, raw);
					break;
				
				default:
					break;
				}
			}
			
			_tester.deliver(raw, receiver);
		}
	}
}

class NIOBinding_ForTest extends NIOBindingImp_SelectorBugWorkaround {

	protected NIOBinding_ForTest(IBrokerShadow brokerShadow) throws IOException {
		super(brokerShadow);
	}
	
//	@Override
//	public boolean send(IRawPacket raw, IConInfoNonListening<?> conInfo) throws IllegalArgumentException {
//		CommunicationTransportLogger.logOutgoing(raw);
//		return super.send(raw, conInfo);
//	}
	
	@Override
	protected UDPChannelInfoListening createUDPChannelInfoListening(
			DatagramChannel listeningChannel, INIO_A_Listener aListener,
			INIO_R_Listener dRListener, INIO_W_Listener dWListener,
			InetSocketAddress localListeningAddress) {
		return new UDPChannelInfoListening_ForTest(
				this, listeningChannel, aListener, dRListener, dWListener, localListeningAddress);
	}
	
	@Override
	public UDPConInfoNonListening makeOutgoingDatagramConnection(
			ISession session, INIOListener listener, INIO_R_Listener listener2,
			INIO_W_Listener wListener, InetSocketAddress remoteListeningAddress) {
		BrokerInternalTimer.inform(
				"MakingOutgoingDataGramConnection:" + 
				" FROM: " + _ccSockAddress.getPort() + 
				" TO: " + Broker.getTCPListeningSocket(remoteListeningAddress).getPort());
		
		return super.makeOutgoingDatagramConnection(
				session, listener, listener2, wListener, remoteListeningAddress);
	}

}

class UDPChannelInfoListening_ForTest extends UDPChannelInfoListening {
	UDPChannelInfoListening_ForTest(NIOBinding nioBinding, DatagramChannel ch,
			INIO_A_Listener aListener, INIO_R_Listener dRListener,
			INIO_W_Listener dWListener, InetSocketAddress listeningAddress) {
		super(nioBinding, ch, aListener, dRListener, dWListener, listeningAddress);
	}

//	@Override
//	public IRawPacket getNextIncomingData() {
//		IRawPacket raw = super.getNextIncomingData();
//		if(raw == null)
//			return null;
//
//		raw.setReceiver(_nioBinding.getLocalAddress());
//		CommunicationTransportLogger.logIncoming(raw);
//		return raw;
//	}
}