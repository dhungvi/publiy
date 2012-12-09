package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.publishSubscribe.Advertisement;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;


public class TMulticast_Advertisement extends TMulticast {
	
	private Advertisement _advertisement;
	private InetSocketAddress _from;
	private boolean _realFrom;
	
	private static final int I_FROM = I_SUB_CONTENT;
	private static final int I_REAL_FROM = I_FROM + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	private static final int I_ADVERTISEMENT_SIZE = I_REAL_FROM + 1;
	private static final int I_ADVERTISEMENT = I_ADVERTISEMENT_SIZE + 2;
	
	public InetSocketAddress getFrom(){
		return _from;
	}
	
	private TMulticast_Advertisement(Sequence sourceSequence, Advertisement adv, InetSocketAddress from){
		super(TMulticastTypes.T_MULTICAST_ADVERTISEMENT, sourceSequence);
		_advertisement = adv;
		_from = from;
		_realFrom = _sourceSequence.getAddress().equals(_from);
	}
	
	public TMulticast_Advertisement(Advertisement adv, InetSocketAddress from, LocalSequencer localSequencer){
		super(TMulticastTypes.T_MULTICAST_ADVERTISEMENT, localSequencer);
		_advertisement = adv;
		_from = from;
		_realFrom = _from.equals(_sourceSequence.getAddress());
		
		if (!Broker.RELEASE || Broker.DEBUG)
			if (_realFrom != (_sourceSequence.getAddress().equals(_from)))
				throw new IllegalStateException(this.toString());
	}
	
	public void setFrom(InetSocketAddress newFrom){
		_from = newFrom;
		_realFrom = _from.equals(_sourceSequence.getAddress());
	}
	
	public boolean isFromReal() {
		return _realFrom;
	}
	
	public TMulticast_Advertisement getClone(){
		TMulticast_Advertisement clone = new TMulticast_Advertisement(this._sourceSequence, this._advertisement, this._from);
		TMulticast.duplicate(this, clone);
		return clone;
	}
	
	public TMulticast_Advertisement getShiftedClone(int shift, Sequence sequence){
		TMulticast_Advertisement clone = getClone();
		clone.shiftSequenceVector(shift);
		
		if (sequence==null)
			throw new NullPointerException("Shifting '" + shift + "': " + this);

		clone._sequencesVector[shift-1] = sequence;
		return clone;
	}

	protected TMulticast_Advertisement(ByteBuffer bdy, int contentSize, int subContentSize, String annotations) {
		super(bdy, contentSize, annotations);

		byte[] fromBArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_FROM);
		bdy.get(fromBArray);
		_from = Sequence.readAddressFromByteArray(fromBArray);
		
		bdy.position(I_REAL_FROM);
		_realFrom = (bdy.get()!=0);
		
		int advertisementSize = getAdvertisementSize(bdy);
		byte[] bArray = new byte[advertisementSize];
		bdy.position(I_ADVERTISEMENT);
		bdy.get(bArray);
		
		_advertisement = readAdvertisementFromByteArray(bArray);
		
