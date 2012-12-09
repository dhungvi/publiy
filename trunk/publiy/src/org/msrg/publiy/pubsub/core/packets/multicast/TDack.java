package org.msrg.publiy.pubsub.core.packets.multicast;

import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.packet.IPacketListener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;

public class TDack implements IPacketable {

	public static final int I_ARRIVED_BUNDLES_COUNT = 0;
	public static final int I_DISCARDED_BUNDLES_COUNT = 4;
	public static final int I_DACK_BUNDLES = 8;
	
	public static final int TDACK_BASE_SIZE = 8;
	
	public final Dack_Bundle[] _arrivedDackBundles;
	public final Dack_Bundle[] _discardedDackBundles;
	
	//TODO: Remove public below
	public 
	TDack(Dack_Bundle[] arrivedDackBundles, Dack_Bundle[] discardedDackBundles){
		if ( arrivedDackBundles == null || discardedDackBundles == null )
			throw new IllegalArgumentException();
		
		_arrivedDackBundles = arrivedDackBundles;
		_discardedDackBundles = discardedDackBundles;
	}

	public static TDack getTDackBundleObject(ByteBuffer bdy){
		if ( bdy == null )
			return null;

		if ( bdy.capacity() < TDACK_BASE_SIZE )
			throw new IllegalArgumentException("ByteBuffer not of the minimum size: " + bdy.capacity());

		int arrivedBundlesCount = bdy.getInt(I_ARRIVED_BUNDLES_COUNT);
		int discardedBundlesCount = bdy.getInt(I_DISCARDED_BUNDLES_COUNT);

		int offset = TDACK_BASE_SIZE;
		Dack_Bundle[] arrivedDackBundles = new Dack_Bundle[arrivedBundlesCount];
		for ( int i=0 ; i<arrivedBundlesCount ; i++ ){
			arrivedDackBundles[i] = Dack_Bundle.getBundleFromBuffer(bdy, offset);
			offset += arrivedDackBundles[i].getSizeInByteBuffer();
		}
		
		Dack_Bundle[] discardedDackBundles = new Dack_Bundle[discardedBundlesCount];
		for ( int i=0 ; i<discardedBundlesCount ; i++ ){
			discardedDackBundles[i] = Dack_Bundle.getBundleFromBuffer(bdy, offset);
			offset += discardedDackBundles[i].getSizeInByteBuffer();
		}
		
		return new TDack(arrivedDackBundles, discardedDackBundles);
	}
	
	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TDACK;
	}

	@Override
	public boolean equals(Object obj){
		throw new UnsupportedOperationException(obj.toString());
	}
	
	@Override
	public int hashCode(){
		throw new UnsupportedOperationException();
	}
		
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		buff.position(I_ARRIVED_BUNDLES_COUNT);
		buff.putInt(_arrivedDackBundles.length);
		
		buff.position(I_DISCARDED_BUNDLES_COUNT);
		buff.putInt(_discardedDackBundles.length);
		
		int offset = TDACK_BASE_SIZE;
		for ( int i=0 ; i<_arrivedDackBundles.length ; i++ ){
			offset += _arrivedDackBundles[i].loadBundleInBuffer(buff, offset);
		}
		
		for ( int i=0 ; i<_discardedDackBundles.length ; i++ ){
			offset += _discardedDackBundles[i].loadBundleInBuffer(buff, offset);
		}
	}
	
	@Override
	public String toString(){
		String str = "TDACK (" + _arrivedDackBundles.length + "-" + _discardedDackBundles.length + ")\n\tARRV[";
		for ( int i=0 ; i<_arrivedDackBundles.length ; i++ )
			str += _arrivedDackBundles[i] + ",\t\t";
		str += "]\n\tDSCR[";
		for ( int i=0 ; i<_discardedDackBundles.length ; i++ )
			str += _discardedDackBundles[i] + ((i==_discardedDackBundles.length-1)?"" : ",");
		return str + "]";
	}

	public static void main(String [] argv){
//		Object[] objs = new HashSet<Object>().toArray(new Object[0]);
//		System.out.println(objs);
		
//		LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 1000));
//		
//		int seqCount = 3;
//		int bundlesCount = 4;
//		Dack_Bundle[] arrvDackBundles = new Dack_Bundle[bundlesCount];
//		Dack_Bundle[] dscdDackBundles = new Dack_Bundle[bundlesCount];
//		
//		for ( int j=0 ; j<bundlesCount ; j++ )
//		{
//			arrvDackBundles[j] = Dack_Bundle.makeSampleBundle(seqCount);
//			dscdDackBundles[j] = Dack_Bundle.makeSampleBundle(seqCount);
//		}
//		
//		TDack dackBundle = new TDack(arrvDackBundles, dscdDackBundles);
//		System.out.println(dackBundle); // OK
//		
//		ByteBuffer bdy = dackBundle.putObjectInBuffer();
//		TDack dackBundle2 = TDack.getTDackBundleObject(bdy);
//		System.out.println(dackBundle2); // OK
	}

	@Override
	public int getContentSize() {
		int buffSize = TDACK_BASE_SIZE;
		for ( int i=0 ; i<_arrivedDackBundles.length ; i++ )
			buffSize += _arrivedDackBundles[i].getSizeInByteBuffer();
		for ( int i=0 ; i<_discardedDackBundles.length ; i++ )
			buffSize += _discardedDackBundles[i].getSizeInByteBuffer();
		
		return buffSize;
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
