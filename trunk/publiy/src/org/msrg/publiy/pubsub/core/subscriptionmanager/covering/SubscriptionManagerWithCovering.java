package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.subscriptionmanager.LocalSubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManager;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;
import org.msrg.publiy.utils.log.casuallogger.exectime.ExecutionTimeType;
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeEntity;


public class SubscriptionManagerWithCovering extends SubscriptionManager implements Runnable {

	protected Thread _coveringComputationsThread;
	protected List<SubscriptionManager_ComputeSubscriptionCoveringEvent> _subComputationEventsQueue = 
		new LinkedList<SubscriptionManager_ComputeSubscriptionCoveringEvent>();
	protected List<StandaloneCoveringSubscription> _coveringSubscriptions =
		new LinkedList<StandaloneCoveringSubscription>();

	protected final StatisticsLogger _logger;
	
	public SubscriptionManagerWithCovering(IBrokerShadow brokerShadow, String dumpFileName, boolean shouldLog) {
		super(brokerShadow, dumpFileName, shouldLog);
		
		if(!brokerShadow.coveringEnabled())
			throw new IllegalStateException();
		
		_logger = _brokerShadow.getStatisticsLogger();
		_coveringComputationsThread = new Thread(this);
		_coveringComputationsThread.start();
	}
	
	@Override
	public void addNewSubscriptionEntry(SubscriptionEntry newSubEntry) {
		super.addNewSubscriptionEntry(newSubEntry);
		insertNewSubcsriptionEntryEvent(newSubEntry);
	}
	
	@Override
	public void addNewLocalSubscriptionEntry(LocalSubscriptionEntry newLocalSubEntry) {
		super.addNewLocalSubscriptionEntry(newLocalSubEntry);
		insertNewSubcsriptionEntryEvent(newLocalSubEntry);
	}
	
	protected void insertNewSubcsriptionEntryEvent(SubscriptionEntry newSubscription) {
		SubscriptionManager_ComputeSubscriptionCoveringEvent newEvent =
			new SubscriptionManager_ComputeSubscriptionCoveringEvent(newSubscription);
		
		synchronized(_subComputationEventsQueue) {
			_subComputationEventsQueue.add(newEvent);
			_subComputationEventsQueue.notify();
		}
	}
	
	@Override
	public void run() {
		while(true) {
			SubscriptionManager_ComputeSubscriptionCoveringEvent event = null;
			
			synchronized(_subComputationEventsQueue)
			{
				while(_subComputationEventsQueue.isEmpty())
				{
					try {
						_subComputationEventsQueue.wait(10000);
						LoggerFactory.getLogger().info(this, "Subscription Computation Events: " + _subComputationEventsQueue.size());
					} catch (InterruptedException e) { e.printStackTrace(); }
				}
				
				event = _subComputationEventsQueue.remove(0);
			}
			
			SubscriptionEntry newSubscrptionEntry = event._subEntry;
			processNewSubscriptionEntry(newSubscrptionEntry);
		}
	}
	
	private void processNewSubscriptionEntry(SubscriptionEntry newSubscrptionEntry) {
		IExecutionTimeEntity coveringComputationExecutionTimeEntity =
			_brokerShadow.getExecutionTypeEntity(
					ExecutionTimeType.EXEC_TIME_COVERING_COMPUTATION_TIME);
		if(coveringComputationExecutionTimeEntity != null)
			coveringComputationExecutionTimeEntity.executionStarted();
		
		// Do Actual Work!
		CoveringSubscriptionComputer.insertNewSubscriptionIntoCoveringSubscriptions(
				newSubscrptionEntry, _coveringSubscriptions);
		
		if(coveringComputationExecutionTimeEntity != null)
			coveringComputationExecutionTimeEntity.executionEnded(true, false);
	}


	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_SUB_MANAGER_COVERING;
	}
}

class SubscriptionManager_ComputeSubscriptionCoveringEvent {
	protected final SubscriptionEntry _subEntry;
	
	SubscriptionManager_ComputeSubscriptionCoveringEvent(SubscriptionEntry subEntry) {
		_subEntry = subEntry;
	}
}
