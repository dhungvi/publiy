package org.msrg.publiy.communication.core.niobinding;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_A_Listener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.component.ComponentStatus;
import org.msrg.publiy.component.IComponentListener;

public class NIOBindingImp extends NIOBinding {
	
	private List<NIOBindingEvent> _nioBindingEvents = new LinkedList<NIOBindingEvent>();
	
	private ComponentStatus _status = ComponentStatus.COMPONENT_STATUS_UNINITIALIZED;
	private final Object _componentStatusChangeWaitObject = new Object();
	private final Set<IComponentListener> _componentListeners = new HashSet<IComponentListener>();
	
	private int _connectingCount = 0;
	private int _readingCount = 0;
	private int _writingCount = 0;
	private int _acceptingCount = 0;
	
	private final LocalSequencer _localSequencer;
	
	private Set<ConInfoListening<?>> _listeningConnections = new HashSet<ConInfoListening<?>>();
	private Set<ConInfoNonListening<?>> _nonlisteningConnections = new HashSet<ConInfoNonListening<?>>();

	/*
	 * If we cannot Selector.open(), then we cannot really do anything -> throw exception!
	 */
	NIOBindingImp(IBrokerShadow brokerShadow) throws IOException{
		super(brokerShadow, brokerShadow.getLocalAddress());
		_localSequencer = _brokerShadow.getLocalSequencer();
		if(_localSequencer == null)
			throw new NullPointerException("LocalSequencer cannot be null.");
	}
	
	public InetSocketAddress getCCSockAddress() {
		return _ccSockAddress;
	}
	
	private void addNIOBindingEvent(NIOBindingEvent event) {
		if(Broker.DEBUG) 
			LoggerFactory.getLogger().debug(this, "Adding event: " + event);
		synchronized (_nioBindingEvents) {
			_nioBindingEvents.add(event);
			_nioBindingEvents.notify();
		}
//		_selector.wakeup();
	}

	private void addNIOBindingEventHead(NIOBindingEvent event) {
		if(Broker.DEBUG) 
			LoggerFactory.getLogger().debug(this, "Adding event (head): " + event);
		synchronized (_nioBindingEvents) {
			_nioBindingEvents.add(0, event);
			_nioBindingEvents.notify();
		}
//		_selector.wakeup();
	}

	private void handleNIOBindingEvent(NIOBindingEvent event) {
		if(Broker.DEBUG) 
			LoggerFactory.getLogger().debug(this, "Handling event: " + event);
		
		switch (event._type) {
		case NIO_SEND:
			handleNIOBindingEvent_sendPacket((NIOBindingEvent_sendPacket)event);
			break;
			
		case NIO_RENEW:
			handleNIOBindingEvent_renewConnection((NIOBindingEvent_renewConnection)event);
			break;
			
		case NIO_MAKE_OUTGOING:
			handleNIOBindingEvent_makeOutgoing((NIOBindingEvent_makeOutgoing)event);
			break;
			
		case NIO_DESTROY:
			handleNIOBindingEvent_destroyConnection((NIOBindingEvent_destroyConnection)event);
			break;
			
		case NIO_KILL_ALL:
			killAllConnections();
			break;
			
		case NIO_MAKE_INCOMING:
			handleNIOBindingEvent_makeIncoming((NIOBindingEvent_makeIncoming)event);
			break;
			
		case NIO_STATE_CHANGED:
			handleNIOBindingEvent_stateChanged((NIOBindingEvent_stateChanged)event);
			break;
			
		case NIO_SUMMARIZE_CHANNELS:
			handleNIOBindingEvent_summarizeChannels((NIOBindingEvent_summarizeChannels)event);
			break;
			
		case NIO_REGISTER_UDP_CONINFO_NONLISTENING:
			handleNIOBindingEvent_RegisterUDPConInfoNonListening(
					(NIOBindingEvent_registerUDPConInfoNonListening)event);
			break;
			
		default:
			throw new UnsupportedOperationException("Unknown NIOBindingEvent: " + event);	
		}
	}
	
	private void handleNIOBindingEvent_summarizeChannels(NIOBindingEvent_summarizeChannels event) {
		System.out.println(summarizeChannels());
	}
	
	private void handleNIOBindingEvent_stateChanged(NIOBindingEvent_stateChanged event) {
		ComponentStatus requestedState = event._requestedState;
		synchronized (_componentStatusChangeWaitObject)
		{
			switch(requestedState) {
			case COMPONENT_STATUS_PAUSING:
				setComponentState(ComponentStatus.COMPONENT_STATUS_PAUSING);
				break;
			case COMPONENT_STATUS_STOPPING:
				setComponentState(ComponentStatus.COMPONENT_STATUS_STOPPING);
				break;
				
			default: 
				throw new UnsupportedOperationException("Donno how to handle this state: " + requestedState);
			}
			
			_componentStatusChangeWaitObject.notify();
		}
	}

	@Override
	public UDPConInfoListening makeIncomingDatagramConnection(
			INIO_A_Listener aListener, INIO_R_Listener dRListener, INIO_W_Listener dWListener,
			InetSocketAddress localListeningAddress) throws IllegalArgumentException{
		// See if we are already listening .. 
				
		LoggerFactory.getLogger().info(this, "makeIncomingDatagramConnection: " + localListeningAddress);
		
		if(localListeningAddress == null)
			throw new IllegalArgumentException("NIOBindingImp::makeIncomingDatagramConnection(.) cannot have a null listening InetSocketAddress.");
		
		if(aListener == null)
			throw new IllegalArgumentException("NIOBindingImp::makeIncomingDatagramConnection(.) cannot have a null accepting listener.");
		
		try{
			DatagramChannel listeningChannel = DatagramChannel.open();
			UDPChannelInfoListening chInfo =
				createUDPChannelInfoListening(listeningChannel, aListener, dRListener, dWListener, localListeningAddress);
			
			UDPConInfoListening conInfoL =
				(UDPConInfoListening) chInfo.getConnnectionInfo();
			synchronized (_listeningConnections) {
				_listeningConnections.add(conInfoL);
			}
			
			NIOBindingEvent_makeIncoming event = new NIOBindingEvent_makeIncoming(_localSequencer, chInfo);
			addNIOBindingEvent(event);

			return conInfoL;
		}catch(IOException iox) {
			iox.printStackTrace();
			return null;
		}
	}
	
