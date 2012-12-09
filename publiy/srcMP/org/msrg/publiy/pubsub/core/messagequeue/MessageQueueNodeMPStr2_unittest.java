package org.msrg.publiy.pubsub.core.messagequeue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.multipath.WorkingManagerBundles_UnitTest;
import org.msrg.publiy.pubsub.core.multipath.WorkingManagersBundle;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.IWorkingOverlayManager;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.multipath.IWorkingSubscriptionManager;

import publishSubscribe.Publication;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

public class MessageQueueNodeMPStr2_unittest extends TestCase {

	static PubForwardingStrategy _strategy = PubForwardingStrategy.PUB_FORWARDING_STRATEGY_3;
	static int _delta = 3;
	
	static {
		System.setProperty("Broker.DELTA", "" + _delta);
	}
	
	protected Set<InetSocketAddress> _printoutAddresses = new HashSet<InetSocketAddress>();
	protected String _printoutAddressesStr = "127.0.0.1:2003,127.0.0.1:2000,127.0.0.1:2012," +
			"127.0.0.1:2015,127.0.0.1:2018,127.0.0.1:2009,127.0.0.1:2010,127.0.0.1:2019," +
			"127.0.0.1:2007,127.0.0.1:2020,127.0.0.1:2002,127.0.0.1:2011,127.0.0.1:2014," +
			"127.0.0.1:2017";
	protected Map<InetSocketAddress, IBrokerShadow> _allBrokerShadows = new HashMap<InetSocketAddress, IBrokerShadow>();
	protected Map<InetSocketAddress, WorkingManagersBundle> _allWorkingManagersBundles = new HashMap<InetSocketAddress, WorkingManagersBundle>();
	protected Map<InetSocketAddress, MessageQueueMP_ForTest> _allMQMPs = new HashMap<InetSocketAddress, MessageQueueMP_ForTest>();
	
	protected Publication _publication;
	protected final String _publicationFromStr = "127.0.0.1:2015";
	protected final String _publictionStr = "word    1";
	
