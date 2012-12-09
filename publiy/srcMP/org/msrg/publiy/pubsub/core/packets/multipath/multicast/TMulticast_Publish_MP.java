package org.msrg.publiy.pubsub.core.packets.multipath.multicast;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.utils.LocalAddressGrabber;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;

public class TMulticast_Publish_MP extends TMulticast_Publish {

	private static final Set<Inet4Address> _localAddresses = LocalAddressGrabber.grabAll4Addresses(true);
	private static final char PREFIX_SEPARATOR = Broker.RELEASE?' ':'_';
	
	private static int BUNDLE_COUNTER = 0;
	public final int _mpBundleIndex;
	public final PubForwardingStrategy _forwardingStrategy;
	public final long _gTime;
	public final long _dTime;
	public final byte _pathLength;
	protected Set<InetSocketAddress> _exclusionSet = new HashSet<InetSocketAddress>(); 
	
	protected TMulticast_Publish_MP(TMulticastTypes tmType, Publication publication,
			InetSocketAddress from, int mpBundleIndex, 
			byte pathLenght, PubForwardingStrategy forwardingStrategy, Sequence sourceSequence) {
		super(tmType, publication, from, sourceSequence);
		
		_forwardingStrategy = forwardingStrategy;
		_mpBundleIndex = mpBundleIndex;
		_gTime = SystemTime.currentTimeMillis();
		_pathLength = pathLenght;
		_dTime = -1;
	}

	
	public TMulticast_Publish_MP(Publication publication,
			InetSocketAddress from, int mpBundleIndex, 
			byte pathLenght, PubForwardingStrategy forwardingStrategy, Sequence sourceSequencer) {
		this(TMulticastTypes.T_MULTICAST_PUBLICATION_MP, publication, from,
				mpBundleIndex, pathLenght, forwardingStrategy, sourceSequencer);
	}

