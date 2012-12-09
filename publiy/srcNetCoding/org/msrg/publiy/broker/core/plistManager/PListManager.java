package org.msrg.publiy.broker.core.plistManager;

import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;

import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.connectionManager.IConnectionManagerNC;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.PublicationInfo;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;


import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.SimpleStringPredicate;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReqBreak;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReply;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_PListReq;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.utils.SortableNodeAddress;
import org.msrg.publiy.utils.SortableNodeAddressSet;
import org.msrg.publiy.utils.log.casuallogger.coding.CasualPListLogger;

public class PListManager implements IPListManager {
	
	public static final int MAX_PLIST_CROSS_SIZE_BROKER_BROKER_REPLY = 5;
	public static final int MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY = 5;
	public static final int MAX_PLIST_LOCAL_SIZE_SRC = 1;
	public static final int MAX_PLIST_LOCAL_SIZE_NONLAUNCH = 20;
	
	public final static String ATTR_CLASS = "CLASS";
	public final static String CLASS_PLIST_REQ = "PList-Req";
	public final static String CLASS_PLIST_REPLY = "PList-Reply";
	
	public final static String ATTR_REPLYING_BROKER = "REPLYING-BROKER";
	public final static String ATTR_HOME_BROKER = "HOME-BROKER";
	public final static String ATTR_COLS = "COLS";
	public final static String ATTR_ROWS = "ROWS";
	public final static String ATTR_CONTENT_SEQUENCE = "CONTENT-SEQUENCE";
	public final static String ATTR_LOCAL_INTERESTED_CLIENTS = "LOCAL-CLIENTS";

	protected final IConnectionManagerNC _connManNC;
	protected final InetSocketAddress _localAddress;
	protected final LocalSequencer _localSequencer;
	protected final Map<Sequence, PList> _plistMap =
		new HashMap<Sequence, PList>();
	protected final IBrokerShadow _brokerShadow;
	
	public PListManager(IConnectionManagerNC connManNC) {
		_connManNC = connManNC;
		_localAddress = _connManNC.getLocalAddress();
		_localSequencer = _connManNC.getLocalSequencer();
		_brokerShadow = _connManNC.getBrokerShadow();
	}
	
//	protected PListManager(InetSocketAddress localAddress) {
//		_connManNC = null;
//		_localAddress = localAddress;
//		_localSequencer = ???
//	}
	
	public static Publication turnIntoPListRequest(
			Publication pub, InetSocketAddress homeBroker, Sequence contentSequence, int rows, int cols) {
		pub.addStringPredicate(ATTR_CLASS, CLASS_PLIST_REQ);
		pub.addStringPredicate(ATTR_HOME_BROKER, Sequence.toStringClean(homeBroker));
		pub.addStringPredicate(ATTR_CONTENT_SEQUENCE, contentSequence.toStringLong());
		pub.addPredicate(ATTR_ROWS, rows);
		pub.addPredicate(ATTR_COLS, cols);
		
		return pub;
	}
	
	public static Publication turnIntoPListBrokerReply(
			Publication pub, InetSocketAddress replyingBrokerAddress,
			Collection<InetSocketAddress> localClients) {
		
		if(!isPListRequest(pub))
			return null;

		InetSocketAddress homeBroker = getPListHomeBroker(pub);
		if(homeBroker == null)
			return null;
		
		Sequence contentSequence = getPListContentSequence(pub);
		if(contentSequence == null)
			return null;
		
		Publication retPub = new Publication();
		retPub.addStringPredicate(ATTR_CLASS, CLASS_PLIST_REPLY);
		retPub.addStringPredicate(ATTR_REPLYING_BROKER, Sequence.toStringClean(replyingBrokerAddress));
		retPub.addStringPredicate(ATTR_HOME_BROKER, Sequence.toStringClean(homeBroker));
		retPub.addStringPredicate(ATTR_CONTENT_SEQUENCE, contentSequence.toStringLong());
		
		if(localClients.size() == 0)
			retPub.addStringPredicate(ATTR_LOCAL_INTERESTED_CLIENTS, "NULL");
		else {
			StringWriter localClientsWriter = new StringWriter();
			boolean first = true;
			for(InetSocketAddress localClient : localClients) {
				if(!first)
					localClientsWriter.append('|');
				
				localClientsWriter.append(localClient.toString());
				first = false;
			}
				
			retPub.addStringPredicate(ATTR_LOCAL_INTERESTED_CLIENTS, localClientsWriter.toString());
		}
		
		return retPub;
	}
	