	protected MessageQueueNodeMP _mqnMPSender;
	protected MessageQueueMP_ForTest _mqMPSender;

	
	protected MessageQueueNodeMP _mqnMPReceiver;
	protected MessageQueueMP_ForTest _mqMPReceiver;

	
	public void setUp() {
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
		
		_publication = Publication.decode(_publictionStr);

		// 2003
		InetSocketAddress localAddress2003 = new InetSocketAddress("127.0.0.1", 2003);
		String actSessions2003Str = "127.0.0.1:2000";
		String softSessions2003Str = "127.0.0.1:2001,127.0.0.1:2012," +
				"127.0.0.1:2009,127.0.0.1:2010," +
				"127.0.0.1:2018";
		String candSessions2003Str = "127.0.0.1:2019,127.0.0.1:2011";
		String topString2003 = 
			"S/127.0.0.1:2003 B/127.0.0.1:2000" + "\n" +
			"B/127.0.0.1:2000 B/127.0.0.1:2009" + "\n" +
			"B/127.0.0.1:2009 B/127.0.0.1:2010" + "\n" +
			"B/127.0.0.1:2010 B/127.0.0.1:2019" + "\n" +
			"B/127.0.0.1:2010 B/127.0.0.1:2011" + "\n" +
			"B/127.0.0.1:2010 B/127.0.0.1:2001" + "\n" +
			"B/127.0.0.1:2009 B/127.0.0.1:2018" + "\n" +
			"B/127.0.0.1:2018 P/127.0.0.1:2015" + "\n" +
			"B/127.0.0.1:2018 S/127.0.0.1:2006" + "\n" +
			"B/127.0.0.1:2000 P/127.0.0.1:2012";
		String subString2003 =
			"127.0.0.1:2011 word    =       1," + "\n" +
			"127.0.0.1:2019 word    =       1," + "\n" +
			"127.0.0.1:2015 word    =       1," + "\n" +
			"127.0.0.1:2012 word    =       1,";
		WorkingManagersBundle workingBundleManager2003 = WorkingManagerBundles_UnitTest.createWorkingManagerBundles(
				localAddress2003, actSessions2003Str, softSessions2003Str, candSessions2003Str,
				topString2003, subString2003);
		_allWorkingManagersBundles.put(localAddress2003, workingBundleManager2003);
		_allBrokerShadows.put(localAddress2003, new BrokerShadow(localAddress2003));
		
		MessageQueueMP_ForTest mqMP2003 = new MessageQueueMP_ForTest(localAddress2003, workingBundleManager2003._sessions, workingBundleManager2003);
		_allMQMPs.put(localAddress2003, mqMP2003);
		
		// 2010
		InetSocketAddress localAddress2010 = new InetSocketAddress("127.0.0.1", 2010);
		String actSessions2010Str = "127.0.0.1:2001,127.0.0.1:2019,127.0.0.1:2009,127.0.0.1:2011";
		String softSessions2010Str = "";
		String candSessions2010Str = "";
		String topString2010 = "B/127.0.0.1:2010 B/127.0.0.1:2019" + "\n" +
				"B/127.0.0.1:2019 S/127.0.0.1:2007" + "\n" +
				"B/127.0.0.1:2019 P/127.0.0.1:2016" + "\n" +
				"B/127.0.0.1:2010 B/127.0.0.1:2001" + "\n" +
				"B/127.0.0.1:2001 S/127.0.0.1:2004" + "\n" +
				"B/127.0.0.1:2001 P/127.0.0.1:2013" + "\n" +
				"B/127.0.0.1:2010 B/127.0.0.1:2011" + "\n" +
				"B/127.0.0.1:2011 B/127.0.0.1:2020" + "\n" +
				"B/127.0.0.1:2020 P/127.0.0.1:2008" + "\n" +
				"B/127.0.0.1:2020 S/127.0.0.1:2017" + "\n" +
				"B/127.0.0.1:2011 B/127.0.0.1:2002" + "\n" +
				"B/127.0.0.1:2002 P/127.0.0.1:2005" + "\n" +
				"B/127.0.0.1:2002 S/127.0.0.1:2014" + "\n" +
				"B/127.0.0.1:2010 B/127.0.0.1:2009" + "\n" +
				"B/127.0.0.1:2009 B/127.0.0.1:2000" + "\n" +
				"B/127.0.0.1:2000 S/127.0.0.1:2012" + "\n" +
				"B/127.0.0.1:2000 P/127.0.0.1:2003" + "\n" +
				"B/127.0.0.1:2009 B/127.0.0.1:2018" + "\n" +
				"B/127.0.0.1:2018 S/127.0.0.1:2015" + "\n" +
				"B/127.0.0.1:2018 P/127.0.0.1:2006";
		String subString2010 = 
			"127.0.0.1:2007 word    =       1," + "\n" +
			"127.0.0.1:2015 word    =       1," + "\n" +
			"127.0.0.1:2017 word    =       1," + "\n" +
			"127.0.0.1:2014 word    =       1," + "\n" +
			"127.0.0.1:2012 word    =       1,";
		WorkingManagersBundle workingBundleManager2010 = WorkingManagerBundles_UnitTest.createWorkingManagerBundles(
				localAddress2010, actSessions2010Str, softSessions2010Str, candSessions2010Str,
				topString2010, subString2010);
		_allWorkingManagersBundles.put(localAddress2010, workingBundleManager2010);
		_allBrokerShadows.put(localAddress2010, new BrokerShadow(localAddress2010));
		MessageQueueMP_ForTest mqMP2010 = new MessageQueueMP_ForTest(localAddress2010, workingBundleManager2010._sessions, workingBundleManager2010);
		_allMQMPs.put(localAddress2010, mqMP2010);
		
		// 2011
		InetSocketAddress localAddress2011 = new InetSocketAddress("127.0.0.1", 2011);
		String actSessions2011Str = "127.0.0.1:2010,127.0.0.1:2020,127.0.0.1:2002";
		String softSessions2011Str = "127.0.0.1:2008";
		String candSessions2011Str = "127.0.0.1:2014";
		String topString2011 =
				"B/127.0.0.1:2011 B/127.0.0.1:2010" + "\n" +
				"B/127.0.0.1:2010 B/127.0.0.1:2009" + "\n" +
				"B/127.0.0.1:2009 B/127.0.0.1:2000" + "\n" +
				"B/127.0.0.1:2000 S/127.0.0.1:2003" + "\n" +
				"B/127.0.0.1:2000 P/127.0.0.1:2012" + "\n" +
				"B/127.0.0.1:2009 B/127.0.0.1:2018" + "\n" +
				"B/127.0.0.1:2018 S/127.0.0.1:2006" + "\n" +
				"B/127.0.0.1:2018 P/127.0.0.1:2015" + "\n" +
				"B/127.0.0.1:2010 B/127.0.0.1:2019" + "\n" +
				"B/127.0.0.1:2019 P/127.0.0.1:2007" + "\n" +
				"B/127.0.0.1:2019 S/127.0.0.1:2016" + "\n" +
				"B/127.0.0.1:2010 B/127.0.0.1:2001" + "\n" +
				"B/127.0.0.1:2001 P/127.0.0.1:2004" + "\n" +
				"B/127.0.0.1:2001 S/127.0.0.1:2013" + "\n" +
				"B/127.0.0.1:2011 B/127.0.0.1:2002" + "\n" +
				"B/127.0.0.1:2002 S/127.0.0.1:2005" + "\n" +
				"B/127.0.0.1:2002 P/127.0.0.1:2014" + "\n" +
				"B/127.0.0.1:2011 B/127.0.0.1:2020" + "\n" +
				"B/127.0.0.1:2020 S/127.0.0.1:2008" + "\n" +
				"B/127.0.0.1:2020 P/127.0.0.1:2017";
		String subString2011 = 
			"127.0.0.1:2017 word    =       1," + "\n" +
			"127.0.0.1:2014 word    =       1,";
		WorkingManagersBundle workingBundleManager2011 = WorkingManagerBundles_UnitTest.createWorkingManagerBundles(
				localAddress2011, actSessions2011Str, softSessions2011Str, candSessions2011Str,
				topString2011, subString2011);
		_allWorkingManagersBundles.put(localAddress2011, workingBundleManager2011);
		_allBrokerShadows.put(localAddress2011, new BrokerShadow(localAddress2011));
		MessageQueueMP_ForTest mqMP2011 = new MessageQueueMP_ForTest(localAddress2011, workingBundleManager2011._sessions, workingBundleManager2011);
		_allMQMPs.put(localAddress2011, mqMP2011);
		
		// 2019
		InetSocketAddress localAddress2019 = new InetSocketAddress("127.0.0.1", 2019);
		String actSessions2019Str = "127.0.0.1:2010,127.0.0.1:2007,127.0.0.1:2016";
		String softSessions2019Str = "";
		String candSessions2019Str = "";
		String topString2019 =
				"B/127.0.0.1:2019 B/127.0.0.1:2010" + "\n" +
				"B/127.0.0.1:2010 B/127.0.0.1:2011" + "\n" +
				"B/127.0.0.1:2011 B/127.0.0.1:2020" + "\n" +
				"B/127.0.0.1:2020 S/127.0.0.1:2008" + "\n" +
				"B/127.0.0.1:2020 P/127.0.0.1:2017" + "\n" +
				"B/127.0.0.1:2011 B/127.0.0.1:2002" + "\n" +
				"B/127.0.0.1:2002 S/127.0.0.1:2005" + "\n" +
				"B/127.0.0.1:2002 P/127.0.0.1:2014" + "\n" +
				"B/127.0.0.1:2010 B/127.0.0.1:2001" + "\n" +
				"B/127.0.0.1:2001 P/127.0.0.1:2004" + "\n" +
				"B/127.0.0.1:2001 S/127.0.0.1:2013" + "\n" +
				"B/127.0.0.1:2010 B/127.0.0.1:2009" + "\n" +
				"B/127.0.0.1:2009 B/127.0.0.1:2000" + "\n" +
				"B/127.0.0.1:2000 P/127.0.0.1:2012" + "\n" +
				"B/127.0.0.1:2000 S/127.0.0.1:2003" + "\n" +
				"B/127.0.0.1:2009 B/127.0.0.1:2018" + "\n" +
				"B/127.0.0.1:2018 S/127.0.0.1:2006" + "\n" +
				"B/127.0.0.1:2018 P/127.0.0.1:2015" + "\n" +
				"B/127.0.0.1:2019 B/127.0.0.1:2007" + "\n" +
				"B/127.0.0.1:2019 B/127.0.0.1:2016";
		String subString2019 = 
//			"127.0.0.1:2007 word    =       1," + "\n" +
//			"127.0.0.1:2015 word    =       1," + "\n" +
//			"127.0.0.1:2012 word    =       1,";
			"127.0.0.1:2007 word    =       1,";
		WorkingManagersBundle workingBundleManager2019 = WorkingManagerBundles_UnitTest.createWorkingManagerBundles(
				localAddress2019, actSessions2019Str, softSessions2019Str, candSessions2019Str,
				topString2019, subString2019);
		_allWorkingManagersBundles.put(localAddress2019, workingBundleManager2019);
		_allBrokerShadows.put(localAddress2019, new BrokerShadow(localAddress2019));
		MessageQueueMP_ForTest mqMP2019 = new MessageQueueMP_ForTest(localAddress2019, workingBundleManager2019._sessions, workingBundleManager2019);
		_allMQMPs.put(localAddress2019, mqMP2019);
	}
	
