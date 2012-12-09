package org.msrg.publiy.tests.roundbasedrouting;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.PubForwardingStrategy;

import junit.framework.TestCase;


import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.multipath.WorkingManagersBundle;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.tests.utils.mock.MockMasterTopologySubscriptionBundle;
import org.msrg.publiy.tests.utils.mock.MockWorkingTopologySubscriptionBundle;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

public class RoundBasedRouting  extends TestCase {

	private final MockMasterTopologySubscriptionBundle _masterBundle;
	private final Map<InetSocketAddress, PublicationSet> _publicationSets;
	private final PubForwardingStrategy _strategy = PubForwardingStrategy.PUB_FORWARDING_STRATEGY_3;
	private static final String _dirname = "Auxiliary/Topologies/ML";
	private static final String _dataset = _dirname + "/data/zipf-MULT10.0-SKW1.0-PG6-PC10-SG6/";
	
	protected Set<InetSocketAddress> _printoutAddresses = new HashSet<InetSocketAddress>();
	protected String _printoutAddressesStr =
		"127.0.0.1:2000,127.0.0.1:2001,127.0.0.1:2002,127.0.0.1:2003,127.0.0.1:2004,127.0.0.1:2005,127.0.0.1:2006,127.0.0.1:2007," +
		"127.0.0.1:2008,127.0.0.1:2009,127.0.0.1:2010,127.0.0.1:2011,127.0.0.1:2012,127.0.0.1:2013,127.0.0.1:2014,127.0.0.1:2015," +
		"127.0.0.1:2016,127.0.0.1:2017,127.0.0.1:2018,127.0.0.1:2019,127.0.0.1:2020";
	protected Map<InetSocketAddress, IBrokerShadow> _allBrokerShadows = new HashMap<InetSocketAddress, IBrokerShadow>();
	protected Map<InetSocketAddress, WorkingManagersBundle> _allWorkingManagersBundles = new HashMap<InetSocketAddress, WorkingManagersBundle>();
	protected Map<InetSocketAddress, MockMessageQueueMP> _allMQMPs = new HashMap<InetSocketAddress, MockMessageQueueMP>();

	RoundBasedRouting() throws IOException {
		LocalSequencer dummyLocalSequencer = LocalSequencer.init(null, Sequence.getRandomAddress());
		_masterBundle = new MockMasterTopologySubscriptionBundle(dummyLocalSequencer, _dirname);
		_publicationSets = new HashMap<InetSocketAddress, PublicationSet>();
		
		Set<PublicationSet> pubSets = PublicationSet.createPublicationSets(_dataset);
		for(PublicationSet pubSet : pubSets)
			_publicationSets.put(pubSet._address, pubSet);
		
		LoggerFactory.modifyLogger(null, LoggingSource.values(),
				null, LoggingSource.values(),
				null, LoggingSource.values(),
				null, LoggingSource.values(),
				null, LoggingSource.values());
		
		String printoutAddressesStrSplit[] = _printoutAddressesStr.split(",");
		if(printoutAddressesStrSplit.length > 0 && !printoutAddressesStrSplit[0].equals("")){
			for(String printoutAddressStrSplit : printoutAddressesStrSplit)
				_printoutAddresses.add(new InetSocketAddress(printoutAddressStrSplit.split(":")[0], new Integer(printoutAddressStrSplit.split(":")[1])));
		}
	}
	
