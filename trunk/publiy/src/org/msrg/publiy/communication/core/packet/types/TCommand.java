package org.msrg.publiy.communication.core.packet.types;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationDumpSpecifier;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

public class TCommand implements IPacketable, Serializable{
	
	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -5333084349483770685L;
	private final TCommandTypes _type;
	private final String _comment;
	
	private TrafficCorrelationDumpSpecifier _dumpSpecifier;
	
	private static final int ITYPE = 0;
	private static final int ICOMMENT_LENGTH = 1;
	private static final int ICOMMENT = 5;
	
	public TCommand(TCommandTypes type, String comments) {
		_type = type;
		_comment = comments;
	}
	
	public void setTrafficCorrelationDumpSpecifier(TrafficCorrelationDumpSpecifier dumpSpect) {
		_dumpSpecifier = dumpSpect;
	}
	
	public TrafficCorrelationDumpSpecifier getTrafficCorrelationDumpSpecifier() {
		return _dumpSpecifier;
	}
	
	public boolean doDisseminate() {
		if(_type == TCommandTypes.CMND_DISSEMINATE)
			return true;
		
		else
			return false;
	}
	
	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TCOMMAND;
	}
	
	public TCommandTypes getType() {
		return _type;
	}
	
	public String getComment() {
		return _comment;
	}
	
	@Override
	public String toString() {
		return "TCommand [" + _type + ":" + _comment + "]";
	}

	@Override
	public int getContentSize() {
		return ICOMMENT + _comment.length();
	}

	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, int bbOffset) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		buff.position(ITYPE);
		buff.put(_type._typeValue);
		
		buff.position(ICOMMENT_LENGTH);
		buff.putInt(_comment.length());
		
		buff.position(ICOMMENT);
		buff.put(_comment.getBytes());
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
	
	public static IPacketable getObjectFromBuffer(ByteBuffer bdy) {
		bdy.position(ITYPE);
		byte typeByte = bdy.get();
		
		bdy.position(ICOMMENT_LENGTH);
		int commentLength = bdy.getInt();
		
		byte[] commentBytes = new byte[commentLength];
		bdy.position(ICOMMENT);
		bdy.get(commentBytes);
		
		TCommandTypes type = TCommandTypes.valueOf(typeByte);
		return new TCommand(type, new String(commentBytes));
	}
	
	public static void main(String[]  argv) {
		LocalSequencer localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 4000));
		IPacketable tCommand = PacketFactory.createTCommandDisseminateMessage("HELLO!");
//		IPacketable tCommand = PacketFactory.createTCommandMarkMessage("HELLO!");
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, tCommand);
		TCommand tCommand2 = (TCommand) PacketFactory.unwrapObject(null, raw);
		System.out.println(tCommand);
		System.out.println(tCommand2);
	}

	@Override
	public IPacketListener getPacketListener() {
		return null;
	}
}
