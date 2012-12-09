package org.msrg.publiy.pubsub.core.subscriptionmanager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.SubscriptionInfo;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.client.subscriber.ISubscriptionListener;


import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngine;
import org.msrg.publiy.publishSubscribe.matching.MatchingEngineImp;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_UnSubscribe;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription_withActiveFrom;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;
import org.msrg.publiy.utils.log.casuallogger.exectime.ExecutionTimeType;
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeEntity;

public class SubscriptionManager implements ISubscriptionManager, ILoggerSource {
	private static final TRecovery_Subscription [] tmpTRSArray = new TRecovery_Subscription[0];

	protected final IBrokerShadow _brokerShadow;
	protected final LocalSequencer _localSequencer;
	protected final List<SubscriptionEntry> _subscriptionEntries;
	protected final List<LocalSubscriptionEntry> _localSubscriptionEntries;
	protected final MatchingEngine _me;
	protected final String _subscriptionsDumpFilename;
	protected final InetSocketAddress _localAddress;
	protected final boolean _shouldLog;
	protected final StatisticsLogger _statisticsLogger;
	
	public SubscriptionManager(IBrokerShadow brokerShadow, String dumpFileName, boolean shouldLog) {
		_brokerShadow = brokerShadow;
		_localSequencer = _brokerShadow.getLocalSequencer();
		_localAddress = _brokerShadow.getLocalAddress();
		_subscriptionEntries = new LinkedList<SubscriptionEntry>();
		_localSubscriptionEntries = new LinkedList<LocalSubscriptionEntry>();
		_me = new MatchingEngineImp();
		_subscriptionsDumpFilename = dumpFileName;
		_shouldLog = shouldLog;
		_statisticsLogger = brokerShadow.getStatisticsLogger();
	}
	
	@Override
	public void applySummary(TRecovery_Subscription trs) {
		Subscription subscription = trs.getSubscription();
		InetSocketAddress from = trs.getFrom();
		Sequence seqId = trs.getSequenceID();
		
		addNewSubscription(subscription, seqId, from, trs.isFromReal());
	}
	
	protected void addNewLocalSubscription(
			Subscription subscription, Sequence seqId, InetSocketAddress from, ISubscriptionListener subcsriberListener) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().info(this, "Adding new local subscription:: " + subscription + "_FROM_" + from);
		
		if(!from.equals(_localAddress))
			throw new IllegalArgumentException(from + " vs. " + _localAddress);

		LocalSubscriptionEntry newLocalSubEntry = new LocalSubscriptionEntry(seqId, subscription, from, subcsriberListener);
		addNewLocalSubscriptionEntry(newLocalSubEntry);
		