	public void tearDown() {
		if (true)
			return;
		
		Writer ioWriter = new StringWriter();

		try {
			for(Map.Entry<InetSocketAddress, WorkingManagersBundle> entries : _allWorkingManagersBundles.entrySet())
				writeResults(ioWriter, entries.getValue());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(ioWriter.toString());
	}
	
	static protected void writeResults(Writer ioWriter, WorkingManagersBundle workingBundleManager) throws IOException {
		ioWriter.write("Master Overlay:\n");
		((OverlayManager)workingBundleManager.getMasterOverlayManager()).dumpOverlay(ioWriter);
	
		for(ISession session : workingBundleManager._sessions)
			ioWriter.write(session.toString());
		
		((OverlayManager)workingBundleManager._workingOverlayManager).dumpOverlay(ioWriter);
		
		ioWriter.write("Working Subcsription manager:\n");
		((SubscriptionManager)workingBundleManager._workingSubscriptionManager).dumpSubscriptions(ioWriter);
	}
	
	void printout(InetSocketAddress remote, String msg) {
		if(_printoutAddresses.contains(remote))
			System.out.println(msg);
	}
	
	public void testMessageQueueNodeMP() {
		InetSocketAddress initialSender = new InetSocketAddress("127.0.0.1", 2003);
		LocalSequencer.destroy();
		LocalSequencer.init(null, initialSender);
		Broker.initWithBrokerShadow(_allBrokerShadows.get(initialSender));
		TMulticast_Publish_MP tmp = new TMulticast_Publish_MP(
				_publication, initialSender, 0, (byte)0, PubForwardingStrategy.PUB_FORWARDING_STRATEGY_0).getShiftedClone(1, new Sequence(initialSender, 0, 0));
		tmp.setFrom(initialSender);
		IRawPacket raw = PacketFactory.wrapObject(tmp);
		SentMessage initialSending = new SentMessage(raw, new InetSocketAddress("127.0.0.1", 2003), null);
		List<SentMessage> sentMessages = new LinkedList<SentMessage>();
		sentMessages.add(initialSending);
		
		while(!sentMessages.isEmpty()) {
			List<SentMessage> newSentMessages = new LinkedList<SentMessage>();
			for(SentMessage sentMessage : sentMessages) {
				InetSocketAddress receiver = sentMessage._receiver;
				InetSocketAddress sender = sentMessage._sender;
				TMulticast_Publish_MP receivedTmp = (TMulticast_Publish_MP) PacketFactory.unwrapObject(sentMessage._raw);
				
				printout(receiver, "Receiver is: " + receiver + ", msg is: " + receivedTmp);
				LocalSequencer.destroy();
				LocalSequencer.init(null, receiver);
	
				MessageQueueMP_ForTest mq = _allMQMPs.get(receiver);
				receivedTmp.setFrom(sender);
				MessageQueueNodeMP_ForTest mqnMP = new MessageQueueNodeMP_ForTest(mq, receivedTmp, null, new Sequence(receiver, 0, 0));
				WorkingManagersBundle workingBundle = _allWorkingManagersBundles.get(receiver);
				if (workingBundle == null) 
					continue;
				
				ISession[] sessions = workingBundle._sessions;
				
				for(ISession session : sessions) {
					IRawPacket newRaw = mqnMP.checkOutAndmorph(session, receivedTmp.isGuided());
					if (newRaw==null)
						continue;
					
					SentMessage newSentMessage = new SentMessage(newRaw, session.getRealSession().getRemoteAddress(), receiver);
					newSentMessages.add(newSentMessage);
					printout(receiver, "\t" + newSentMessage);
				}
			}
			sentMessages.clear();
			sentMessages.addAll(newSentMessages);
		}
	}
}

private class MessageQueueNodeMP_ForTest extends MessageQueueNodeMP {

