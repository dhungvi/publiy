package org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier;

import java.nio.ByteBuffer;

import org.msrg.publiy.communication.core.packet.IPacketListener;

public class TrafficCorrelationSessionEventsDumpSpecifier extends TrafficCorrelationDumpSpecifier {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = 7239279083229284136L;

	protected TrafficCorrelationSessionEventsDumpSpecifier(String comment) {
		super(TrafficCorrelationDumpSpecifierType.DUMP_SESSIONS_EVENTS, comment, false);
	}
	
	protected TrafficCorrelationSessionEventsDumpSpecifier(ByteBuffer buff) {
		super(buff);
	}

	@Override
	public IPacketListener getPacketListener() {
		return null;
	}
}