	protected UDPChannelInfoListening createUDPChannelInfoListening(
			DatagramChannel listeningChannel, INIO_A_Listener aListener,
			INIO_R_Listener dRListener, INIO_W_Listener dWListener,
			InetSocketAddress localListeningAddress) {
		return new UDPChannelInfoListening(
				this, listeningChannel, aListener, dRListener, dWListener, localListeningAddress);
	}
	
	@Override
	public IConInfoListening<ServerSocketChannel> makeIncomingConnection(INIO_A_Listener aListener, INIO_R_Listener dRListener, INIO_W_Listener dWListener,
			InetSocketAddress localListeningAddress) throws IllegalArgumentException{
		// See if we are already listening .. 
				
		LoggerFactory.getLogger().info(this, "makeIncomingConnection: " + localListeningAddress);
		
		if(localListeningAddress == null)
			throw new IllegalArgumentException("NIOBindingImp::makeIncomingConnection(.) cannot have a null listening InetSocketAddress.");
		
		if(aListener == null)
			throw new IllegalArgumentException("NIOBindingImp::makeIncomingConnection(.) cannot have a null accepting listener.");
		
		try{
			ServerSocketChannel listeningChannel = ServerSocketChannel.open();
			TCPChannelInfoListening chInfo =
				new TCPChannelInfoListening(
						this, listeningChannel, aListener, dRListener, dWListener, localListeningAddress);
			
			NIOBindingEvent_makeIncoming event = new NIOBindingEvent_makeIncoming(_localSequencer, chInfo);
			addNIOBindingEvent(event);

			ConInfoListening<ServerSocketChannel> conInfoL =
				(ConInfoListening<ServerSocketChannel>) chInfo.getConnnectionInfo();
			synchronized (_listeningConnections) {
				_listeningConnections.add(conInfoL);
			}
			
			return conInfoL;
		}catch(IOException iox) {
			iox.printStackTrace();
			return null;
		}
	}
	
	private void handleNIOBindingEvent_makeIncoming(NIOBindingEvent_makeIncoming event) {
		// We have to start listening
		try{
			ChannelInfoListening<?> chInfo = event._chInfo;
			SelectableChannel listeningChannel = chInfo.getChannel();
			InetSocketAddress localListeningAddress = chInfo.getListeningAddress();
			switch(chInfo._channelType) {
			case TCP:
			{
				ServerSocket listeningSocket = ((ServerSocketChannel)listeningChannel).socket();
				listeningSocket.setReuseAddress(true);
				listeningSocket.bind(localListeningAddress);
				if(listeningSocket.isBound())
					((ConInfoListening<?>)chInfo.getConnnectionInfo()).makeBound();
				break;
			}
				
			case UDP:
			{
				DatagramSocket listeningDatagramSocket = ((DatagramChannel)listeningChannel).socket();
				listeningDatagramSocket.bind(localListeningAddress);
				if(listeningDatagramSocket.isBound())
					((ConInfoListening<?>)chInfo.getConnnectionInfo()).makeBound();
				else
					throw new IllegalSelectorException();
				
				{
					boolean error = false;
					_selectorLock.lock();
					try {
						listeningChannel.configureBlocking(false);
						SelectionKey newKey =
							listeningChannel.register(_selector, SelectionKey.OP_READ, chInfo);
						chInfo.setKey(newKey);
					} catch(IOException iox2) {
						chInfo.destroyChannel();
						iox2.printStackTrace();
						_selectorLock.unlock();
					}
					_selectorLock.unlock();
					if(error)
						chInfo.getConnnectionInfo().makeCancelled();
				}
				
				return;
			}
				
			default:
				throw new UnsupportedOperationException("Unknown channel type: " + chInfo);
			}
			
			_selectorLock.lock();
			{
				listeningChannel.configureBlocking(false);
				try{
					SelectionKey key = listeningChannel.register(_selector, SelectionKey.OP_ACCEPT, chInfo);
					chInfo.setKey(key);
				}catch(IOException iox2) {
					// Cautious: deregister the connection! Just cancel it the normal way :).
					chInfo.destroyChannel();
					iox2.printStackTrace();
					_selectorLock.unlock();
					return;
				}
				// Successfully set up the nio.Channel
				// Now update the local data structures
				ConInfoListening<?> conInfoL = (ConInfoListening<?>) chInfo.getConnnectionInfo();
				synchronized (_listeningConnections) {
					_listeningConnections.add(conInfoL);	
				}
				conInfoL.makeListening();
			}
			_selectorLock.unlock();
			return;// (IConInfoListening) chInfo.getConnnectionInfo();
			
		}catch(IOException iox) {
			iox.printStackTrace();
			return;
		}
	}
	
	@Override
	public void renewConnection(ISession session) {
		LoggerFactory.getLogger().info(this, "RenewConnection: " + session);
		
		NIOBindingEvent_renewConnection event = new NIOBindingEvent_renewConnection(_localSequencer, session);
		addNIOBindingEvent(event);
	}
	
	private void handleNIOBindingEvent_renewConnection(NIOBindingEvent_renewConnection event) {
		ISession session = event._session;
		synchronized (session) {
			TCPConInfoNonListening conInfoNL = (TCPConInfoNonListening) session.getConInfoNL();
			
			if(conInfoNL.isConnected())
				return;
			
			else
			{
				Set<INIOListener> listeners = conInfoNL.getINIOListeners();
				Set<INIO_R_Listener> rListeners = conInfoNL.getINIO_R_Listeners();
				Set<INIO_W_Listener> wListeners = conInfoNL.getINIO_W_Listeners();
					
				destroyConnectionImmediately(conInfoNL);
				session.resetEverythingForRecovery();

				InetSocketAddress remote = session.getRemoteAddress();
				if(remote == null)
					return;
				
				renewConnectionNow(session, conInfoNL, listeners, rListeners, wListeners, remote);
			}
		}
	}
	
