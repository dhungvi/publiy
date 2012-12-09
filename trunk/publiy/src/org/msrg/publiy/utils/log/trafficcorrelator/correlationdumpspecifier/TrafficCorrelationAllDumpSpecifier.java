package org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier;

import java.nio.ByteBuffer;

import org.msrg.publiy.communication.core.packet.IPacketListener;

class TrafficCorrelationAllDumpSpecifier extends TrafficCorrelationDumpSpecifier {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = 5877750460507797433L;

	protected TrafficCorrelationAllDumpSpecifier(String comment, boolean merge) {
		super(TrafficCorrelationDumpSpecifierType.DUMP_ALL, comment, merge);
	}
	
	protected TrafficCorrelationAllDumpSpecifier(ByteBuffer buff) {
		super(buff);
	}

	@Override
	public IPacketListener getPacketListener() {
		return null;
	}
};