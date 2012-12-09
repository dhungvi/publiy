package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.utils.Writers;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.communication.core.sessions.ISession;

public class TMulticast implements IPacketable {
	
	public static final int I_SUBTYPE = 0;
	public static final int I_IS_GUIDED = I_SUBTYPE + 1;
	public static final int I_GUIDED_BYTE_SIZE = I_IS_GUIDED + 1;
	
	public static final int I_DELTA_VIOLATING = I_GUIDED_BYTE_SIZE + 4;
	public static final int I_LEGIT_VIOLATING = I_DELTA_VIOLATING + 1;
	public static final int I_LEGIT_VIOLATED = I_LEGIT_VIOLATING + 1;
	
	public static final int I_SOUCE_SEQUENCE = I_LEGIT_VIOLATED + 1;
	public static final int I_SHIFTS = I_SOUCE_SEQUENCE + Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	public static final int I_SEQUENCES = I_SHIFTS + 1;
	public static final int I_SUB_CONTENT_SIZE = I_SEQUENCES + 	(Broker.VSEQ_LENGTH) * Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	public static final int I_SUB_CONTENT = I_SUB_CONTENT_SIZE + Integer.SIZE/8;
	
	public static final int TMULTICAST_BASE_SIZE = I_SUB_CONTENT;

	// _sourceSequence may not be required if we use a different dup detection.
	protected Sequence _sourceSequence;
	protected String _annotations = "";
	
	protected boolean _deltaViolating = false;
	protected boolean _legitimacyViolating = false;
	protected boolean _legitimacyViolated = false;
	
	protected TMulticastTypes _subType = TMulticastTypes.T_MULTICAST_UNKNOWN; 
	protected boolean _isGuided = false;
	protected Set<InetSocketAddress> _guidedInfo;
	protected byte _shifted = 0;
	protected Sequence [] _sequencesVector;
	private Sequence _localSequence;
	
	protected static void duplicate(TMulticast tmSource, TMulticast tmCopy) {
		tmCopy._sourceSequence = tmSource._sourceSequence;
		tmCopy._subType = tmSource._subType;
		tmCopy._shifted = tmSource._shifted;
		tmCopy._sequencesVector = new Sequence[tmSource._sequencesVector.length];
		for(int i=0 ; i<tmCopy._sequencesVector.length; i++)
			tmCopy._sequencesVector[i] = tmSource._sequencesVector[i];
		tmCopy._localSequence = tmSource._localSequence;
		tmCopy._isGuided = tmSource._isGuided;
		if(tmSource._isGuided)
			tmCopy._guidedInfo = new HashSet<InetSocketAddress>(tmSource._guidedInfo);
		tmCopy._annotations = new String(tmSource._annotations);
		
		tmCopy._deltaViolating = tmSource._deltaViolating;
		tmCopy._legitimacyViolating = tmSource._legitimacyViolating;
		tmCopy._legitimacyViolated = tmSource._legitimacyViolated;
	}
	
	public TMulticastTypes getType() {
		throw new UnsupportedOperationException("Subclasses must implement their own.");
//		return _subType;
	}
	
	protected TMulticast(TMulticastTypes subType, LocalSequencer localSequencer) {
		this(subType, localSequencer.getNext());
	}

	protected TMulticast(TMulticastTypes subType, Sequence sourceSequence) {
		_localSequence = null;
		_sourceSequence = sourceSequence;
//		_sourceSequence = LocalSequencer.getLocalSequencer().getNext();
		_subType = subType;
		int seqVectLen = getSeqVecLength();
		_sequencesVector = new Sequence[seqVectLen];
		_sequencesVector[0] = sourceSequence;
		_shifted = 0;
		// TODO: URGENT! have a look at below!
//		_sequencesVector[0] = _sourceSequence;
		
//		for(int i=0 ; i<_sequencesVector.length ; i++)
//			_sequencesVector[i] = _sourceSequence;
	}
	
	public void loadGuidedInfo(Set<InetSocketAddress> recipients) {
		if(_isGuided == true)
//			throw new IllegalStateException("Cannot reload guided recipient info in an already guided msg.");
			return;
		
		_isGuided = true;
		_guidedInfo = new HashSet<InetSocketAddress>(recipients);
	}
	
	protected byte[] getGuidedInfoInByteArray() {
		if(!_isGuided)
			throw new IllegalStateException("Not a guided TMulticast msg.");
		
		int bArraySize = _guidedInfo.size() * Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
		byte[] bArray = new byte[bArraySize];
		int index = 0;
		Iterator<InetSocketAddress> recipientsIt = _guidedInfo.iterator();
		while(recipientsIt.hasNext())
		{
			InetSocketAddress recipient = recipientsIt.next();
			byte[] recipientBArray = Sequence.getAddressInByteArray(recipient);
			for(int i=0 ; i<Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE ; i++)
				bArray[index++] = recipientBArray[i];
		}
		
		return bArray;
	}
	