	private TCPConInfoNonListening renewConnectionNow(ISession session, TCPConInfoNonListening oldConnection, Set<INIOListener> listeners, 
			Set<INIO_R_Listener> rListeners, Set<INIO_W_Listener> wListeners, InetSocketAddress remoteListeningAddress) {
		
		if(remoteListeningAddress == null)
			throw new IllegalArgumentException("NIOBindingImp::renewConnectionNow(.) remote address cannot be null.");
		TCPConInfoNonListening conInfoNL;
		boolean isConnected;
		synchronized (session) {
			// We have to connect to the remote address
			try{
				SocketChannel nonlisteningChannel = SocketChannel.open();
				nonlisteningChannel.configureBlocking(false);
				
				_selectorLock.lock();
				{
					nonlisteningChannel.configureBlocking(false);
					if(wListeners == null || wListeners.isEmpty())
						throw new IllegalArgumentException("WriteListerners cannot be empty: " + wListeners);
					
					Iterator<INIOListener> listenersIt = listeners.iterator();
					INIOListener listener = null;
					if(listenersIt.hasNext())
						listener = listenersIt.next();
					
					Iterator<INIO_R_Listener> rListenerIt = rListeners.iterator();
					INIO_R_Listener rListener = null;	
					if(rListenerIt.hasNext())
						rListener = rListenerIt.next();
					
					TCPChannelInfoNonListening newChInfo =
						new TCPChannelInfoNonListening(this, session, nonlisteningChannel, listener, 
							rListener, wListeners, remoteListeningAddress);
					
					conInfoNL = (TCPConInfoNonListening) newChInfo.getConnnectionInfo();
					conInfoNL.getSession().setRemoteCC(remoteListeningAddress);
					addToConnectionNLSet(conInfoNL);
					
					while ( listenersIt.hasNext()) {
						listener = listenersIt.next();
						conInfoNL.registerInterestedListener(listener);
					}
					while ( rListenerIt.hasNext()) {
						rListener = rListenerIt.next();
						conInfoNL.registerInterestedReadingListener(rListener);
					}
					
					try
					{
						SelectionKey key = nonlisteningChannel.register(_selector, SelectionKey.OP_CONNECT, newChInfo);
						newChInfo.setKey(key);
						isConnected = nonlisteningChannel.connect(remoteListeningAddress);
						if(isConnected == false)
							newChInfo.getConnnectionInfo().makeConnecting();
						else
							newChInfo.getConnnectionInfo().makeConnected();
						
					}catch(IOException iox3) {
						newChInfo.destroyChannel();
						_selectorLock.unlock();
						throw iox3;
					}
				}
				_selectorLock.unlock();
				
			}catch(IOException iox) {
				iox.printStackTrace();
				return null;
			}
		}
		
		return conInfoNL;
	}
	
	@Override
	public IConInfoNonListening<SocketChannel> makeOutgoingConnection(ISession session, INIOListener listener, INIO_R_Listener rListener, 
			Collection<INIO_W_Listener> wListeners, InetSocketAddress remoteListeningAddress) {
		// See if we are already having a connected channel to the remote server
		
		LoggerFactory.getLogger().info(this, "makeOutgoingConnection: " + remoteListeningAddress);
				
		if(remoteListeningAddress == null)
			throw new IllegalArgumentException("NIOBindingImp::makeOutgoingConnection(.) remote address cannot be null.");

		if(wListeners == null || wListeners.isEmpty())
			throw new IllegalArgumentException("WriteListerners cannot be empty: " + wListeners);

		try{
			SocketChannel nonlisteningChannel = SocketChannel.open();
			nonlisteningChannel.configureBlocking(false);
			
			TCPChannelInfoNonListening newChInfo =
				new TCPChannelInfoNonListening(this, session, nonlisteningChannel, listener, rListener, wListeners, remoteListeningAddress);
			newChInfo.getConnnectionInfo().getSession().setRemoteCC(remoteListeningAddress);
			
			TCPConInfoNonListening conInfoNL = (TCPConInfoNonListening) newChInfo.getConnnectionInfo();
			addToConnectionNLSet(conInfoNL);
			
			NIOBindingEvent event = new NIOBindingEvent_makeOutgoing(_localSequencer, newChInfo);
			addNIOBindingEvent(event);
			
			return conInfoNL;
		}catch(IOException iox) {
			iox.printStackTrace();
			return null;
		}
	}
	
	@Override
	public UDPConInfoNonListening makeOutgoingDatagramConnection(
			ISession session, INIOListener listener, INIO_R_Listener rListener, 
			Collection<INIO_W_Listener> wListeners, InetSocketAddress remoteListeningAddress) {
		// See if we are already having a connected channel to the remote server
		
		LoggerFactory.getLogger().info(this, "makeOutgoingDatagramConnection: " + remoteListeningAddress);
				
		if(remoteListeningAddress == null)
			throw new IllegalArgumentException("NIOBindingImp::makeOutgoingDatagramConnection(.) remote address cannot be null.");

		if(wListeners == null || wListeners.isEmpty())
			throw new IllegalArgumentException("WriteListerners cannot be empty: " + wListeners);

		NIOBindingEvent_registerUDPConInfoNonListening connEvent =
			new NIOBindingEvent_registerUDPConInfoNonListening(
					_localSequencer, session, listener, rListener, wListeners, remoteListeningAddress);
		addNIOBindingEvent(connEvent);
		synchronized (connEvent) {
			while(!connEvent.isSet()) {
				try {connEvent.wait();} catch (InterruptedException itx) {}
			}
			
			UDPConInfoNonListening connInfoNL = connEvent.getConInfoNL();
			LoggerFactory.getLogger().info(this, "DONE: makeOutgoingDatagramConnection: " + connEvent._remoteListeningAddress);
			return connInfoNL;
		}
	}
	
	protected void handleNIOBindingEvent_RegisterUDPConInfoNonListening(
			NIOBindingEvent_registerUDPConInfoNonListening connEvent) {
		UDPChannelInfoNonListening newChInfo = null;
		UDPConInfoNonListening conInfoNL = null;
		try{
		_selectorLock.lock();
		{
			DatagramChannel nonlisteningChannel = DatagramChannel.open();
			nonlisteningChannel.connect(connEvent._remoteListeningAddress);
			nonlisteningChannel.configureBlocking(false);
			
			newChInfo =
				new UDPChannelInfoNonListening(
						this, connEvent._session, nonlisteningChannel,
						connEvent._listener, connEvent._rListener, 
						connEvent._wListeners, connEvent._remoteListeningAddress);
			newChInfo.getConnnectionInfo().getSession().setRemoteCC(connEvent._remoteListeningAddress);
			
			conInfoNL = (UDPConInfoNonListening) newChInfo.getConnnectionInfo();
			addToConnectionNLSet(conInfoNL);
			SelectionKey key = nonlisteningChannel.register(_selector, 0, newChInfo);
			newChInfo.setKey(key);
		}
		_selectorLock.unlock();
		newChInfo.getConnnectionInfo().makeConnected();
		
		connEvent.setConInfoNL(conInfoNL);
		}catch(IOException iox) {
			iox.printStackTrace();
			connEvent.setConInfoNL(null);
		}
	}
	
