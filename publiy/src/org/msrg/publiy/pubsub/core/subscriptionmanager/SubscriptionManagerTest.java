package org.msrg.publiy.pubsub.core.subscriptionmanager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import junit.framework.TestCase;

public class SubscriptionManagerTest extends TestCase {

	protected final int _localIndex = 5;
	protected final InetSocketAddress _bAddresses[]= new InetSocketAddress[10];

	protected IOverlayManager _overlayManager;
	protected IBrokerShadow _brokerShadow;
	protected LocalSequencer localSequencer;

	@Override
	public void setUp() {
		int delta = 4;
		int nodesCount = 10;
		int linksCount = nodesCount - 1;
		
		for(int i=0 ; i<nodesCount ; i++)
			_bAddresses[i] = new InetSocketAddress("127.0.0.1", 1000 + i);
		InetSocketAddress localAddress = _bAddresses[_localIndex];
		
		_brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, localAddress).setDelta(delta);
		localSequencer = _brokerShadow.getLocalSequencer();
		
		TRecovery_Join[] trjs = new TRecovery_Join[linksCount];
		for (int i=0 ; i<trjs.length ; i++) {
			NodeTypes nodeType1 = NodeTypes.values()[2]; //i % NodeTypes.values().length];
			NodeTypes nodeType2 = NodeTypes.values()[2]; //(i+1) % NodeTypes.values().length];

			trjs[i] = new TRecovery_Join(localSequencer.getNext(),
					_bAddresses[i], nodeType1, _bAddresses[i+1], nodeType2);
		}
		
		_overlayManager = new OverlayManager(_brokerShadow);
		_overlayManager.applyAllJoinSummary(trjs);
		for(int i=0 ; i<nodesCount ; i++) {
			NodeTypes nodeType = NodeTypes.values()[2];// % NodeTypes.values().length];
			InetSocketAddress remote = _bAddresses[i];
			if(localAddress.equals(remote))
				assertEquals(NodeTypes.NODE_BROKER, _overlayManager.getNodeType(remote));
			else
				assertEquals(nodeType, _overlayManager.getNodeType(remote));
		}
	}
	
	@Override
	public void tearDown() { }
	
	protected int getAddressIndex(InetSocketAddress addr) {
		for(int i=0 ; i<_bAddresses.length ; i++)
			if(addr.equals(_bAddresses[i]))
				return i;
		
		return -1;
	}
	
	public void testWithDelta() {
		int delta = 3;
		
		for(int remoteIndex=1 ; remoteIndex<_bAddresses.length ; remoteIndex++) {
			InetSocketAddress remote = _bAddresses[remoteIndex];
			for(int fromIndex=1 ; fromIndex<_bAddresses.length ; fromIndex++) {
				InetSocketAddress from = _bAddresses[fromIndex];
				
				InetSocketAddress newFrom = 
						_overlayManager.getNewFromForMorphedMessage(remote, from);
				
				if(_localIndex < remoteIndex && _localIndex < fromIndex)
					assertTrue(newFrom == null);
				
				else if(_localIndex > remoteIndex && _localIndex > fromIndex)
					assertTrue(newFrom == null);
				
				else {
					int newFromIndex = getAddressIndex(newFrom);
					assertTrue(Math.abs(newFromIndex  - fromIndex) <= delta+1);
					if(Math.abs(fromIndex - remoteIndex) > delta + 1)
						assertTrue(Math.abs(newFromIndex  - remoteIndex) == delta+1);
				}
				
				Subscription sub = new Subscription();
				TRecovery_Subscription trs = new TRecovery_Subscription(sub, _overlayManager.getBrokerShadow().getLocalSequencer().getNext(), from);
				IRawPacket raw = trs.morph(remote, _overlayManager);
				if(newFrom == null)
					assertTrue(raw == null);
				else {
					IPacketable packet = PacketFactory.unwrapObject(_brokerShadow, raw);
					TRecovery_Subscription morphedTRS = (TRecovery_Subscription)packet;
					assertTrue(morphedTRS.getFrom().equals(newFrom));
				}
			}
		}
//		LocalSequencer.init(null, Broker.bAddress2);
////		IOverlayManager overlayManager = new OverlayManager("topDump.top");
//		ISubscriptionManager subscriptionManager = new SubscriptionManager(null, Broker.bAddress2, "sDump.txt", true);
//		Subscription subscription = new Subscription();
//		SimplePredicate sp = SimplePredicate.buildSimplePredicate("salam", '>', 12);
//		subscription.addPredicate(sp);
//		TMulticast_Subscribe tms = new TMulticast_Subscribe(subscription, Broker.bAddress3);
//		
////		System.out.print(tms);
//		subscriptionManager.handleMessage(tms);
//		
//		
//		Publication publication = new Publication();
//		publication.addPredicate("salam", 10);
//		TMulticast_Publish tmp = new TMulticast_Publish(publication, Broker.bAddress1);
//		
//		Set<InetSocketAddress> mSet = subscriptionManager.getMatchingSet(tmp.getPublication());
//		System.out.println(mSet); // OK
	}
}