		if (_realFrom != (_sourceSequence.getAddress().equals(_from)))
			throw new IllegalStateException(this.toString());
	}
	
	public Advertisement getAdvertisement() {
		return _advertisement;
	}
	
	public static int getAdvertisementSize(ByteBuffer bdy){
		return bdy.getShort(I_ADVERTISEMENT_SIZE);
	}
	
	public static void setAdvertisementSize(ByteBuffer bdy, int advertisementSize){
		if ( advertisementSize >= Short.MAX_VALUE )
			throw new IllegalArgumentException("Too large: " + advertisementSize);
		
		bdy.putShort(I_ADVERTISEMENT_SIZE, (short)advertisementSize);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff){
		putObjectInBuffer(localSequencer, buff, true);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, boolean addLocalSeq) {
		super.putObjectInBuffer(localSequencer, buff, addLocalSeq);
		
		byte[] advInByteArray = getAdvertisementInByteArray(_advertisement);

		byte[] fromBArray = Sequence.getAddressInByteArray(_from);
		buff.position(I_FROM);
		buff.put(fromBArray);
		
		buff.position(I_REAL_FROM);
		buff.put((byte)(_realFrom?1:0));
		
		int advSize = getTMSpecificContentSize();
		setAdvertisementSize(buff, advSize - 2 - 1 - Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
		
		buff.position(I_ADVERTISEMENT);
		buff.put(advInByteArray);
	}		
	
	public static Advertisement readAdvertisementFromByteArray(byte[] bArray){
		int sPredCount = bArray[0];
		Advertisement advertisement = new Advertisement();
		
		int index = 1;
		for ( int i=0 ; i<sPredCount ; i++ )
		{
			int sPredSize = bArray[index++];
			String attr = "";
			for ( int j=0 ; j<sPredSize - 5 ; j++ )
				attr += "" + (char) bArray[index++];
			char operator = (char) bArray[index++];
			int value1 = (bArray[index++] << 8 * 3) & 0xFF000000;
			int	value2 = (bArray[index++] << 8 * 2) & 0xFF0000;
			int	value3 = (bArray[index++] << 8 * 1) & 0xFF00;
			int value4 = (bArray[index++] << 8 * 0) & 0xFF;
			
			int value = value1 + value2 + value3 + value4;
				
			SimplePredicate sPred = SimplePredicate.buildSimplePredicate(attr, operator, value);
			
			advertisement.addPredicate(sPred);
		}
		
		return advertisement;
	}
	
	public static byte[] getAdvertisementInByteArray(Advertisement adv){
		byte[] bytes = new byte[1000];
		
		LinkedList<SimplePredicate> sPredicates = adv._predicates;
		bytes[0] = (byte) sPredicates.size();
		
		int index = 1;
		
		Iterator<SimplePredicate> sPredIt = sPredicates.iterator();
		while (sPredIt.hasNext())
		{
			SimplePredicate sPred = sPredIt.next();
			int predicateSize = sPred._attribute.length() + 1 + 4;
			bytes[index++] = (byte) predicateSize;
			
			byte[] sPredBytes = sPred._attribute.getBytes();
			for ( int i=0 ; i<sPredBytes.length ; i++ )
				bytes[index++] = sPredBytes[i];
			
			bytes[index++] = (byte) sPred._operator;
			bytes[index++] = (byte)((byte) (sPred._intValue >> 8 * 3) & 0xFF);
			bytes[index++] = (byte)((byte) (sPred._intValue >> 8 * 2) & 0xFF);
			bytes[index++] = (byte)((byte) (sPred._intValue >> 8 * 1) & 0xFF);
			bytes[index++] = (byte)((byte) (sPred._intValue >> 8 * 0) & 0xFF);
		}
		
		byte[] retBytes = new byte[index];
		for ( int i=0 ; i<index ; i++ )
			retBytes[i] = bytes[i];
		return retBytes;
	}
	
	@Override
	public String toString(){
		if ( !_isGuided )
			return "TMulticast_Advertisement: " + _from + " _ " + _advertisement + (_realFrom?"*":".");// + " _id_ " + _sourceSequence;
		else
			return "G_TMulticast_Advertisement: " + _from + " _ " + _advertisement + (_realFrom?"*":".");// + " _id_ " + _sourceSequence + "{{" + _guidedInfo + "}}";
	}
	
	public static void main(String[] argv){
		InetSocketAddress localAddr = new InetSocketAddress("127.0.0.1", 1000);
		InetSocketAddress localAddr2 = new InetSocketAddress("127.0.0.1", 2000);
		Advertisement adv = new Advertisement();
		SimplePredicate sp1 = SimplePredicate.buildSimplePredicate("SALAM", '=', 113100);
		SimplePredicate sp2 = SimplePredicate.buildSimplePredicate("SALAM", '=', 131100);
		SimplePredicate sp3 = SimplePredicate.buildSimplePredicate("SALAM", '=', 113100);
		adv.addPredicate(sp1);
		adv.addPredicate(sp2);
		adv.addPredicate(sp3);
		
		Set<InetSocketAddress> guidedInfo = new HashSet<InetSocketAddress>();
		guidedInfo.add(Broker.bAddress3);
		guidedInfo.add(Broker.bAddress4);
		guidedInfo.add(Broker.bAddress5);
		
		LocalSequencer localSequencer = LocalSequencer.init(null, localAddr);
		TMulticast_Advertisement tms = new TMulticast_Advertisement(adv, localAddr2, localSequencer);
		tms.loadGuidedInfo(guidedInfo);
		tms.annotate("AnnotationString");
		
//		tms.setFrom(localAddr2);
		tms = tms.getShiftedClone(1, localSequencer.getNext());
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, tms);
		TMulticast_Advertisement tms2 = (TMulticast_Advertisement) PacketFactory.unwrapObject(null, raw, true);
		
//		ByteBuffer buff = tms.putObjectInBuffer();
//		TMulticast_Advertisement tms2 = new TMulticast_Advertisement(buff);
//		String annotations = tms2.getAnnotations();
//		System.out.println(annotations); // OK
		
		System.out.println(tms);
		System.out.println(tms2);
		System.out.println(tms.getClone());
	}

	@Override
	public int getTMSpecificContentSize() {
		byte[] advInByteArray = getAdvertisementInByteArray(_advertisement);
		int advContentSize = Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE + 1 + 2 + advInByteArray.length;
		
		return advContentSize;
	}

	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_ADVERTISEMENT;
	}
	
	@Override
	public TMulticastTypes getType() {
		return TMulticastTypes.T_MULTICAST_ADVERTISEMENT;
	}
}
