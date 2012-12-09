package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

public class TText implements IPacketable{
	private String _str;
	
	public TText(String str){
		_str = str;
	}

	public TText(ByteBuffer buff, int offset) {
		int len = buff.getInt(offset+1);
		byte[] bArray = new byte[len];
		buff.position(offset+1+Integer.SIZE/8);
		buff.get(bArray);
		_str = new String(bArray);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		buff.put(getObjectType()._codedValue);
		buff.putInt(_str.length());
		buff.put(_str.getBytes());
	}

	public PacketableTypes getObjectType() {
		return PacketableTypes.TTEXT;
	}
	
	@Override
	public String toString(){
		return "[TText:"+_str+"]";
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!TText.class.isAssignableFrom(obj.getClass()))
			return false;
		
		TText tObj = (TText)obj;
		return tObj._str.equals(_str);
	}
	
	@Override
	public int getContentSize() {
		return 1 + Integer.SIZE/8 + _str.length();
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
	
	public static void main(String[] argv) {
		LocalSequencer localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.2", 3000));
		TText tText = new TText("Hi Reza!");
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, tText);
		
		IPacketable packet = PacketFactory.unwrapObject(null, raw, true);
		System.out.println(packet);
		if(!packet.equals(tText))
			throw new IllegalStateException();
		if(!tText.equals(packet))
			throw new IllegalStateException();
	}
	
	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, int bbOffset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IPacketListener getPacketListener() {
		return null;
	}

	public String getString() {
		return _str;
	}
}
