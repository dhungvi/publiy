package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IHasBrokerInfo;

import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.PSSessionInfo;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;
import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationDumpSpecifier;
import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationMessageDumpSpecifier;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionLocal;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;

public class PSSession implements IHasBrokerInfo<PSSessionInfo>, ILoggerSource {
	
	protected final LocalSequencer _localSequencer;
	protected IConnectionManager _connectionManager;
	protected final InetSocketAddress _remote;
	protected MessageQueue _mQ;
	protected IMessageQueueNode _lastProcessedMQNode;
	protected IMessageQueueNode _nextProcessMQNode;
	protected ISession _session;
	
	@Override
	public String toString() {
		return "PSSession<<" + _session + ">>";
	}
	
	protected final void setConnectionManager(IConnectionManager connectionManager) {
		_connectionManager = connectionManager;
	}
	
	public String lastProcessedMQNodeString() {
		return (_lastProcessedMQNode==null?"NULL":_lastProcessedMQNode.toString());
	}
	
	public InetSocketAddress getRemoteAddress() {
		return _session.getRemoteAddress();
	}
	
	public ISession getISession() {
		return _session;
	}
	
	public void setISession(ISession newSession) {
		_session = newSession;
	}
	
	public SessionConnectionType getSessionConnectionType() {
		return _session.getSessionConnectionType();
	}
	
	protected PSSession(ISessionLocal localSession, MessageQueue mq) {
		_mQ = mq;
		_remote = localSession.getRemoteAddress();
		setConnectionManager(mq._connectionManager);
		_localSequencer = mq._localSequencer;
		if(_remote == null)
			throw new NullPointerException("Remote is null: " + this);
	}
	
	protected PSSession(ISession session, MessageQueue mQ, IOverlayManager overlayManager) {
		_mQ = mQ;
		_localSequencer = _mQ._localSequencer;
		setConnectionManager(_mQ._connectionManager);
		_remote = session.getRemoteAddress();
		if(_remote == null)
			throw new NullPointerException("Remote is null: " + this);
		resetSession(session, true);
	}
	
	void swapMessageQueue(MessageQueue newMQ, IOverlayManager overlayManager) {
		synchronized (_mQ) {
			_mQ = newMQ;
			setConnectionManager(_mQ._connectionManager);
			resetSession(_session, false);
		}
	}
	
	void resetSession(ISession session, boolean initializeMQNode) {
		switch(session.getSessionConnectionType()) {
		case S_CON_T_ACTIVE:
		case S_CON_T_UNJOINED:
		case S_CON_T_CANDIDATE:
		case S_CON_T_DELTA_VIOLATED:
		case S_CON_T_SOFT:
			break;
			
		default:
			throw new IllegalStateException("Session '" + session + "' cannot be associated with a PSSession since it is '" + session.getSessionConnectionType() + "'.");
		}

		try {
			resetSessionPrivately(session, initializeMQNode);
		} catch(IllegalStateException sEx) {
			if(Broker.CORRELATE) {
				TrafficCorrelationDumpSpecifier dumpSpecifier =
					TrafficCorrelationMessageDumpSpecifier.getAllCorrelationDump("FATAL:" + sEx, false);
				TrafficCorrelator.getInstance().dump(dumpSpecifier, true);
			}
			throw sEx;
		}
	}
	