	public boolean isGuided() {
		return _isGuided;
	}
	
	public Set<InetSocketAddress> getClonedGuidedInfo() {
		if(_guidedInfo == null)
			return null;
		else
			return new HashSet<InetSocketAddress>(_guidedInfo);
	}
	
	protected TMulticast(ByteBuffer bdy, int contentSize, String annotations) {
		// bdy is of type TMULTICAST
		this(getSubType(bdy), Sequence.readSequenceFromByteBuff(bdy, I_SOUCE_SEQUENCE));
		
		byte isDeltaViolating = bdy.get(I_DELTA_VIOLATING);
		_deltaViolating = isDeltaViolating != 0;
		byte isLegitViolated = bdy.get(I_LEGIT_VIOLATED);
		_legitimacyViolated = isLegitViolated != 0;
		byte isLegitViolating = bdy.get(I_LEGIT_VIOLATING);
		_legitimacyViolating = isLegitViolating != 0;
		
//		byte [] bSeq = new byte[Sequence.SEQ_IN_BYTES_ARRAY_SIZE];
//		bdy.position(I_SOUCE_SEQUENCE);
//		bdy.get(bSeq);
//		_sourceSequence = Sequence.readSequenceFromBytesArray(bSeq);
//		_sourceSequence = Sequence.readSequenceFromByteBuff(bdy, I_SOUCE_SEQUENCE)
				
		bdy.position(I_SHIFTS);
		_shifted = bdy.get();
		
		int seqVectLen = getSeqVecLength();
		for(int i=0 ; i<seqVectLen ; i++) {
//			bdy.position(I_SEQUENCES + i * Sequence.SEQ_IN_BYTES_ARRAY_SIZE);
//			bdy.get(bSeq);
//			_sequencesVector[i] = Sequence.readSequenceFromBytesArray(bSeq);
			_sequencesVector[i] = Sequence.readSequenceFromByteBuff(bdy, I_SEQUENCES + i * Sequence.SEQ_IN_BYTES_ARRAY_SIZE);
		}
		
		byte isGuidedB = bdy.get(I_IS_GUIDED);
		if(isGuidedB == 0)
			_isGuided = false;

		else {
			_isGuided = true;
			_guidedInfo = new HashSet<InetSocketAddress>();
			
			int subContentSize = getSubContentSize(bdy);
			int i_guided_info = subContentSize + I_SUB_CONTENT;
			int guidedSize = bdy.getInt(I_GUIDED_BYTE_SIZE);
			
			for(int i=0 ; i<guidedSize ; i++) {
				byte[] addressBArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
				bdy.position(i_guided_info + i * Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
				bdy.get(addressBArray);
				InetSocketAddress recipient = Sequence.readAddressFromByteArray(addressBArray);
				_guidedInfo.add(recipient);
			}
		}
		
		_annotations = annotations;
	}
	
	private static int getSubContentSize(ByteBuffer bdy) {
		return bdy.getInt(I_SUB_CONTENT_SIZE);
	}
	
	private static void setSubContentSize(ByteBuffer bdy, int subContentSize) {
		if(subContentSize>=Integer.MAX_VALUE)
			throw new IllegalArgumentException("Too large: " + subContentSize);
		
		bdy.putInt(I_SUB_CONTENT_SIZE, subContentSize);
	}
	
	public static TMulticast getTMulticastObject(IBrokerShadow brokerShadow, ByteBuffer bdy, int contentSize, String annotations) {
		if(bdy == null)
			return null;
		
		if(bdy.capacity() < TMULTICAST_BASE_SIZE)
			throw new IllegalArgumentException("TMulticast::getTMulticastObject - ERROR, buffer is smaller than the base.");
		
		TMulticastTypes tmType = getSubType(bdy);
		int subContentSize = getSubContentSize(bdy);
		
		switch(tmType) {
		case T_MULTICAST_JOIN:
			return new TMulticast_Join(bdy, contentSize, subContentSize, annotations);
			
		case T_MULTICAST_DEPART:
			return new TMulticast_Depart(bdy, contentSize, subContentSize, annotations);
			
		case T_MULTICAST_CONF:
			return new TMulticast_Conf(bdy, contentSize, subContentSize, annotations);
			
		case T_MULTICAST_PUBLICATION:
			return new TMulticast_Publish(bdy, contentSize, subContentSize, annotations);
			
		case T_MULTICAST_PUBLICATION_MP:
			return new TMulticast_Publish_MP(bdy, contentSize, subContentSize, annotations);
			
		case T_MULTICAST_PUBLICATION_BFT:
			return new TMulticast_Publish_BFT(brokerShadow, bdy, contentSize, subContentSize, annotations);
			
		case T_MULTICAST_PUBLICATION_BFT_DACK:
			return new TMulticast_Publish_BFT_Dack(brokerShadow, bdy, contentSize, subContentSize, annotations);
			
		case T_MULTICAST_SUBSCRIPTION:
			return new TMulticast_Subscribe(bdy, contentSize, subContentSize, annotations);
			
		case T_MULTICAST_UNKNOWN:
			return new TMulticast(bdy, contentSize, annotations);
			
			
//		case T_MULTICAST_UNSUBSCRIPTION:
//			return new TMulticast_UnSubscribe(bdy);
		
		case T_MULTICAST_ADVERTISEMENT:
			return new TMulticast_Advertisement(bdy, contentSize, subContentSize, annotations);
			
		default:
			throw new UnsupportedOperationException("TMulticast::getTMulticastObject - ERROR, unknown type '" + tmType + "'.");
		}
	}

	private static TMulticastTypes getSubType(ByteBuffer bdy) {
		bdy.position(I_SUBTYPE);
		byte subType = bdy.get();
		return TMulticastTypes.getType(subType);
	}
	
	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TMULTICAST;
	}

	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		putObjectInBuffer(localSequencer, buff, true);
	}
	
	@Override
	public final int getContentSize() {
		int guidedSize = getGuidedSize();
		int tmSize = getTMSpecificContentSize();
		return TMULTICAST_BASE_SIZE + tmSize + guidedSize;
	}
	
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, boolean addLocalSeq) {
		InetSocketAddress localAddress = localSequencer.getLocalAddress();
		if(!getSenderAddress().equals(localAddress))
			throw new IllegalStateException(this.toStringTooLong() + " vs. " + getSenderAddress() + " vs. " + localAddress);
	
		TMulticastTypes subType = getType();
		buff.position(I_SUBTYPE);
		buff.put(subType.getValue());
		
		int subContentSize = getTMSpecificContentSize();
		setSubContentSize(buff, subContentSize);

		buff.put(I_DELTA_VIOLATING, (byte)(_deltaViolating ? 1 : 0));
		buff.put(I_LEGIT_VIOLATED, (byte)(_legitimacyViolated ? 1 : 0));
		buff.put(I_LEGIT_VIOLATING, (byte)(_legitimacyViolating ? 1 : 0));
		
		buff.position(I_SOUCE_SEQUENCE);
		buff.put(_sourceSequence.getInByteArray());
		
		buff.position(I_SHIFTS);
		buff.put(_shifted);
		
		buff.position(I_SEQUENCES);
		if(addLocalSeq == true && false) // This is no longer valid: to be removed later.
		{
//			buff.position(I_SEQUENCES);
//			buff.put(_localSequence.getInByteArray());

			for(int i=0 ; i<_sequencesVector.length ; i ++) {
				buff.position(I_SEQUENCES + (i)*Sequence.SEQ_IN_BYTES_ARRAY_SIZE);
				if(_sequencesVector[i] != null)
					buff.put(_sequencesVector[i].getInByteArray());
				else
					buff.put(Sequence.NULL_SEQ_BYTES);
			}
		}
		else
		{
			for(int i=0 ; i<_sequencesVector.length ; i ++) {
				buff.position(I_SEQUENCES + (i)*Sequence.SEQ_IN_BYTES_ARRAY_SIZE);
				if(_sequencesVector[i] != null)
					buff.put(_sequencesVector[i].getInByteArray());
				else
					buff.put(Sequence.NULL_SEQ_BYTES);
			}
		}
		
		if(_isGuided)
		{
			buff.put(I_IS_GUIDED, (byte)1);
			byte[] guidedBArray = getGuidedInfoInByteArray();
			int guidedSetSize = _guidedInfo.size();
			buff.position(I_GUIDED_BYTE_SIZE);
			buff.putInt(guidedSetSize);
			
			buff.position(subContentSize + I_SUB_CONTENT);
			buff.put(guidedBArray);
		}
		else
			buff.put(I_IS_GUIDED, (byte)0);

		return;
	}
	
	@Override
	public String toString() {
		String str = "TMulticast ("+_subType+") @ " + _sourceSequence + "[";
		for(int i=0 ; i<_sequencesVector.length ; i++)
			str += _sequencesVector[i] + ",";
		return str + "]";
	}
	
	public Sequence getLocalSequence() {
		return _localSequence;
	}
	
