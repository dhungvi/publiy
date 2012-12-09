package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.PubForwardingStrategy;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;

public class TMulticast_Publish extends TMulticast {
	
	public static final int PUBLICATION_DEFAULT_WORKLOAD_SIZE;
	static{
		String publicationWorkloadSizeStr = System.getProperty("TMPublish.WorkloadSize");
		if(publicationWorkloadSizeStr != null && ! publicationWorkloadSizeStr.equalsIgnoreCase(""))
			PUBLICATION_DEFAULT_WORKLOAD_SIZE = new Integer(publicationWorkloadSizeStr).intValue();
		else
			PUBLICATION_DEFAULT_WORKLOAD_SIZE = 0;	
	}

	protected InetSocketAddress _from;
	protected final Publication _publication;
	protected final byte[] _workload;
	
	protected final static int I_FROM = TMULTICAST_BASE_SIZE;
	protected final static int I_PUB_SIZE = I_FROM + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	protected final static int I_WORKLOAD_SIZE = I_PUB_SIZE + 2;
	protected final static int I_PUB = I_WORKLOAD_SIZE + 2;
	
	public TMulticast_Publish(Publication publication, InetSocketAddress from, LocalSequencer localSequencer) {
		this(TMulticastTypes.T_MULTICAST_PUBLICATION, publication, from, localSequencer);
	}

	public TMulticast_Publish(Publication publication, InetSocketAddress from, Sequence sourceSequence) {
		this(TMulticastTypes.T_MULTICAST_PUBLICATION, publication, from, sourceSequence);
	}

	protected TMulticast_Publish(TMulticastTypes tmType, Publication publication, InetSocketAddress from, Sequence sourceSequence) {
		this(tmType, publication, from, new byte[PUBLICATION_DEFAULT_WORKLOAD_SIZE], sourceSequence);
	}
	
	protected TMulticast_Publish(TMulticastTypes tmType, Publication publication, InetSocketAddress from, LocalSequencer localSequencer) {
		this(tmType, publication, from, new byte[PUBLICATION_DEFAULT_WORKLOAD_SIZE], localSequencer.getNext());
	}
	
	public TMulticast_Publish(Publication publication, InetSocketAddress from, byte[] workload, Sequence sourceSequence) {
		this(TMulticastTypes.T_MULTICAST_PUBLICATION, publication, from, workload, sourceSequence);
	}
	
	protected TMulticast_Publish(TMulticastTypes tmType, Publication publication, InetSocketAddress from, byte[] workload, Sequence sourceSequence) {
		super(tmType, sourceSequence);
		
		_workload = workload;
		_publication = publication;
		_from = from;
	}

	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_PUBLICATION;
	}

	@Override
	public TMulticastTypes getType() {
		return TMulticastTypes.T_MULTICAST_PUBLICATION;
	}
	
	public void setFrom(InetSocketAddress newFrom) {
		_from = newFrom;
	}
	
	public InetSocketAddress getFrom() {
		return _from;
	}
	
	protected TMulticast_Publish createNewTMulticastPublish(Publication publication, InetSocketAddress from, byte[] workload, Sequence sourceSequence) {
		return new TMulticast_Publish(publication, from, workload, sourceSequence);
	}
	
	public TMulticast_Publish getShiftedClone(int shift, Sequence seq) {
		TMulticast_Publish clone = createNewTMulticastPublish(this._publication, this._from, this._workload, this._sourceSequence);
		TMulticast.duplicate(this, clone);
		clone.shiftSequenceVector(shift);
		
		if(seq==null)
			throw new NullPointerException("Shifting '" + shift + "': " + this);
		
		clone._sequencesVector[shift-1] = seq;
		return clone;
	}
	
	private static int getPubSize(ByteBuffer bdy) {
		return bdy.getShort(I_PUB_SIZE);
	}
	
	private static int getWorkloadSize(ByteBuffer bdy) {
		return bdy.getShort(I_WORKLOAD_SIZE);
	}
	
	protected TMulticast_Publish(ByteBuffer bdy, int contentSize, int subContentSize, String annotations) {
		super(bdy, contentSize, annotations);

		byte[] fromBArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_FROM);
		bdy.get(fromBArray);
		_from = Sequence.readAddressFromByteArray(fromBArray);

		int pubSize = getPubSize(bdy);
		byte[] pubBArray = new byte[pubSize];
		bdy.position(I_PUB);
		bdy.get(pubBArray);
		_publication = readPublicationFromByteArray(pubBArray);
		
		int workloadSize = getWorkloadSize(bdy);
		int workloadIndex = I_PUB + pubSize;
		_workload = new byte[workloadSize];
		bdy.position(workloadIndex);
		bdy.get(_workload);
	}
	
	@Override
	public String toString() {
		if(!_isGuided)
			return getPrefix() + ": " + _publication + " " + _sourceSequence;
		else
			return getPrefix() + ": " + _publication + " " + _sourceSequence
																 + "{{" + _guidedInfo + "}}";
	}
	
	protected String getPrefix() {
		if(!_isGuided) 
			return "TMulticast_Publication";
		else
			return "G_TMulticast_Publication";
	}
	
	public String toStringLong() {
		String str = toString();
		return str + "[" + _workload.length + "]";
	}
	
	@Override
	public String toStringTooLong() {
		return super.toStringTooLong() + "[" + _publication + "]_[" + new String(_workload) + "]";
	}
	
	public Publication getPublication() {
		return _publication;
	}
	
	public static byte[] getPublicationInByteArray(Publication pub) {
		return pub.encode().getBytes();
	}
	
