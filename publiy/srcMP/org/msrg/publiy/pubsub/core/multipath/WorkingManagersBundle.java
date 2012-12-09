package org.msrg.publiy.pubsub.core.multipath;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.overlaymanager.IWorkingOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.WorkingOverlayManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.multipath.IWorkingSubscriptionManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.multipath.WorkingSubscriptionManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.multipath.WorkingSubscriptionManagerWithCovering;
import org.msrg.publiy.utils.log.casuallogger.exectime.ExecutionTimeType;
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeEntity;

public class WorkingManagersBundle {

	private static int _WORKING_VERSION = 0;
	
	public final ISession[] _sessions;
	public final IWorkingOverlayManager _workingOverlayManager;
	public final IWorkingSubscriptionManager _workingSubscriptionManager;
	final int _workingVersion;
	
	private WorkingManagersBundle(int workingVersion, ISession[] sessions, IWorkingOverlayManager workingOverlayManager, IWorkingSubscriptionManager workingSubscriptionManager){
		_workingVersion = workingVersion;
		_sessions = sessions;
		_workingOverlayManager = workingOverlayManager;
		_workingSubscriptionManager = workingSubscriptionManager;
	}
	
	public InetSocketAddress getLocalAddress() {
		return _workingOverlayManager.getLocalAddress();
	}
	
	public static WorkingManagersBundle createWorkingManagersBundle(
			IOverlayManager overlayManager, ISession[] activeSessions, ISubscriptionManager subscriptionManager){
		IWorkingOverlayManager workingOverlayManager = createWorkingOverlayManager(overlayManager, activeSessions);
		IWorkingSubscriptionManager workingSubscriptionManager = createWorkingSubscriptionManager(workingOverlayManager, subscriptionManager);
		for ( int i=0 ; i<activeSessions.length ; i++ ){
			InetSocketAddress remote = activeSessions[i].getRemoteAddress();
			if ( remote != null ){
				InetSocketAddress realRemote = workingOverlayManager.mapsToRealWokringNodeAddress(remote);
				ISession realSession = getSessionPrivately(activeSessions, realRemote);
				if ( realSession == null )
					throw new NullPointerException("Processing: " + activeSessions[i] + " failed! " 
								+ realRemote + " has no session! " + realSession); 
				activeSessions[i].setRealSession(realSession);
			}
		}

		WorkingManagersBundle workingManagersBundle = new WorkingManagersBundle(_WORKING_VERSION, activeSessions, workingOverlayManager, workingSubscriptionManager);
		_WORKING_VERSION++;
		
		return workingManagersBundle;
	}
	
	protected static IWorkingOverlayManager createWorkingOverlayManager(IOverlayManager overlayManager, ISession[] activeSessions) {
		IBrokerShadow brokerShadow = overlayManager.getBrokerShadow();
		IExecutionTimeEntity genericExecutionTimeEntity =
				brokerShadow.getExecutionTypeEntity(ExecutionTimeType.EXEC_TIME_WORKING_OVERLAY_MAN_CREATE);
		if(genericExecutionTimeEntity != null)
			genericExecutionTimeEntity.executionStarted();
		
		IWorkingOverlayManager workingOverlayManager = new WorkingOverlayManager(_WORKING_VERSION, overlayManager, activeSessions);

		if (genericExecutionTimeEntity != null)
			genericExecutionTimeEntity.executionEnded(true, false);
		
		return workingOverlayManager;
	}
	
	protected static IWorkingSubscriptionManager createWorkingSubscriptionManager (
			IWorkingOverlayManager workingOverlayManager, ISubscriptionManager subscriptionManager) {
		IBrokerShadow brokerShadow = workingOverlayManager.getBrokerShadow();
		IExecutionTimeEntity genericExecutionTimeEntity = 
				brokerShadow.getExecutionTypeEntity(ExecutionTimeType.EXEC_TIME_WORKING_SUBSCRIPTION_MAN_CREATE);
		if(genericExecutionTimeEntity != null)
			genericExecutionTimeEntity.executionStarted();
		
		IWorkingSubscriptionManager workingSubscriptionManager;
		if(brokerShadow.isNC())
			workingSubscriptionManager = new WorkingSubscriptionManagerWithCovering(_WORKING_VERSION, subscriptionManager, workingOverlayManager);
		else
			workingSubscriptionManager = new WorkingSubscriptionManager(_WORKING_VERSION, subscriptionManager, workingOverlayManager);
		
		if (genericExecutionTimeEntity != null)
			genericExecutionTimeEntity.executionEnded(true, false);
		
		return workingSubscriptionManager;
	}
	
	public IOverlayManager getMasterOverlayManager() {
		return _workingOverlayManager.getMasterOverlayManager();
	}
	
	public ISubscriptionManager getMasterSubscriptionManager() {
		return _workingSubscriptionManager.getMasterSubscriptionManager();
	}

	private static ISession getSessionPrivately(ISession[] activeSessions, InetSocketAddress realRemote) {
		for ( int i=0 ; i<activeSessions.length ; i++ )
			if ( activeSessions[i].getRemoteAddress().equals(realRemote) )
				return activeSessions[i];
		
		return null;
	}
}
