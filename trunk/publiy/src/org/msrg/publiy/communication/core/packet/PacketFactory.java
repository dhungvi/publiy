package org.msrg.publiy.communication.core.packet;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.communication.core.packet.types.TCommand;
import org.msrg.publiy.communication.core.packet.types.TCommandTypes;
import org.msrg.publiy.communication.core.packet.types.TText;

public class PacketFactory {
	public static ByteBuffer getUnPreparedHeader() {
		ByteBuffer hdr = ByteBuffer.allocate(RawPacket.HEADER_SIZE);
		hdr.put(RawPacket.IHEADER_TYPE, RawPacket.TYPE_UNKNOWN);
		return hdr;
	}
	
	public static boolean isOutward(InetSocketAddress here, InetSocketAddress there) {
		byte[] bHere = here.getAddress().getAddress();
		byte[] bThere = there.getAddress().getAddress();
		
		for(int i=0 ; i<4 ; i++)
			if(bHere[i] > bThere[i])
				return true;
			else if(bHere[i] < bThere[i])
				return false;
		
		int pHere = here.getPort();
		int pThere = there.getPort();
		
		if(pHere > pThere)
			return true;
		else
			return false;
	}
	
	public static int getBodyLen(ByteBuffer hdr) {
		if(hdr==null)
			return -1;
		if(hdr.capacity() != RawPacket.HEADER_SIZE)
			return -1;
		if(hdr.hasRemaining())
			return -1;
		int len = hdr.getInt(RawPacket.IHEADER_MSG_LENGTH);
		return len;
	}
	
	public static IPacketable unwrapObject(IBrokerShadow brokerShadow, IRawPacket raw) {
		return unwrapObject(brokerShadow, raw, false);
	}
	
	public static IPacketable unwrapObject(IBrokerShadow brokerShadow, IRawPacket raw, boolean createAnew) {
		if(raw == null)
			return null;
		
		try{
			if(!createAnew) {
				IPacketable obj = raw.getObject();
				if(obj != null)
					return obj;
			}
			
			int bodyPosition = raw.getBody().position();
			int headerPosition = raw.getHeader().position();
			
			PacketableTypes type = raw.getType();
			String annotations = raw.getAnnotations();
			int contentSize = raw.getContentSize();
			IPacketable object = PacketableTypes.getObjectFromBuffer(brokerShadow, raw.getBody(), type, contentSize, "");
			
			object.annotate(annotations);
			
			raw.getBody().position(bodyPosition);
			raw.getHeader().position(headerPosition);
			
			raw.setObject(object);
			
			// TODO: remove below check.	
			if(object.getObjectType() == PacketableTypes.TMULTICAST) {
				TMulticast tm = (TMulticast) object;
				if(tm.getSenderAddress() == null)
					throw new IllegalStateException(tm.toStringTooLong());
			}

			return object;
		}catch(Exception ex) {
			ex.printStackTrace();
			System.err.println("Header: " + getByteArrayString(raw.getHeader().array()));
			System.err.println("Body: " + getByteArrayString(raw.getBody().array()));

			throw new UnknownError();
		}
	}
	
	public static String getByteArrayString(byte[] bArray) {
		if(bArray == null)
			return null;
		String str = "";
		for(int i=0 ; i<bArray.length ; i++)
			str += bArray[i] + ",";
		
		return str;
	}
	
	/*
	 * The header of the returned RawPacket is partly initialized;
	 * 'SRC_ADDR', 'DST_ADDR', 'SESSION', 'SEQ' need to be set. 
	 */
	@Deprecated
	public static IRawPacket wrapObject(IPacketable obj) throws IllegalArgumentException {
//		return wrapObject(LocalSequencer.getLocalSequencer(), obj);
		throw new UnsupportedOperationException();
	}
	