	public static List<SortableNodeAddress> getLocalMatchingClientsFromPListBrokersReply(
			SortableNodeAddressSet sotableLaunchNodes, Publication pub) {
		PListTypes type = getPListType(pub);
		if(type != PListTypes.PLIST_REPLY)
			return null;
		
		List<String> crossClientsStrList = pub.getStringPredicate(ATTR_LOCAL_INTERESTED_CLIENTS);
		if(crossClientsStrList == null || crossClientsStrList.isEmpty())
			return null;

		String crossClientsStr = crossClientsStrList.get(0);
		List<SortableNodeAddress> crossClients = new LinkedList<SortableNodeAddress>();
		if(crossClientsStr.trim().equals("NULL"))
			return crossClients;
		
		String crossClientsStrSplit[] = crossClientsStr.split("\\|");
		if(crossClientsStrSplit.length == 0)
			return crossClients;
		
		for(String crossClientStr : crossClientsStrSplit) {
			SortableNodeAddress crossClient = sotableLaunchNodes.getSortableNodeAddress(
					Sequence.readAddressFromString(crossClientStr));
			
			crossClients.add(crossClient);
		}

		return crossClients;
	}
	
	public static PListTypes getPListType(Publication pub) {
		List<String> classValueList = pub.getStringPredicate(ATTR_CLASS);
		if(classValueList == null || classValueList.isEmpty())
			return null;
		
		String classValue = classValueList.get(0);
		if(classValue.equals(CLASS_PLIST_REPLY))
			return PListTypes.PLIST_REPLY;
		else if(classValue.equals(CLASS_PLIST_REQ))
			return PListTypes.PLIST_REQ;
		
		return null;
	}
	
	public static boolean isPListReply(Publication pub) {
		PListTypes type = getPListType(pub);
		if(type == null)
			return false;
		
		return type == PListTypes.PLIST_REPLY;
	}
	
	public static boolean isPListRequest(Publication pub) {
		PListTypes type = getPListType(pub);
		if(type == null)
			return false;
		
		return type == PListTypes.PLIST_REQ;
	}
	
	public static InetSocketAddress getPListReplyingBroker(Publication pub) {
		if(getPListType(pub) == null)
			return null;
		
		List<String> replyingBrokerStrValueList = pub.getStringPredicate(ATTR_REPLYING_BROKER);
		if(replyingBrokerStrValueList == null || replyingBrokerStrValueList.isEmpty())
			return null;
		
		String replyingBrokerStrValue = replyingBrokerStrValueList.get(0);
		return Sequence.readAddressFromString(replyingBrokerStrValue);
	}
	
	public static int getPListRows(Publication pub) {
		if(getPListType(pub) == null)
			throw new NullPointerException("Wrong type: " + pub.toString());
		
		List<Integer> rowsStrValueList = pub.getPredicate(ATTR_ROWS);
		if(rowsStrValueList == null || rowsStrValueList.isEmpty())
			throw new NullPointerException("Missing attribute: " + pub.toString());
		
		Integer rowsValue = rowsStrValueList.get(0);
		return rowsValue.intValue();
	}
	
	public static int getPListCols(Publication pub) {
		if(getPListType(pub) == null)
			throw new NullPointerException("Wrong type: " + pub.toString());
		
		List<Integer> colsStrValueList = pub.getPredicate(ATTR_COLS);
		if(colsStrValueList == null || colsStrValueList.isEmpty())
			throw new NullPointerException("Missing attribute: " + pub.toString());
		
		Integer colsValue = colsStrValueList.get(0);
		return colsValue.intValue();
	}
	
	public static InetSocketAddress getPListHomeBroker(Publication pub) {
		if(getPListType(pub) == null)
			return null;
		
		List<String> homeBrokerStrValueList = pub.getStringPredicate(ATTR_HOME_BROKER);
		if(homeBrokerStrValueList == null || homeBrokerStrValueList.isEmpty())
			return null;
		
		String homeBrokerStrValue = homeBrokerStrValueList.get(0);
		return Sequence.readAddressFromString(homeBrokerStrValue);
	}
	
