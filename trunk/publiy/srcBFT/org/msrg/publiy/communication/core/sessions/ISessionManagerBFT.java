package org.msrg.publiy.communication.core.sessions;

import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.IBFTConnectionManager;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.pubsub.core.overlaymanager.IBFTOverlayManager;

import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.communication.core.niobinding.INIOBinding;
import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;

public class ISessionManagerBFT extends ISessionManagerPS {

	protected final IBFTOverlayManager _bftOverlayManager;

	public ISessionManagerBFT(LocalSequencer localSequencer, INIOBinding nioBinding,
			IBFTConnectionManager bftConnectionManager) {
		super(localSequencer, nioBinding, bftConnectionManager);
		_bftOverlayManager = (IBFTOverlayManager) bftConnectionManager.getOverlayManager();
	}

	@Override
	protected ISession updatePrivately(IBrokerShadow brokerShadow, ISession session, TSessionInitiation sInit) {
		// TODO: change TSessionInitiation to TSessionInitiationBFT
		return super.updatePrivately(brokerShadow, session, sInit);
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_SESSION_MAN_PS;
	}
}
