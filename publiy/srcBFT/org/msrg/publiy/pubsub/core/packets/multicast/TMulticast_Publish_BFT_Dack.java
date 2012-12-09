package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Date;

import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.publishSubscribe.BFTPublication;
import org.msrg.publiy.publishSubscribe.Publication;

public class TMulticast_Publish_BFT_Dack extends TMulticast_Publish_BFT {

	public final static String BFT_DACK_COUNTER = "BFTDack_counter";
	public final static String BFT_DACK_GEN_TIME = "BFTDack_gtime";
	public final static String BFT_DACK_LOCAL_ADDRESS = "BFTDack_localAddress";
	
	private static int COUNTER = 0;
	
	protected TMulticast_Publish_BFT_Dack(BFTPublication publication, InetSocketAddress from, byte[] payload, Sequence sourceSequence) {
		super(getStaticType(), publication, from, payload, sourceSequence);
	}
	
	@Deprecated
	protected TMulticast_Publish_BFT_Dack(InetSocketAddress sourceAddress) {
		super(TMulticastTypes.T_MULTICAST_PUBLICATION_BFT_DACK,
				new BFTPublication(sourceAddress).
					addStringPredicate(BFT_DACK_GEN_TIME, new Date().toString().replace(' ', '_')).
					addStringPredicate(BFT_DACK_LOCAL_ADDRESS, sourceAddress.toString()).
					addPredicate(BFT_DACK_COUNTER, COUNTER++),
				sourceAddress,
				null);
	}

	public TMulticast_Publish_BFT_Dack(InetSocketAddress from, Sequence sourceSequence) {
		super(TMulticastTypes.T_MULTICAST_PUBLICATION_BFT_DACK,
				new BFTPublication(sourceSequence.getAddress()).
					addStringPredicate(BFT_DACK_GEN_TIME, new Date().toString().replace(' ', '_')).
					addStringPredicate(BFT_DACK_LOCAL_ADDRESS, sourceSequence.getAddress().toString()).
					addPredicate(BFT_DACK_COUNTER, COUNTER++),
				from,
				sourceSequence);
		_sequencesVector[0] = sourceSequence;
	}

	protected TMulticast_Publish_BFT_Dack(IBrokerShadow brokerShadow,
			ByteBuffer bdy, int contentSize, int subContentSize, String annotations) {
		super(brokerShadow, bdy, contentSize, subContentSize, annotations);
	}

	public static TMulticastTypes getStaticType() {
		return TMulticastTypes.T_MULTICAST_PUBLICATION_BFT_DACK;
	}
	
	@Override
	protected TMulticast_Publish_BFT createNewTMulticastPublish(Publication publication, InetSocketAddress from, byte[] workload, Sequence sourceSequence) {
		return new TMulticast_Publish_BFT_Dack((BFTPublication) publication, from, workload, sourceSequence);
	}
	
	@Override
	public TMulticastTypes getType() {
		return TMulticastTypes.T_MULTICAST_PUBLICATION_BFT_DACK;
	}
	
	@Override
	public String toString() {
		return getType().toString() + "=" + _verifiable;
	}

	public String toString(BrokerIdentityManager idMan) {
		return getType().toString() + "=" + _verifiable.toString(idMan);
	}
}