	public static Sequence getPListContentSequence(Publication pub) {
		if(getPListType(pub) == null)
			return null;
		
		List<String> contentSequenceStrValueList = pub.getStringPredicate(ATTR_CONTENT_SEQUENCE);
		if(contentSequenceStrValueList == null || contentSequenceStrValueList.isEmpty())
			return null;
		
		String contentSequenceStrValue = contentSequenceStrValueList.get(0);
		return Sequence.readSequenceFromString(contentSequenceStrValue);
	}
	
	public static Subscription getPListReplySubscription(InetSocketAddress localAddress) {
		Subscription sub =
			new Subscription().
			addStringPredicate(
					SimpleStringPredicate.buildSimpleStringPredicate(ATTR_CLASS, '~', CLASS_PLIST_REPLY)).
			addStringPredicate(
					SimpleStringPredicate.buildSimpleStringPredicate(ATTR_HOME_BROKER, '~', Sequence.toStringClean(localAddress)));
		
		return sub;
	}

	@Override
	public void handlePListBrokerRequest(Publication pub) {
		Sequence contentSequence = PListManager.getPListContentSequence(pub);
		InetSocketAddress homeBroker = PListManager.getPListHomeBroker(pub);
		int rows = PListManager.getPListRows(pub);
		int cols = PListManager.getPListCols(pub);
		CasualPListLogger plistLogger = _brokerShadow.getPlistLogger();
		if(plistLogger != null)
			plistLogger.plistBrokerRequest(homeBroker, contentSequence);
		
		PList plist = addPList(
				pub, contentSequence,
				rows, cols,
				null, homeBroker);
		
		Set<InetSocketAddress> localMatches = 
			_connManNC.getMatchingSet(pub, true);
		plist.loadAllLocalClients(localMatches);
		
		Set<InetSocketAddress> plistClients =
			new HashSet<InetSocketAddress>();
		
		SortedSet<SortableNodeAddress> sortedLocalClients =
			_connManNC.convertToSortableLaunchNodeAddress(localMatches);
		while(plistClients.size() < MAX_PLIST_CROSS_SIZE_BROKER_BROKER_REPLY
				&& !sortedLocalClients.isEmpty()) {
			SortableNodeAddress localClient = sortedLocalClients.first();
			sortedLocalClients.remove(localClient);
			plistClients.add(localClient);
		}
		
		_connManNC.incrementLaunchSortableNodeAddress(plistClients, 5);
		
		if(plistLogger != null)
			plistLogger.plistBrokerReply(
					plist._sourceAddress, contentSequence, null, plistClients);
		
		Publication plistReplyPublication =
			turnIntoPListBrokerReply(pub, _localAddress, plistClients);
		_connManNC.sendPListReplyPublication(plistReplyPublication);
	}

	@Override
	public void handlePListBrokerReply(Publication pub) {
		Sequence contentSequence = PListManager.getPListContentSequence(pub);
		InetSocketAddress homeBroker = PListManager.getPListHomeBroker(pub);
		
		if(!homeBroker.equals(_localAddress))
			return;
		
		PList plist = _plistMap.get(contentSequence);
		if(plist == null)
			throw new IllegalStateException();
		
		Set<InetSocketAddress> newLaunchClients =
			new HashSet<InetSocketAddress>(
					PListManager.getLocalMatchingClientsFromPListBrokersReply(
							_connManNC.getSortableLaunchNodesSet(), pub));
		Set<InetSocketAddress> launchClients =
			plist.loadLaunchClients(newLaunchClients);
		
		InetSocketAddress source = plist._sourceAddress;
		CasualPListLogger plistLogger = _brokerShadow.getPlistLogger();
		if(plistLogger != null)
			plistLogger.plistBrokerReply(
					source, contentSequence,
					null, newLaunchClients);

		informNewLaunchClients(
				contentSequence,
				plist._rows, plist._cols,
				newLaunchClients, launchClients);
		
		
		TNetworkCoding_PListReply pList =
			new TNetworkCoding_PListReply(
					true,
					_localAddress, contentSequence,
					plist._rows, plist._cols,
					null, newLaunchClients);
		_connManNC.sendPListReply(pList, source);
	}
	
