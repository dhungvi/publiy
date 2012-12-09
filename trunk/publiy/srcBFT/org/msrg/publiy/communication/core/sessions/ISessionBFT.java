package org.msrg.publiy.communication.core.sessions;

import java.net.InetSocketAddress;
import java.util.List;

import org.msrg.publiy.communication.core.packet.IRawPacket;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;

import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBFTSuspectable;
import org.msrg.publiy.broker.IBFTSuspectedRepo;
import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.IBFTVerifierProxy;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class ISessionBFT extends ISession implements IBFTSuspectable {

	protected final IBFTBrokerShadow _brokerShadow;
	protected final IBFTSuspectedRepo _suspectedRepo;
	protected final IBFTVerifierProxy _vProxy;
	protected final IBFTIssuerProxy _iProxy;
	
	protected ISessionBFT(IBFTBrokerShadow brokerShadow, IBFTVerifierProxy vProxy, IBFTIssuerProxy iProxy, SessionTypes type) {
		this(brokerShadow, vProxy, iProxy, SessionObjectTypes.ISESSION_BFT, type);
	}
	
	protected ISessionBFT(IBFTBrokerShadow brokerShadow, IBFTVerifierProxy vProxy, IBFTIssuerProxy iProxy, SessionObjectTypes sessionObjectType, SessionTypes type) {
		super(brokerShadow, sessionObjectType, type);
		
		_brokerShadow = brokerShadow;
		_suspectedRepo = _brokerShadow.getBFTSuspectedRepo();
		_vProxy = vProxy;
		_iProxy = iProxy;
	}

	@Override
	public boolean isSuspected() {
		if(_remote == null)
			throw new IllegalStateException();
		
		return _suspectedRepo.isSuspected(_remote);
	}

	@Override
	public InetSocketAddress getAddress() {
		return _remote;
	}

	@Override
	public List<BFTSuspecionReason> getReasons() {
		if(_remote == null)
			throw new IllegalStateException();

		return _suspectedRepo.getReasons(_remote);
	}
	
	@Override
	public Sequence getLastReceivedOrConfirmedLocalSequenceAtOtherEndPoint() {
		return null;
	}
	
	@Override
	public void setLastReceivedSequence(Sequence lastReceivedSequence) {
		throw new UnsupportedOperationException();
	}
}