	public void init(MockWorkingTopologySubscriptionBundle mockWorkingBundle) {
		InetSocketAddress[] allNodeAddresses = _masterBundle.getNodeAddresses();
		for (InetSocketAddress nodeAddress : allNodeAddresses) {
			IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, nodeAddress);

			WorkingManagersBundle workingBundleManager = mockWorkingBundle.getWorkingManagersBundle(nodeAddress);
			_allWorkingManagersBundles.put(nodeAddress, workingBundleManager);
			_allBrokerShadows.put(nodeAddress, new BrokerShadow(NodeTypes.NODE_BROKER, nodeAddress).setDelta(Broker.DELTA));
			
			MockMessageQueueMP mqMP = new MockMessageQueueMP(brokerShadow, _strategy, nodeAddress, workingBundleManager._sessions, workingBundleManager);
			_allMQMPs.put(nodeAddress, mqMP);
		}
	}
	
	void runRound() throws IOException {
		while (true) {
//			Map<InetSocketAddress, String> _softSessionAddresses = null;
//			Map<InetSocketAddress, String> _candidateSessionAddresses = null;
//			
//			if (_softSessionAddresses == null || _candidateSessionAddresses == null )
//				break;
			
			Map<InetSocketAddress, InetSocketAddress[]> softAddressesMap = getSoftAddressesMap();
			Map<InetSocketAddress, InetSocketAddress[]> candidateAddressesMap = getCandidateAddressesMap();
			
			MockWorkingTopologySubscriptionBundle mockWorkingBundle =
				new MockWorkingTopologySubscriptionBundle(_allBrokerShadows, _masterBundle, softAddressesMap, candidateAddressesMap);
			init(mockWorkingBundle);
			runSubRound(mockWorkingBundle);
			
			break;
		}
	}
	
	void runSubRound(MockWorkingTopologySubscriptionBundle mockWorkingBundle) throws IOException {
//		_publicationDeliverySet.clear();
		for (PublicationSet publicationSet : _publicationSets.values()) {
			InetSocketAddress initialSender = publicationSet._address;
			for(Publication publication : publicationSet._publications) {
				SentMessage.clear();
				runForPublication(publication, initialSender);
			}
			
			break;
		}
	}
	
	public void runForPublication(Publication publication, InetSocketAddress initialSender) throws IOException {
		LocalSequencer localSequencer = LocalSequencer.init(null, initialSender);
		Broker broker = new Broker(initialSender, BrokerOpState.BRKR_UNKNOWN, null, _strategy, new Properties());
		broker.initWithBrokerShadow((BrokerShadow)_allBrokerShadows.get(initialSender));
		TMulticast_Publish_MP tmp = new TMulticast_Publish_MP(
				publication, initialSender, 0, (byte)0, PubForwardingStrategy.PUB_FORWARDING_STRATEGY_0, localSequencer.getNext()).getShiftedClone(1, localSequencer.getNext());
		tmp.setFrom(initialSender);
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, tmp);
		SentMessage initialSending = new SentMessage(raw, initialSender, null);
		List<SentMessage> sentMessages = new LinkedList<SentMessage>();
		sentMessages.add(initialSending);
		
		while(!sentMessages.isEmpty()) {
			List<SentMessage> newSentMessages = new LinkedList<SentMessage>();
			for(SentMessage sentMessage : sentMessages) {
				InetSocketAddress receiver = sentMessage._receiver;
				InetSocketAddress sender = sentMessage._sender;
				IBrokerShadow brokerShadow = _allBrokerShadows.get(receiver);
				TMulticast_Publish_MP receivedTmp = (TMulticast_Publish_MP) PacketFactory.unwrapObject(brokerShadow, sentMessage._raw);
				
				printout(receiver, "Receiver is: " + receiver.toString() + ", msg is: " + receivedTmp);
				LocalSequencer.init(null, receiver);
	
				MockMessageQueueMP mq = _allMQMPs.get(receiver);
				receivedTmp.setFrom(sender);
				MockMessageQueueNodeMP mqnMP = new MockMessageQueueNodeMP(mq, receivedTmp, null, new Sequence(receiver, 0, 0));
				WorkingManagersBundle workingBundle = _allWorkingManagersBundles.get(receiver);
				if (workingBundle == null) 
					continue;
				
				Set<InetSocketAddress> matchingSet = workingBundle.getMasterSubscriptionManager().getMatchingSet(publication);
				ISession[] sessions = workingBundle._sessions;
				boolean neverCheckout = true;
				
				for(ISession session : sessions) {
					IRawPacket newRaw = mqnMP.checkOutAndmorph(session, receivedTmp.isGuided());
					if (newRaw==null)
						continue;
					
					neverCheckout = false;
					SentMessage newSentMessage = new SentMessage(newRaw, session.getRealSession().getRemoteAddress(), receiver);
					newSentMessages.add(newSentMessage);
					printout(receiver, "\t" + newSentMessage);
				}
				
				if (!matchingSet.isEmpty() && neverCheckout)
					throw new IllegalStateException(receiver.toString() + ", MatchingSet: " + matchingSet.toString());
			}
			sentMessages.clear();
			sentMessages.addAll(newSentMessages);
		}
	}

	void printout(InetSocketAddress remote, String msg) {
		if(_printoutAddresses.contains(remote))
			System.out.println(msg);
	}
	
	Map<InetSocketAddress, InetSocketAddress[]> getSoftAddressesMap() {
		return new HashMap<InetSocketAddress, InetSocketAddress[]>();
	}
	
	Map<InetSocketAddress, InetSocketAddress[]> getCandidateAddressesMap() {
		return new HashMap<InetSocketAddress, InetSocketAddress[]>();
	}
	
	public static void main(String[] argv) throws IOException {
		BrokerInternalTimer.start();
		RoundBasedRouting roundBasedRouter = new RoundBasedRouting();
		roundBasedRouter.runRound();
		System.out.println("END");
		
		Set<PublicationSet> pubSets = PublicationSet.createPublicationSets(_dataset);
		for (PublicationSet pubSet : pubSets) {
			InetSocketAddress initialSender = pubSet._address;
			for (Publication publication : pubSet._publications) {
				roundBasedRouter.runForPublication(publication, initialSender);
				System.exit(1);
			}
		}
	}
}