	void resetSessionPrivately(ISession session, boolean initializeMQNode) {
		if(!Broker.RELEASE)
			LoggerFactory.getLogger().info(this, "Resetting session[" + initializeMQNode + "]: " + session + ":" + _lastProcessedMQNode);
			
		if(_proceedInProgress == true)
			throw new IllegalStateException();
		
		if(!_remote.equals(session.getRemoteAddress()))
			throw new IllegalArgumentException("Remote cannot be changed: " + this + " vs. " + session);
			
		_session = session;
		Sequence lastSequenceAtOtherEndPoint = session.getLastReceivedOrConfirmedLocalSequenceAtOtherEndPoint();
		
		int index = 0;
		// TODO: URGENT! look at below
		if(initializeMQNode)
		{
			IMessageQueueNode lastMQNodeReceivedAtEndPoint =
				_mQ.getBehindHead(lastSequenceAtOtherEndPoint, _session, true);
			if(lastMQNodeReceivedAtEndPoint == null) {
				if(_lastProcessedMQNode != null)
					cancel();
				setLastProcessMQNode(null);
				setNextProcessMQNode(_mQ.getRealHead());
				index = 1;
			} else {
				if(_lastProcessedMQNode == null) {
//					IMessageQueueNode head = _mQ.getRealHead();
//					while (head!=null && head!=lastMQNodeReceivedAtEndPoint) {
//						head.passThrough(_remote);
//						head = head.getNext();
//					}
//					lastMQNodeReceivedAtEndPoint.passThrough(_remote);
					
					setLastProcessMQNode(lastMQNodeReceivedAtEndPoint);
					setNextProcessMQNode(_lastProcessedMQNode==null?_mQ.getRealHead():_lastProcessedMQNode.getNext());
					index = 2;
				} else {
					if(lastSequenceAtOtherEndPoint.succeeds(_lastProcessedMQNode.getNodeSequence())) {
						while (_lastProcessedMQNode.getNext()!=null &&
								lastSequenceAtOtherEndPoint.succeeds(_lastProcessedMQNode.getNext().getNodeSequence())) {
							if(Broker.CORRELATE)
								TrafficCorrelator.getInstance().sessionVisited(session, _lastProcessedMQNode.getTMulticast(), _mQ.getPSSessionSize());
							_lastProcessedMQNode.getNext().passThrough(_remote);
							setLastProcessMQNode(_lastProcessedMQNode.getNext());
						}
						setNextProcessMQNode(_lastProcessedMQNode.getNext());
						index = 3;
					} else {
						IMessageQueueNode tmpNode = lastMQNodeReceivedAtEndPoint.getNext();
						while (tmpNode != null &&
								_lastProcessedMQNode.getNodeSequence().succeedsOrEquals(tmpNode.getNodeSequence())) {
							
							if(Broker.CORRELATE)
								TrafficCorrelator.getInstance().sessionCancelled(_session, tmpNode.getTMulticast(), _mQ.getPSSessionSize());
							
							tmpNode.cancelCheckoutVisited(_remote);
							tmpNode = tmpNode.getNext();
						}
						setLastProcessMQNode(lastMQNodeReceivedAtEndPoint);
						setNextProcessMQNode(_lastProcessedMQNode==null?_mQ.getRealHead():_lastProcessedMQNode.getNext());
						index = 4;
					}
				}
			}
		}
		
		if(!Broker.RELEASE)
			LoggerFactory.getLogger().info(this, "Usingg last[" + lastSequenceAtOtherEndPoint + "]" +
					session + " is reset to(" + index + "): " + _nextProcessMQNode +
					" \n \t real head is: " + _mQ.getRealHead());
	}
	
	private void setNextProcessMQNode(IMessageQueueNode nextProcessMQNode) {
		// Perform sanity checks.
		if(!Broker.RELEASE && nextProcessMQNode!=null) {
			if(!nextProcessMQNode.isConfirmed() &&
				((MessageQueueNode)nextProcessMQNode).visitedSetContains(_remote)) {
				if(Broker.CORRELATE) {
					TrafficCorrelationDumpSpecifier dumpSpecifier =
						TrafficCorrelationDumpSpecifier.getMessageCorrelationDump(nextProcessMQNode.getTMulticast(), "FATAL:Went_Wrong", false);
					TrafficCorrelator.getInstance().dump(dumpSpecifier, true);
				}
			throw new IllegalStateException(_remote.toString() + ":: " + this.toString() + ":: " + nextProcessMQNode + " vs. " + _nextProcessMQNode);
			}
		}
		_nextProcessMQNode = nextProcessMQNode;
	}

	private void setLastProcessMQNode(IMessageQueueNode lastProcessedMQNode) {
		if(!Broker.RELEASE && lastProcessedMQNode!=null && !lastProcessedMQNode.isConfirmed() && !lastProcessedMQNode.visitedBy(_remote))
			throw new IllegalStateException(this + " vs. " + lastProcessedMQNode);

		if(!Broker.RELEASE && lastProcessedMQNode!=null) {
			if(!lastProcessedMQNode.isConfirmed() &&
					!((MessageQueueNode)lastProcessedMQNode).visitedSetContains(_remote))
				throw new IllegalStateException("Not seen this one.." + _session + "\t " + lastProcessedMQNode);
		
			IMessageQueueNode head = _mQ.getRealHead();
			while (false &&
					head!=null && lastProcessedMQNode.getNodeSequence().succeeds(head.getNodeSequence())) {
				if(!head.isConfirmed() &&
						!((MessageQueueNode)head).visitedSetContains(_remote))
					throw new IllegalStateException("Not seen this one.." + _session + "\t " + head);
				
				head = head.getNext();
			}
		}
			
		_lastProcessedMQNode = lastProcessedMQNode;
		
	}
	
