package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.security.KeyPair;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.utils.security.dsa.DSAKeyGen;


import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import junit.framework.TestCase;

public class TMulticast_Publish_BFT_HeartBeatTest extends TestCase {

	protected final InetSocketAddress _sAddr = new InetSocketAddress("127.0.0.1", 4012);
	protected final InetSocketAddress _from = new InetSocketAddress("127.0.0.1", 3121);
	protected final LocalSequencer _localSequencer = LocalSequencer.init(null, _sAddr);
	
	protected final InetSocketAddress _remote = new InetSocketAddress("127.0.0.1", 5611);
	protected final IBrokerShadow _remoteBrokerShadow = new BFTBrokerShadow(NodeTypes.NODE_BROKER, 3, _remote, null, null, null, null);
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
		KeyPair sourceKP = DSAKeyGen.getInstance().generateKeys(null, null);
		((BFTBrokerShadow)_remoteBrokerShadow).getKeyManager().addKeyPair(_sAddr, sourceKP);

		KeyPair remoteKP = DSAKeyGen.getInstance().generateKeys(null, null);
		((BFTBrokerShadow)_remoteBrokerShadow).getKeyManager().addKeyPair(_remote, remoteKP);
	}		
	
	@Override
	public void tearDown() { }
	
	public void testTMulticastPublicationBFTHeartBeatEncodeDecode() {
		Sequence sourceSequence = _localSequencer.getNext();
		TMulticast_Publish_BFT_Dack bftHB = new TMulticast_Publish_BFT_Dack(_from, sourceSequence);
		assertEquals(_sAddr, bftHB.getSourceAddress());
		assertEquals(sourceSequence, bftHB.getSourceSequence());

		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, bftHB);
		TMulticast_Publish_BFT_Dack bftHB2 =
				(TMulticast_Publish_BFT_Dack) PacketFactory.unwrapObject(_remoteBrokerShadow, raw, true);
		assertNotNull(bftHB2);
		assertEquals(bftHB, bftHB2);
		assertFalse(bftHB == bftHB2);
	}
}
