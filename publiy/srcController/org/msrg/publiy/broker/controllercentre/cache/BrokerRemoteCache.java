package org.msrg.publiy.broker.controllercentre.cache;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.broker.controller.message.ErrorMessage;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.BrokerInfoComponentStatus;
import org.msrg.publiy.broker.info.BrokerInfoOpState;
import org.msrg.publiy.broker.info.ExceptionInfo;
import org.msrg.publiy.broker.info.ISessionInfo;
import org.msrg.publiy.broker.info.JoinInfo;
import org.msrg.publiy.broker.info.PSSessionInfo;
import org.msrg.publiy.broker.info.SubscriptionInfo;

public class BrokerRemoteCache implements IBrokerRemoteCache {

	public static CachedBrokerInfo<BrokerInfoOpState> NO_OPSTATE_CACHE_AVAILABLE = 
		new CachedBrokerInfo<BrokerInfoOpState>((BrokerInfoOpState[])null, null);
	public static CachedBrokerInfo<BrokerInfoComponentStatus> NO_COMPONENTSTATE_CACHE_AVAILABLE = 
		new CachedBrokerInfo<BrokerInfoComponentStatus>((BrokerInfoComponentStatus[])null, null);
	public static CachedBrokerInfo<SubscriptionInfo> NO_SUBSCRIPTION_CACHE_AVAILABLE = 
		new CachedBrokerInfo<SubscriptionInfo>((SubscriptionInfo[])null, null);
	public static CachedBrokerInfo<ISessionInfo> NO_ISESSION_CACHE_AVAILABLE = 
		new CachedBrokerInfo<ISessionInfo>((ISessionInfo[])null, null);
	public static CachedBrokerInfo<PSSessionInfo> NO_PSSESSION_CACHE_AVAILABLE = 
		new CachedBrokerInfo<PSSessionInfo>((PSSessionInfo[])null, null);
	public static CachedBrokerInfo<JoinInfo> NO_TOPOLOGY_CACHE_AVAILABLE = 
		new CachedBrokerInfo<JoinInfo>((JoinInfo[])null, null);
	
	private InetSocketAddress _brokerAddress;
	private InetSocketAddress _brokerControllerAddress;
	
	private CachedBrokerInfo<BrokerInfoOpState> _cachedBrokerOpState = NO_OPSTATE_CACHE_AVAILABLE;
	private CachedBrokerInfo<BrokerInfoComponentStatus> _cachedBrokerComponentState = NO_COMPONENTSTATE_CACHE_AVAILABLE;
	private CachedBrokerInfo<SubscriptionInfo> _cachedBrokerSubscriptions = NO_SUBSCRIPTION_CACHE_AVAILABLE;
	private CachedBrokerInfo<ISessionInfo> _cachedBrokerISessionInfos = NO_ISESSION_CACHE_AVAILABLE;
	private CachedBrokerInfo<PSSessionInfo> _cachedBrokerPSSessionInfos = NO_PSSESSION_CACHE_AVAILABLE;
	private CachedBrokerInfo<JoinInfo> _cachedBrokerTopologyInfos = NO_TOPOLOGY_CACHE_AVAILABLE;
	private List<CachedBrokerInfo<ExceptionInfo>> _cachedExceptionInfos = new LinkedList<CachedBrokerInfo<ExceptionInfo>>();
	
	public BrokerRemoteCache(InetSocketAddress brokerAddress, InetSocketAddress brokerControllerAddress){
		_brokerAddress = brokerAddress;
		_brokerControllerAddress = brokerControllerAddress;
	}

	@Override
	public InetSocketAddress getBrokerAddress() {
		return _brokerAddress;
	}

	@Override
	public InetSocketAddress getBrokerControllerAddress() {
		return _brokerControllerAddress;
	}

	@Override
	public CachedBrokerInfo<PSSessionInfo> getCachedPSSessions() {
		return _cachedBrokerPSSessionInfos;
	}

