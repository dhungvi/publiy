package org.msrg.publiy.broker.info;

import java.net.InetSocketAddress;

import org.msrg.publiy.pubsub.core.messagequeue.PSSession;

public class PSSessionInfo extends IBrokerInfo {
	
	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = -4461784281166678313L;
	private ISessionInfo _sessionInfo;
	
	public PSSessionInfo(PSSession psSession, InetSocketAddress localAddress){
		super(BrokerInfoTypes.BROKER_INFO_PS_SESSIONS);
		_sessionInfo = (ISessionInfo) psSession.getISession().getInfo();
	}
	
	public ISessionInfo getISessionInfo(){
		return _sessionInfo;
	}
	
	@Override
	protected String toStringPrivately(){
		return _sessionInfo.getLocalAddress() + "-" + _sessionInfo.getRemoteAddress() + "_" + _sessionInfo.getSessionConnectionType() + "_" + _sessionInfo.getSessionType();
	}
	
}
