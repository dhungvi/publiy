package org.msrg.publiy.pubsub.core.subscriptionmanager.multipath;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;


import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.client.subscriber.ISubscriptionListener;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.IWorkingOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.overlaymanager.WorkingRemoteSet;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_UnSubscribe;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.pubsub.core.subscriptionmanager.LocalSubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManager;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.log.casuallogger.exectime.ExecutionTimeType;
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeEntity;

public class WorkingSubscriptionManager extends SubscriptionManager implements IWorkingSubscriptionManager {

	protected final ISubscriptionManager _masterSubscriptionManager;
	protected final IWorkingOverlayManager _workingOverlayManager;
	protected final int _workingVersion;
	
	public WorkingSubscriptionManager(int workingVersion, ISubscriptionManager masterSubManager, IWorkingOverlayManager workingOverlayManager){
		super(masterSubManager.getBrokerShadow(), masterSubManager.getSubscriptionsDumpFilename() + "w", false);

		_workingVersion = workingVersion;
		
		addMastersSubEntries(masterSubManager, workingOverlayManager);
		
		_workingOverlayManager = workingOverlayManager;
		_masterSubscriptionManager = masterSubManager;
		dumpSubscriptions(_subscriptionsDumpFilename, !Broker.RELEASE);
	}
	
	protected void addMastersSubEntries(ISubscriptionManager masterSubManager, IWorkingOverlayManager workingOverlayManager) {
		for (LocalSubscriptionEntry localSubscriptionEntry : masterSubManager.getLocalSubscriptionEntries())
			_localSubscriptionEntries.add(localSubscriptionEntry);
		
		for (SubscriptionEntry masterSubEntry : masterSubManager.getSubscriptionEntries())
		 {
			InetSocketAddress masterFrom = masterSubEntry._orgFrom;
			InetSocketAddress workingFrom =
				workingOverlayManager.mapsToWokringNodeAddress(masterFrom);
			if(workingFrom == null)
				workingFrom = workingOverlayManager.isClientOf(masterFrom);
			masterSubEntry.setActiveFrom(workingFrom);
		}
		
		_subscriptionEntries.addAll(masterSubManager.getSubscriptionEntries());
	}

	@Override
	public String toString(){
		return "W-" + super.toString();
	}

	@Override
	public void handleMessage(TMulticast_Subscribe tms){
		throw new UnsupportedOperationException(tms.toStringTooLong());
	}
	
	@Override
	public void handleMessage(TMulticast_UnSubscribe tmus){
		throw new UnsupportedOperationException(tmus.toStringTooLong());
	}

	@Override
	public void applySummary(TMulticast_Subscribe tms, Path<INode> pathFromFrom) {
		if ( pathFromFrom == null )
			throw new NullPointerException("Pathfromfrom is null: " + tms);
		
		ISubscriptionListener subListener = tms._subscriber;
		if(subListener!=null) {
			super.addNewLocalSubscription(tms.getSubscription(), tms.getSourceSequence(), tms.getFrom(), subListener);
		} else {
			InetSocketAddress[] remotes = pathFromFrom.getAddresses();
			for ( int i=0 ; i<remotes.length ; i++ )
				if ( _workingOverlayManager.containsRemote(remotes[i]) ){
					super.applySummary(
							new TRecovery_Subscription(
									tms.getSubscription(),
									tms.getSourceSequence(),
									remotes[i]));
					return;
				}
		}
	}

	@Override
	public void applySummary(TMulticast_UnSubscribe tmus) {
		throw new UnsupportedOperationException(tmus.toStringTooLong());
	}

	@Override
	public Set<InetSocketAddress> getMatchingSet(Publication publication){
		throw new UnsupportedOperationException();
	}

	@Override
	public WorkingRemoteSet getMatchingWokringSet() {
		return new WorkingRemoteSet(_workingVersion);
	}
	
	@Override
	public final WorkingRemoteSet getMatchingWokringSet(Publication publication) {
		Set<InetSocketAddress> matchingSet = new HashSet<InetSocketAddress>();
		
		IExecutionTimeEntity genericExecutionTimeEntity = _brokerShadow.getExecutionTypeEntity(ExecutionTimeType.EXEC_TIME_MATCHING);
		if (genericExecutionTimeEntity != null)
			genericExecutionTimeEntity.executionStarted();

		int failed = 0;
		int succeed = 0;
		int totalSubsMatched = 0;
		int totalPredsMatched = 0;
		int totalSubsCovered = 0;
		int totalPredsCovered = 0;
		long start = SystemTime.nanoTime();
		
		synchronized (_subscriptionEntries)
		{
			for(SubscriptionEntry subEntry : _subscriptionEntries)
			{
				Subscription subscription = subEntry.getSubscription();
				InetSocketAddress from = subEntry._activeFrom;
				if(from == null && _brokerShadow.isNC()) {
					totalSubsCovered++;
					totalPredsCovered += (publication._predicates.size() * subscription._predicates.size());
					continue;
				}
				
				totalSubsMatched++;
				totalPredsMatched += (publication._predicates.size() * subscription._predicates.size());

				if ( _me.match(subscription, publication) ){
					matchingSet.add(from);
					succeed++;
				}
				else
					failed++;
			}
		}
		if (genericExecutionTimeEntity != null)
			genericExecutionTimeEntity.executionEnded(true, false);
		
		long end = SystemTime.nanoTime();
		if (_statisticsLogger != null)
			_statisticsLogger.numberOfSubscriptionsPredicatesMatched(
					(end-start), totalSubsMatched, totalPredsMatched, totalSubsCovered, totalPredsCovered);

		WorkingRemoteSet matchingWorkingSet = new WorkingRemoteSet(_workingVersion);
		matchingWorkingSet.setMatchingRemotes(matchingSet);
		return matchingWorkingSet;
	}

	@Override
	public int getWorkingVersion() {
		return _workingVersion;
	}

	@Override
	public ISubscriptionManager getMasterSubscriptionManager() {
		return _masterSubscriptionManager;
	}
	
	@Override
	public void addNewSubscriptionEntry(SubscriptionEntry newSubEntry) {
		InetSocketAddress activeFrom =
			_workingOverlayManager.mapsToWokringNodeAddress(newSubEntry._orgFrom);
		if(activeFrom == null) {
			InetSocketAddress potentialActiveFrom = _workingOverlayManager.isClientOf(newSubEntry._orgFrom);
			if(potentialActiveFrom != null && potentialActiveFrom.equals(_localAddress))
				activeFrom = newSubEntry._orgFrom;
			else
				activeFrom = potentialActiveFrom;
		}
		
		newSubEntry.setActiveFrom(activeFrom);
		super.addNewSubscriptionEntry(newSubEntry);
	}
	
	@Override
	public void addNewLocalSubscriptionEntry(LocalSubscriptionEntry newLocalSubEntry) {
		super.addNewLocalSubscriptionEntry(newLocalSubEntry);
	}
	
	@Override
	protected boolean shouldLogAdditionRemovalOfSubs() {
		return false;
	}
}
