package org.msrg.publiy.communication.core.niobinding;

import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastAnnotatorFactory;

import org.msrg.publiy.utils.annotations.AnnotationEvent;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.TrafficLogger;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;


import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIOListener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.RawPacketPriority;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;

public abstract class ConInfoNonListening<T extends SelectableChannel> implements IConInfoNonListening<T>, ILoggerSource {
	
	protected final IBrokerShadow _brokerShadow;
	
	protected final static int CONNECTION_MAX_OUTGOING_QUEUE_SIZE = 2;
	protected final static int CONNECTION_MAX_INCOMING_QUEUE_SIZE = 2000000000;
	protected final static int CONNECTION_OUTGOING_QUEUE_NOTIFYING_LIMIT = CONNECTION_MAX_OUTGOING_QUEUE_SIZE / 2;
	protected final static int CONNECTION_INCOMING_QUEUE_NOTIFYING_LIMIT = CONNECTION_MAX_INCOMING_QUEUE_SIZE;// / 4;
	protected final static int CONNECTION_INCOMING_FULL_YIELD_COUNTER = 100;
	protected static int _COUNTER_ID = 0;
	protected final static Object _COUNTER_ID_LOCK = new Object();
	protected final int _counterId;
	
	protected long incoming_notifier_counter = 0;
	
	protected final TrafficLogger _trafficLogger;
	protected final LocalSequencer _localSequencer;
	private LinkedList<IRawPacket> _incoming;
	private int _incomingQSize;
	private LinkedList<IRawPacket> _outgoing;
	private int _outgoingQSize;
	
	protected ChannelInfoNonListening<T> _chInfoNL;
	protected Set<INIOListener> _listeners;
	protected Set<INIO_R_Listener> _rListeners;
	protected Set<INIO_W_Listener> _wListeners;
	
	Set<INIOListener> getINIOListeners() {
		return _listeners;
	}
	
	Set<INIO_R_Listener> getINIO_R_Listeners() {
		return _rListeners;
	}

	Set<INIO_W_Listener> getINIO_W_Listeners() {
		return _wListeners;
	}
	
	private boolean _isReaderReady = false;
	private boolean _isWriterReady = false;
	
	protected boolean isReaderReady() {
		return _isReaderReady;
	}
	
	protected boolean isWriterReady() {
		return _isWriterReady;
	}
	
	protected ConnectionInfoNonListeningStatus _state;
	protected final ISession _session;
	private Sequence _lastUpdateSequence;
	private Sequence _cancellationSequence;
	private Sequence _connectedSequence;
	
	@Override
	public Sequence getLastUpdateSequence() {
		return _lastUpdateSequence;
	}

	@Override
	public Sequence getCancellationSequence() {
		return _cancellationSequence;
	}
	
	@Override
	public Sequence getConnectedSequence() {
		return _connectedSequence;
	}
	
	protected void setState(ConnectionInfoNonListeningStatus newState) {
		if(newState == ConnectionInfoNonListeningStatus.NL_CANCELLED)
			_cancellationSequence = _localSequencer.getNext();
		else if(newState == ConnectionInfoNonListeningStatus.NL_CONNECTED)
			_connectedSequence = _localSequencer.getNext();
		
		if(newState == ConnectionInfoNonListeningStatus.NL_CONNECTING && 
				this._state != ConnectionInfoNonListeningStatus.NL_CONNECTING) {
			NIOBinding myNIOBinding = _chInfoNL.getNIOBinding();
			myNIOBinding.updateConnectingCount(+1);
		}else if(this._state==ConnectionInfoNonListeningStatus.NL_CONNECTING && 
				newState != ConnectionInfoNonListeningStatus.NL_CONNECTING) {
			NIOBinding myNIOBinding = _chInfoNL.getNIOBinding();
			myNIOBinding.updateConnectingCount(-1);
		}
		
		ConnectionInfoNonListeningStatus oldStatus = _state;
		_state = newState;
		_lastUpdateSequence = _localSequencer.getNext();
		if(Broker.CORRELATE)
			TrafficCorrelator.getInstance().conInfoNLStateChanged(this, oldStatus, newState);
	}
	