//	@Deprecated
//	public static byte[] getPublicationInByteArray_deprecated(Publication pub) {
//		byte[] bArray = new byte[1000];
//		int index = 1;
//		
//		Set<Entry<String, List<Integer>>> predSet = pub._predicates.entrySet();
//		Iterator<Entry<String, List<Integer>>> predIt = predSet.iterator();
//		while ( predIt.hasNext())
//		{
//			Entry<String, List<Integer>> entryList = predIt.next();
//			String attr = entryList.getKey();
//			List<Integer> valueList = entryList.getValue();
//			int intValue = value.intValue();
//			
//			bArray[index++] = (byte)attr.length();
//			byte[] attrBytes = attr.getBytes();
//			for(int i=0 ; i<attr.length() ; i++)
//				bArray[index++] = attrBytes[i];
//			
//			bArray[index++] = (byte) ((intValue >> 8 * 3) & 0xFF);
//			bArray[index++] = (byte) ((intValue >> 8 * 2) & 0xFF);
//			bArray[index++] = (byte) ((intValue >> 8 * 1) & 0xFF);
//			bArray[index++] = (byte) ((intValue >> 8 * 0) & 0xFF);
//		}
//		
//		bArray[0] = (byte)predSet.size();
//		
//		byte[] retBytes = new byte[index];
//		for(int i=0 ; i<index ; i++)
//			retBytes[i] = bArray[i];
//		return retBytes;
//	}

	public Publication readPublicationFromByteArray(byte[] bArray) {
		return Publication.decode(new String(bArray));
	}
	
//	@Deprecated
//	public static Publication readPublicationFromByteArray_deprecated(byte[] bArray) {
//		Publication publication = new Publication();
//		int index = 0;
//		int predCount = bArray[index++];
//		
//		for(int i=0 ; i<predCount ; i++)
//		{
//			int attrLen = bArray[index++];
//			String attr = "";
//			for(int j=0 ; j<attrLen ; j++)
//				attr += (char)bArray[index++];
//			int value1 = (bArray[index++] << 8 * 3) & 0xFF000000;
//			int value2 = (bArray[index++] << 8 * 2) & 0xFF0000;
//			int value3 = (bArray[index++] << 8 * 1) & 0xFF00;
//			int value4 = (bArray[index++] << 8 * 0) & 0xFF;
//			
//			int value = value1 + value2 + value3 + value4;
//			publication.addPredicate(attr, value);
//		}
//		
//		return publication;
//	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		putObjectInBuffer(localSequencer, buff, true);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, boolean addLocalSeq) {
		super.putObjectInBuffer(localSequencer, buff, addLocalSeq);
		
		byte[] fromBArray = Sequence.getAddressInByteArray(_from);
		buff.position(I_FROM);
		buff.put(fromBArray);
		
		byte[] pubInByteArray = getPublicationInByteArray(_publication);
		int pubSize = pubInByteArray.length;
		if(pubSize >= Short.MAX_VALUE)
			throw new IllegalStateException("Too large: " + pubSize);
		
		buff.position(I_PUB_SIZE);
		buff.putShort((short)pubSize);
		buff.position(I_PUB);
		buff.put(pubInByteArray);
		
		buff.position(I_WORKLOAD_SIZE);
		buff.putShort((short)_workload.length);
		buff.position(I_PUB + pubSize);
		buff.put(_workload);
	}		
	
	public PubForwardingStrategy getPubForwardingStrategy() {
		return PubForwardingStrategy.PUB_FORWARDING_STRATEGY_0;
	}
	
	@Override
	public int getTMSpecificContentSize() {
		byte[] pubInByteArray = getPublicationInByteArray(_publication);
		int subContentSize = Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE + 2 + 2 + pubInByteArray.length + _workload.length;

		return subContentSize + super.getTMSpecificContentSize();
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
		
		TMulticast_Publish tmPubObj = (TMulticast_Publish) obj;
		if(tmPubObj._workload==null) {
			if(_workload!=null)
				return false;
		} else {
			if(_workload == null)
				return false;
			if(_workload.length != tmPubObj._workload.length)
				return false;
			for(int i=0 ; i<_workload.length ; i++)
				if(_workload[i] != tmPubObj._workload[i])
					return false;
		}
		return _publication.equals(tmPubObj._publication);
	}
	
	public static void main(String[] argv) {
		LocalSequencer localSequencer = LocalSequencer.init(null, Broker.bAddress8);
		Publication publication = new Publication();
		publication.addPredicate("Salam", 100);

		TMulticast_Publish tmp = new TMulticast_Publish(publication, Broker.bAddress1, localSequencer);
		TMulticast_Publish[] tmps = TMulticast_Publish_MP.getBundledTMulticast_Publish_MPs(publication, Broker.bAddress1, localSequencer);
		tmps = TMulticast_Publish_MP.getBundledTMulticast_Publish_MPs(publication, Broker.bAddress1, localSequencer);
		for(int i=0 ; i<tmps.length ; i++)
			System.out.println("XXXX\t" + tmps[i]);
		
		tmp = tmps[1];
		Set<InetSocketAddress> guidedInfo = new HashSet<InetSocketAddress>();
		guidedInfo.add(Broker.bAddress3);
		guidedInfo.add(Broker.bAddress4);
		guidedInfo.add(Broker.bAddress5);
		tmp.loadGuidedInfo(guidedInfo);
		
		tmp.annotate("THIS IS AN ANNOTATION!!");
		System.out.println("YYY: " + tmp.toStringTooLong());
		
		Sequence[] seqs = new Sequence[Broker.VSEQ_LENGTH];
		for(int i=0 ; i<seqs.length ; i++)
			seqs[i] = Sequence.getPsuedoSequence(Broker.bAddress8, i);
		tmp._sequencesVector = seqs;
				
		for(int i=0; i<tmp._sequencesVector.length ; i++)
			System.out.print(tmp._sequencesVector[i] + " "); // OK
		System.out.println(); // OK
		
		tmp.shiftSequenceVector(1);
		tmp.shiftSequenceVector(2);
		for(int i=0; i<tmp._sequencesVector.length ; i++)
			System.out.print(tmp._sequencesVector[i] + " "); // OK
		
		System.out.println(); // OK

		tmp.setDeltaViolating(true);
		tmp.setLegitimacyViolated(true);
		tmp.setLegitimacyViolating(true);
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, tmp);
		tmp = (TMulticast_Publish) PacketFactory.unwrapObject(null, raw);
		if(!tmp.getDeltaViolating())
			throw new IllegalStateException("Should be true");
		else
			System.out.println("OKAY: delta");
		
		System.out.println("SHIFTS: " + tmp._shifted);
		if(!tmp.getLegitimacyViolated())
			throw new IllegalStateException("Should be false");
		else
			System.out.println("OKAY: legited");

		if(!tmp.getLegitimacyViolating())
			throw new IllegalStateException("Should be false");
		else
			System.out.println("OKAY: legiting");
		
		System.out.println(tmp.toStringTooLong());
		
		for(int i=0; i<tmp._sequencesVector.length ; i++)
			System.out.print(tmp._sequencesVector[i] + " "); // OK
		
