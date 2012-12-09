package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;
import java.util.Set;

import org.msrg.publiy.pubsub.core.IOverlayManager;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;

public class TBFTMessageManipulator_TamperPublication extends
		AbstractThreeCriteriaTBFTMessageManipulator {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -6320964091347203443L;

	protected static final String CMD = "TAMPER";
	
	public TBFTMessageManipulator_TamperPublication(
			int dropCount, Set<InetSocketAddress> affectRemotes,
			Set<InetSocketAddress> affectSources,
			Set<InetSocketAddress> affectSubscribers) {
		super(TCommandTypes.CMND_BFT_PUBLICATION_MANIPULATE_TAMPER,
				CMD, dropCount, affectRemotes, affectSources, affectSubscribers);
	}

	@Override
	public IRawPacket manipulatePrivately(IRawPacket checkedOutRaw,
			ISession session, IOverlayManager overlayManager) {
		// TODO:
		return null;
	}

}