	private void handleNIOBindingEvent_makeOutgoing(NIOBindingEvent_makeOutgoing event) {
		ChannelInfoNonListening<SocketChannel> newChInfo = event._chInfo;
		ConInfoNonListening<SocketChannel> conInfoNL = newChInfo.getConnnectionInfo();
		SocketChannel nonlisteningChannel = newChInfo.getChannel();
		InetSocketAddress remoteListeningAddress = conInfoNL.getRemoteAddress();
		boolean isConnected;
		ISession session = conInfoNL.getSession();
		synchronized (session)
		{
			// We have to connect to the remote address
			try{
				_selectorLock.lock();
				{
					nonlisteningChannel.configureBlocking(false);
					try
					{
						SelectionKey key = nonlisteningChannel.register(_selector, SelectionKey.OP_CONNECT, newChInfo);
						newChInfo.setKey(key);
						isConnected = nonlisteningChannel.connect(remoteListeningAddress);
						if(isConnected == false)
							newChInfo.getConnnectionInfo().makeConnecting();
						else
							newChInfo.getConnnectionInfo().makeConnected();
						
					}catch(IOException iox3) {
						newChInfo.destroyChannelSilently();
						_selectorLock.unlock();
						throw iox3;
					}

				}
				_selectorLock.unlock();
				
			}catch(IOException iox) {
				iox.printStackTrace();
				return;
			}
		}
		
		return; //newChInfo.getConnnectionInfo();
	}

	@Override
	public void prepareToStart() {
		synchronized (_componentStatusChangeWaitObject) {
			if(_status != ComponentStatus.COMPONENT_STATUS_UNINITIALIZED)
				throw new IllegalStateException("Status is " + _status);
			
			setComponentState(ComponentStatus.COMPONENT_STATUS_INITIALIZING);
		
			synchronized(_selector) {
				_selector.notify();
			}
		
			setComponentState(ComponentStatus.COMPONENT_STATUS_INITIALIZED);
		}
	}
	
	protected void resetEverything() {
		int reading=0, writing=0, connecting=0;
		for(ConInfoNonListening<?> conInfoNL : _nonlisteningConnections) {
			switch(conInfoNL._chInfoNL._channelType) {
			case TCP:
				if(conInfoNL.isConnecting())
					connecting++;

				if(conInfoNL.isReaderReady())
					reading++;
			
				if(conInfoNL.isWriterReady())
					writing++;
				break;
				
			case UDP:
				if(conInfoNL.isReadyForWrite())
					writing++;
				
				break;
			}
		}
		
		int listening = 0;
		for(IConInfoListening<?> conInfoL : _listeningConnections) {
			if(conInfoL.isListening())
				switch(conInfoL.getChannelType()) {
				case UDP:
					reading++;
					listening++;
					break;
				case TCP:
					listening++;
					break;
				}
		}
		
		_connectingCount = connecting;
		_readingCount = reading;
		_writingCount = writing;
		_acceptingCount = listening;
	}
	
	private final int JUNK_FILTER_COUNT = (Broker.RELEASE?200:50);
	private void spilloutJunk(String c) {
		if(!Broker.DEBUG && Broker.RELEASE)
			return;
		
		if(++_junkFilterCounter % JUNK_FILTER_COUNT == 0)
			System.out.println(c + " " + BrokerInternalTimer.read().toString() + 
									" \t" + _nioBindingEvents.size() + " \t " + 
								_status +
								" R:" + _readingCount +
								" W:" + _writingCount +
								" A:" + _acceptingCount +
								" C:" + _connectingCount + "\t" + 
								Writers.write(_nonlisteningConnections.toArray(new ConInfoNonListening[0])));
		
	}
	
	
	private final void checkForIssue(int n) {
		if(n != 0)
			return;
		
		boolean issue = makeSureThisIsAnIssue();
		if(issue)
			handleIssue();
	}
	
	private final int ISSUE_FILTER_COUNTER = 2000;
	private final int ISSUE_TIMEOUT = 1000;
	private long _lastTime = 0;
	private int _junkFilterCounter = 0, _junkIssueFilterCounter;
	private final boolean makeSureThisIsAnIssue() {
		if(++_junkIssueFilterCounter % ISSUE_FILTER_COUNTER != 0)
			return false;
			
		long now = SystemTime.currentTimeMillis();
		if(now - _lastTime < ISSUE_TIMEOUT) {
			_lastTime = now;
			return true;
		}
		
		_lastTime = now;
		return false;
	}

	
	protected void handleIssue() {
		LoggerFactory.getLogger().info(this, "Handling issue ...");
		String channelsSummary = summarizeChannels();
		LoggerFactory.getLogger().info(this, "Issue: " + BrokerInternalTimer.read().toString() + " \t" + channelsSummary);
	}
	
	@Override
	public void dumpChannelSummary() {
		NIOBindingEvent_summarizeChannels event = new NIOBindingEvent_summarizeChannels(_localSequencer);
		addNIOBindingEventHead(event);
	}
	
	protected String summarizeChannels() {
		String output = "Channels Summary: ";
		boolean error = false;
		_selectorLock.lock();
		{
			int checkAcceptingCount = 0;
			int checkReadingCount = 0;
			int checkWritingCount = 0;
			int checkConnectingCount = 0;
			
			Iterator<SelectionKey> keysIt = _selector.keys().iterator();
			while ( keysIt.hasNext()) {
				SelectionKey key = keysIt.next();
				int ops = key.interestOps();
				ChannelInfo<?> chInfo = (ChannelInfo<?>) key.attachment();
				IConInfo<?> connInfo = chInfo.getConnnectionInfo();
				output += connInfo + "=" + ops + "; ";
				
				// Check
				if((ops & SelectionKey.OP_ACCEPT) !=0) {
					checkAcceptingCount++;
					if(!_listeningConnections.contains(connInfo)) {
						output += "[not in listenningConnections]";
						error = true;
					}
				}
				
				if((ops & SelectionKey.OP_READ) !=0) {
					checkReadingCount++;
					if(!_nonlisteningConnections.contains(connInfo)) {
						output += "[not in nonlistenningConnections]";
						error = true;
					}
				}
				
				if((ops & SelectionKey.OP_WRITE) !=0) {
					checkWritingCount++;
					if(!_nonlisteningConnections.contains(connInfo)) {
						output += "[not in nonlistenningConnections]";
						error = true;
					}
				}
				
				if((ops & SelectionKey.OP_CONNECT) !=0) {
					checkConnectingCount++;
					if(!_nonlisteningConnections.contains(connInfo)) {
						output += "[not in nonlistenningConnections]";
						error = true;
					}
				}
			}
			
			if(checkAcceptingCount != _acceptingCount) {
				output += "[(A)" + checkAcceptingCount + "!=" + _acceptingCount + "]";
				error = true;
			}
			if(checkConnectingCount != _connectingCount) {
				output += "[(C)" + checkConnectingCount + "!=" + _connectingCount + "]";
				error = true;
			}
			if(checkReadingCount != _readingCount) {
				output += "[(R)" + checkReadingCount + "!=" + _readingCount + "]";
				error = true;
			}
			if(checkWritingCount != _writingCount) {
				output += "[(W)" + checkWritingCount + "!=" + _writingCount + "]";
				error = true;
			}
		}
		_selectorLock.unlock();
		
		if(error)
			return "ERROR: " + output;
		else
			return output;
	}
	