	boolean advance() {
		if(!_session.isRealSessionTypePubSubEnabled() && _session.getSessionConnectionType() != SessionConnectionType.S_CON_T_DELTA_VIOLATED)
			return false;
		
		if(_proceedInProgress == false)
			throw new IllegalStateException("_proceedInProgress is false!" + this);
		
		if(!_session.shouldMoveAlongTheMQ())
			return false;
		
		if(_nextProcessMQNode==null) {
			if(_lastProcessedMQNode == null) {
				IMessageQueueNode lastMQNode = _mQ.getBehindHead(
						_session.getLastReceivedOrConfirmedLocalSequenceAtOtherEndPoint(), _session, false);
				setLastProcessMQNode(lastMQNode);
				if(_lastProcessedMQNode == null)
					setNextProcessMQNode(_mQ.getRealHead());
				else
					setNextProcessMQNode(_lastProcessedMQNode.getNext());
			}
			else
				setNextProcessMQNode(_lastProcessedMQNode.getNext());
		}
		
		if(_nextProcessMQNode==null)
			return false;
		
		try{
			IRawPacket raw = _nextProcessMQNode.checkOutAndmorph(_session, _session.isPeerRecovering());
	
			if(Broker.DEBUG)
				LoggerFactory.getLogger().debug(this, "" + (raw==null?"NO ":"YES ") + (_nextProcessMQNode.isConfirmed()?"C ":"NC ") + _nextProcessMQNode.getTMulticast() + " was checked out by:" + _session);
	
			boolean ret = true;
			if(raw == null) {
				if(Broker.CORRELATE)
					TrafficCorrelator.getInstance().sessionVisited(_session, _nextProcessMQNode.getTMulticast(), _mQ.getPSSessionSize());
				
				_nextProcessMQNode.checkForConfirmation();
			} else {
				if(Broker.CORRELATE)
					TrafficCorrelator.getInstance().sessionCheckedout(_session, _nextProcessMQNode.getTMulticast(), _mQ.getPSSessionSize());
				
				boolean bufferFull = send(raw);
				if(bufferFull)
					ret = false;
			}
			
			if(Broker.DEBUG)
				LoggerFactory.getLogger().debug(this, _remote.toString() + 
					"[" + _nextProcessMQNode.getConfirmationStatus() + "] checked out: " + (raw==null?"":raw.getQuickString()) + 
					" \tOriginalMSG: " + _nextProcessMQNode.getTMulticast());
			
			setLastProcessMQNode(_nextProcessMQNode);
			setNextProcessMQNode(_nextProcessMQNode.getNext());
			
			if(_nextProcessMQNode==null)
				return ret && false; // which is false!
			else
				return ret || true;	// which is true
		}catch (IllegalStateException x) {
			System.out.println("NextNodeForThisSession: " +
					 (_nextProcessMQNode==null?null:_nextProcessMQNode.getMQNSequence()));
			System.out.println("LastNodeForThisSession: " +
					 (_lastProcessedMQNode==null?null:_lastProcessedMQNode.getMQNSequence()));
			throw x;
		}
	}
	
	@Override
	public int hashCode() {
		return _session.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!this.getClass().isInstance(obj))
			return false;
		
		PSSession pssObj = (PSSession) obj;
		
		if(pssObj._remote.equals(this._remote))
			return true;
		
