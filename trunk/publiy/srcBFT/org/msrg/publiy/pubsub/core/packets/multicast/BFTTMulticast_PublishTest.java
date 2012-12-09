package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.security.KeyPair;


import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.core.sequence.IBFTIssuer;
import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxyRepo;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuer;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxyRepo;
import org.msrg.publiy.broker.core.sequence.SimpleBFTVerifier;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.BFTPublication;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.utils.security.dsa.DSAKeyGen;

public class BFTTMulticast_PublishTest extends TMulticast_PublishTest {
	
	protected final int _delta = 3;
	protected final InetSocketAddress _sAddr = new InetSocketAddress("127.0.0.1", 1000);
	protected final InetSocketAddress _iAddr = new InetSocketAddress("127.0.0.1", 1001);
	protected final InetSocketAddress _vAddr = new InetSocketAddress("127.0.0.1", 1002);
	protected KeyPair _keypair;
	protected IBFTIssuer _issuer;
	protected IBFTBrokerShadow _brokerShadow;
	
	@Override
	public void setUp() {
		super.setUp();
		_keypair = DSAKeyGen.getInstance().generateKeys(null, null);
		_issuer = new SimpleBFTIssuer(_iAddr, _keypair.getPrivate(), null);
		_brokerShadow = new BFTBrokerShadow(NodeTypes.NODE_BROKER, _delta, _vAddr, null, null, "NONE", null);
		
		IBFTIssuerProxy iProxy = new SimpleBFTIssuerProxy(null, new SimpleBFTVerifier(_vAddr), _iAddr, _keypair.getPublic());
		IBFTIssuerProxyRepo iProxyRepo = new SimpleBFTIssuerProxyRepo((BFTBrokerShadow)_brokerShadow);
		iProxyRepo.registerIssuerProxy(iProxy);
	}

	@Override
	public void tearDown() { }
	
	@Override
	protected TMulticast_Publish_BFT createTMulticast_Publish(Publication pub, InetSocketAddress from) {
		return new TMulticast_Publish_BFT((BFTPublication) pub, from, _payload, _localSequencer.getNext()).getNonShiftedClone(_localSequencer.getNext());
	}
	
	@Override
	protected BFTPublication createPublication() {
		return new BFTPublication(_sAddr);
	}
	
	@Override
	public void testTMulticastSubscribeEncodeDecode() {
		for(int i=0 ; i<10 ; i++)
			((TMulticast_Publish_BFT)_tmPub).addSequencePair(_issuer, _vAddr, 100, 1000 + i);
		assertEquals(1, ((TMulticast_Publish_BFT)_tmPub).getSequencePairs().size());
		for(int i=1 ; i<10 ; i++)
			((TMulticast_Publish_BFT)_tmPub).addSequencePair(_issuer, new InetSocketAddress("127.0.0.1", 2000 + i), 100, 1000 + i);
		assertEquals(10, ((TMulticast_Publish_BFT)_tmPub).getSequencePairs().size());
		
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, _tmPub);
		assertTrue(_tmPub == PacketFactory.unwrapObject(_brokerShadow, raw, false));
		
		IPacketable packet = PacketFactory.unwrapObject(_brokerShadow, raw, true);
		
		assertTrue(_tmPub != packet);
		assertTrue(_tmPub.equals(packet));
		assertTrue(packet.equals(_tmPub));
		assertEquals(10, ((TMulticast_Publish_BFT)packet).getSequencePairs().size());
	}
}
