package org.msrg.publiy.communication.core.sessions;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.pubsub.core.packets.recovery.TempSessionRecoveryDataRepository;

import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.packet.types.TConf_Ack;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;

public class ISessionLocal extends ISession {
	
	protected final InetSocketAddress _localAddress;

	public ISessionLocal(IBrokerShadow brokerShadow, SessionObjectTypes sessionObjectType, SessionTypes type) {	
		super(brokerShadow, sessionObjectType, type);
		
		_remote = _localAddress = brokerShadow.getLocalAddress();
	}

	@Override
	public boolean connectionHasBeenProcessed() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void connectionProcessed() {
		throw new UnsupportedOperationException();
	}
		
	@Override
	public void setIConInfo(IConInfoNonListening<?> conInfoNL) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISession setRemoteCC(InetSocketAddress remoteCC) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void storeSessionInitiationData(TSessionInitiation sInit) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void storeSessionRecoveryData(TRecovery tr) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void storeSessionConfirmedMulticastData(TMulticast tm) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isOutgoing() {
		return false;
	}
	
	@Override
	public String toStringElaborate() {
		return "ISessionLocal (" + _localAddress + ")";
	}
	
	@Override
	public boolean isConnected() {
		return false;
	}
	
	@Override
	public TempSessionRecoveryDataRepository getTempSessionDataRepository() {
		throw new UnsupportedOperationException();
	}
		
	@Override
	public void flushTempRepositoryData() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void flushPendingMessages() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public TempSessionRecoveryDataRepository clearTempRepositoryData() {
		return null;
	}
	
	@Override
	public void setLastReceivedSequence(Sequence seq) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Sequence getLastReceivedOrConfirmedLocalSequenceAtOtherEndPoint() {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized boolean receiveTMulticast_Conf(TMulticast_Conf conf) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public TConf_Ack flushTConf_Acks() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEnded() {
		return true;
	}
	
	@Override
	public boolean isRealSessionTypePubSubEnabled() {
		return false;
	}
	
	@Override
	public boolean shouldMoveAlongTheMQ() {
		return false;
	}
	
	@Override
	public boolean isPeerRecovering() {
		return false;
	}

	@Override
	public boolean isLocal() {
		return true;
	}
}