		return false;
	}
	
	public boolean send(IRawPacket raw) {
		return _connectionManager.getSessionManager().send(_session, raw);
	}
	
	void cancel() {
		int cancelationCount = cancelPrivately();
		setLastProcessMQNode(null);
		setNextProcessMQNode(null);

		checkCancellation(cancelationCount);
		LoggerFactory.getLogger().debug(this, "Cancelled #" + cancelationCount + " check out of: " + this);
	}
	
	void checkCancellation(int cancelationCount) {
		if(Broker.RELEASE)
			return;
		IMessageQueueNode head = _mQ.getRealHead();
		Sequence lastReceivedOrConfirmed = _session.getLastReceivedOrConfirmedLocalSequenceAtOtherEndPoint();
		IMessageQueueNode prev = _mQ.getBehindHead(lastReceivedOrConfirmed, _session, false);
		int distanceFromHead = 0;
		
		try{
			while ( head!=null) {
				head.cancelCheckoutUnVisited(_remote);
				head = head.getNext();
				distanceFromHead++;
			}
		}catch(IllegalStateException isEx) {
			int distanceFromTail = 0, visited=0;
			while ( head!=null) {
				if(head.visitedBy(_remote))
					visited++;
				distanceFromTail++;
				head = head.getNext();
			}
			LoggerFactory.getLogger().infoX(this, isEx, "ERRROR[" + cancelationCount + "," + distanceFromHead + ", " + distanceFromTail + "(" + visited + ")"
					+ "]: lastReceivedOrConfirmed: " + lastReceivedOrConfirmed + ", getBehindHead:" + prev);
			throw isEx;
		}
	}
	
	int cancelPrivately() {
		if(_lastProcessedMQNode == null)
			return -2;
		
		int cancelationCount = 0;
		IMessageQueueNode head = _mQ.getRealHead();
		while (head != null &&
				_lastProcessedMQNode.getNodeSequence().succeedsOrEquals(head.getNodeSequence())) {
			if(Broker.CORRELATE)
				TrafficCorrelator.getInstance().sessionCancelled(_session, head.getTMulticast(), _mQ.getPSSessionSize());
			
			head.cancelCheckoutVisited(_remote);
			head = head.getNext();
			if(head == _lastProcessedMQNode) {
				if(Broker.CORRELATE)
					TrafficCorrelator.getInstance().sessionCancelled(_session, head.getTMulticast(), _mQ.getPSSessionSize());
				
				head.cancelCheckoutVisited(_remote);
				break;
			}
		}
		
		final Sequence lastProcessedSequence = _lastProcessedMQNode.getNodeSequence();
		if(!Broker.RELEASE) {
			try{
				IMessageQueueNode next = _lastProcessedMQNode.getNext(); // _mQ.getBehindHead(lastReceivedOrConfirmed, _session, false);
				while ( next!=null) {
					next.cancelCheckoutUnVisited(_remote);
					next = next.getNext();
				}
			}catch(IllegalStateException ilEx) {
				LoggerFactory.getLogger().infoX(this, ilEx, "ERROR FOR: " + _session + " LAST_PROCESSED: " + lastProcessedSequence);
				throw ilEx;
			}
		}
		
		return cancelationCount;
	}

	@Override
	public PSSessionInfo getInfo() {
		PSSessionInfo psSessionInfo = new PSSessionInfo(this, _session.getLocalAddress());
		return psSessionInfo;
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_PSSESSION;
	}

	private boolean _proceedInProgress = false;
	public boolean setProceeding(boolean b) {
		boolean oldProceedingValue = _proceedInProgress;
		_proceedInProgress = b;
		
		return oldProceedingValue;
	}

	public boolean isSessionTypePubSubEnabled() {
		return _session.isRealSessionTypePubSubEnabled();
	}

	public void resetSessionOnly(ISession newSession) {
		if(!Broker.RELEASE)
			LoggerFactory.getLogger().info(this, "Resetting session: " + newSession);
					
		if(!_remote.equals(newSession.getRemoteAddress()))
			throw new IllegalArgumentException("Cannot change _remote: " + this + " vs. " + newSession);
		
		Sequence newLastReceivedSequence = null;
		if(_session.mustGoBackInMQ()) 
			newLastReceivedSequence = _session.getLastReceivedOrConfirmedLocalSequenceAtOtherEndPoint();
		else
			newLastReceivedSequence = _localSequencer.getNext();
//		Sequence oldLastReceivedSequence = newSession.getLastReceivedOrConfirmedLocalSequenceAtOtherEndPoint();
//		if(oldLastReceivedSequence != null) {
//			if(newLastReceivedSequence == null)
//				newLastReceivedSequence = oldLastReceivedSequence;
//			else if(oldLastReceivedSequence.succeeds(newLastReceivedSequence))
//				newLastReceivedSequence = oldLastReceivedSequence;
//		}
		_session = newSession;
		_connectionManager.setLastReceivedSequence2(newSession, newLastReceivedSequence, false, false); //true?
		newSession.setLastReceivedSequence(newLastReceivedSequence);
	}

	ISession getSession() {
		return _session;
	}
}
