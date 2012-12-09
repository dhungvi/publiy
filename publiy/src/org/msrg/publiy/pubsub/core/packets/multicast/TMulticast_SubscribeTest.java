package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;
import org.msrg.publiy.publishSubscribe.matching.SimpleStringPredicate;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import junit.framework.TestCase;

public class TMulticast_SubscribeTest extends TestCase {

	protected Subscription _sub;
	protected TMulticast_Subscribe _tmSub;
	protected final InetSocketAddress _from = Sequence.getRandomAddress();
	protected LocalSequencer _localSequencer;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
		_localSequencer = LocalSequencer.init(null, _from);
		_sub = new Subscription().
				addPredicate(SimplePredicate.buildSimplePredicate("ATTR1", '=', 12)).
				addStringPredicate(SimpleStringPredicate.buildSimpleStringPredicate("ATTRSTR1", '~', "HI"));
		_tmSub = new TMulticast_Subscribe(_sub, _from, _localSequencer).getShiftedClone(1, _localSequencer.getNext());
	}
	
	@Override
	public void tearDown() { }
	
	public void testTMulticastSubscribeEncodeDecode() {
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, _tmSub);
		IPacketable packet = PacketFactory.unwrapObject(null, raw, true);
		
		assertTrue(_tmSub != packet);
		assertTrue(_tmSub.equals(packet));
		assertTrue(packet.equals(_tmSub));
		
		BrokerInternalTimer.inform(_tmSub.toString());
		BrokerInternalTimer.inform(packet.toString());
		BrokerInternalTimer.inform("OK!");
	}
	
	public void testConfirmation() {
//			TMulticast tm1 = new TMulticast(TMulticastTypes.T_MULTICAST_UNKNOWN);
//			TMulticast tm1 = new TMulticast_Join(Broker.bAddress8, Broker.bAddress7);
		Subscription subscription = new Subscription();
		SimplePredicate sp1 = SimplePredicate.buildSimplePredicate("SALAM", '=', 113100);
		SimplePredicate sp2 = SimplePredicate.buildSimplePredicate("SALAM", '=', 131100);
		SimplePredicate sp3 = SimplePredicate.buildSimplePredicate("SALAM", '=', 113100);
		subscription.addPredicate(sp1);
		subscription.addPredicate(sp2);
		subscription.addPredicate(sp3);
		TMulticast tm1 = new TMulticast_Subscribe(subscription, Broker.bAddress7, _localSequencer);
		Publication publication = new Publication();
		publication.addPredicate("Salam", 100);
//			TMulticast tm1 = new TMulticast_Publish(publication, Broker.bAddress8).getConfirmation(Broker.bAddress12, Sequence.getPsuedoSequence(Broker.bAddress9, 600));
		tm1.annotate("HI");

		tm1 = tm1.getConfirmation(Broker.bAddress12, Broker.bAddress10, Sequence.getPsuedoSequence(Broker.bAddress9, 600));
		
		Set<InetSocketAddress> recipients = new HashSet<InetSocketAddress>();
		recipients.add(Broker.bAddress0);
		recipients.add(Broker.bAddress3);
		recipients.add(Broker.bAddress4);
		
		tm1.loadGuidedInfo(recipients);
		
//			tm1.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress2, 200));
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, tm1);
		IPacketable packet = PacketFactory.unwrapObject(null, raw);
		
		System.out.println("=?" + packet.equals(tm1));
		System.out.println("=?" + tm1.equals(packet));
		
		System.out.println(packet);
		System.out.println(packet.getObjectType().toString() + " " + packet.getAnnotations());
		System.out.println(raw.getType() + " " + raw.getAnnotations());
		System.out.println(tm1.getType() + " " + tm1.getAnnotations());
		
		System.out.println("\n");
		
		raw.annotate("BYE");
		packet = PacketFactory.unwrapObject(null, raw);
		System.out.println(packet);
		System.out.println(packet.getObjectType().toString() + " " + packet.getAnnotations());
		System.out.println(raw.getType() + " " + raw.getAnnotations());
		System.out.println(tm1.getType() + " " + tm1.getAnnotations());
		
		System.out.println(TMulticast.getSourceSequenceFromRaw(raw));
		System.out.println(((TMulticast)packet).getClonedGuidedInfo());
		
//			TMulticast tm2 = new TMulticast(tm1.putObjectInBuffer());
//			TMulticast tm3 = new TMulticast(tm2.putObjectInBuffer());
//			TMulticast tm4 = new TMulticast(tm3.putObjectInBuffer());
//			TMulticast tm5 = new TMulticast(tm4.putObjectInBuffer());
//			
//			System.out.println(tm1 + "\n" + tm2 + "\n" + tm3 + "\n" + tm4 + "\n" + tm5); //OK
	}
}