	@Override
	public void run() {
		try
		{
			while(true) {
				boolean shouldContinue = doComponentStateTransitions();
				if(shouldContinue == false)
					return;

				runMe();
			}
		}catch(Exception ex) {
			ex.printStackTrace();
			LoggerFactory.getLogger().infoX(this, ex, "Exception in '" + this + "'");
		}
	}
	
	private boolean doComponentStateTransitions() {
		synchronized (_componentStatusChangeWaitObject) {
			switch ( _status) {
			case COMPONENT_STATUS_UNINITIALIZED:
				throw new IllegalStateException("NIO is uninitialized: " + _status);
				
			case COMPONENT_STATUS_INITIALIZED:
			case COMPONENT_STATUS_STARTING:
				setComponentState(ComponentStatus.COMPONENT_STATUS_RUNNING);
				break;
				
			case COMPONENT_STATUS_PAUSING:
				setComponentState(ComponentStatus.COMPONENT_STATUS_PAUSED);
				break;

			case COMPONENT_STATUS_INITIALIZING:
			case COMPONENT_STATUS_PAUSED:
				try{_componentStatusChangeWaitObject.wait();}catch(InterruptedException itx) {}
				break;
				
			case COMPONENT_STATUS_PROBLEM:
				return false;
				
			case COMPONENT_STATUS_STOPPED:
				return false;
				
			case COMPONENT_STATUS_STOPPING:
				killAllConnections();
				setComponentState(ComponentStatus.COMPONENT_STATUS_STOPPED);
				NIOBindingFactory.destroyNIOBinding(_localSequencer.getLocalAddress());
				break;

			case COMPONENT_STATUS_RUNNING:
				break;
				
			default:
				throw new UnsupportedOperationException("Donno how to handle this: " + _status);
			}
			
			return true;
		}
	}
	
	private void runMe() {
		int n = 0;
		while (_status == ComponentStatus.COMPONENT_STATUS_RUNNING)
		{
			while ( (_status == ComponentStatus.COMPONENT_STATUS_RUNNING) 
					&& 
					(!_nioBindingEvents.isEmpty() || (_readingCount+_writingCount+_acceptingCount+_connectingCount != 0)))
			{
				
				while ( !_nioBindingEvents.isEmpty() && _status == ComponentStatus.COMPONENT_STATUS_RUNNING) {
					NIOBindingEvent event;
					synchronized (_nioBindingEvents) {
						event = _nioBindingEvents.remove(0);	
					}
					handleNIOBindingEvent(event);
				}

				if(_status != ComponentStatus.COMPONENT_STATUS_RUNNING)
					break;
				
				try{
					_selectorLock.lock();
					{
						n = _selector.select(NIOBindingFactory.SELECT_TIMEOUT);
					}
					_selectorLock.unlock();
				}catch(IOException ix) {
					ix.printStackTrace();
					System.exit(-100);
				}

				if(_status != ComponentStatus.COMPONENT_STATUS_RUNNING)
					break;

				checkForIssue(n);
				spilloutJunk("*" + n + "*");
				if(n==0)
					continue;

				Iterator<SelectionKey> it = _selector.selectedKeys().iterator();
				while ( it.hasNext() && _status == ComponentStatus.COMPONENT_STATUS_RUNNING) {
					SelectionKey key = it.next();
					try{
						if(key.isAcceptable()) {
							processAcceptable(key);
						}
						if(key.isConnectable()) {
							processConnectable(key);
						}
						if(key.isReadable()) {
							processReadable(key);
						}
						if(key.isWritable()) {
							processWritable(key);
						}
					}catch(CancelledKeyException ckx) {
//						ckx.printStackTrace();
						if(key == null)
							throw new IllegalStateException("Key is null. " + _nonlisteningConnections);

						ChannelInfo<?> chInfo = (ChannelInfo<?>) key.attachment();
						
						if(chInfo != null)
						{
							if(ChannelInfoListening.class.isInstance(chInfo)) {
								ChannelInfoListening<?> chInfoL = (ChannelInfoListening<?>) chInfo;
								chInfoL.destroyChannel();
								synchronized (_listeningConnections) {
									_listeningConnections.remove(chInfo.getConnnectionInfo());	
								}
							}else if(ChannelInfoNonListening.class.isInstance(chInfo)) {
								ChannelInfoNonListening<?> chInfoNL = (ChannelInfoNonListening<?>) chInfo;
								chInfoNL.destroyChannel();
							}
							else
								throw new IllegalStateException("Donno how to handle this: " + chInfo);
				
						}
					}
					it.remove();
				}
			}
			synchronized(_nioBindingEvents) {
				if(_status == ComponentStatus.COMPONENT_STATUS_RUNNING && _readingCount+_writingCount+_acceptingCount == 0 && _nioBindingEvents.isEmpty()) {
					try{
						_nioBindingEvents.wait();
					}catch(InterruptedException ix) {
						ix.printStackTrace();
					}
				}
			}
		}
	}

	private void killAllConnections() {
		LoggerFactory.getLogger().info(this, "killAllConnections");
		
		ConInfoListening<?>[] allListeningConnections;
		synchronized (_listeningConnections) {
			allListeningConnections = _listeningConnections.toArray(new ConInfoListening[0]);
		}
		if(allListeningConnections != null) {
			for ( int i=0 ; i<allListeningConnections.length ; i++) {
				destroyConnectionImmediately(allListeningConnections[i]);				
			}
		}
		
		ConInfoNonListening<?>[] allNonListeningConnections;
		synchronized (_nonlisteningConnections) {
			allNonListeningConnections = _nonlisteningConnections.toArray(new ConInfoNonListening[0]);
		}
		if(allNonListeningConnections != null) {
			for ( int i=0 ; i<allNonListeningConnections.length ; i++) {
				destroyConnectionImmediately(allNonListeningConnections[i]);
			}
		}
	}

