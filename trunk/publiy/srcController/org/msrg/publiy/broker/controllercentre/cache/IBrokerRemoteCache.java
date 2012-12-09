package org.msrg.publiy.broker.controllercentre.cache;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.controller.message.ErrorMessage;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.BrokerInfoComponentStatus;
import org.msrg.publiy.broker.info.BrokerInfoOpState;
import org.msrg.publiy.broker.info.ExceptionInfo;
import org.msrg.publiy.broker.info.ISessionInfo;
import org.msrg.publiy.broker.info.JoinInfo;
import org.msrg.publiy.broker.info.PSSessionInfo;
import org.msrg.publiy.broker.info.SubscriptionInfo;

public interface IBrokerRemoteCache {

	public InetSocketAddress getBrokerAddress();
	public InetSocketAddress getBrokerControllerAddress();
	
	public CachedBrokerInfo<BrokerInfoOpState> getCachedOpState();
	public CachedBrokerInfo<BrokerInfoComponentStatus> getCachedComponentState();
	public CachedBrokerInfo<JoinInfo> getCachedTopologyLinks();
	public CachedBrokerInfo<SubscriptionInfo> getCachedSubscriptions();
	public CachedBrokerInfo<ISessionInfo> getCachedSessions();
	public CachedBrokerInfo<PSSessionInfo> getCachedPSSessions();
	public CachedBrokerInfo<ExceptionInfo>[] getCachedExceptions();

	
	void receivedErrorMessage(ErrorMessage errorMessage);
	
	void updateStatus(BrokerInfoOpState opState, Sequence remoteSequence);
	void updateStatus(BrokerInfoComponentStatus componentState, Sequence remoteSequence);
	void updateTopologyLinks(JoinInfo[] joinInfos, Sequence remoteSequence);
	void updateSubscriptions(SubscriptionInfo[] subscriptionInfos, Sequence remoteSequence);
	void updateISessionInfos(ISessionInfo[] sessionInfos, Sequence remoteSequence);
	void updatePSSessionInfos(PSSessionInfo[] psSessionInfos, Sequence remoteSequence);
	void updateExceptionInfos(ExceptionInfo[] exceptionInfos, Sequence remoteSequence);
	
}