	protected ConInfoNonListening(IBrokerShadow brokerShadow, ISession session, ChannelInfoNonListening<T> chNL, INIOListener listener, INIO_R_Listener rListener, 
			Collection<INIO_W_Listener> wListeners) {
	
		_brokerShadow = brokerShadow;
		_localSequencer = brokerShadow.getLocalSequencer();
		_trafficLogger = brokerShadow.getTrafficLogger();

		synchronized (_COUNTER_ID_LOCK) {
			_counterId = ++_COUNTER_ID;	
		} 
		_incoming = new LinkedList<IRawPacket>();
		_incomingQSize = 0;
		_outgoing = new LinkedList<IRawPacket>();
		_outgoingQSize = 0;
		_chInfoNL = chNL;

		_session = ISessionManager.attachSession(brokerShadow, session, this);
		
		setState(ConnectionInfoNonListeningStatus.NL_UNINITIALIZED);
		
		_listeners = new HashSet<INIOListener>();
		if(listener != null) {
			registerInterestedListener(listener);
		}
		
		_rListeners = new HashSet<INIO_R_Listener>();
		if(rListener != null) {
			registerInterestedReadingListener(rListener);
		}
		
		_wListeners = new HashSet<INIO_W_Listener>();
		if(wListeners != null) {
			Iterator<INIO_W_Listener> it = wListeners.iterator();
			while(it.hasNext()) {
				INIO_W_Listener wListener = it.next();
				if(_wListeners.add(wListener))
					registerInterestedWritingListener(wListener);
			}
		}
		
		if(Broker.CORRELATE)
			TrafficCorrelator.getInstance().conInfoNLCreated(this, _session);
	}
	
	protected void makeConnected() {
		synchronized (_session) {
			if(this._state == ConnectionInfoNonListeningStatus.NL_CONNECTED)
				return;
			setState(ConnectionInfoNonListeningStatus.NL_CONNECTED);
			NIOBinding myNIOBinding = _chInfoNL.getNIOBinding();
			if(_isReaderReady == false) {
				_isReaderReady = true;
				myNIOBinding.updateReaderCount(1);
				
				SelectionKey key = _chInfoNL.getKey();
				_chInfoNL.getNIOBinding().interestedInReads(key);
			}
		}
		notifyAllListeners();
	}
	
	void makeCancelledSilently() {
		synchronized(_session) {
			if(this._state == ConnectionInfoNonListeningStatus.NL_CANCELLED)
				return;
			setState(ConnectionInfoNonListeningStatus.NL_CANCELLED);
			NIOBinding myNIOBinding = _chInfoNL.getNIOBinding();
			if(_isReaderReady)
				myNIOBinding.updateReaderCount(-1);
			_isReaderReady = false;
			if(_isWriterReady)
				myNIOBinding.updateWriterCount(-1);
			_isWriterReady = false;
		}		
	}
	
	public void makeCancelled() {
		makeCancelledSilently();
		notifyAllListeners();
	}
	
	protected void notifyReaders() {
		synchronized(_rListeners) {
			Iterator<INIO_R_Listener> rIt = _rListeners.iterator();
			while(rIt.hasNext()) {
				INIO_R_Listener rListener = rIt.next();
				rListener.conInfoUpdated(this);
			}
		}
	}
	
	protected void notifyWriters() {
		synchronized (_wListeners) {
			Iterator<INIO_W_Listener> wIt = _wListeners.iterator();
			while(wIt.hasNext()) {
				INIO_W_Listener wListener = wIt.next();
				wListener.conInfoUpdated(this);
			}
		}
	}
	
