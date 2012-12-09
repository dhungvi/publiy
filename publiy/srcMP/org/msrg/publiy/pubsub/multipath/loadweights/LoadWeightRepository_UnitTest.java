package org.msrg.publiy.pubsub.multipath.loadweights;

import java.net.InetSocketAddress;


import org.msrg.publiy.broker.BrokerShadow;

import org.msrg.publiy.communication.core.packet.types.TLoadWeight;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.utils.HistoryType;
import org.msrg.publiy.utils.IHistoryRepository;
import org.msrg.publiy.utils.SystemTime;
import junit.framework.TestCase;

public class LoadWeightRepository_UnitTest extends TestCase {
	
	static final int _profilerCacheSize = 10;
	static final int _bucketRefreshRate = 1000;
	protected final LoadWeightRepository _localLoadWeightRepository;
	protected final LoadWeightRepository _remoteLoadWeightRepository;
	protected final InetSocketAddress _local = new InetSocketAddress("127.0.0.1", 2000);
	protected final InetSocketAddress _remote = new InetSocketAddress("127.0.0.1", 2003);
	protected final ISession _session;
	protected final int _delta = 3;
	
	public LoadWeightRepository_UnitTest() {
		BrokerShadow localBrokerShadow =
				new BrokerShadow(NodeTypes.NODE_BROKER, _local).setDelta(_delta).setMP(true);
		_localLoadWeightRepository =
				new LoadWeightRepository_ForTest(localBrokerShadow);
		BrokerShadow remoteBrokerShadow =
				new BrokerShadow(NodeTypes.NODE_BROKER, _local).setDelta(_delta).setMP(true);
		_remoteLoadWeightRepository =
				new LoadWeightRepository_ForTest(remoteBrokerShadow);
		_session = ISessionManager.createDummyISession(localBrokerShadow, _remote);
	}
	
	@Override
	public void setUp() { }
	
	public void testRepository() {
		SystemTime.resetTime();
		IHistoryRepository historyRepository = _localLoadWeightRepository.getMessageProfilerLogger();
		
		System.out.println("Time(" + SystemTime.currentTimeMillis() + ") " + historyRepository.toString());
		for(int i=0 ; i<14 ; i++) {
			SystemTime.setSystemTime(i*100);
			historyRepository.logMessage(HistoryType.HIST_T_PUB_OUT_BUTLAST, _session.getRemoteAddress(), 10);
			System.out.println("Time(" + SystemTime.currentTimeMillis() + ") " + historyRepository.toString());
		}
		
		SystemTime.setSystemTime(1999 + 1);
		TLoadWeight tLoadWeightFromRemote = new TLoadWeight(_remote, 100, 0.3);
		_localLoadWeightRepository.addFreshOutgoingLoadWeight(tLoadWeightFromRemote);
		int currentOutgoingLoad = _localLoadWeightRepository.getCurrentOutgoingLoad(_remote);
		int cappedOugoingLoad = _localLoadWeightRepository.getCappedOutgoingLoad(_remote);
		System.out.println("CurrentLoad: " + currentOutgoingLoad + ", cappedLoad: " + cappedOugoingLoad);
		System.out.println("Local: " + _localLoadWeightRepository);
//		assertTrue(currentOutgoingLoad==12);
//		assertTrue(cappedOugoingLoad==1);
		
		LoadWeight outLoadWeight = _localLoadWeightRepository.getOutgoingLoadWeigth(_remote);
		System.out.println("Load weight: " + outLoadWeight);
	}
}


class LoadWeightRepository_ForTest extends LoadWeightRepository {
	
	private static IHistoryRepository _historyRepository;
	
	LoadWeightRepository_ForTest(BrokerShadow brokerShadow) {
		super(brokerShadow);
	}
	
	@Override
	protected IHistoryRepository getMessageProfilerLogger() {
		if (_historyRepository == null)
			_historyRepository = new LoadProfiler(LoadWeightRepository_UnitTest._profilerCacheSize, LoadWeightRepository_UnitTest._bucketRefreshRate);
		
		return _historyRepository;
	}
}