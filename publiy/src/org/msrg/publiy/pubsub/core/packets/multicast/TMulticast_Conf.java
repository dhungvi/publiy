package org.msrg.publiy.pubsub.core.packets.multicast;

import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class TMulticast_Conf extends TMulticast {
	
	private final int I_ORIGINAL_TM_TYPE = TMULTICAST_BASE_SIZE;
	private final int I_CONFIRMED_TO = I_ORIGINAL_TM_TYPE + 1;
	private final int I_CONFIRMED_FROM = I_CONFIRMED_TO + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;;
	private final int I_CONFIRMAITON_FROM_SEQUANCE = I_CONFIRMED_FROM + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
//	private final int I_ANNO2TATIONS = I_CONFIRMAITON_FROM_SEQUANCE + Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	private final int ADDITIONAL_BUFFER_SIZE = 2 + 1 + 2*Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE + Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	
	final TMulticastTypes _originalTMulticastType;
	final InetSocketAddress _confirmedTo;
	final InetSocketAddress _confirmedFrom;
	final private Sequence _confirmationFromSequence;
	
	public TMulticast_Conf(TMulticast tm, InetSocketAddress confirmedTo, InetSocketAddress confirmedFrom, Sequence confirmationFromSequence) {
		this(tm.getType(),
				tm._sequencesVector,
				tm._sourceSequence,
				confirmedTo,
				confirmedFrom,
				confirmationFromSequence);
		
		if(tm.getType() == TMulticastTypes.T_MULTICAST_CONF)
			throw new IllegalStateException("Connot make a confirmation for a confirmation msg.");
		
		if(_confirmedTo == null)
			throw new IllegalStateException(tm.toString());
	}
	
	public InetSocketAddress getConfirmedFrom() {
		return _confirmedFrom;
	}
	
	private TMulticast_Conf(
			TMulticastTypes originalTMulticastType,
			Sequence[] sequenceVector, 
			Sequence sourceSequence,
			InetSocketAddress confirmTo,
			InetSocketAddress confirmFrom,
			Sequence confirmationFromSequence) {
		super(TMulticastTypes.T_MULTICAST_CONF, sourceSequence);
		super._sequencesVector = new Sequence[getSeqVecLength()];
		for(int i=0 ; i<sequenceVector.length ; i++)
			super._sequencesVector[i] = sequenceVector[i];
		
		if(originalTMulticastType == TMulticastTypes.T_MULTICAST_CONF )
			throw new IllegalStateException(originalTMulticastType.toString());
		this._originalTMulticastType = originalTMulticastType;
		this._confirmedTo = confirmTo;
		this._confirmedFrom = confirmFrom;
		this._confirmationFromSequence = confirmationFromSequence;
	}
	
	public TMulticast_Conf getClone() {
		return new TMulticast_Conf(this._originalTMulticastType, this._sequencesVector, 
				this._sourceSequence, this._confirmedTo, this._confirmedFrom, this._confirmationFromSequence);
	}
	
	public TMulticast_Conf getCloneAndShift(int shift, Sequence sequence) {
		TMulticast_Conf tmcClone = getClone();
		return tmcClone.shiftSequenceVector(shift, sequence);
	}
	
	private TMulticast_Conf shiftSequenceVector(int shift, Sequence seq) {
		shiftSequenceVector(shift);
		
		if(seq==null)
			throw new NullPointerException("Shifting '" + shift + "': " + this);

		_sequencesVector[shift-1] = seq;
		return this;
	}
	
	public InetSocketAddress getConfirmedTo() {
		return _confirmedTo;
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		putObjectInBuffer(localSequencer, buff, true);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, boolean addLocalSeq) {
		super.putObjectInBuffer(localSequencer, buff, addLocalSeq);
		
		buff.position(I_ORIGINAL_TM_TYPE);
		buff.put(_originalTMulticastType.getValue());
		
		byte[] bArray = Sequence.getAddressInByteArray(_confirmedTo);
		buff.position(I_CONFIRMED_TO);
		buff.put(bArray);
		
		bArray = Sequence.getAddressInByteArray(_confirmedFrom);
		buff.position(I_CONFIRMED_FROM);
		buff.put(bArray);
		
		bArray = _confirmationFromSequence.getInByteArray();
		buff.position(I_CONFIRMAITON_FROM_SEQUANCE);
		buff.put(bArray);
	}
	
	public TMulticast_Conf(ByteBuffer bdy, int contentSize, int subContentSize, String annotations) {
		super(bdy, contentSize, annotations);
		
		if(subContentSize != getTMSpecificContentSize())
			throw new IllegalArgumentException("SubContentSize is wrong: " + subContentSize + " != " + getTMSpecificContentSize());
		
		bdy.position(I_ORIGINAL_TM_TYPE);
		byte bType = bdy.get();
		_originalTMulticastType = TMulticastTypes.getType(bType);
		
		if(this.getType() != getStaticType())
			throw new IllegalStateException("TMulticast_Conf's type mismatch '" + getType() + "'.");
		
		byte[] bArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_CONFIRMED_TO);
		bdy.get(bArray);
		_confirmedTo = Sequence.readAddressFromByteArray(bArray);
		
		bdy.position(I_CONFIRMED_FROM);
		bdy.get(bArray);
		_confirmedFrom = Sequence.readAddressFromByteArray(bArray);
		
		bArray = new byte[Sequence.SEQ_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_CONFIRMAITON_FROM_SEQUANCE);
		bdy.get(bArray);
		_confirmationFromSequence = Sequence.readSequenceFromBytesArray(bArray);
	}
	
	@Override
	public String toString() {
		StringWriter writer = new StringWriter();
		writer.append("TMulticast_Conf- ");
		writer.append(TMulticastTypes.getType(_originalTMulticastType.getValue()).toString());
		writer.append("@ " + _sourceSequence + "[");
		for(int i=0 ; i<_sequencesVector.length ; i++)
			writer.append((i==0?"":",") + _sequencesVector[i]);
		writer.append(']');

		return writer.toString();
	}

	@Override
	public int hashCode() {
		return _confirmationFromSequence.hashCode();
	}
	
	public Sequence getConfirmationFromSequence() {
		return _confirmationFromSequence;
	}
	
	public boolean isConfirmationOf(TMulticast tm) {
		if(tm==null)
			return false;
		return tm.getSourceSequence().equalsExact(this.getSourceSequence());
			
	}
	
	@Override
	public boolean equals(Object obj) {
		if(Sequence.class.isInstance(obj)) {
			Sequence oSequence = (Sequence) obj;
			return oSequence.equalsExact(_confirmationFromSequence);
		}
		
		if(Conf_Ack.class.isInstance(obj)) {
			Conf_Ack confAck = (Conf_Ack) obj;
			return confAck.equals(this);
		}
		
		if(TMulticast_Conf.class.isInstance(obj)) {
			TMulticast_Conf tmc = (TMulticast_Conf) obj;
			return ( tmc._confirmationFromSequence.equalsExact(this._confirmationFromSequence));
		}

		return super.equals(obj);
	}
	
	public TMulticastTypes getOriginalTMulticastType() {
		return _originalTMulticastType;
	}

	@Override
	public int getTMSpecificContentSize() {
		return ADDITIONAL_BUFFER_SIZE;
	}
	
	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_CONF;
	}
	
	@Override
	public TMulticastTypes getType() {
		return TMulticastTypes.T_MULTICAST_CONF;
	}
}