	@Override
	public PList getPList(Sequence contentSequence) {
		return _plistMap.get(contentSequence);
	}
	
	@Override
	public PList addPList(
			Publication pub, Sequence contentSequence, int rows, int cols,
			InetSocketAddress sourceAddress, InetSocketAddress homeBroker) {
		if(sourceAddress != null && !homeBroker.equals(_localAddress))
			throw new IllegalArgumentException(homeBroker + " vs. " + _localAddress);
		
		PList plist = _plistMap.get(contentSequence);
		if(plist != null)
			throw new IllegalStateException();
		
		if(sourceAddress == null) sourceAddress = contentSequence._address;
		plist = new PList(contentSequence, rows, cols, sourceAddress, homeBroker);
		_plistMap.put(contentSequence, plist);
		
		return plist;
	}
	
	@Override
	public void handlePListBreak(TNetworkCoding_CodedPieceIdReqBreak tCodedPieceBreak) {
		Sequence contentSequence = tCodedPieceBreak.getSourceSequence();
		InetSocketAddress sender = tCodedPieceBreak._sender;
		int breakingSize = tCodedPieceBreak._breakingSize;
		if(breakingSize >= 0)
			throw new IllegalArgumentException("Expecting breakingSize of -1 only. Got: " + breakingSize);
		
		PList plist = _plistMap.get(contentSequence);
		plist.downloadCompleted(sender);
	}

	static final int LOCAL_LAUNCH_SET_SIZE = MAX_PLIST_CROSS_SIZE_BROKER_BROKER_REPLY;
	protected static Set<InetSocketAddress> createLocalLaunchClients(
			SortableNodeAddressSet sortableLaunchNodesSet, PList plist) {
		Set<InetSocketAddress> localClients = plist.getLocalClients();
		SortedSet<SortableNodeAddress> sortedLocalClients =
			sortableLaunchNodesSet.convertToSortableSet(localClients);
		
		Set<InetSocketAddress> localLaunchSet =
			new HashSet<InetSocketAddress>();
		while(localLaunchSet.size() < LOCAL_LAUNCH_SET_SIZE
				&& !sortedLocalClients.isEmpty()) {
			InetSocketAddress sortedLocalClient = sortedLocalClients.first();
			sortedLocalClients.remove(sortedLocalClient);
			localLaunchSet.add(sortedLocalClient);
		}
		
		return plist.loadLaunchClients(localLaunchSet);
	}
	
	String makeString(Set<InetSocketAddress> addresses) {
		StringWriter writer = new StringWriter();
		writer.append('{');
		for(InetSocketAddress ad : addresses)
			if(ad != null)
				writer.append(ad.getPort() + ",");
		writer.append('}');
		
		return writer.toString();
	}
	
	protected void informNewLaunchClients(
			Sequence sourceSequence, int rows, int cols,
			Set<InetSocketAddress> newLaunchClients, Set<InetSocketAddress> launchClients) {
		if(!launchClients.containsAll(newLaunchClients))
			throw new IllegalStateException(
					launchClients + " vs. " + newLaunchClients.toString());
		
		CasualPListLogger plistLogger = _brokerShadow.getPlistLogger();
		for(InetSocketAddress launchclient : launchClients)
		{
			TNetworkCoding_PListReply tPListReply;
			if(newLaunchClients.contains(launchclient)) {
				tPListReply = new TNetworkCoding_PListReply(
						true,
						_localAddress, sourceSequence,
						rows, cols,
						null, launchClients);
				if(plistLogger != null)
					plistLogger.plistLaunchClientReply(
							launchclient, sourceSequence, null, launchClients);
			} else {
				tPListReply = new TNetworkCoding_PListReply(
						true,
						_localAddress, sourceSequence,
						rows, cols,
						null, newLaunchClients);
				if(plistLogger != null)
					plistLogger.plistLaunchClientReply(
							launchclient, sourceSequence, null, newLaunchClients);
			}
			_connManNC.sendPListReply(tPListReply, launchclient);
		}
	}
	