	private void destroyConnectionImmediately(IConInfoListening<?> conInfoL) {
		conInfoL.getChannelInfo().destroyChannel();
		
		synchronized (_listeningConnections) {
			_listeningConnections.remove(conInfoL);
			switch(conInfoL.getChannelType()) {
			case TCP:
				_acceptingCount--;
				break;
				
			case UDP:
				break;
			}
		}
	}
	
	private void destroyConnectionImmediately(IConInfoNonListening<?> conInfoNL) {
		ISession session = conInfoNL.getSession();
		synchronized (session) {
			conInfoNL.getChannelInfo().destroyChannel();
		}
		
		removeFromConnectionNLSet((ConInfoNonListening<?>)conInfoNL);
		if(Broker.CORRELATE)
			TrafficCorrelator.getInstance().conInfoNLDiscarded(conInfoNL, session);
	}
	
	@Override
	public void destroyConnection(IConInfoListening<?> conInfoL) {
		LoggerFactory.getLogger().info(this, "destroyConnection" + conInfoL);
		
		if(conInfoL == null)
			return;
		if(conInfoL.getNIOBinding() != this)
			return;

		NIOBindingEvent_destroyConnection event = new NIOBindingEvent_destroyConnection(_localSequencer, conInfoL);
		addNIOBindingEvent(event);
	}
		
	@Override
	public void destroyConnection(IConInfoNonListening<?> conInfoNL) {
		LoggerFactory.getLogger().info(this, "destroyConnection" + conInfoNL);
		
		if(conInfoNL == null)
			return;
		if(conInfoNL.getNIOBinding() != this)
			return;
	
		NIOBindingEvent_destroyConnection event = new NIOBindingEvent_destroyConnection(_localSequencer, conInfoNL);
		addNIOBindingEvent(event);
		LoggerFactory.getLogger().info(this, "destroyConnection (ended)" + conInfoNL);
	}
	
	private void handleNIOBindingEvent_destroyConnection(NIOBindingEvent_destroyConnection event) {
		IConInfo<?> conInfo = event._conInfo;
		switch(event._conInfoType) {
		case ConnectionTypeListening:
			ConInfoListening<?> conInfoL = (ConInfoListening<?>) event._conInfo;
			destroyConnectionImmediately(conInfoL);
			break;
		
		case ConnectionTypeNonListening:
			ConInfoNLKeepAlive conInfoNL = (ConInfoNLKeepAlive) conInfo;
			destroyConnectionImmediately(conInfoNL);
			break;
		
		default: 
			throw new UnsupportedOperationException("Unknown connection type: " + conInfo);
		}
	}
	
	private void handleNIOBindingEvent_sendPacket(NIOBindingEvent_sendPacket event) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean send(IRawPacket raw, IConInfoNonListening<?> conInfoNL) {
		if(conInfoNL == null) {
			LoggerFactory.getLogger().warn(this, "Sending on NULL connectionInfoNL. Message=" + raw);
			return false;
		}
		try{
			if(Broker.DEBUG) 
				LoggerFactory.getLogger().debug(this, "sending raw: " + raw.getQuickString() + ", on conInfoNL: " + conInfoNL);
			ConInfoNonListening<?> conInfoNL_I = (ConInfoNonListening<?>) conInfoNL;
			return conInfoNL_I.sendPacket(this, raw);
		}catch(CancelledKeyException ckx) {
			destroyConnection(conInfoNL);
			return false;
		}
	}
	
	private void processAcceptable(SelectionKey key) {
		ChannelInfoListening<?> chInfoL = (ChannelInfoListening<?>) key.attachment();
		ConInfoListening<?> conInfoL = (ConInfoListening<?>) chInfoL.getConnnectionInfo();
		//chInfo must be in CHINFO_STATUS_LISTENING state.
		switch(chInfoL._channelType) {
		case TCP:
			ServerSocketChannel channel = (ServerSocketChannel) chInfoL.getChannel();
			InetSocketAddress remoteSocketAddr = null;
			TCPConInfoNonListening newConInfoNL = null;
			SocketChannel newChannel = null;
			Socket newSocket = null;
			ChannelInfoNonListening<?> newChInfo = null;
			try{
				newChannel = channel.accept();
				newSocket = newChannel.socket();
				remoteSocketAddr = new InetSocketAddress(newSocket.getInetAddress(), newSocket.getPort());
//				INIOReadingListener nioReadingListener = chInfo.getConnnectionInfo().();
				// TODO: perhaps it is not a good idea to inherit the reader.
				// TODO: This must consult with a "connection listeners manager", to be done.
				Set<INIO_W_Listener> defaultWListeners = new HashSet<INIO_W_Listener>();
				defaultWListeners.add(conInfoL.getDefault_W_Listener());
				ISession session = ISessionManager.getUnknownPeerSession(_brokerShadow);
				newChInfo = new TCPChannelInfoNonListening(this, session, newChannel, null, conInfoL.getDefault_R_Listener(), defaultWListeners, remoteSocketAddr);
				newConInfoNL = (TCPConInfoNonListening) newChInfo.getConnnectionInfo();
				addToConnectionNLSet(newConInfoNL);
			}catch(IOException iox) {
				iox.printStackTrace();
				if(newChInfo != null)
					newChInfo.destroyChannel();
			}

			boolean isConnected = newChannel.isConnected();
			_selectorLock.lock();
			{
				if(isConnected) {
					try{
						newChannel.configureBlocking(false);
						SelectionKey newKey =
							newChannel.register(_selector, SelectionKey.OP_READ, newChInfo);
						newChInfo.setKey(newKey);
						interestedInReads(newKey);
						newChInfo.getConnnectionInfo().makeConnected();
					}catch(IOException iox2) {
						newChInfo.destroyChannel();
						iox2.printStackTrace();
						_selectorLock.unlock();
						return;
					}
				}else{
					try{
						newChannel.configureBlocking(false);
						SelectionKey newKey = newChannel.register(_selector, SelectionKey.OP_CONNECT, newChInfo);
						chInfoL.setKey(newKey);
					}catch(IOException iox3) {
						newChInfo.destroyChannel();
						iox3.printStackTrace();
						_selectorLock.unlock();
						return;
					}
				}
			}
			_selectorLock.unlock();
			
			INIO_A_Listener nioAcceptingListener = conInfoL.getAcceptingListener();
			nioAcceptingListener.newIncomingConnection(conInfoL, newConInfoNL);
			
			if(isConnected) {
				newConInfoNL.makeConnected();
			}else{
				newConInfoNL.makeConnecting();
			}
			return;
			
		case UDP:
			throw new UnsupportedOperationException("Not supported yet!");
			
		default:
			throw new UnsupportedOperationException("Unknown channel Type: " + chInfoL);
		}
	}
	
