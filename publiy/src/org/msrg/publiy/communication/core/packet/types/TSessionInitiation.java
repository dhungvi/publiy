package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;

public class TSessionInitiation implements IPacketable {

	public static final int I_LOCALADDRESS = 0;
	public static final int I_REMOTEADDRESS = I_LOCALADDRESS + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	public static final int I_TYPE = I_REMOTEADDRESS + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	public static final int I_SUBTYPE = I_TYPE + 1;
	public static final int I_ISACTIVE = I_SUBTYPE + 1;
	public static final int I_SEQUENCE = I_ISACTIVE + 1;
	public static final int I_LAST_SEQUENCE = I_SEQUENCE + Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	
	public static final int TSESSION_IN_BYTE_SIZE = I_LAST_SEQUENCE + Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	
	private final InetSocketAddress _sourceCC;
	private final InetSocketAddress _destinationCC;
	private final SessionInitiationTypes _sessionType;
	private final boolean _isActiveLocally;
	private final Sequence _sourceSequence;
	private String _annotations = "";
	private Sequence _lastReceivedSequence;
	
	public TSessionInitiation(LocalSequencer localSequencer, InetSocketAddress remoteCC, SessionInitiationTypes type, Sequence lastSequenceFromOtherEndPoint) {
		this(localSequencer, remoteCC, type, false, lastSequenceFromOtherEndPoint);
	}
	
	public TSessionInitiation(LocalSequencer localSequencer, InetSocketAddress remoteCC, SessionInitiationTypes type, boolean isActiveLocally, Sequence lastSequenceFromOtherEndPoint) {
		_sourceSequence = localSequencer.getNext();
		_sourceCC = _sourceSequence.getAddress();
		_destinationCC = remoteCC;
		_sessionType = type;
//		_sessionSubType = subType;
		_lastReceivedSequence = lastSequenceFromOtherEndPoint;
		_isActiveLocally = isActiveLocally;
	}
	
	public SessionInitiationTypes getSessionType() {
		return _sessionType;
	}

	public InetSocketAddress getSourceAddress() {
		return _sourceCC;
	}
	
	public InetSocketAddress getDestinationAddress() {
		return _destinationCC;
	}
	
	public Sequence getSourceSequence() {
		return _sourceSequence;
	}
	
	public TSessionInitiation(ByteBuffer buff) {
//		byte[] ipL = new byte[4];
//		byte[] ipR = new byte[4];
//		short portL = 0, portR = 0;
//		byte[] seqB = new byte[18];
//		byte[] seqL = new byte[18];
//		((ByteBuffer)buff.position(I_LOCALADDRESS)).get(ipL);
//		portL = buff.getShort(I_LOCALADDRESS + 4);
//		((ByteBuffer)buff.position(I_REMOTEADDRESS)).get(ipR);
//		portR = buff.getShort(I_REMOTEADDRESS + 4);
		
		byte[] bArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		
		buff.position(I_LOCALADDRESS);
		buff.get(bArray);
		_sourceCC = Sequence.readAddressFromByteArray(bArray);
		
		buff.position(I_REMOTEADDRESS);
		buff.get(bArray);
		_destinationCC = Sequence.readAddressFromByteArray(bArray);
		
//		_sourceCC = new InetSocketAddress(""+ipL[0] + "." + ipL[1] + "." + ipL[2] + "." + ipL[3], portL);
//		_destinationCC = new InetSocketAddress(""+ipR[0] + "." + ipR[1] + "." + ipR[2] + "." + ipR[3], portR);
		
		_sessionType = SessionInitiationTypes.values()[((ByteBuffer)buff.position(I_TYPE)).get()];
		
		_isActiveLocally = (buff.get(I_ISACTIVE)==0) ? false : true;
		
		byte[] bArraySeq = new byte[Sequence.SEQ_IN_BYTES_ARRAY_SIZE];
		buff.position(I_SEQUENCE);
		buff.get(bArraySeq);
		_sourceSequence = Sequence.readSequenceFromBytesArray(bArraySeq);
		
		buff.position(I_LAST_SEQUENCE);
		buff.get(bArraySeq);
		_lastReceivedSequence = Sequence.readSequenceFromBytesArray(bArraySeq);
	}
	
	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TSESSIONINITIATION ;
	}

	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		byte[] bArray = Sequence.getAddressInByteArray(_sourceCC);
		buff.position(I_LOCALADDRESS);
		buff.put(bArray);
		
		bArray = Sequence.getAddressInByteArray(_destinationCC);
		buff.position(I_REMOTEADDRESS);
		buff.put(bArray);
		
		buff.position(I_TYPE);
		buff.put(_sessionType.value());
		
		buff.put(I_ISACTIVE, _isActiveLocally ? (byte)1 : (byte)0);
		
		byte[] bArraySeq = _sourceSequence.getInByteArray();
		buff.position(I_SEQUENCE);
		buff.put(bArraySeq);
		
		if(_lastReceivedSequence == null)
			bArraySeq = Sequence.NULL_SEQ_BYTES;
		else
			bArraySeq = _lastReceivedSequence.getInByteArray();
		buff.position(I_LAST_SEQUENCE);
		buff.put(bArraySeq);
		