	public TMulticast_Publish_MP(ByteBuffer bdy, int contentSize,
			int subContentSize, String annotations) {
		super(bdy, contentSize, subContentSize, annotations);
		
		_dTime = SystemTime.currentTimeMillis();
		int I_EXCLUSION_LENGTH = bdy.capacity()-(getAnnotations().length()+1);
		int exclusionLenght = bdy.get(I_EXCLUSION_LENGTH);
		int endOffset = bdy.capacity() - (1+4+1+8+getGuidedSize()+getAnnotations().length()+1+exclusionLenght*Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
		
		int I_FORWARDING_STRATEGY = endOffset;
		byte forwardingStrategyByteCode = bdy.get(I_FORWARDING_STRATEGY);
		_forwardingStrategy = PubForwardingStrategy.getPubForwardingStrategy(forwardingStrategyByteCode);
	
		int I_BUNDLE_INDEX = endOffset + 1;
		_mpBundleIndex = bdy.getInt(I_BUNDLE_INDEX);
		
		int I_TIME = endOffset + 1 + 4;
		_gTime = bdy.getLong(I_TIME);
		
		int I_PATH_LENGHT = endOffset + 1 + 4 + 8;
		_pathLength = bdy.get(I_PATH_LENGHT);
		
		byte[] exclusionAddressBytes = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		int I_EXCLUSION = I_PATH_LENGHT + 1;
		for (int i=0 ; i<exclusionLenght ; i++) {
			bdy.position(I_EXCLUSION + i*Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
			bdy.get(exclusionAddressBytes, 0, Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
			InetSocketAddress exclusionAddress = Sequence.readAddressFromByteArray(exclusionAddressBytes);
			_exclusionSet.add(exclusionAddress);
		}
	}

	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff, boolean addLocalSeq) {
		super.putObjectInBuffer(localSequencer, buff, addLocalSeq);
		
		int endOffset = buff.capacity() - (1+4+1+8+getGuidedSize()+getAnnotations().length()+1+_exclusionSet.size()*Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
		
		int I_FORWARDING_STRATEGY = endOffset;
		byte forwardingStrategyByteCode = _forwardingStrategy.getByteCode();
		buff.put(I_FORWARDING_STRATEGY, forwardingStrategyByteCode);
		
		int I_BUNDLE_INDEX = endOffset + 1;
		buff.putInt(I_BUNDLE_INDEX, _mpBundleIndex);
		
		int I_TIME = endOffset + 1 + 4;
		buff.putLong(I_TIME, _gTime);

		int I_PATH_LENGHT = endOffset + 1 + 4 + 8;
		buff.put(I_PATH_LENGHT, (byte) (_pathLength+1));
		
		int I_EXCLUSION = I_PATH_LENGHT + 1;
		InetSocketAddress[] exclusionAddresses = _exclusionSet.toArray(new InetSocketAddress[0]);
		for(int i=0 ; i<exclusionAddresses.length ; i++) {
			byte[] exclusionAddressBytes = Sequence.getAddressInByteArray(exclusionAddresses[i]);
			buff.position(I_EXCLUSION + i*Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
			buff.put(exclusionAddressBytes, 0, Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
		}
		int I_EXCLUSION_LENGTH = buff.capacity()-(getAnnotations().length()+1);
		buff.put(I_EXCLUSION_LENGTH, (byte)_exclusionSet.size());

	}

	public  PubForwardingStrategy getPubForwardingStrategy(){
		return _forwardingStrategy;
	}

	@Override
	public TMulticast_Publish_MP getShiftedClone(int shift, Sequence seq){
		TMulticast_Publish_MP clone =
				new TMulticast_Publish_MP(
						this._publication, this._from, this._mpBundleIndex, this._pathLength, this._forwardingStrategy, this._sourceSequence);
		TMulticast.duplicate(this, clone);
		clone.shiftSequenceVector(shift);
		
		if (seq==null)
			throw new NullPointerException("Shifting '" + shift + "': " + this);
		
		clone._sequencesVector[shift-1] = seq;
		clone._exclusionSet.addAll(this._exclusionSet);
		return clone;
	}

	@Override
	public int getTMSpecificContentSize(){
		return super.getTMSpecificContentSize() + (1 + 4 + 8 + 1) + (1 + _exclusionSet.size()*Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
	}

	public static int getBundleSize(){
		return PubForwardingStrategy.values().length - 1;
	}
	
	public static synchronized TMulticast_Publish_MP[] getBundledTMulticast_Publish_MPs(Publication publication, InetSocketAddress from, LocalSequencer localSequencer){
		PubForwardingStrategy[] forwardingStrategies = PubForwardingStrategy.values();
		TMulticast_Publish_MP[] bundle = new TMulticast_Publish_MP[forwardingStrategies.length-1];
		for ( int i=1 ; i<forwardingStrategies.length ; i++ )
			bundle[i-1] = new TMulticast_Publish_MP(publication, from, BUNDLE_COUNTER, (byte)0, forwardingStrategies[i], localSequencer.getNext());
		
		BUNDLE_COUNTER++;
		return bundle;
	}
	
	@Override
	protected String getPrefix(){
		return super.getPrefix() + "_MP[" + PREFIX_SEPARATOR 
			+ _forwardingStrategy + PREFIX_SEPARATOR 
			+ _mpBundleIndex + PREFIX_SEPARATOR 
			+ _pathLength + PREFIX_SEPARATOR + 
			_gTime + PREFIX_SEPARATOR +
			_dTime + PREFIX_SEPARATOR + "]";
	}
	
	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_PUBLICATION_MP;
	}
	
	@Override
	public TMulticastTypes getType() {
		return TMulticastTypes.T_MULTICAST_PUBLICATION_MP;
	}
	
	@Override
	public String toString() {
		InetSocketAddress sourceAddress = _sourceSequence.getAddress();
		boolean isLocal = _localAddresses.contains(sourceAddress);
//		boolean isLocal = LocalSequencer.getLocalSequencer().getLocalAddress().getAddress().equals(sourceAddress.getAddress()
		String delayStr = (isLocal? String.valueOf(_dTime - _gTime) : "XXX") + "\t" + _publication;
			
		if ( Broker.RELEASE )
			return getPrefix() + PREFIX_SEPARATOR
				+ Writers.write(sourceAddress) + PREFIX_SEPARATOR
				+ delayStr + " " + _sourceSequence
				+ (_exclusionSet.isEmpty()?"" : " Exluded:" + Writers.write(_exclusionSet));
		else
			return super.toString();
	}
	
	public void addExclusion(InetSocketAddress excludedAddress) {
		_exclusionSet.add(excludedAddress);
	}
	
	public Set<InetSocketAddress> getExclusionSet() {
		return _exclusionSet;
	}

	public static void main(String[] argv) {
		LocalSequencer localSequencer = LocalSequencer.init(null, Broker.bAddress8);
		Publication publication = new Publication();
		publication.addPredicate("Salam", 100);

		TMulticast_Publish_MP tmpMP =
				new TMulticast_Publish_MP(
						publication, Broker.bAddress8, 1, (byte)0, PubForwardingStrategy.PUB_FORWARDING_STRATEGY_0, localSequencer.getNext());
//		tmpMP.annotate("HEY");
//		tmpMP.annotate("HOY");
		tmpMP.setFrom(Broker.bAddress8);
		tmpMP.addExclusion(Broker.bAddress0);
		tmpMP.addExclusion(Broker.bAddress2);
		tmpMP.addExclusion(Broker.bAddress3);
		TMulticast_Publish_MP tmpMP2 = tmpMP.getShiftedClone(1, localSequencer.getNext());
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, tmpMP2);
		TMulticast_Publish_MP tmpMP3 = (TMulticast_Publish_MP) PacketFactory.unwrapObject(null, raw, true);
		
		Object rawObj = raw.getObject();
		if(!rawObj.equals(tmpMP2))
			throw new IllegalStateException("Raw's object is different from origginal: " + raw.getObject());

		System.out.println(tmpMP);
		System.out.println(tmpMP2);
		System.out.println(tmpMP3);
	}
}
