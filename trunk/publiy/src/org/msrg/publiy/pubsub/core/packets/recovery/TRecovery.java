package org.msrg.publiy.pubsub.core.packets.recovery;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.pubsub.core.IOverlayManager;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.communication.core.sessions.ISession;

public class TRecovery implements IPacketable {

	public static final int I_SUBTYPE = 0;
	public static final int I_SOUCE_SEQUENCE = I_SUBTYPE + 1;
	public static final int I_CONTENT_SIZE = I_SOUCE_SEQUENCE + Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	public static final int I_CONTENT = I_CONTENT_SIZE + 2;
	
	public static final int TRECOVERY_BASE_SIZE = I_CONTENT;

	// _sourceSequence may not be required if we use a different dup detection.
	protected final Sequence _sourceSequence;
	protected final TRecoveryTypes _subType;// = TRecoveryTypes.T_RECOVERY_UNKNOWN;
	
	public TRecoveryTypes getType() {
		return _subType;
	}
	
	TRecovery(TRecoveryTypes subType, Sequence sourceSequence) {
		_sourceSequence = sourceSequence;
		_subType = subType;
	}
	
	protected TRecovery(ByteBuffer bdy) {
		// bdy is of type TRECOVERY
		byte [] bSeq = new byte[Sequence.SEQ_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_SOUCE_SEQUENCE);
		bdy.get(bSeq);
		_subType = getSubType(bdy);
		_sourceSequence = Sequence.readSequenceFromBytesArray(bSeq);
	}
	
	public static TRecovery getLastTRecoveryJoin(Sequence sourceSequence) {
		return new TRecovery(TRecoveryTypes.T_RECOVERY_LAST_JOIN, sourceSequence);
	}
	
	public static TRecovery getLastTRecoverySubscription(Sequence sourceSequence) {
		return new TRecovery(TRecoveryTypes.T_RECOVERY_LAST_SUBSCRIPTION, sourceSequence);
	}

	public static TRecovery getTRecoveryObject(ByteBuffer bdy) {
		if ( bdy == null )
			return null;
		
		if ( bdy.capacity() < TRECOVERY_BASE_SIZE )
			throw new IllegalArgumentException("TRecovery::getTRecoveryObject - ERROR, buffer is smaller than the base.");
		
		TRecoveryTypes tmType = getSubType(bdy);
		
		switch(tmType) {
		case T_RECOVERY_JOIN:
		{
			return new TRecovery_Join(bdy);
		}
			
		case T_RECOVERY_LAST_JOIN:
		{
//			byte[] bArray = new byte[Sequence.SEQ_IN_BYTES_ARRAY_SIZE];
//			bdy.position(I_SOUCE_SEQUENCE);
//			bdy.get(bArray);
//			Sequence sequence = Sequence.readSequenceFromBytesArray(bArray);
			TRecovery tr_lastJoin = new TRecovery(bdy); //TRecoveryTypes.T_RECOVERY_LAST_JOIN, sequence);
			return tr_lastJoin;
		}
		
		case T_RECOVERY_LAST_SUBSCRIPTION:
		{
//			byte[] bArray = new byte[Sequence.SEQ_IN_BYTES_ARRAY_SIZE];
//			bdy.position(I_SOUCE_SEQUENCE);
//			bdy.get(bArray);
//			Sequence sequence = Sequence.readSequenceFromBytesArray(bArray);
			TRecovery tr_lastSub = new TRecovery(bdy); //TRecoveryTypes.T_RECOVERY_LAST_SUBSCRIPTION, sequence);
			
			return tr_lastSub;
		}
		
		case T_RECOVERY_SUBSCRIPTION:
		{
			return new TRecovery_Subscription(bdy);
		}
		
		default:
			throw new UnsupportedOperationException("TRecovery::getTRecoveryObject - ERROR, unknown type '" + tmType + "'.");
		}
	}

	private static TRecoveryTypes getSubType(ByteBuffer bdy) {
		byte subType = bdy.get(I_SUBTYPE);
		return TRecoveryTypes.getType(subType);
	}
	
	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TRECOVERY;
	}

	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		buff.position(I_SUBTYPE);
		buff.put(_subType.getValue());

		buff.position(I_SOUCE_SEQUENCE);
		buff.put(_sourceSequence.getInByteArray());
	}
	
	@Override
	public String toString() {
		String str = "TRecovery ("+_subType+") @ " + _sourceSequence;
		return str;
	}
	
	public String toStringShort() {
		return toString();
	}
	
	@Override
	public Sequence getSourceSequence() {
		return _sourceSequence;
	}
	
	public TRecoveryTypes getStaticType() {
		return TRecoveryTypes.T_RECOVERY_UNKNOWN;
	}
	
	public InetSocketAddress getSenderAddress() {
		return _sourceSequence.getAddress();
	}
	
	public InetSocketAddress getSourceAddress() {
		return _sourceSequence.getAddress();
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null )
			return false;
		if ( !this.getClass().isInstance(obj) )
			return false;
		TRecovery tmObj = (TRecovery) obj;
		
		if ( tmObj.getSourceSequence().equalsExact(this.getSourceSequence()) )
			return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		int hash = getSourceSequence().hashCodeExact();
		return hash;
	}
	
	public boolean isLastSubscription() {
		return (_subType == TRecoveryTypes.T_RECOVERY_LAST_SUBSCRIPTION );
	}
	
	public boolean isLastJoin() {
		return (_subType == TRecoveryTypes.T_RECOVERY_LAST_JOIN );
	}
	
	public IRawPacket morph(ISession session, IOverlayManager overlayManager) {
		return PacketFactory.wrapObject(overlayManager.getLocalSequencer(), this);
	}

	@Override
	public final int getContentSize() {
		return TRECOVERY_BASE_SIZE + getTRSpecificContentSize();
	}
	
	protected int getTRSpecificContentSize() {
		return 0;
	}

	@Override
	public void annotate(String annotation) {
		return;
	}

	@Override
	public String getAnnotations() {
		return "";
	}
	
	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, int bbOffset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IPacketListener getPacketListener() {
		return null;
	}
}