	@Override
	public void handlePListClientRequest(
			TNetworkCoding_PListReq pListReq) {
		Publication publication = pListReq._publication;
		InetSocketAddress sender = pListReq._sender;
		Sequence contentSequence = pListReq._sequence;
		boolean fromSource = pListReq._fromSource;
		
		PList pList = getPList(contentSequence);

		CasualPListLogger plistLogger = _brokerShadow.getPlistLogger();
		if(plistLogger != null)
			if(pList == null || pList.treatAsLaunchClient(sender))
				plistLogger.plistLaunchClientRequest(sender, contentSequence);
			else
				plistLogger.plistNonLaunchClientRequest(sender, contentSequence);
			
		if(pList == null && fromSource) {
			pList = addPList(
					publication, contentSequence,
					pListReq._rows, pListReq._cols,
					contentSequence._address, _localAddress);
			
			// Inquire about other interested clients in other regions
			Publication pListRequestPublication =
				turnIntoPListRequest(
						publication, _localAddress, contentSequence,
						pListReq._rows, pListReq._cols);
			PubForwardingStrategy pubForwardingStrategy =
				_connManNC.getBrokerShadow().getPublicationForwardingStrategy();
			TMulticast_Publish_MP tmpMP =
				new TMulticast_Publish_MP(
						pListRequestPublication, _localAddress, 0,
						(byte)0, pubForwardingStrategy, _localSequencer.getNext());
			_connManNC.sendTMPListRequest(tmpMP);
			
			// Identify local clients
			Set<InetSocketAddress> localMatchingClients =
				_connManNC.getMatchingSet(publication, true);
			pList.loadAllLocalClients(localMatchingClients);
			
			// Create lunch clients
			SortableNodeAddressSet sortableLaunchNodesSet =
				_connManNC.getSortableLaunchNodesSet();
			Set<InetSocketAddress> launchClients =
				createLocalLaunchClients(sortableLaunchNodesSet, pList);
			_connManNC.incrementLaunchSortableNodeAddress(launchClients, 1);
			
			informNewLaunchClients(
					contentSequence,
					pList._rows, pList._cols,
					new HashSet<InetSocketAddress>(launchClients),
					launchClients);
			
			TNetworkCoding_PListReply tPListReply =
				createPListReplyForLaunchClient(
						pList, contentSequence, sender);
			_connManNC.sendPListReply(tPListReply, sender);
		} else {
			TNetworkCoding_PListReply tPListReply;
			if(pList.treatAsLaunchClient(sender))
				tPListReply = createPListReplyForLaunchClient(
						pList, contentSequence, sender);
			else
				tPListReply = createPListReplyForNonLaunchClient(
						pList, contentSequence, sender);
					
			_connManNC.sendPListReply(tPListReply, sender);
		}
	}
	
	TNetworkCoding_PListReply createPListReplyForNonLaunchClient(
			PList pList, Sequence contentSequence, InetSocketAddress requester) {
		Set<InetSocketAddress> localClients;
		TNetworkCoding_PListReply tPListReply;
		
		int plistReplySize = MAX_PLIST_LOCAL_SIZE_NONLAUNCH;
		SortableNodeAddressSet sortableNonLaunchSet = _connManNC.getSortableNonLaunchNodesSet();
		localClients = pList.getLocalClients(sortableNonLaunchSet, plistReplySize, requester);
		_connManNC.incrementNonLaunchSortableNodeAddress(localClients, 1);
		
		tPListReply =
			new TNetworkCoding_PListReply(
					false,
					_localAddress, contentSequence,
					pList._rows, pList._cols,
					localClients, null);
	
		CasualPListLogger plistLogger = _brokerShadow.getPlistLogger();
		if(plistLogger != null)
			plistLogger.plistNonLaunchClientReply(requester, contentSequence, localClients, null);

		return tPListReply;
	}
	