	@Override
	public CachedBrokerInfo<ISessionInfo> getCachedSessions() {
		return _cachedBrokerISessionInfos;
	}

	@Override
	public CachedBrokerInfo<BrokerInfoOpState> getCachedOpState() {
		return _cachedBrokerOpState;
	}

	@Override
	public CachedBrokerInfo<BrokerInfoComponentStatus> getCachedComponentState() {
		return _cachedBrokerComponentState;
	}

	@Override
	public CachedBrokerInfo<SubscriptionInfo> getCachedSubscriptions() {
		return _cachedBrokerSubscriptions;
	}

	@Override
	public CachedBrokerInfo<JoinInfo> getCachedTopologyLinks() {
		return _cachedBrokerTopologyInfos;
	}

	@Override
	public void updateISessionInfos(ISessionInfo[] sessionInfos,
			Sequence remoteSequence) {
		_cachedBrokerISessionInfos = new CachedBrokerInfo<ISessionInfo>(sessionInfos, remoteSequence);
	}

	@Override
	public void updatePSSessionInfos(PSSessionInfo[] psSessionInfos,
			Sequence remoteSequence) {
		_cachedBrokerPSSessionInfos = new CachedBrokerInfo<PSSessionInfo>(psSessionInfos, remoteSequence);
	}
	
	@Override
	public void updateStatus(BrokerInfoComponentStatus componentState, Sequence remoteSequence){
		System.out.println("Updating " + componentState);
		_cachedBrokerComponentState = new CachedBrokerInfo<BrokerInfoComponentStatus>(componentState, remoteSequence);
	}

	@Override
	public void updateStatus(BrokerInfoOpState opState, Sequence remoteSequence) {
		System.out.println("Updating " + opState);
		_cachedBrokerOpState = new CachedBrokerInfo<BrokerInfoOpState>(opState, remoteSequence);
	}

	@Override
	public void updateSubscriptions(SubscriptionInfo[] subscriptionInfos,
			Sequence remoteSequence) {
		_cachedBrokerSubscriptions = new CachedBrokerInfo<SubscriptionInfo>(subscriptionInfos, remoteSequence);
	}

	@Override
	public void updateTopologyLinks(JoinInfo[] joinInfos,
			Sequence remoteSequence) {
		_cachedBrokerTopologyInfos = new CachedBrokerInfo<JoinInfo>(joinInfos, remoteSequence);
	}

	@Override
	public CachedBrokerInfo<ExceptionInfo>[] getCachedExceptions() {
		synchronized (_cachedExceptionInfos) {
			CachedBrokerInfo<ExceptionInfo>[] ret = _cachedExceptionInfos.toArray(new CachedBrokerInfo[0]);
			return ret;
		}
	}

	@Override
	public void receivedErrorMessage(ErrorMessage errorMessage) {
		ExceptionInfo[] exceptionInfos = (ExceptionInfo[])errorMessage.getBrokerInfos();
		if ( exceptionInfos == null )
			return;
		Sequence remoteSequence = errorMessage.getSourceInstanceID();
		
		synchronized (_cachedExceptionInfos) {
			for ( int i=0 ; i<exceptionInfos.length ; i++ )
			{
				CachedBrokerInfo<ExceptionInfo> newCachedExceptionInfo = new CachedBrokerInfo<ExceptionInfo>(exceptionInfos[i], remoteSequence);
				_cachedExceptionInfos.add(newCachedExceptionInfo);
			}
		}
	}

	@Override
	public void updateExceptionInfos(ExceptionInfo[] exceptionInfos, 
			Sequence remoteSequence) {
		if ( exceptionInfos == null )
			return;
		for ( int i=0 ; i<exceptionInfos.length ; i++ )
		{
			CachedBrokerInfo<ExceptionInfo> newCachedExceptionInfo = new CachedBrokerInfo<ExceptionInfo>(exceptionInfos[i], remoteSequence);
			_cachedExceptionInfos.add(newCachedExceptionInfo);
		}
	}

}
