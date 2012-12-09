package org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;

public abstract class TrafficCorrelationDumpSpecifier implements IPacketable, Serializable {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = -4472044811989871120L;
	public final TrafficCorrelationDumpSpecifierType _type;
	public final boolean _merge;
	public final String _comment;
	private final Writer _ioWriter;
	private String _content;
	
	private static final int I_DUMP_SPEC_TYPE = 0;
	private static final int I_MERGE = I_DUMP_SPEC_TYPE + 1;
	private static final int I_COMMENT_SIZE = I_MERGE + 1;
	private static final int I_CONTENT_SIZE = I_COMMENT_SIZE + 4;
	private static final int I_COMMENT = I_CONTENT_SIZE + 4;
	
	public static TrafficCorrelationDumpSpecifier getSessionsEventsCorrelationDump(String comment) {
		return new TrafficCorrelationSessionEventsDumpSpecifier(comment);
	}
	
	public static TrafficCorrelationDumpSpecifier getAllCorrelationDump(String comment, boolean merge) {
		return new TrafficCorrelationAllDumpSpecifier(comment, merge);
	}

	public static TrafficCorrelationDumpSpecifier getMessageCorrelationDump(TMulticast tm, String comment, boolean merge) {
		return new TrafficCorrelationMessageDumpSpecifier(tm.getSourceSequence(), comment, merge);
	}
	
	protected TrafficCorrelationDumpSpecifier(TrafficCorrelationDumpSpecifierType type, String comment, boolean merge) {
		_type = type;
		_comment = comment;
		_merge = merge;
		_ioWriter = createNewWriterPrivately();
	}
	
	protected TrafficCorrelationDumpSpecifier(ByteBuffer buff) {
		_type = getType(buff);
		
		buff.position(I_MERGE);
		byte mergeValue = buff.get();
		_merge = (mergeValue==1);
		
		buff.position(I_COMMENT_SIZE);
		int commentSize = buff.getInt();
		
		buff.position(I_CONTENT_SIZE);
		int contentSize = buff.getInt();
		
		buff.position(I_COMMENT);
		byte [] commentBytes = new byte[commentSize];
		buff.position(I_COMMENT);
		buff.get(commentBytes);
		_comment = new String(commentBytes);
		
		if(contentSize>=0) {
			byte[] contentBytes = new byte[contentSize];
			buff.position(I_COMMENT + commentSize);
			buff.get(contentBytes);
			_content = new String(contentBytes);
			_ioWriter = null;
		}else {
			_content = null;
			_ioWriter = new StringWriter();
		}
	}
	
	protected Writer createNewWriterPrivately() {
		return new StringWriter();
	}
	
	public Writer getWriter() {
		return _ioWriter;
	}
	
	public synchronized String getString() {
		if (_content == null)
			_content = _ioWriter.toString();
		
		return _content;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null)
			return false;
		
		if(!obj.getClass().isInstance(this))
			return false;
		
		TrafficCorrelationDumpSpecifier objDumpSpecifier = (TrafficCorrelationDumpSpecifier) obj;
		if (objDumpSpecifier._merge == _merge)
			if (objDumpSpecifier._comment.equals(_comment))
				if (objDumpSpecifier._type.equals(_type))
					if (objDumpSpecifier._content == null)
						return _content == null;
					else
						return objDumpSpecifier._content.equals(_content);
		