//	public void setLocalSequence(Sequence localSequence) {
////		if(_localSequence != null)
////			throw new IllegalStateException();
//		
//		_localSequence = localSequence;
//	}
	
	public static Sequence getSourceSequenceFromRaw(IRawPacket raw) {
		ByteBuffer hdr = raw.getHeader();
		ByteBuffer bdy = raw.getBody();
		int hdrPosition = hdr.position();
		int bdyPosition = bdy.position();
		
		if(raw.getType() != PacketableTypes.TMULTICAST)
			return null;
		
		byte[] bArray = new byte[Sequence.SEQ_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_SOUCE_SEQUENCE);
		bdy.get(bArray);
		
		Sequence sourceSequence = Sequence.readSequenceFromBytesArray(bArray);
		
		hdr.position(hdrPosition);
		bdy.position(bdyPosition);
		
		return sourceSequence;
	}
	
	@Override
	public Sequence getSourceSequence() {
		return _sourceSequence;
	}
	
	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_UNKNOWN;
	}

	public final TMulticast_Conf getConfirmation(InetSocketAddress confirmTo, InetSocketAddress confirmedFrom, Sequence confirmationFromSequence) {
		TMulticast_Conf conf = new TMulticast_Conf(this, confirmTo, confirmedFrom, confirmationFromSequence);
		return conf;
	}
	
	public final InetSocketAddress getSenderAddress() {
		for(int i=0 ; i<_sequencesVector.length ; i++)
			if(_sequencesVector[i] != null)
				return _sequencesVector[i].getAddress();
		
		throw new NullPointerException(this.toString());
//		return null;
//		return _sourceSequence.getAddress();
	}
	
	public Sequence getSenderSequence() {
		for(int i=0 ; i<_sequencesVector.length ; i++)
			if(_sequencesVector[i] != null)
				return _sequencesVector[i];
		
		return null;
	}

	public InetSocketAddress getSourceAddress() {
		return _sourceSequence.getAddress();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(TMulticast_Conf.class.isAssignableFrom(obj.getClass())) 
			return ((TMulticast_Conf)obj).equals(this);
		else if(Sequence.class.isAssignableFrom(obj.getClass()))
			return ((Sequence)obj).equalsExact(_sourceSequence);
		else if(TMulticast.class.isAssignableFrom(obj.getClass())) {
			TMulticast tmObj = (TMulticast) obj;
			if(tmObj.getSourceSequence().equalsExact(this.getSourceSequence()))
				return true;
			else
				return false;
		}
		
		throw new UnsupportedOperationException(obj + "");
	}
	
	@Override
	public int hashCode() {
		int hash = getSourceSequence().hashCode();
		return hash;
	}

	public IRawPacket morph(LocalSequencer localSequencer, ISession outRecoveringSession) {
		return PacketFactory.wrapObject(localSequencer, this);
	}
	
	public static TMulticastTypes getTMulticastType(IRawPacket raw) {
		PacketableTypes type = raw.getType();
		if(type != PacketableTypes.TMULTICAST)
			throw new IllegalArgumentException();
		
		TMulticastTypes tmType = TMulticast.getSubType(raw.getBody());
		return tmType;
	}
	
	@Override
	public final String getAnnotations() {
		return _annotations;
	}
	
	public Sequence[] getSequenceVector() {
		return _sequencesVector;
	}

	public void shiftSequenceVector(int shift) {
		for(int i=0 ; i<_sequencesVector.length-shift ; i++)
			_sequencesVector[_sequencesVector.length-i-1] = _sequencesVector[_sequencesVector.length-i-1-shift];
		
		for(int i=0 ; i<shift ; i++)
			_sequencesVector[i] = null;
		
		_shifted += shift;
	}

	@Override
	public final void annotate(String annotation) {
		_annotations += annotation;
	}

	public int getTMSpecificContentSize() {
		return 0;
	}
	
	public int getGuidedSize() {
		if(isGuided())
			return 4 + _guidedInfo.size() * Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
		else
			return 0;
	}
	

	public String toStringTooLong() {
		return toString() + Writers.write(_sequencesVector);
	}
	
	public int getTrailingSize() {
		return 0;
	}
	
	public void setDeltaViolating(boolean deltaViolating) {
		_deltaViolating = deltaViolating;
	}
	
	public void setLegitimacyViolating(boolean legitimacyViolating) {
		_legitimacyViolating = legitimacyViolating;
	}
	
	public void setLegitimacyViolated(boolean legitimacyViolated) {
		_legitimacyViolated = legitimacyViolated;
	}
	
	public boolean getDeltaViolating() {
		return _deltaViolating;
	}
	
	public byte getPathLength() {
		return _shifted;
	}
	
	public boolean getLegitimacyViolating() {
		return _legitimacyViolating;
	}
	
	public boolean getLegitimacyViolated() {
		return _legitimacyViolated;
	}
	
	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, int bbOffset) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public IPacketListener getPacketListener() {
		return null;
	}
	
	protected int getSeqVecLength() {
		return Broker.VSEQ_LENGTH;
	}

	public String toString(BrokerIdentityManager idMan) {
		return toString();
	}
}