	private void processConnectable(SelectionKey key) {
		ChannelInfoNonListening<?> chInfo = (ChannelInfoNonListening<?>) key.attachment();
		if(chInfo._channelType != ChannelTypes.TCP)
			throw new IllegalStateException();
		
		
		TCPConInfoNonListening conInfo = (TCPConInfoNonListening) chInfo.getConnnectionInfo();
		SocketChannel channel = (SocketChannel) chInfo.getChannel();
		try{
			boolean finished = channel.finishConnect();
			if(finished == true) {
				_selectorLock.lock();
				synchronized(key)
				{
					try{
						channel.configureBlocking(false);
						key.interestOps( (key.interestOps() &  (~SelectionKey.OP_CONNECT)) | SelectionKey.OP_READ);
					}catch(IOException iox2) {
						iox2.printStackTrace();
						_selectorLock.unlock();
						throw iox2;
					}
					conInfo.makeConnected();	
				}
				_selectorLock.unlock();
			}
		}catch(IOException iox) {
			chInfo.destroyChannel();
			iox.printStackTrace();
		}
	}
	
	private void processReadable(UDPChannelInfoListening chInfoNL, DatagramChannel channel) throws IOException {
		ByteBuffer buff = chInfoNL.getInBody();
		SocketAddress remote = channel.receive(buff);
		
		IRawPacket raw = PacketFactory.reconstructPacket(remote, buff);
		if(raw == null)
			return;
		
		chInfoNL.addIncomingDataToList(raw);
		chInfoNL.removeInBuffers();
		IPacketListener packetListener = raw.getPacketListener();
		if(packetListener != null)
			packetListener.packetSent(raw.getObject());
	}
	
	private void processReadable(ChannelInfoNonListening<?> chInfoNL, SocketChannel channel) throws IOException {
		ByteBuffer bdy = chInfoNL.getInBody();
		ByteBuffer hdr = chInfoNL.getInHeader();
		int lastRead = -2;
		while ( hdr.hasRemaining()) {
			lastRead=channel.read(hdr);
			if(lastRead == 0)
				return;
			else if(lastRead == -1)
				throw new IOException("'" + chInfoNL.getConnnectionInfo() + "' returned -1.");	
		}

		if(bdy == null) {
			bdy = chInfoNL.getInBody();
		}
		
		while ( bdy.hasRemaining()) {
			lastRead=channel.read(bdy);
			if(lastRead == 0)
				return;
			else if(lastRead == -1)
				throw new IOException("'" + chInfoNL.getConnnectionInfo() + "' returned -1.");
		}
		
		IRawPacket raw = PacketFactory.reconstructPacket(hdr, bdy);
		if(chInfoNL.getConnnectionInfo().addIncomingDataToList(raw)) {
			chInfoNL.removeInBuffers();
			IPacketListener packetListener = raw.getPacketListener();
			if(packetListener != null)
				packetListener.packetSent(raw.getObject());
		}
	}
	
	protected void processReadable(SelectionKey key) {
		try{
//			System.out.println("NIOBindingImp::processReadable().");
			ChannelInfo<?> chInfo = (ChannelInfo<?>) key.attachment();
			if(chInfo.getKey() != key)
				new Exception("Channel key does not match: " + chInfo).printStackTrace();
			
			switch(chInfo._type) {
			case TCP:
			{
				SocketChannel channel = (SocketChannel) key.channel();
				processReadable((TCPChannelInfoNonListening)chInfo, channel);
				break;
			}
			
			case UDP:
			{
				DatagramChannel channel = (DatagramChannel) key.channel();
				processReadable((UDPChannelInfoListening)chInfo, channel);
				break;
			}
				
			default:
				throw new UnsupportedOperationException();
			}
		}catch(IOException iox) { //caused by the read() operations on the Channels.
			ChannelInfoNonListening<?> chInfoNL = (ChannelInfoNonListening<?>) key.attachment();
			chInfoNL.destroyChannel();
			iox.printStackTrace();
		}
	}

	private void processWritable(SelectionKey key) {
		ChannelInfoNonListening<?> chInfoNL = (ChannelInfoNonListening<?>)key.attachment();
		ByteBuffer [] out = chInfoNL.getOutBuffs(key);
	
		if(out != null) {
			// Previous send not complete.
			try{
				switch(chInfoNL._channelType)
				{
				case TCP:
					{
						SocketChannel channel = (SocketChannel) key.channel();
						channel.write(out);
						break;
					}	
				case UDP:
					{
						DatagramChannel channel = (DatagramChannel) key.channel();
						try {
							int a = channel.send(out[1], chInfoNL.getRemoteAddress());
							if(a != out[1].capacity())
								new IllegalStateException("Incomplete datagram sent: " + a + " vs. " + out[1].capacity()).printStackTrace();
						} catch (PortUnreachableException x) {
							IRawPacket raw = chInfoNL.getOutRawPacket();
							BrokerInternalTimer.inform(
									getLocalAddress().getPort() + ": PortUnreachableException MSG destination: " +
									chInfoNL.getRemoteAddress() + " " + (raw == null? "NULL" : raw.getObject()));
						}
						break;
					}
					
				default:
					throw new UnsupportedOperationException("" + chInfoNL._channelType);
				}
				
			}catch(IOException iox) {
				chInfoNL.destroyChannel();
				iox.printStackTrace();
			}
			if(!out[1].hasRemaining()) {
				//Successfully sent the data.
				chInfoNL.emptyOutBuffers();
			}
		}
	}

	@Override
	void updateReaderCount(int i) {
		synchronized (_nioBindingEvents)
		{
			_readingCount+=i;
			if(_readingCount < 0)
				throw new IllegalStateException();
			
			if(_readingCount != 0)
				_nioBindingEvents.notify();
		}
//		_selector.wakeup();
	}
	
