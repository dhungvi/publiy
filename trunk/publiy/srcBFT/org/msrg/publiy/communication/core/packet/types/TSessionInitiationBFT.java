package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class TSessionInitiationBFT extends TSessionInitiation {

	public TSessionInitiationBFT(LocalSequencer localSequencer,
			InetSocketAddress remoteCC, SessionInitiationTypes type,
			Sequence lastSequenceFromOtherEndPoint) {
		super(localSequencer, remoteCC, type, lastSequenceFromOtherEndPoint);
	}

	public TSessionInitiationBFT(ByteBuffer buff) {
		super(buff);
	}
	
	public TSessionInitiationBFT(LocalSequencer localSequencer,
			InetSocketAddress remoteCC, SessionInitiationTypes type,
			boolean isActiveLocally, Sequence lastSequenceFromOtherEndPoint) {
		super(localSequencer, remoteCC, type, isActiveLocally,
				lastSequenceFromOtherEndPoint);
	}
	
	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TSESSIONINITIATIONBFT;
	}
}