		return;
	}

	@Override
	public void addNewLocalSubscriptionEntry(LocalSubscriptionEntry newLocalSubEntry) {
		if(_statisticsLogger != null)
			if(shouldLogAdditionRemovalOfSubs()) {
				if(Broker.DEBUG)
					LoggerFactory.getLogger().debug(this, "Adding new local subscription:: " + newLocalSubEntry);
				_statisticsLogger.addedNewLocalSubscriptions(1);
			}

		synchronized (_localSubscriptionEntries) {
			if(!_localAddress.equals(newLocalSubEntry._orgFrom))
				throw new IllegalArgumentException(_localAddress + " vs. " + newLocalSubEntry._orgFrom);
			
			_localSubscriptionEntries.add(newLocalSubEntry);
			
			dumpSubscriptions(_subscriptionsDumpFilename);
		}
		return;
	}

	protected final void addNewSubscription(Subscription subscription, Sequence seqId, InetSocketAddress from, boolean fromReal) {
		if(Broker.DEBUG && _shouldLog)
			LoggerFactory.getLogger().info(this, "Adding new subscription:: " + subscription + "_FROM_" + from);
		
		SubscriptionEntry newSubEntry = new SubscriptionEntry(seqId, subscription, from, fromReal);
		addNewSubscriptionEntry(newSubEntry);
		
		return;
	}
	
	@Override
	public void addNewSubscriptionEntry(SubscriptionEntry newSubEntry) {
		if(_statisticsLogger != null)
			if(shouldLogAdditionRemovalOfSubs()) {
				if(Broker.DEBUG)
					LoggerFactory.getLogger().info(this, "Adding new subscription:: " + newSubEntry);
				_statisticsLogger.addedNewSubscriptions(1);
			}
		
		synchronized (_subscriptionEntries) {
			if(_localAddress.equals(newSubEntry._orgFrom))
				throw new IllegalArgumentException(_localAddress + " vs. " + newSubEntry._orgFrom);
			
			_subscriptionEntries.add(newSubEntry);
			
			dumpSubscriptions(_subscriptionsDumpFilename);
		}
		return;
	}
	
	@Override
	public Set<Subscription> getMatchingSubscriptionSet(Publication publication) {
		Set<Subscription> matchingSet = new HashSet<Subscription>();

		int failed = 0;
		int succeed = 0;
		int totalSubsMatched = 0;
		int totalPredsMatched = 0;
		long start = SystemTime.nanoTime();
		
		synchronized (_subscriptionEntries) {
			for(SubscriptionEntry subEntry : _subscriptionEntries) {
				Subscription subscription = subEntry.getSubscription();
				
				totalSubsMatched++;
				totalPredsMatched += (publication._predicates.size() * subscription._predicates.size());
				
				if(_me.match(subscription, publication)) {
					succeed++;
					matchingSet.add(subscription);
				}
				else
					failed++;
			}
			
//			((SpecialHashSet<InetSocketAddress>)matchingSet).attach(new SubscriptionMatchingSummary(getSubscriptionEntries(), failed, succeed, new HashSet<InetSocketAddress>(matchingSet)));
		}
		
		long end = SystemTime.nanoTime();
		if(_statisticsLogger != null)
			_statisticsLogger.numberOfSubscriptionsPredicatesMatched((end-start), totalSubsMatched, totalPredsMatched, 0, 0);
		
		return matchingSet;
	}

	@Override
	public Set<InetSocketAddress> getMatchingSet(Publication publication) {
		Set<InetSocketAddress> matchingSet = new HashSet<InetSocketAddress>();
		
		IExecutionTimeEntity genericExecutionTimeEntity = null;
		if(Broker.LOG_EXECTIME) {
			genericExecutionTimeEntity = _brokerShadow.getExecutionTypeEntity(ExecutionTimeType.EXEC_TIME_MATCHING);
			if(genericExecutionTimeEntity != null)
				genericExecutionTimeEntity.executionStarted();
		}
		
		{
			int failed = 0;
			int succeed = 0;
			int totalSubsMatched = 0;
			int totalPredsMatched = 0;
			long start = SystemTime.nanoTime();

			synchronized (_subscriptionEntries) {
				for(SubscriptionEntry subEntry : _subscriptionEntries) {
					Subscription subscription = subEntry.getSubscription();
					InetSocketAddress from = subEntry._orgFrom;
					if(from == null)
						throw new IllegalStateException(
								"Subscription Entry's activeFrom is null: " + subEntry);
					
					totalSubsMatched ++;
					totalPredsMatched += (publication._predicates.size() * subscription._predicates.size());
					
					if(_me.match(subscription, publication)) {
						succeed++;
						matchingSet.add(from);
					}
					else
						failed++;
				}
				
	//			((SpecialHashSet<InetSocketAddress>)matchingSet).attach(new SubscriptionMatchingSummary(getSubscriptionEntries(), failed, succeed, new HashSet<InetSocketAddress>(matchingSet)));
			}
			
			long end = SystemTime.nanoTime();
			if(_statisticsLogger != null)
				_statisticsLogger.numberOfSubscriptionsPredicatesMatched((end-start), totalSubsMatched, totalPredsMatched, 0, 0);
		}
		
		if(genericExecutionTimeEntity != null)
			genericExecutionTimeEntity.executionEnded(true, false);
		
		return matchingSet;
	}

	@Override
	public TRecovery_Subscription[] getLocalSummary(InetSocketAddress remote, IOverlayManager overlayManager) {
		Set<TRecovery_Subscription> subsSummarySet = new HashSet<TRecovery_Subscription>();

		for(LocalSubscriptionEntry subEntry : _localSubscriptionEntries) {
			Sequence seqID = subEntry._seqID;
			Subscription subscription = subEntry.getSubscription();
			InetSocketAddress oldFrom = subEntry._activeFrom;
			
			InetSocketAddress newFrom = overlayManager.getNewFromForMorphedMessage(remote, oldFrom);
			
			TRecovery_Subscription trs = new TRecovery_Subscription(subscription, seqID, newFrom);
			subsSummarySet.add(trs);
		}
					
		return subsSummarySet.toArray(tmpTRSArray);
	}
	
	@Override
	public TRecovery_Subscription[] getAllSummary() {
		List<TRecovery_Subscription> subsSummarySet = new LinkedList<TRecovery_Subscription>();
		synchronized (_subscriptionEntries) {
			for(SubscriptionEntry subEntry : _subscriptionEntries) {
				Sequence seqID = subEntry._seqID;
				Subscription subscription = subEntry.getSubscription();
				InetSocketAddress from = subEntry._orgFrom;
				InetSocketAddress activeFrom = subEntry._activeFrom;
				TRecovery_Subscription trs =
					new TRecovery_Subscription_withActiveFrom(subscription, seqID, from, activeFrom);
				subsSummarySet.add(trs);
			}
		}
		
		synchronized (_localSubscriptionEntries) {
			for(LocalSubscriptionEntry subEntry : _localSubscriptionEntries) {
				Sequence seqID = subEntry._seqID;
				Subscription subscription = subEntry.getSubscription();
				InetSocketAddress from = subEntry._orgFrom;
				InetSocketAddress activeFrom = from;
				TRecovery_Subscription trs =
					new TRecovery_Subscription_withActiveFrom(subscription, seqID, from, activeFrom);
				subsSummarySet.add(trs);
			}
		}
		
		return subsSummarySet.toArray(new TRecovery_Subscription[0]);
	}
	
	@Override
	public TRecovery_Subscription[] getAllSummary(InetSocketAddress remote, IOverlayManager overlayManager) {
		Set<TRecovery_Subscription> subsSummarySet = new HashSet<TRecovery_Subscription>();
		Path<INode> pathFromRemote = overlayManager.getPathFrom(remote);
		
		synchronized (_subscriptionEntries) {
			for(SubscriptionEntry subEntry : _subscriptionEntries) {
				Sequence seqID = subEntry._seqID;
				Subscription subscription = subEntry.getSubscription();
				InetSocketAddress oldFrom = subEntry._orgFrom;
				InetSocketAddress newFrom = oldFrom;
				
				Path<INode> pathFromOldFrom = overlayManager.getPathFrom(oldFrom);
				if(pathFromRemote.passes(oldFrom)) {
					// This means that oldFrom is also the source of subscription.
					newFrom = oldFrom;
				} else if(pathFromOldFrom.passes(remote)) {
					continue;
				} else {
					newFrom = overlayManager.getNewFromForMorphedMessage(remote, oldFrom);
				}
				
				if(newFrom != null)
				{
					TRecovery_Subscription trs = new TRecovery_Subscription(subscription, seqID, newFrom);
					subsSummarySet.add(trs);
				}
			}
		}
		
		for(LocalSubscriptionEntry subEntry : _localSubscriptionEntries) {
			Sequence seqID = subEntry._seqID;
			Subscription subscription = subEntry.getSubscription();
			InetSocketAddress oldFrom = subEntry._orgFrom;
			
			InetSocketAddress newFrom = oldFrom; //getNewFromForMorphedMessage(remote, overlayManager, oldFrom);
			
			if(newFrom != null) {
				TRecovery_Subscription trs = new TRecovery_Subscription(subscription, seqID, newFrom);
				subsSummarySet.add(trs);
			}
		}
					
		return subsSummarySet.toArray(tmpTRSArray);
	}

	@Override
	public void handleMessage(TMulticast_Subscribe tms) {
		ISubscriptionListener subscriptionListener = tms._subscriber;
		
		Subscription subscription = tms.getSubscription();
		InetSocketAddress from = tms.getFrom();
		Sequence seqId = tms.getSourceSequence();
		
		if(subscriptionListener == null)
			addNewSubscription(subscription, seqId, from, tms.isFromReal());
		else {
			addNewLocalSubscription(subscription, seqId, from, subscriptionListener);
		}
	}

	@Override
	public void handleMessage(TMulticast_UnSubscribe tmus) {
		throw new UnsupportedOperationException("Unsubscribe not implemented yet.");			
	}

	@Override
	public void applyAllSubscriptionSummary(TRecovery_Subscription[] trss) {
		for(int i=0 ; i<trss.length ; i++)
			applySummary(trss[i]);
	}

	public void dumpSubscriptions(Writer ioWriter) throws IOException {
		TRecovery_Subscription [] trss = getAllSummary();//_overlayManager.getLocalAddress(), _overlayManager);
		for(int i=0 ; i<trss.length ; i++) {
			Subscription subscription = trss[i].getSubscription();
			InetSocketAddress from = trss[i].getFrom();
			InetSocketAddress activeFrom = ((TRecovery_Subscription_withActiveFrom)trss[i]).getActiveFrom();
			Sequence sequence = trss[i].getSequenceID();
			boolean isFromReal = trss[i].isFromReal();
			
			ioWriter.write(
					subscription + " _F:" + from + " _AF:" + activeFrom + " _@_ SS" + sequence + (isFromReal?"*":".") + "\n");
		}
		
		for(int i=0 ; i<40 ; i++)
			ioWriter.write("=");
		
		ioWriter.write("#************** " + BrokerInternalTimer.read() + "\n");
	}
	
	@Override
	public boolean dumpSubscriptions(String dumpFileName) {
		return dumpSubscriptions(dumpFileName, true);
	}
	
	public boolean dumpSubscriptions(String dumpFileName, boolean force) {
		if(Broker.RELEASE)
			return true;
		
		if(!_shouldLog)
			return true;
		
		if(!force && !Broker.DEBUG)
			return true;
		
		if(dumpFileName == null)
			return true;
		
		try{
			FileWriter fwriter = new FileWriter(_subscriptionsDumpFilename, false);
			dumpSubscriptions(fwriter);
			fwriter.close();
		}catch (IOException iox) {
			return false;
		}
		return true;

	}

	@Override
	public void informLocalSubscribers(TMulticast_Publish tmp) {
		Publication publication = tmp.getPublication();
		if(publication==null)
			throw new NullPointerException(tmp.toStringTooLong());
		
		if(_brokerShadow != null && !_brokerShadow.canDeliverMessages())
			return;
		
		for(LocalSubscriptionEntry subEntry : _localSubscriptionEntries) {
			Subscription subscription = subEntry.getSubscription();
			ISubscriptionListener subscriptionListener = subEntry._localSubscriptionListener;
			
			if(_me.match(subscription, publication)) {
				if(subscriptionListener != null)
					subscriptionListener.matchingPublicationDelivered(tmp);
				break;
			}
		}
	}

	@Override
	public SubscriptionInfo[] getSubscriptionInfos() {
		synchronized (_subscriptionEntries) {
			int index = 0;
			SubscriptionInfo[] subscriptionInfos = new SubscriptionInfo[_subscriptionEntries.size()];
			for(SubscriptionEntry subEntry : _subscriptionEntries)
				subscriptionInfos[index++] = subEntry.getInfo();
			
			return subscriptionInfos;
		}
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_SUB_MANAGER;
	}

	@Override
	public String getSubscriptionsDumpFilename() {
		return _subscriptionsDumpFilename;
	}

	@Override
	public String toString() {
		String str1 = Writers.write(_subscriptionEntries.toArray(new SubscriptionEntry[0]));
		String str2 = Writers.write(_localSubscriptionEntries.toArray(new LocalSubscriptionEntry[0]));
		
		return 	"SUB-Manager:: " + str1 + "\n" + 
				"\t(locals):: " + str2;
	}
	
	public Collection<LocalSubscriptionEntry> getLocalSubscriptionEntries() {
		return _localSubscriptionEntries;
	}
	
	public Collection<SubscriptionEntry> getSubscriptionEntries() {
		return _subscriptionEntries;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return _localAddress;
	}

	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}

	@Override
	public Set<InetSocketAddress> getLocalMatchingSet(Publication publication) {
		Set<InetSocketAddress> matchingSet = new HashSet<InetSocketAddress>();
		synchronized (_localSubscriptionEntries) {
			for(SubscriptionEntry subEntry : _localSubscriptionEntries) {
				Subscription subscription = subEntry.getSubscription();
				InetSocketAddress from = subEntry._orgFrom;
				if(from == null)
					throw new IllegalStateException(
							"Subscription Entry's activeFrom is null: " + subEntry);
				
				if(_me.match(subscription, publication))
					matchingSet.add(from);
			}
		}
		
		synchronized (_subscriptionEntries) {
			for(SubscriptionEntry subEntry : _subscriptionEntries) {
				Subscription subscription = subEntry.getSubscription();
				InetSocketAddress from = subEntry._orgFrom;
				InetSocketAddress joinPointBroker =
					_brokerShadow.getBrokerIdentityManager().getJoinpointAddress(from);
				if(joinPointBroker == null || !joinPointBroker.equals(_localAddress))
					continue;
				if(from == null)
					throw new IllegalStateException(
							"Subscription Entry's activeFrom is null: " + subEntry);
				
				if(_me.match(subscription, publication))
					matchingSet.add(from);
			}
		}
		
		return matchingSet;
	}
	
	protected boolean shouldLogAdditionRemovalOfSubs() {
		return true;
	}
}