	@Override
	void updateConnectingCount(int i) {
		synchronized (_nioBindingEvents)
		{
			_connectingCount+=i;
			if(_connectingCount < 0)
				throw new IllegalStateException();
			
			if(_connectingCount != 0)
				_nioBindingEvents.notify();
		}
//		_selector.wakeup();
	}

	@Override
	void updateWriterCount(int i) {
		synchronized (_nioBindingEvents)
		{
			_writingCount+=i;
			if(_writingCount < 0)
				throw new IllegalStateException();
			
			if(_writingCount != 0)
				_nioBindingEvents.notify();
		}
//		_selector.wakeup();
	}

	@Override
	void updateAcceptingCount(int i) {
		synchronized (_nioBindingEvents)
		{
			_acceptingCount+=i;
			if(_acceptingCount < 0)
				throw new IllegalStateException(i + " v. " + _acceptingCount + " v. " + this);
			
			if(_acceptingCount != 0)
				_nioBindingEvents.notify();
		}
//		_selector.wakeup();
	}

	@Override
	public IConInfoNonListening<SocketChannel> makeOutgoingConnection(ISession session,
			INIOListener listener, INIO_R_Listener rListener,
			INIO_W_Listener listener3, InetSocketAddress remoteListeningAddress) {
		Set<INIO_W_Listener> wListeners = new HashSet<INIO_W_Listener>();
		wListeners.add(listener3);
		return makeOutgoingConnection(session, listener, rListener, wListeners, remoteListeningAddress);
	}

	@Override
	public UDPConInfoNonListening makeOutgoingDatagramConnection(ISession session,
			INIOListener listener, INIO_R_Listener rListener,
			INIO_W_Listener listener3, InetSocketAddress remoteListeningAddress) {
		Set<INIO_W_Listener> wListeners = new HashSet<INIO_W_Listener>();
		wListeners.add(listener3);
		return makeOutgoingDatagramConnection(
				session, listener, rListener, wListeners, remoteListeningAddress);
	}


	////////////////////////////////////////////////////////////////////////
	//							ICOMPONENT								  //
	////////////////////////////////////////////////////////////////////////

	//IComponent
	@Override
	public void addNewComponentListener(IComponentListener comListener) {
		System.out.print("Adding new component listener for NIO: " + comListener);
		synchronized (_componentListeners) {
			_componentListeners.add(comListener);
		}
		System.out.println(" Done.");
	}

	//IComponent
	@Override
	public final void removeComponentListener(IComponentListener comListener) {
		System.out.print("Removing old component listener for NIO: " + comListener);
		synchronized(_componentListeners) {
			_componentListeners.remove(comListener);
		}
		System.out.println(" Done.");
	}
	
	//IComponent
	@Override
	public final ComponentStatus getComponentState() {
		return _status;
	}
	
	@Override
	public String getComponentName() {
		return toString();
	}
	
	@Override
	public void awakeFromPause() {
		synchronized (_componentStatusChangeWaitObject) {
			if(_status == ComponentStatus.COMPONENT_STATUS_PAUSED) {
				setComponentState(ComponentStatus.COMPONENT_STATUS_STARTING);
				_componentStatusChangeWaitObject.notify();
			}
		}
		
		synchronized (_nioBindingEvents) {
			_nioBindingEvents.notify();	
		}
//		_selector.wakeup();
	}

	@Override
	public void pauseComponent() {
		NIOBindingEvent_stateChanged stateChangeEvent = new NIOBindingEvent_stateChanged(_localSequencer, ComponentStatus.COMPONENT_STATUS_PAUSING);
		addNIOBindingEventHead(stateChangeEvent);
		
//		synchronized (_componentStatusChangeWaitObject) {
//			_componentStatusChangeWaitObject.notify();			
//		}
	}

	@Override
	public void stopComponent() {
		NIOBindingEvent_stateChanged stateChangeEvent = new NIOBindingEvent_stateChanged(_localSequencer, ComponentStatus.COMPONENT_STATUS_STOPPING);
		addNIOBindingEventHead(stateChangeEvent);
		
//		synchronized (_componentStatusChangeWaitObject) {
//			_componentStatusChangeWaitObject.notify();			
//		}

	}

	private final void setComponentState(ComponentStatus state) {
		LoggerFactory.getLogger().info(this, "Component '" + getComponentName() + "' state change to " + state + " from " + _status);
		
		_status = state;
		if(_componentListeners == null)
			return;
		
		synchronized (_nioBindingEvents) {
			_nioBindingEvents.notify();
		}
//		_selector.wakeup();
			
		synchronized(_componentListeners) {
			Iterator<IComponentListener> it = _componentListeners.iterator();
			while ( it.hasNext()) {
				it.next().componentStateChanged(this);
			}
		}
	}
	
	@Override
	public String toString() {
		if(_localSequencer == null)
			return "NIOBinding(unbound)";
		
		return "NIOBinding(" + _localSequencer.getLocalAddress().getPort() + ")";
	}

	@Override
	public void switchListeners(INIOListener newListener, INIOListener oldListener) {
		synchronized (_listeningConnections) {
			for(IConInfoListening<?> conInfoL : _listeningConnections)
				conInfoL.switchListeners(newListener, oldListener);
		}
		
		synchronized (_nonlisteningConnections) {
			for(IConInfoNonListening<?> conInfoNL : _nonlisteningConnections) {
				conInfoNL.switchListeners(newListener, oldListener);
			}
		}
	}
	
	private boolean addToConnectionNLSet(ConInfoNonListening<?> conInfoNL) {
		synchronized (_nonlisteningConnections) {
			boolean b = _nonlisteningConnections.add(conInfoNL);

			return b;
		}
	}
	
	private boolean removeFromConnectionNLSet(ConInfoNonListening<?> conInfoNL) {
		synchronized (_nonlisteningConnections) {
			boolean b = _nonlisteningConnections.remove(conInfoNL);

			return b;
		}
	}

	boolean _componentThreadStarted = false;
	@Override
	public void startComponent() {
		if(_componentThreadStarted)
			throw new IllegalStateException("Component Thread Already started.");
		
		_componentThreadStarted = true;
		super.start();
	}

	public InetSocketAddress getLocalAddress() {
		return _ccSockAddress;
	}

	@Override
	public LocalSequencer getLocalSequencer() {
		return _localSequencer;
	}
}