//		((ByteBuffer)buff.position(I_LOCALADDRESS)).put(_sourceCC.getAddress().getAddress());
//		((ByteBuffer)buff.position(I_LOCALADDRESS + 4)).putShort((short) _sourceCC.getPort());
//		((ByteBuffer)buff.position(I_REMOTEADDRESS)).put(_destinationCC.getAddress().getAddress());
//		((ByteBuffer)buff.position(I_REMOTEADDRESS + 4)).putShort((short) _destinationCC.getPort());
//		((ByteBuffer)buff.position(I_TYPE)).put(_sessionType.value());
//		((ByteBuffer)buff.position(I_SUBTYPE)).put(_sessionSubType.value());
//		((ByteBuffer)buff.position(I_SEQUENCE)).put(_sourceSequence.getInByteArray());
//		if(_lastReceivedSequence == null)
//			((ByteBuffer)buff.position(I_LAST_SEQUENCE)).put(Sequence.NULL_SEQ_BYTES);
//		else
//			((ByteBuffer)buff.position(I_LAST_SEQUENCE)).put(_lastReceivedSequence.getInByteArray());
	}
	
	public void setLastReceivedSequence(Sequence seq) {
		_lastReceivedSequence = seq;
	}
	
	@Override
	public String toString() {
		return "TSessionInit[" + _sourceCC + "->" + _destinationCC + "_" + _sessionType + "," //+ _sessionSubType 
																							+ "@" + _sourceSequence + "___STARTFROM: " + _lastReceivedSequence;
	}
	
	public Sequence getLastReceiveSequence() {
		return _lastReceivedSequence;
	}

	@Override
	public int getContentSize() {
		return TSESSION_IN_BYTE_SIZE;
	}

	@Override
	public void annotate(String annotation) {
		_annotations += annotation;
	}

	@Override
	public String getAnnotations() {
		return _annotations;
	}
	
	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, int bbOffset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IPacketListener getPacketListener() {
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!super.equals(obj))
			return false;
		
		if(!this.getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		TSessionInitiation tSessionObj = (TSessionInitiation) obj;
		if(this._isActiveLocally != tSessionObj._isActiveLocally)
			return false;
		
		if(!this._destinationCC.equals(tSessionObj._destinationCC))
			return false;
		
		if(this._lastReceivedSequence == null ^ tSessionObj._lastReceivedSequence == null)
			return false;
		
		if(this._lastReceivedSequence != null)
			if(!this._lastReceivedSequence.equals(tSessionObj._lastReceivedSequence))
				return false;
		
		if(!this._sourceSequence.equals(tSessionObj._sourceSequence))
			return false;
		
		if(!this._sourceCC.equals(tSessionObj._sourceCC))
			return false;

		if(!this._annotations.equals(tSessionObj._annotations))
			return false;
		
		return true;
	}
}
