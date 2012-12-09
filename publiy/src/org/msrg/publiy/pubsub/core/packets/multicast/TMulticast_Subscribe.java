package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.client.subscriber.ISubscriptionListener;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;


import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;


public class TMulticast_Subscribe extends TMulticast {
	
	public ISubscriptionListener _subscriber;
	
	private Subscription _subscription;
	private InetSocketAddress _from;
	private boolean _realFrom;
	
	private static final int I_FROM = I_SUB_CONTENT;
	private static final int I_REAL_FROM = I_FROM + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	private static final int I_SUBSCRIPTION_SIZE = I_REAL_FROM + 1;
	private static final int I_SUB = I_SUBSCRIPTION_SIZE + 2;
	
	public InetSocketAddress getFrom() {
		return _from;
	}
	
	private TMulticast_Subscribe(Sequence sourceSequence, Subscription sub, InetSocketAddress from) {
		super(getStaticType(), sourceSequence);
		_subscription = sub;
		_from = from;
		_realFrom = _sourceSequence.getAddress().equals(_from);
	}
	
	public TMulticast_Subscribe(Subscription sub, InetSocketAddress from, LocalSequencer localSequencer) {
		super(getStaticType(), localSequencer);
		_subscription = sub;
		_from = from;
		_realFrom = _from.equals(_sourceSequence.getAddress());
		
		if (!Broker.RELEASE || Broker.DEBUG)
			if (_realFrom != (_sourceSequence.getAddress().equals(_from)))
				throw new IllegalStateException(this.toString());
	}
	
	public void setFrom(InetSocketAddress newFrom) {
		_from = newFrom;
		_realFrom = _from.equals(_sourceSequence.getAddress());
	}
	
	public boolean isFromReal() {
		return _realFrom;
	}
	
	public TMulticast_Subscribe getClone() {
		TMulticast_Subscribe clone = new TMulticast_Subscribe(this._sourceSequence, this._subscription, this._from);
		TMulticast.duplicate(this, clone);
		return clone;
	}
	
	public TMulticast_Subscribe getShiftedClone(int shift, Sequence sequence) {
		TMulticast_Subscribe clone = getClone();
		clone.shiftSequenceVector(shift);
		
		if (sequence==null)
			throw new NullPointerException("Shifting '" + shift + "': " + this);

		clone._sequencesVector[shift-1] = sequence;
		return clone;
	}

	protected TMulticast_Subscribe(ByteBuffer bdy, int contentSize, int subContentSize, String annotations) {
		super(bdy, contentSize, annotations);

		byte[] fromBArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_FROM);
		bdy.get(fromBArray);
		_from = Sequence.readAddressFromByteArray(fromBArray);
		
		bdy.position(I_REAL_FROM);
		_realFrom = (bdy.get()!=0);
		
		int subscriptionSize = getSubscriptionSize(bdy);
		byte[] bArray = new byte[subscriptionSize];
		bdy.position(I_SUB);
		bdy.get(bArray);
		
		_subscription = readSubscriptionFromByteArray(bArray);
		
