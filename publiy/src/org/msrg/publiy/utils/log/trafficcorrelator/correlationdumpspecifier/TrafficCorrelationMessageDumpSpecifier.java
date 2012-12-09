package org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier;

import java.nio.ByteBuffer;

import org.msrg.publiy.communication.core.packet.IPacketListener;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class TrafficCorrelationMessageDumpSpecifier extends TrafficCorrelationDumpSpecifier {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = 2954787163364655380L;
	public final Sequence _tmSourceSequence;
	
	TrafficCorrelationMessageDumpSpecifier(Sequence tmSourceSequence, String comment, boolean merge) {
		this(TrafficCorrelationDumpSpecifierType.DUMP_ONE, tmSourceSequence, comment, merge);
	}
	
	TrafficCorrelationMessageDumpSpecifier(ByteBuffer buff) {
		super(buff);
		
		int index = super.getContentSize();
		buff.position(index);
		byte [] seqBytes = new byte[Sequence.SEQ_IN_BYTES_ARRAY_SIZE];
		buff.get(seqBytes);
		_tmSourceSequence = Sequence.readSequenceFromBytesArray(seqBytes);
	}
	
	protected TrafficCorrelationMessageDumpSpecifier(TrafficCorrelationDumpSpecifierType type, Sequence tmSourceSequence, String comment, boolean merge) {
		super(type, comment, merge);
		_tmSourceSequence = tmSourceSequence;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!super.equals(obj))
			return false;
		
		TrafficCorrelationMessageDumpSpecifier objDumpSpecifier = (TrafficCorrelationMessageDumpSpecifier) obj;
		if (_tmSourceSequence == null)
			return objDumpSpecifier._tmSourceSequence == null;
		
		return _tmSourceSequence.equals(objDumpSpecifier._tmSourceSequence);
	}
	
	@Override
	public String toString() {
		return super.toString() + ":" + _tmSourceSequence;
	}
	
	@Override
	public int getContentSize() {
		return super.getContentSize() + Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		super.putObjectInBuffer(localSequencer, buff);
		
		int index = super.getContentSize();
		buff.position(index);
		buff.put(_tmSourceSequence.getInByteArray());
	}

	@Override
	public IPacketListener getPacketListener() {
		return null;
	}
};