	public static IRawPacket wrapObject(LocalSequencer localSequencer, IPacketable obj) throws IllegalArgumentException {
		if(obj == null)
			return null;
		
		//TODO: URGENT! Remove below
		if(obj.getObjectType() == PacketableTypes.TMULTICAST) {
			TMulticast tm = (TMulticast) obj;
			if(tm.getSenderAddress() == null)
				throw new IllegalStateException(tm.toStringTooLong());
			if(!tm.getSenderAddress().equals(localSequencer.getLocalAddress()))
				throw new IllegalStateException(tm.getSenderAddress() + " vs. " + localSequencer.getLocalAddress());
		}
		
		String annotations = obj.getAnnotations();
		int annotationSize = annotations.length();
		int contentSize = obj.getContentSize();
		PacketableTypes type = obj.getObjectType();

		ByteBuffer hdr = getUnPreparedHeader();
		ByteBuffer bdy = ByteBuffer.allocate(contentSize + annotationSize);
		obj.putObjectInBuffer(localSequencer, bdy);
		
		bdy.position(contentSize);
		bdy.put(annotations.getBytes());
		
		RawPacket raw = new RawPacket(obj, hdr, bdy, type, contentSize, annotationSize);
		
		return raw.rewind();
	}
	
	public static IRawPacket reconstructPacket(SocketAddress remote, ByteBuffer bdyBuff) {
		ByteBuffer hdrBuff = getUnPreparedHeader();
		IRawPacket raw = RawPacket.reconstruct(hdrBuff, bdyBuff);
		raw.setLength(bdyBuff.limit());
		raw.setSender((InetSocketAddress) remote);
		raw.setType(PacketableTypes.getPacketableTypes(bdyBuff.get(0)));
		
		return raw;
	}
	
	public static IRawPacket reconstructPacket(ByteBuffer hdrBuff, ByteBuffer bdyBuff) {
		IRawPacket raw = RawPacket.reconstruct(hdrBuff, bdyBuff);
		return raw;
	}

	public static IPacketable reconstruct(byte[] hdr, byte[] bdy) {
		ByteBuffer hdrBuff = ByteBuffer.wrap(hdr);
		ByteBuffer bdyBuff = ByteBuffer.wrap(bdy);
		
//		hdrBuff.putInt(RawPacket.IHEADER_TYPE, PacketableTypes.TTEXT);
		
		IRawPacket raw = RawPacket.reconstruct(hdrBuff, bdyBuff);
		
//		System.out.println(raw.getType());
		
		return unwrapObject(null, raw);
	}
	
	public static void reconstructionFunction() {
		LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 2002));
		byte[] hdrBArray = new byte[] {
				102,0,59,0,0,0,0,0,0,0,0,0,0,127,0,0,1,0,0,15,-88,127,0,0,1,0,0,15,-94,0,0,0,59,0,0,
//				102,0,0,0,0,0,0,0,0,0,0,0,0,127,0,0,1,0,0,-11,-15,127,0,0,1,0,0,15,-94,0,0,117,48,0,0,
				};
		byte[] bdyBArray = new byte[] {
				102,5,127,0,0,1,0,0,15,-88,127,0,0,1,0,0,15,-88,0,0,1,44,38,-7,-105,-66,0,0,1,-5,1,9,99,111,117,
				110,116,9,49,9,78,79,68,69,73,68,9,52,48,48,56,9,119,111,114,100,9,50,0
//				102,5,127,0,0,1,0,0,15,-88,127,0,0,1,0,0,15,-88,0,0,1,44,38,-32,-41,-119,0,0,2,2,1,9,99,111,117,110,116,9,49,9,78,79,68,
//				69,73,68,9,52,48,48,56,9,119,111,114,100,9,50,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
			};
		IPacketable obj = reconstruct(hdrBArray, bdyBArray);
		
		System.out.println(obj); //OK
	}
	
	public static TCommand createTCommandMarkMessage(String comment) {
		return new TCommand(TCommandTypes.CMND_MARK, comment);
	}
	
	public static TCommand createTCommandDisseminateMessage(String comment) {
		return new TCommand(TCommandTypes.CMND_DISSEMINATE, comment);
	}
	
	public static void main2(String[] argv) {
		TText text = new TText("Salam.");
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, new InetSocketAddress("127.0.0.1", 2000));
		IRawPacket raw = wrapObject(brokerShadow.getLocalSequencer(), text);
		IPacketable packetable = unwrapObject(brokerShadow, raw);
		String str = packetable.toString();
		System.out.println(str); //OK
	}
	
	public static void main(String[] argv) {
		reconstructionFunction();
//		reconstructionBodyFunction();
	}
}