		if (_realFrom != (_sourceSequence.getAddress().equals(_from)))
			throw new IllegalStateException(this.toString());
	}
	
	public Subscription getSubscription() {
		return _subscription;
	}
	
	public static int getSubscriptionSize(ByteBuffer bdy) {
		return bdy.getShort(I_SUBSCRIPTION_SIZE);
	}
	
	public static void setSubscriptionSize(ByteBuffer bdy, int subscriptionSize) {
		if(subscriptionSize >= Short.MAX_VALUE)
			throw new IllegalArgumentException("Too large: " + subscriptionSize);
		
		bdy.putShort(I_SUBSCRIPTION_SIZE, (short)subscriptionSize);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		putObjectInBuffer(localSequencer, buff, true);
	}
	
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, boolean addLocalSeq) {
		super.putObjectInBuffer(localSequencer, buff, addLocalSeq);
		
		byte[] fromBArray = Sequence.getAddressInByteArray(_from);
		buff.position(I_FROM);
		buff.put(fromBArray);
		
		buff.position(I_REAL_FROM);
		buff.put((byte)(_realFrom?1:0));
		
		int subSize = getTMSpecificContentSize();
		setSubscriptionSize(buff, subSize - 2 - 1 - Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
		
		byte[] subInByteArray = getSubscriptionInByteArray(_subscription);
		buff.position(I_SUB);
		buff.put(subInByteArray);
	}		
	
	public static Subscription readSubscriptionFromByteArray(byte[] bArray) {
		return Subscription.decode(new String(bArray));
	}
	
	@Deprecated
	public static Subscription readSubscriptionFromByteArray_deprecated(byte[] bArray) {
		int sPredCount = bArray[0];
		Subscription subscription = new Subscription();
		
		int index = 1;
		for ( int i=0 ; i<sPredCount ; i++)
		{
			int sPredSize = bArray[index++];
			String attr = "";
			for ( int j=0 ; j<sPredSize - 5 ; j++)
				attr += "" + (char) bArray[index++];
			char operator = (char) bArray[index++];
			int value1 = (bArray[index++] << 8 * 3) & 0xFF000000;
			int	value2 = (bArray[index++] << 8 * 2) & 0xFF0000;
			int	value3 = (bArray[index++] << 8 * 1) & 0xFF00;
			int value4 = (bArray[index++] << 8 * 0) & 0xFF;
			
			int value = value1 + value2 + value3 + value4;
				
			SimplePredicate sPred = SimplePredicate.buildSimplePredicate(attr, operator, value);
			
			subscription.addPredicate(sPred);
		}
		
		return subscription;
	}
	
	public static byte[] getSubscriptionInByteArray(Subscription sub) {
		return Subscription.encode(sub).getBytes();
	}
	
	@Deprecated
	public static byte[] getSubscriptionInByteArray_deprecated(Subscription sub) {
		byte[] bytes = new byte[1000];
		
		if(sub == null) {
			bytes[0] = 0;
			return bytes;
		}
			
		bytes[0] = (byte) sub._predicates.size();
		
		int index = 1;
		
		for(SimplePredicate sPred : sub._predicates) {
			int predicateSize = sPred._attribute.length() + 1 + 4;
			bytes[index++] = (byte) predicateSize;
			
			byte[] sPredBytes = sPred._attribute.getBytes();
			for ( int i=0 ; i<sPredBytes.length ; i++)
				bytes[index++] = sPredBytes[i];
			
			bytes[index++] = (byte) sPred._operator;
			bytes[index++] = (byte)((byte) (sPred._intValue >> 8 * 3) & 0xFF);
			bytes[index++] = (byte)((byte) (sPred._intValue >> 8 * 2) & 0xFF);
			bytes[index++] = (byte)((byte) (sPred._intValue >> 8 * 1) & 0xFF);
			bytes[index++] = (byte)((byte) (sPred._intValue >> 8 * 0) & 0xFF);
		}
		
		byte[] retBytes = new byte[index];
		for ( int i=0 ; i<index ; i++)
			retBytes[i] = bytes[i];
		return retBytes;
	}
	
	@Override
	public String toString() {
		if(!_isGuided)
			return "TMulticast_Subscription: " + _from + " _ " + _subscription + (_realFrom?"*":".");// + " _id_ " + _sourceSequence;
		else
			return "G_TMulticast_Subscription: " + _from + " _ " + _subscription + (_realFrom?"*":".");// + " _id_ " + _sourceSequence + "{{" + _guidedInfo + "}}";
	}
	
	public static void main(String[] argv) {
		InetSocketAddress localAddr = new InetSocketAddress("127.0.0.1", 1000);
		InetSocketAddress localAddr2 = new InetSocketAddress("127.0.0.1", 2000);
		Subscription sub = new Subscription();
		SimplePredicate sp1 = SimplePredicate.buildSimplePredicate("SALAM", '=', 113100);
		SimplePredicate sp2 = SimplePredicate.buildSimplePredicate("SALAM", '=', 131100);
		SimplePredicate sp3 = SimplePredicate.buildSimplePredicate("SALAM", '=', 113100);
		sub.addPredicate(sp1);
		sub.addPredicate(sp2);
		sub.addPredicate(sp3);
		
		Set<InetSocketAddress> guidedInfo = new HashSet<InetSocketAddress>();
		guidedInfo.add(Broker.bAddress3);
		guidedInfo.add(Broker.bAddress4);
		guidedInfo.add(Broker.bAddress5);
		
		LocalSequencer localSequencer = LocalSequencer.init(null, localAddr);
		TMulticast_Subscribe tms = new TMulticast_Subscribe(sub, localAddr2, localSequencer);
		tms.loadGuidedInfo(guidedInfo);
		tms.annotate("AnnotationString");
		
//		tms.setFrom(localAddr2);
		tms = tms.getShiftedClone(1, localSequencer.getNext());
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, tms);
		TMulticast_Subscribe tms2 = (TMulticast_Subscribe) PacketFactory.unwrapObject(null, raw, true);
		
//		ByteBuffer buff = tms.putObjectInBuffer();
//		TMulticast_Subscribe tms2 = new TMulticast_Subscribe(buff);
//		String annotations = tms2.getAnnotations();
//		System.out.println(annotations); // OK
		
		System.out.println(tms);
		System.out.println(tms2);
		System.out.println(tms.getClone());
	}
	
	@Override
	public boolean equals(Object obj) {
		if(Sequence.class.isInstance(obj)) {
			Sequence oSequence = (Sequence) obj;
			return oSequence.equalsExact(_sourceSequence);
		}
		
		if(!super.equals(obj))
			return false;
		
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		TMulticast_Subscribe tmSubObj = (TMulticast_Subscribe) obj;
		return tmSubObj._subscription.equals(_subscription);
	}

	@Override
	public int getTMSpecificContentSize() {
		byte[] subInByteArray = getSubscriptionInByteArray(_subscription);
		int subContentSize = Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE + 1 + 2 + subInByteArray.length;
		
		return subContentSize;
	}

	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_SUBSCRIPTION;
	}
	
	@Override
	public TMulticastTypes getType() {
		return TMulticastTypes.T_MULTICAST_SUBSCRIPTION;
	}
}