	MessageQueueNodeMP_ForTest(MessageQueueMP_ForTest mq, TMulticast tm,
			ITMConfirmationListener confirmationListener, Sequence seq) {
		super(mq, tm, confirmationListener, seq);
	}
	
	@Override
	protected ISession getISession(InetSocketAddress remote) {
		return ((MessageQueueMP_ForTest)_mQ).getISession(remote);
	}
}

class MessageQueueMP_ForTest extends MessageQueueMP {
	WorkingManagersBundle _workingManagersBundle;
	ISession[] _sessions;
	public MessageQueueMP_ForTest(InetSocketAddress localAddress, ISession[] sessions, WorkingManagersBundle workingManagersBundle) {
		super(MessageQueueNodeMPStr2_unittest._strategy);
		_workingManagersBundle = workingManagersBundle;
		_localAddress = localAddress;
		_sessions = sessions;
	}

	protected ISession getISession(InetSocketAddress remote) {
		for (int i=0 ; i<=_sessions.length ; i++)
			if(_sessions[i].getRemoteAddress().equals(remote))
				return _sessions[i];
		return null;
	}
	
	@Override
	public IWorkingOverlayManager getWorkingOverlayManager(){
		return _workingManagersBundle._workingOverlayManager;
	}
	
	@Override
	public IWorkingSubscriptionManager getWorkingSubscriptionManager(){
		return _workingManagersBundle._workingSubscriptionManager;
	}
	