	protected void notifyListeners() {
		synchronized (_listeners) {
			Iterator<INIOListener> it = _listeners.iterator();
			while(it.hasNext()) {
				INIOListener listener = it.next();
				listener.conInfoUpdated(this);
			}
		}
	}
	
	protected void notifyAllListeners() {
		notifyListeners();
		notifyReaders();
		notifyWriters();
	}
	
	void makeConnecting() {
		synchronized(_session) {
			if(this._state == ConnectionInfoNonListeningStatus.NL_CONNECTING)
				return;
			setState(ConnectionInfoNonListeningStatus.NL_CONNECTING);
		}
		notifyAllListeners();
	}

	@Override
	public ConnectionInfoNonListeningStatus getStatus() {
		return _state;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return _chInfoNL.getRemoteAddress();
	}

	@Override
	public IRawPacket getNextIncomingData() {
		IRawPacket raw;
		synchronized( _incoming) {
			if(_incomingQSize == 0)
				return null;
			raw = _incoming.remove();
			_incomingQSize--;
			if(Broker.CORRELATE) {
				if(raw.getType() == PacketableTypes.TMULTICAST) {
					TMulticast tm = (TMulticast) PacketFactory.unwrapObject(_brokerShadow, raw);
					TrafficCorrelator.getInstance().connectionInDequeue(tm, this);
				}
			}
			
			if(_incomingQSize == CONNECTION_MAX_INCOMING_QUEUE_SIZE - 1) {
				if(_isReaderReady == false) {
					_isReaderReady = true;
					_chInfoNL.getNIOBinding().updateReaderCount(1);
					SelectionKey key = _chInfoNL.getKey();
					
					if(key == null)
						throw new IllegalStateException("Key is null for connection: " + this);
					_chInfoNL.getNIOBinding().interestedInReads(key);
					
				}
			}
		}
		return raw;
	}
	
	@Override
	public INIOBinding getNIOBinding() {
		return _chInfoNL.getNIOBinding();
	}

	boolean addIncomingDataToList(IRawPacket raw) {
		if(raw == null)
			return false;
		
		_session.assertOrderedSequence(raw.getSeq());
		
		TMulticastAnnotatorFactory.getTMulticast_Annotator().
						annotate(_localSequencer, raw, AnnotationEvent.ANNOTATION_EVENT_MESSAGE_RECEIVED, "Message is received");

		synchronized (_incoming) {
			if(Broker.DEBUG)
				LoggerFactory.getLogger().debug(this, "NIO-R(" + _session + "/" + _incomingQSize + "=" + _incoming.size() + "): " + raw.getQuickString());

			if(_trafficLogger != null)
				_trafficLogger.logIncoming(_session, raw);
			
			if(_incomingQSize == CONNECTION_MAX_INCOMING_QUEUE_SIZE)
				return false;
			if(_incomingQSize == CONNECTION_MAX_INCOMING_QUEUE_SIZE - 1) {
				NIOBinding myNIOBinding = _chInfoNL.getNIOBinding();
				if(_isReaderReady == true) {
					_isReaderReady = false;
					myNIOBinding.updateReaderCount(-1);
					myNIOBinding.uninterestedInReads(_chInfoNL.getKey());
				}
			}
			raw.getHeader().rewind();
			raw.getBody().rewind();
			_incoming.add(raw);
			_incomingQSize++;
			if(Broker.CORRELATE) {
				if(raw.getType() == PacketableTypes.TMULTICAST) {
					TMulticast tm = (TMulticast) PacketFactory.unwrapObject(_brokerShadow, raw);
					TrafficCorrelator.getInstance().connectionInEnqueue(tm, this);
				}
			}

			if(_incomingQSize != 1)
			{
				if(_incomingQSize >= CONNECTION_INCOMING_QUEUE_NOTIFYING_LIMIT)
					if(++incoming_notifier_counter % CONNECTION_INCOMING_FULL_YIELD_COUNTER == 0)
						Thread.yield();
				return true;
			}
		}
		
		synchronized ( _rListeners)
		{
			Iterator<INIO_R_Listener> rIt = _rListeners.iterator();
			while(rIt.hasNext()) {
				INIO_R_Listener rL = rIt.next();
				rL.conInfoGotFirstDataItem(this);
			}
		}
		
		return true;
	}
	
