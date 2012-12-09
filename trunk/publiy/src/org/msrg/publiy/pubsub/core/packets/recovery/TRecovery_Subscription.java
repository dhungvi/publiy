package org.msrg.publiy.pubsub.core.packets.recovery;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class TRecovery_Subscription extends TRecovery {
	
	private static int I_FROM = I_CONTENT;
	private static int I_REAL_FROM = I_FROM + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
	private static int I_SEQ_ID = I_REAL_FROM + 1;
	private static int I_SUB_SIZE = I_SEQ_ID + Sequence.SEQ_IN_BYTES_ARRAY_SIZE;
	private static int I_SUB = I_SUB_SIZE + 2;
	
	final private InetSocketAddress _from;
	final private boolean _realFrom;
	final Subscription _subscription;
	final Sequence _seqID;
	
	public InetSocketAddress getFrom(){
		return _from;
	}
	
	public boolean isFromReal() {
		return _realFrom;
	}
	
	public Sequence getSequenceID(){
		return _seqID;
	}
	
	public Subscription getSubscription(){
		return _subscription;
	}
	
	public TRecovery_Subscription(Subscription subscription, Sequence sourceSequence, InetSocketAddress from) {
		super(TRecoveryTypes.T_RECOVERY_SUBSCRIPTION, sourceSequence);
		_subscription = subscription;
		_seqID = sourceSequence;
		_from = from;
		_realFrom = (_seqID==null?true:_from.equals(_seqID.getAddress()));
		
		if ( _from == null)
			throw new IllegalArgumentException();
		
		if (!Broker.RELEASE || Broker.DEBUG)
			if (_realFrom != (_seqID.getAddress().equals(_from)))
				throw new IllegalStateException(this.toString());
	}
	
	public TRecovery_Subscription(ByteBuffer bdy){
		super(bdy);
		
		if ( getType() != TRecoveryTypes.T_RECOVERY_SUBSCRIPTION )
			throw new IllegalStateException ("Type mismatch: '" + getType() + "' !=TRecovery_Join.");

		byte[] fromBArray = new byte[Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_FROM);
		bdy.get(fromBArray);
		_from = Sequence.readAddressFromByteArray(fromBArray);
		
		bdy.position(I_REAL_FROM);
		_realFrom = bdy.get()==0?false:true;
		
		byte[] seqBArray = new byte[Sequence.SEQ_IN_BYTES_ARRAY_SIZE];
		bdy.position(I_SEQ_ID);
		bdy.get(seqBArray);
		_seqID = Sequence.readSequenceFromBytesArray(seqBArray);
		
		int subSize = getSubscriptionSize(bdy);
		byte[] subBArray = new byte[subSize];
		bdy.position(I_SUB);
		bdy.get(subBArray);
		_subscription = TMulticast_Subscribe.readSubscriptionFromByteArray(subBArray);
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buff) {
		super.putObjectInBuffer(localSequencer, buff);
		
		byte[] fromBArray = Sequence.getAddressInByteArray(_from);
		byte[] seqBArray = _seqID.getInByteArray();
		byte[] subBArray = TMulticast_Subscribe.getSubscriptionInByteArray(_subscription);
		
		buff.position(I_FROM);
		buff.put(fromBArray);
		
		buff.position(I_REAL_FROM);
		buff.put((byte)(_realFrom?1:0));
		
		buff.position(I_SEQ_ID);
		buff.put(seqBArray);
		
		setSubscriptionSize(buff, subBArray.length);
		
		buff.position(I_SUB);
		buff.put(subBArray);
	}

	@Override
	public String toString(){
		String str = _subType.toString() + "("+ _subscription +") F:" + _from + "@ " + _sourceSequence;
		return str;
	}

	public IRawPacket morph(InetSocketAddress remote, IOverlayManager overlayManager){
//		Path pathFromFrom = overlayManager.getPathFrom(_from);
//		Path pathFromRemote = overlayManager.getPathFrom(remote);
//		
//		if ( pathFromRemote.intersect(pathFromFrom) )
//			return null;
		
		InetSocketAddress newFrom = overlayManager.getNewFromForMorphedMessage(remote, _from);
		
		if ( newFrom == null )
			return null;
		
		TRecovery_Subscription trsClone = new TRecovery_Subscription(_subscription, _sourceSequence, newFrom);
		IRawPacket raw = PacketFactory.wrapObject(overlayManager.getLocalSequencer(), trsClone);
		return raw;
	}
	
	@Override
	public IRawPacket morph(ISession session, IOverlayManager overlayManager){
		InetSocketAddress remote = session.getRemoteAddress();
		
		return morph(remote, overlayManager);
	}
	
	@Override
	public TRecoveryTypes getStaticType(){
		return TRecoveryTypes.T_RECOVERY_JOIN;
	}
	
	private static void setSubscriptionSize(ByteBuffer bdy, int subscriptionSize){
		if ( subscriptionSize >= Short.MAX_VALUE )
			throw new IllegalStateException("Too large: " + subscriptionSize);
		
		bdy.position(I_SUB_SIZE);
		bdy.putShort((short) subscriptionSize);
	}
	
	private static int getSubscriptionSize(ByteBuffer bdy){
		return bdy.getShort(I_SUB_SIZE);
	}
	
	@Override
	public int getTRSpecificContentSize() {
		byte[] subBArray = TMulticast_Subscribe.getSubscriptionInByteArray(_subscription);
		return I_SUB + subBArray.length;
//		int subSize = Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE + 1 + Sequence.SEQ_IN_BYTES_ARRAY_SIZE + 1 + subBArray.length;
//		int contentSize = 2 + subSize;
//		
//		return contentSize;
	}

	public String toStringShort(){
		return "TRS[" + _subscription + "_FROM_" + _from + "]";
	}
	
	public static TRecovery_Subscription[] createTRecoverySubscriptions(String subscriptionEntryLines, LocalSequencer localSequencer, boolean realFrom) {
		String[] subscriptionEntryLinesArray = subscriptionEntryLines.split("\n");
		if (subscriptionEntryLinesArray.length == 0 || subscriptionEntryLinesArray[0].equals(""))
			return new TRecovery_Subscription[0];
		TRecovery_Subscription[] trs = new TRecovery_Subscription[subscriptionEntryLinesArray.length];
		for (int i=0 ; i<subscriptionEntryLinesArray.length ; i++) {
			String from = subscriptionEntryLinesArray[i].substring(0, subscriptionEntryLinesArray[i].indexOf(" "));
			String subscriptionStr = subscriptionEntryLinesArray[i].substring(subscriptionEntryLinesArray[i].indexOf(" ")+1);
			String[] fromSplit = from.split(":");
			InetSocketAddress fromAddress = new InetSocketAddress(fromSplit[0], new Integer(fromSplit[1]));
			Subscription subscription = Subscription.decode(subscriptionStr);
			trs[i] = new TRecovery_Subscription(subscription, localSequencer.getNext(), fromAddress);
		}
		
		return trs;
	}
}