	TNetworkCoding_PListReply createPListReplyForLaunchClient(
			PList pList, Sequence contentSequence, InetSocketAddress requester) {
		Set<InetSocketAddress> launchClients = pList.getLaunchClients();
		Set<InetSocketAddress> localClients = null;
		TNetworkCoding_PListReply tPListReply;
		
		if(launchClients.size() != 0) {
			tPListReply =
				new TNetworkCoding_PListReply(
						true,
						_localAddress, contentSequence,
						pList._rows, pList._cols,
						null, launchClients);
		} else {
			int plistReplySize = MAX_PLIST_LOCAL_SIZE_NONLAUNCH;
			SortableNodeAddressSet sortableNonLaunchSet = _connManNC.getSortableNonLaunchNodesSet(); 
			localClients = pList.getLocalClients(sortableNonLaunchSet, plistReplySize, requester);
//			_connManNC.incrementSortableNodeAddress(localClients, 0.1);
			
			tPListReply = new TNetworkCoding_PListReply(
					false,
					_localAddress, contentSequence,
					pList._rows, pList._cols,
					localClients, null);
		}
		
		CasualPListLogger plistLogger = _brokerShadow.getPlistLogger();
		if(plistLogger != null)
			plistLogger.plistLaunchClientReply(
					requester, contentSequence, localClients, launchClients);
		
		return tPListReply;
	}
	
	@Override
	public String toString() {
		StringWriter writer = new StringWriter();
		writer.append("PListManager: ");
		boolean first = true;
		for(Entry<Sequence, PList> entry : _plistMap.entrySet()) {
			if(first)
				writer.append(',');
			writer.append("{" + entry.getValue() + "}");
			first = false;
		}
		
		return writer.toString();
	}

	@Override
	public PublicationInfo[] getReceivedPublications() {
		return null;
	}

	@Override
	public void matchingPublicationDelivered(TMulticast_Publish tmp) {
		return;
	}

	@Override
	public void matchingPublicationDelivered(
			Sequence sourceSequence, Publication publication) {
		return;
	}

	@Override
	public void tmConfirmed(TMulticast tm) {
		return;
	}

	@Override
	public int getCount() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PublicationInfo getLastReceivedPublications() {
		throw new UnsupportedOperationException();
	}
	
	public static void main(String[] argv) {
		InetSocketAddress homeBroker = new InetSocketAddress(2000);
		Subscription sub =
			new Subscription().
			addStringPredicate(SimpleStringPredicate.buildSimpleStringPredicate(ATTR_CLASS, '~', CLASS_PLIST_REPLY)).
			addStringPredicate(SimpleStringPredicate.buildSimpleStringPredicate(ATTR_HOME_BROKER, '~', homeBroker.toString()));
		
		int rows = 100; int cols = 10000; 
		InetSocketAddress sourceAddress = new InetSocketAddress(1000);
		Sequence contentSequence = Sequence.getRandomSequence(sourceAddress);
		Set<InetSocketAddress> localMatches = new HashSet<InetSocketAddress>();
		InetSocketAddress m1 = new InetSocketAddress(1001);
		InetSocketAddress m2 = new InetSocketAddress(1002);
		InetSocketAddress m3 = new InetSocketAddress(1003);
		
		PList plist = new PList(contentSequence, rows, cols, sourceAddress, homeBroker);
		localMatches.add(m1); localMatches.add(m2); localMatches.add(m3);
		SortableNodeAddressSet sortableNodeSet = new SortableNodeAddressSet();
		sortableNodeSet.incrementSortableNodeAddress(m3, 1);
		plist.loadAllLocalClients(localMatches);
		SortableNodeAddressSet sortableLaunchNodesSet = new SortableNodeAddressSet();
		Set<InetSocketAddress> localLaunchClients = createLocalLaunchClients(sortableLaunchNodesSet, plist);
		if(!localLaunchClients.contains(m1) || !localLaunchClients.contains(m2) || localLaunchClients.size() != 2)
			throw new IllegalStateException(localLaunchClients.toString());
		
		System.out.println(localLaunchClients.toString());
		BrokerInternalTimer.start();
		BrokerInternalTimer.inform(sub.toString());
	}

	@Override
	public long getLastPublicationReceiptTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Integer> getDeliveredPublicationCounterPerPublisher() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Long> getLastPublicationDeliveryTimesPerPublisher() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Publication> getLastPublicationDeliveredPerPublisher() {
		throw new UnsupportedOperationException();
	}
}