//		tmp.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress7, 700));
		raw = PacketFactory.wrapObject(localSequencer, tmp);
		tmp = (TMulticast_Publish) PacketFactory.unwrapObject(null, raw);

		System.out.println("SHIFTS: " + tmp._shifted);
		raw = PacketFactory.wrapObject(localSequencer, tmp);
		tmp = (TMulticast_Publish) PacketFactory.unwrapObject(null, raw);
		
		System.out.println(tmp.toStringTooLong());
		System.out.println(); // OK
		for(int i=0; i<tmp._sequencesVector.length ; i++)
			System.out.print(tmp._sequencesVector[i] + " "); // OK
		
		tmp.annotate("Hi");
		
		TMulticast_Publish tmp2 = (TMulticast_Publish) PacketFactory.unwrapObject(null, raw);
		String annotations = tmp2.getAnnotations();
		System.out.println(annotations); // OK
		
		System.out.println("SHIFTS: " + tmp._shifted);
		System.out.println("SHIFTS: " + tmp2._shifted);
	}
	
	public static void main2(String[] argv) {
//		InetSocketAddress localAddr = new InetSocketAddress("127.0.0.1", 1000);
//		InetSocketAddress localAddr2 = new InetSocketAddress("127.0.0.1", 2000);
//		LocalSequencer.init(null, localAddr);
//
//		Publication publication = new Publication();
//		publication.addPredicate("Salam", 100);
//		publication.addPredicate("BYEYEYEY", 10000000);
//
//		Set<InetSocketAddress> guidedInfo = new HashSet<InetSocketAddress>();
//		guidedInfo.add(Broker.bAddress3);
//		guidedInfo.add(Broker.bAddress4);
//		guidedInfo.add(Broker.bAddress5);
//
//		TMulticast_Publish tmp = new TMulticast_Publish(publication, localAddr2);
//		tmp.loadGuidedInfo(guidedInfo);
//		tmp.annotate("AnnotationString");
//		ByteBuffer buff = tmp.putObjectInBuffer();
//		TMulticast_Publish tmp2 = new TMulticast_Publish(buff);
//		String annotations = tmp2.getAnnotations();
//		System.out.println(annotations); // OK
		
//		System.out.println(tmp);
//		System.out.println(tmp2);
	}
}