	@Override
	public IOverlayManager getOverlayManager() {
		return _workingManagersBundle.getMasterOverlayManager();
	}
	
	@Override
	public ISubscriptionManager getSubscriptionManager() {
		return _workingManagersBundle.getMasterSubscriptionManager();
	}
}

class SentMessage {
	final IRawPacket _raw;
	final InetSocketAddress _receiver;
	final InetSocketAddress _sender;
	
	static Set<String> _senderRecieverPairs = new HashSet<String>();
	static Set<String> _recieverPairs = new HashSet<String>();
	SentMessage(IRawPacket raw, InetSocketAddress receiver, InetSocketAddress sender) {
		_raw = raw;
		_receiver = receiver;
		_sender = sender;
		
		IPacketable packet = raw.getObject();
		String sendReceiveIdentifier = "'" + _sender + "' -> '" + _receiver + "': " + ((TMulticast_Publish_MP)packet).getSourceSequence();
		if (!_senderRecieverPairs.add(sendReceiveIdentifier) )
			System.out.println("DUPLICATE: " + sendReceiveIdentifier);
		String receiveIdentifier = "'" + _receiver + "': " + ((TMulticast_Publish_MP)packet).getSourceSequence();
		if (!_recieverPairs.add(receiveIdentifier) )
			System.out.println("DUPLICATE: " + receiveIdentifier);
	}

	public String toString() {
		return "Sent (" + ((_sender==null)?null:_sender.getPort()) + "_" + ((_receiver==null)?null:_receiver.getPort()) + "): " + _raw.getQuickString();
	}
}