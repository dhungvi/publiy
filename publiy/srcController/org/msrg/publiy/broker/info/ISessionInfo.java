package org.msrg.publiy.broker.info;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionLocal;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.communication.core.sessions.SessionTypes;

public class ISessionInfo extends IBrokerInfo {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = -5251973721341139042L;
	private InetSocketAddress _localAddress;
	private InetSocketAddress _remoteAddress;
	private String _retryString;
	private SessionTypes _sessionType;
	private SessionConnectionType _sessionConnectionType;
	private Sequence _lastReceivedOrConfirmedLocalSequenceAtOtherEndPoint;
	
	public ISessionInfo(ISession session, InetSocketAddress localAddress) {
		super(BrokerInfoTypes.BROKER_INFO_SESSIONS);
		_localAddress = localAddress;
		_remoteAddress = session.getRemoteAddress();
		_retryString = session.getRetryString();
		_sessionType = session.getSessionType();
		if ( !session.isLocal() )
			_lastReceivedOrConfirmedLocalSequenceAtOtherEndPoint = session.getLastReceivedOrConfirmedLocalSequenceAtOtherEndPoint();
		_sessionConnectionType = session.getSessionConnectionType();
	}
	
	public ISessionInfo(ISessionLocal localSession, InetSocketAddress localAddress){
		super(BrokerInfoTypes.BROKER_INFO_SESSIONS);
		_localAddress = localAddress;
		_remoteAddress = localSession.getRemoteAddress();
		_retryString = "INVALID";
		_sessionType = localSession.getSessionType();
		_lastReceivedOrConfirmedLocalSequenceAtOtherEndPoint = null;
		_sessionConnectionType = localSession.getSessionConnectionType();
	}
	
	public SessionTypes getSessionType(){
		return _sessionType;
	}
	
	public SessionConnectionType getSessionConnectionType(){
		return _sessionConnectionType;
	}
	
	public InetSocketAddress getLocalAddress(){
		return _localAddress;
	}
	
	public InetSocketAddress getRemoteAddress(){
		return _remoteAddress;
	}
	
	public Sequence getLastReceivedOrConfirmedLocalSequenceAtOtherEndPoint(){
		return _lastReceivedOrConfirmedLocalSequenceAtOtherEndPoint;
	}
	
	public String getRetryString(){
		return _retryString;
	}
	
	@Override
	protected String toStringPrivately() {
		return getLocalAddress() + "-" + getRemoteAddress() + ":" + getSessionConnectionType() + ":" + getSessionType() + "[" + getRetryString() + "]";
	}

	public boolean isPrimaryActive() {
		return _sessionConnectionType._isPrimaryActive;
	}
	
	public boolean isBeingConnected() {
		return _sessionConnectionType._isConnecting;
	}

}
