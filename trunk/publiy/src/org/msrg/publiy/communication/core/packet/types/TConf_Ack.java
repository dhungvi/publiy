package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;

import org.msrg.publiy.pubsub.core.packets.multicast.Conf_Ack;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class TConf_Ack implements IPacketable {
	
	private static final int I_ACK_FROM_ADDR = 0;
	private static final int I_ACK_TO_ADDR = I_ACK_FROM_ADDR + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	private static final int I_ACK_SEQ_SIZE = I_ACK_TO_ADDR + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	private static final int I_ACK_SEQUENCES = I_ACK_SEQ_SIZE + 4;
	
	final InetSocketAddress _ackFrom;
	final InetSocketAddress _ackedTo;
	final Sequence[] _conf_source_sequences;
	final int _conf_source_sequences_size;
	
	public TConf_Ack(InetSocketAddress ackFrom, InetSocketAddress ackTo, TMulticast_Conf[] tmcs, int tmcsSize){
		if ( tmcs.length > Broker.AGGREGATED_CONFIRMATION_ACK_COUNT )
			throw new IllegalStateException("tmcs length is: " + tmcs.length + ", tmcsSize: " + tmcsSize);
		
		_ackFrom = ackFrom;
		_ackedTo = ackTo;
		if ( _ackedTo == null )
			throw new IllegalStateException("_ackTo is null.");
		
		if ( tmcs == null )
			throw new IllegalArgumentException("TMulticast_Conf[] cannot be null.");
		_conf_source_sequences = new Sequence[tmcsSize];
		int counter = 0;
		for ( int i=0 ; i<tmcsSize ; i++ )
			if ( tmcs[i] != null )
				_conf_source_sequences[counter++] = tmcs[i].getConfirmationFromSequence();//getSourceSequence();
		
		_conf_source_sequences_size = counter;
	}
	
	public static TConf_Ack getTConf_Ack(ByteBuffer bdy){
		return new TConf_Ack(bdy);
	}
	
	protected TConf_Ack (ByteBuffer bdy) {
		byte[] bArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		
		bdy.position(I_ACK_FROM_ADDR);
		bdy.get(bArray);
		_ackFrom = Sequence.readAddressFromByteArray(bArray);
		
		bdy.position(I_ACK_TO_ADDR);
		bdy.get(bArray);
		_ackedTo = Sequence.readAddressFromByteArray(bArray);
		if ( _ackedTo == null )
			throw new IllegalStateException("_ackTo is null.");
		
		_conf_source_sequences_size = bdy.getInt(I_ACK_SEQ_SIZE);
		_conf_source_sequences = new Sequence[_conf_source_sequences_size];
		byte [] bSeqArray = new byte[Sequence.SEQ_IN_BYTES_ARRAY_SIZE];
		
		for ( int i=0 ; i<_conf_source_sequences_size ; i++ ){
			bdy.position(I_ACK_SEQUENCES + i * Sequence.SEQ_IN_BYTES_ARRAY_SIZE);
			bdy.get(bSeqArray);
			_conf_source_sequences[i] = Sequence.readSequenceFromBytesArray(bSeqArray);
		}
	}
	
	public String toString(){
		String strConfSequences = "";
		for ( int i=0 ; i<_conf_source_sequences_size ; i++ )
			strConfSequences += _conf_source_sequences[i].toString() + ",";
		String str = "TConf_Ack '" + _conf_source_sequences_size + "' TM_Conf [" + strConfSequences + "]";
		return str;
	}
	
	public boolean equals(Object obj){
		throw new IllegalArgumentException("Equal is not defined over aggregated ack.");
	}

	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TCONF_ACK;
	}

	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		byte[] fromBArray = Sequence.getAddressInByteArray(_ackFrom);
		buff.position(I_ACK_FROM_ADDR);
		buff.put(fromBArray);
		
		byte[] toBArray = Sequence.getAddressInByteArray(_ackedTo);
		buff.position(I_ACK_TO_ADDR);
		buff.put(toBArray);
		
		buff.putInt(I_ACK_SEQ_SIZE, _conf_source_sequences_size);
		
		for ( int i=0 ; i<_conf_source_sequences_size ; i++ ){
			Sequence confSourceSequence = _conf_source_sequences[i];
			byte[] seqArray = confSourceSequence.getInByteArray();
			buff.position(I_ACK_SEQUENCES + i * Sequence.SEQ_IN_BYTES_ARRAY_SIZE);
			buff.put(seqArray);
		}
	}
	
	public Conf_Ack[] getIndividualConfAcks(){
		Conf_Ack[] conAcks = new Conf_Ack[_conf_source_sequences_size];
		for ( int i=0 ; i<_conf_source_sequences_size ; i++ )
			conAcks[i] = new Conf_Ack(_ackFrom, _ackedTo, _conf_source_sequences[i]);
		
		return conAcks;
	}

	@Override
	public int getContentSize() {
		return 2 * Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE + 4 + _conf_source_sequences_size * Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	}

	public int getTConfSize(){
		return _conf_source_sequences_size;
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
	public Sequence getSourceSequence() {
		return null;
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
