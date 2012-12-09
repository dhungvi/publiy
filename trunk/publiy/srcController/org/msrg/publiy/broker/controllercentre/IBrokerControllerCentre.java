package org.msrg.publiy.broker.controllercentre;


import java.net.InetSocketAddress;
import java.util.Properties;

import org.msrg.publiy.broker.PubForwardingStrategy;


import org.msrg.publiy.broker.controller.sequence.IConnectionSequenceUpdateListener;
import org.msrg.publiy.broker.controller.sequence.IMessageSequenceUpdateListener;
import org.msrg.publiy.broker.controller.sequence.SequenceUpdateable;
import org.msrg.publiy.broker.controllercentre.cache.CachedBrokerInfo;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.BrokerInfoComponentStatus;
import org.msrg.publiy.broker.info.BrokerInfoOpState;
import org.msrg.publiy.broker.info.ExceptionInfo;
import org.msrg.publiy.broker.info.ISessionInfo;
import org.msrg.publiy.broker.info.JoinInfo;
import org.msrg.publiy.broker.info.PSSessionInfo;
import org.msrg.publiy.broker.info.SubscriptionInfo;
import org.msrg.publiy.gui.failuretimeline.ExtendedFailureTimelineEvents;
import org.msrg.publiy.node.NodeTypes;

public interface IBrokerControllerCentre {

	public void prepareAndStart();
	public void clearAllBrokers();
	
	public void registerOverlayNodeUpdateListener(IOverlayNodeUpdateListener overlayNodeUpdateListener, InetSocketAddress remote);
	
	public boolean addBroker(InetSocketAddress brokerAddress);
	public boolean addBroker(InetSocketAddress brokerAddress, InetSocketAddress brokerControllerAddress);
	public SequenceUpdateable<IConnectionSequenceUpdateListener> connect(InetSocketAddress brokerControllerAddress);
	
	public InetSocketAddress getBrokerControllerAddress(InetSocketAddress brokerAddress);
	public InetSocketAddress createBrokerControllerAddress(InetSocketAddress brokerAddress);
	
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueJoin(NodeTypes nodeType, InetSocketAddress brokerAddress, InetSocketAddress brokerJoinPointAddress, PubForwardingStrategy brokerForwardingStrategy, IBrokerRemoteQuerier brokerQuerier, Properties arguments);
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueRecover(NodeTypes nodeType, InetSocketAddress brokerAddress, PubForwardingStrategy brokerForwardingStrategy, IBrokerRemoteQuerier brokerQuerier, Properties arguments);
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueKill(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueTCommandMark(InetSocketAddress brokerAddress, String markName, IBrokerRemoteQuerier brokerQuerier);
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueTCommandDisseminate(InetSocketAddress brokerAddress, String markName, IBrokerRemoteQuerier brokerQuerier);
	public SequenceUpdateable<IMessageSequenceUpdateListener> issuePause(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);
	public SequenceUpdateable<IMessageSequenceUpdateListener> issuePlay(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueSpeedCommand(InetSocketAddress brokerAddress, int delay, IBrokerRemoteQuerier brokerQuerier);
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueBye(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueLoadPreparedSubs(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);
	
	public Sequence getComponentState(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);
	public Sequence getOpState(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);
	public Sequence getTopologyLinks(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);
	public Sequence getSubscriptions(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);
	public Sequence getSessions(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);
	public Sequence getPSSessions(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);
	public Sequence getExceptionInfos(InetSocketAddress brokerAddress, IBrokerRemoteQuerier brokerQuerier);

	public CachedBrokerInfo<BrokerInfoComponentStatus> getCachedComponentState(InetSocketAddress brokerAddress);
	public CachedBrokerInfo<BrokerInfoOpState> getCachedOpState(InetSocketAddress brokerAddress);
	public CachedBrokerInfo<JoinInfo> getCachedTopologyLinks(InetSocketAddress brokerAddress);
	public CachedBrokerInfo<SubscriptionInfo> getCachedSubscriptions(InetSocketAddress brokerAddress);
	public CachedBrokerInfo<ISessionInfo> getCachedSessions(InetSocketAddress brokerAddress);
	public CachedBrokerInfo<PSSessionInfo> getCachedPSSessions(InetSocketAddress brokerAddress);
	public CachedBrokerInfo<ExceptionInfo>[] getCachedExceptionInfos(InetSocketAddress brokerAddress);
	
	public SequenceUpdateable<IMessageSequenceUpdateListener> issueBFTMessageManipulate(InetSocketAddress nodeAddress, IBrokerRemoteQuerier brokerQuerier, ExtendedFailureTimelineEvents extendedCommand);
	
}
