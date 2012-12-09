package org.msrg.publiy.communication.core.packet.types;

import java.nio.ByteBuffer;

import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.pubsub.core.IOverlayManager;

import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;

public abstract class TBFTMessageManipulator extends TCommand {

	/**
	 * Auto generated.
	 */
	private static final long serialVersionUID = 1271687751682570936L;

	public TBFTMessageManipulator(TCommandTypes type, String comments) {
		super(type, comments);
	}

	public abstract IRawPacket manipulate(IRawPacket checkedOutRaw, ISession session, IOverlayManager overlayManager);
	
	public static TBFTMessageManipulator decode(String str, BrokerIdentityManager idMan) {
		if(str == null)
			return null;
		
		String[] parts = str.split(" ");
		if(parts.length == 0)
			return null;
		
		String cmd = parts[0];
		if(cmd.equals(TBFTMessageManipulator_FilterPublication.getStaticCommand()))
			return TBFTMessageManipulator_FilterPublication.decode(str, idMan);
		
		throw new UnsupportedOperationException("Unknown command: " + str);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer bb, int bbOffset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getContentSize() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void annotate(String annotation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAnnotations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Sequence getSourceSequence() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PacketableTypes getObjectType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IPacketListener getPacketListener() {
		throw new UnsupportedOperationException();
	}

	public abstract String toString(BrokerIdentityManager _idMan);
	
}