	boolean isReadyForWrite() {
		return _isWriterReady;
	}
	
	boolean isReadyForRead() {
		return _isReaderReady;
	}
	
//	boolean isReadyForConnect() {
//		return _chInfoNL._channel.isConnectionPending();
//	}

	@Override
	public abstract boolean isConnected();
	@Override
	public abstract boolean isConnecting();
	
	IRawPacket removeOutgoingDataFromList(SelectionKey key) {
		boolean notifyWriters = false;
		IRawPacket raw = null;
		
		synchronized(_outgoing) {
			if(_outgoingQSize == 0) {
				NIOBinding myNIOBinding = _chInfoNL.getNIOBinding();
				if(_isWriterReady == true) {
					_isWriterReady = false;
					myNIOBinding.updateWriterCount(-1);
					_chInfoNL.getNIOBinding().uninterestedInWrites(key);
				}
				return null;
			}
			
			raw = _outgoing.remove();
			_outgoingQSize--;
			if(Broker.CORRELATE) {
				if(raw.getType()==PacketableTypes.TMULTICAST) {
					TMulticast tm = (TMulticast) PacketFactory.unwrapObject(_brokerShadow, raw);
					TrafficCorrelator.getInstance().connectionOutDequeue(this, tm);
				}
			}
			
			if(_outgoingQSize <= CONNECTION_OUTGOING_QUEUE_NOTIFYING_LIMIT) { //MAX_OUTGOING_QUEUE_SIZE - 1) {
				notifyWriters = true;
			}
		}
		
		if(notifyWriters == true) {
			synchronized (_wListeners) {
				Iterator<INIO_W_Listener> it = _wListeners.iterator();
				while(it.hasNext())
					it.next().conInfoGotEmptySpace(this);
			}	
		}

		TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(_localSequencer, raw, AnnotationEvent.ANNOTATION_EVENT_MESSAGE_SENT, "Message is being sent");
		raw.lockChange();
		
		return raw;
	}
	
	protected final boolean sendPacket(INIOBinding nioBinding, IRawPacket raw) throws IllegalArgumentException {
		synchronized(_outgoing) {
			if(!isConnected())
				return false;
			NIOBinding myNIOBinding = _chInfoNL.getNIOBinding();
			if(myNIOBinding != nioBinding)
				throw new IllegalArgumentException("ConnectionInfo::sendPacket(.) conInfo '" + this + "' does not belong to this NIOBinding '" + nioBinding + "'");
			
			if(raw.getPacketPriority() == RawPacketPriority.High) {
				_outgoing.add(0, raw);
				_outgoingQSize++;
			} else if(_outgoingQSize >= CONNECTION_MAX_OUTGOING_QUEUE_SIZE) {
				return false;
			} else {
				_outgoing.add(raw);
				_outgoingQSize++;
				if(Broker.CORRELATE) {
					if(raw.getType()==PacketableTypes.TMULTICAST) {
						TMulticast tm = (TMulticast) PacketFactory.unwrapObject(_brokerShadow, raw);
						TrafficCorrelator.getInstance().connectionOutEnqueue(this, tm);
					}
				}
			}
			
			if(_outgoingQSize == 1) {
				// This has been the first packet to sent, make the channel OP_WRITE
				if(_isWriterReady == false) {
					_isWriterReady = true;
					myNIOBinding.interestedInWrites(_chInfoNL.getKey());
					myNIOBinding.updateWriterCount(1);
				}
			}
		}
		return true;
	}

	@Override
	public boolean isListening() {
		return false;
	}
	