		return false;
	}
	
	@Override
	public String toString() {
		return "Correlation-" + _type + ":" + _comment + ":\"" + _content + "\"";
	}
	
	@Override
	public int getContentSize() {
		return 1 + 1 + 4 + 4 + _comment.length() + getString().length();
	}

	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TCORRELATE_DUMP_SPEC;
	}

	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		buff.position(I_DUMP_SPEC_TYPE);
		buff.put(_type._byteValue);
		
		buff.position(I_MERGE);
		buff.put(_merge?(byte)1:(byte)0);
		
		int commentSize = _comment.length();
		buff.position(I_COMMENT_SIZE);
		buff.putInt(commentSize);
		
		int contentSize = _content==null?-1:_content.length();
		buff.position(I_CONTENT_SIZE);
		buff.putInt(contentSize);
		
		buff.position(I_COMMENT);
		buff.put(_comment.getBytes());
		
		if(contentSize>=0) {
			buff.position(I_COMMENT + commentSize);
			buff.put(_content.getBytes());
		}
	}

	@Override
	public final void annotate(String annotation) {
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
	
	public static TrafficCorrelationDumpSpecifierType getType(ByteBuffer buff) {
		buff.position(I_DUMP_SPEC_TYPE);
		byte typeValue = buff.get();
		return TrafficCorrelationDumpSpecifierType.valueOf(typeValue);
	}
	
	public static TrafficCorrelationDumpSpecifier getObjectFromBuffer(ByteBuffer buff) {
		TrafficCorrelationDumpSpecifierType type = getType(buff);
		switch (type) {
		case DUMP_ALL:
			return new TrafficCorrelationAllDumpSpecifier(buff);
			
		case DUMP_ONE:
			return new TrafficCorrelationMessageDumpSpecifier(buff);
			
		case DUMP_SESSIONS_EVENTS:
			return new TrafficCorrelationSessionEventsDumpSpecifier(buff);
			
		default:
			throw new UnsupportedOperationException("Unknown type: " + type);
		}
	}
	
	public static void main(String[] argv) throws IOException {
		LocalSequencer localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 3030));
		
		int i=0;
		TrafficCorrelationDumpSpecifier[] dumpSpecs = new TrafficCorrelationDumpSpecifier[3*2];
		dumpSpecs[i++] = new TrafficCorrelationSessionEventsDumpSpecifier("Yellow");
		dumpSpecs[i++] = new TrafficCorrelationSessionEventsDumpSpecifier("Yellow");
		dumpSpecs[i-1]._ioWriter.append("SOMETHING");
		
		dumpSpecs[i++] = new TrafficCorrelationAllDumpSpecifier("Hellow", true);
		dumpSpecs[i++] = new TrafficCorrelationAllDumpSpecifier("Hellow", true);
		dumpSpecs[i-1]._ioWriter.append("SOMETHING");
		
		dumpSpecs[i++] = new TrafficCorrelationMessageDumpSpecifier(
				new Sequence(new InetSocketAddress("127.0.0.1", 100), 1077, 1000),
					"Mellow", true);
		dumpSpecs[i++] = new TrafficCorrelationMessageDumpSpecifier(
				new Sequence(new InetSocketAddress("127.0.0.1", 100), 1077, 1000),
					"Mellow", true);
		dumpSpecs[i-1]._ioWriter.append("SOMETHING");
		
		for(TrafficCorrelationDumpSpecifier dumpSpec0 : dumpSpecs) {
			System.out.println("DumpSpec0[BEFORE][" + (dumpSpec0.equals(dumpSpec0)) + "]: " + dumpSpec0);
			
			IRawPacket raw = PacketFactory.wrapObject(localSequencer, dumpSpec0);
			TrafficCorrelationDumpSpecifier dumpSpec1 = (TrafficCorrelationDumpSpecifier) PacketFactory.unwrapObject(null, raw);
			TrafficCorrelationDumpSpecifier dumpSpec2 = (TrafficCorrelationDumpSpecifier) PacketFactory.unwrapObject(null, raw, true);
			
			System.out.println("DumpSpec0[AFTER][" + (dumpSpec0.equals(dumpSpec0)) + "]: " + dumpSpec0);
			System.out.println("DumpSpec1[" + (dumpSpec1==dumpSpec0) + "][" + (dumpSpec0.equals(dumpSpec0)) + "]" + ": " + dumpSpec1);
			System.out.println("DumpSpec2[" + (dumpSpec2==dumpSpec0) + "][" + (dumpSpec0.equals(dumpSpec0)) + "]" + dumpSpec2);
			System.out.println("DONE");
		}
	}
	
	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, int bbOffset) {
		throw new UnsupportedOperationException();
	}
};
