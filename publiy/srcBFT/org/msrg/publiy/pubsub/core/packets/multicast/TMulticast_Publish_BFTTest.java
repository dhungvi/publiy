package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.security.KeyPair;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.BFTPublication;
import org.msrg.publiy.utils.security.dsa.DSAKeyGen;

import org.msrg.publiy.broker.BFTBrokerIdentityManager;
import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.core.sequence.IBFTIssuer;
import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxyRepo;
import org.msrg.publiy.broker.core.sequence.IBFTVerifier;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.SequencePair;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuer;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxyRepo;
import org.msrg.publiy.broker.core.sequence.SimpleBFTVerifier;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.security.keys.KeyManager;
import junit.framework.TestCase;

public class TMulticast_Publish_BFTTest extends TestCase {

	protected final int maxSequnecePairs = 1000;
	protected final int _delta = 1;
	protected final InetSocketAddress _senderAddress =
			new InetSocketAddress("127.0.0.1", 1313);
	protected final InetSocketAddress _receiverAddress =
			new InetSocketAddress("127.0.0.1", 1414);
	
	protected LocalSequencer _senderLocalSequencer, _receiverLocalSequencer;
	protected BFTBrokerShadow _senderBFTBrokerShadow, _receiverBFTBrokerShadow;

	protected IBFTIssuerProxyRepo _receiverIProxyRepo;
	protected BFTPublication _bftPublication;
	
	protected BrokerIdentityManager _senderIdMan, _receiverIdMan;
	protected KeyManager _senderKeyMan, _receiverKeyMan;
	protected IBFTIssuer _senderIssuer;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
		_senderLocalSequencer = LocalSequencer.init(null, _senderAddress);
		_receiverLocalSequencer = LocalSequencer.init(null, _receiverAddress);
		
		_senderBFTBrokerShadow = new BFTBrokerShadow(
				NodeTypes.NODE_BROKER, _delta, _senderAddress, null, null, null, null);
		_receiverBFTBrokerShadow = new BFTBrokerShadow(
				NodeTypes.NODE_BROKER, _delta, _receiverAddress, null, null, null, null);
		
		_senderIdMan = new BFTBrokerIdentityManager(_senderAddress, _delta);
		_receiverIdMan = new BFTBrokerIdentityManager(_receiverAddress, _delta);
		
		KeyPair senderKeyPair = DSAKeyGen.getInstance().generateKeys(null, null);
		KeyPair receiverKeyPair = DSAKeyGen.getInstance().generateKeys(null, null);
		_senderKeyMan = new KeyManager(null, _senderIdMan);
		_senderKeyMan.addKeyPair(_senderAddress, senderKeyPair);
		_senderKeyMan.addKeyPair(_receiverAddress, receiverKeyPair);
		_receiverKeyMan = new KeyManager(null, _receiverIdMan);
		_receiverKeyMan.addKeyPair(_senderAddress, senderKeyPair);
		_receiverKeyMan.addKeyPair(_receiverAddress, receiverKeyPair);
		
		_senderIssuer = new SimpleBFTIssuer(_senderAddress, senderKeyPair.getPrivate(), null);
		IBFTVerifier receiverVerifier = new SimpleBFTVerifier(_receiverAddress);
		_receiverIProxyRepo = new SimpleBFTIssuerProxyRepo(_receiverBFTBrokerShadow);
		_receiverIProxyRepo.registerIssuerProxy(new SimpleBFTIssuerProxy(_receiverIdMan, receiverVerifier, _senderAddress, _receiverKeyMan));
		
		_senderBFTBrokerShadow.setIssuerProxyRepo(_receiverIProxyRepo);
		_bftPublication = new BFTPublication(_senderAddress);
	}
	
	
	@Override
	public void tearDown() {
		return;
	}
	
	public void test() {
		TMulticast_Publish_BFT tmBFT =
				new TMulticast_Publish_BFT(_bftPublication, _senderAddress, _senderLocalSequencer.getNext());
		for(int i=0 ; i<maxSequnecePairs ; i++) {
			SequencePair sp = new SequencePair(_senderIssuer, i, i, new InetSocketAddress("127.0.0.1", 9000+i), _bftPublication);
			tmBFT.addSequencePair(sp);
		}
		
		BrokerInternalTimer.inform("Wrapping bft publication...");
		IRawPacket raw = PacketFactory.wrapObject(_senderLocalSequencer, tmBFT);
		BrokerInternalTimer.inform("Wrapping bft publication DONE.");

		BrokerInternalTimer.inform("Unwrapping bft publication...");
		IPacketable packet = PacketFactory.unwrapObject(_senderBFTBrokerShadow, raw, true);
		BrokerInternalTimer.inform("Unwrapping bft publication DONE.");

		BrokerInternalTimer.inform("Checking equality...");
		assertFalse(tmBFT == packet);
		assertEquals(tmBFT, packet);
		assertEquals(maxSequnecePairs, tmBFT.getSequencePairs().size());
		assertEquals(maxSequnecePairs, ((TMulticast_Publish_BFT)packet).getSequencePairs().size());
		BrokerInternalTimer.inform("Checking equality DONE.");
	}
}