	@Override
	public String toString() {
		return toStringShort();// + " {" + _lastUpdateSequence + "}";
	}
	
	public String toStringShort() {
		if(_session != null)
		{
			InetSocketAddress remote = _session.getRemoteAddress();
			if(remote != null)
				return "ConInfoNL##" + _counterId + ":(" + remote.toString().substring(1) + ")@" + _state + "<" + _incomingQSize + "/" + _outgoingQSize + ">";
		}
		
		return "ConInfoNL: (" + _chInfoNL.getRemoteAddress().getPort() + ") @" + _state + "<" + _incomingQSize + "/" + _outgoingQSize + ">";
	}


	@Override
	public void registerInterestedReadingListener(INIO_R_Listener rListener) {
		synchronized (_rListeners) {
			rListener.becomeMyReaderListener(this);
			_rListeners.add(rListener);
		}
	}

	@Override
	public void registerInterestedWritingListener(INIO_W_Listener wListener) {
		wListener.becomeMyWriteListener(this);
		_wListeners.add(wListener);
	}

	@Override
	public void registerInterestedListener(INIOListener listener) {
		synchronized(_listeners) {
			listener.becomeMyListener(this);
			_listeners.add(listener);
		}
	}
	
	@Override
	public ChannelInfo<T> getChannelInfo() {
		return _chInfoNL;
	}
	
	protected boolean updateIsReaderReady() {
		if(_incomingQSize != ConInfoNonListening.CONNECTION_MAX_INCOMING_QUEUE_SIZE)
			_isReaderReady = true;
		else
			_isReaderReady = false;
		
		return _isReaderReady;
	}
	
	protected boolean updateIsWriterReady() {
		if(_outgoingQSize != 0)
			_isWriterReady = true;
		else
			_isWriterReady = false;
		
		return _isWriterReady;
	}
	
	@Override
	public int getIncomingQSize() {
		return _incomingQSize;
	}
	
	@Override
	public int getOutgoingQSize() {
		return _outgoingQSize;
	}

	@Override
	public void deregisterUninterestedReadingListener(INIO_W_Listener wListener) {
		synchronized (_wListeners) {
			_wListeners.remove(wListener);
		}		
	}

	@Override
	public void deregisterUnnterestedReadingListener(INIO_R_Listener rListener) {
		synchronized (_rListeners) {
			_rListeners.remove(rListener);
		}
	}

	@Override
	public ISession getSession() {
		return _session;
	}
	
	@Override
	public int hashCode() {
		return _chInfoNL.getRemoteAddress().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(this.getClass().isAssignableFrom(obj.getClass())) {
			InetSocketAddress objAddr = ((ConInfoNonListening<?>)obj).getRemoteAddress();
			InetSocketAddress thisAddr = getRemoteAddress();
			return thisAddr.equals(objAddr);
		}

		return false;
	}
	
	@Override
	public boolean isCancelled() {
		return _state == ConnectionInfoNonListeningStatus.NL_CANCELLED;
	}

	@Override
	public void switchListeners(INIOListener newListener, INIOListener oldListener) {
		if(_listeners.remove(oldListener))
			_listeners.add(newListener);
		
		if(_rListeners.remove(oldListener))
			_rListeners.add((INIO_R_Listener)newListener);
		
		if(_wListeners.remove(oldListener))
			_wListeners.add((INIO_W_Listener)newListener);
	}
	
	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_CON_NL;
	}

	public String toStringFull() {
		return toString() + "{" + _listeners + "/" + _rListeners + "/" + _wListeners + "}";
	}

	@Override
	public String toStringLong() {
		InetSocketAddress remote = _session.getRemoteAddress();
		return "ConInfoNL-" + ((remote==null)?"null":remote.getPort()) + ":(" + _incomingQSize + "/" + _outgoingQSize + ")";
	}

	@Override
	public int getCounterId() {
		return _counterId;
	}
	
	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}
}
