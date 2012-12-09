package org.msrg.publiy.pubsub.multipath.loadweights;

import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;

import org.msrg.publiy.utils.ElementTotal;
import org.msrg.publiy.utils.HistoryType;
import org.msrg.publiy.utils.IHistoryRepository;
import org.msrg.publiy.utils.LimittedSizeList;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.MessageProfilerLogger;

import org.msrg.publiy.communication.core.packet.types.TLoadWeight;

public class LoadWeightRepository {
	
	private final Object _lock = new Object();
	private final Map<InetSocketAddress, LoadWeight> _outgoingLoadWeights =
			new HashMap<InetSocketAddress, LoadWeight>();
	private final Map<InetSocketAddress, LoadWeight> _incomingLoadWeights =
			new HashMap<InetSocketAddress, LoadWeight>();
	private final IBrokerShadow _brokerShadow;
	private final InOutBWEnforcer _ioBWEnforcer;
	private final InetSocketAddress _localAddress;
	private final MessageProfilerLogger _messageProfilerLogger;
	
	public LoadWeightRepository(BrokerShadow brokerShadow) {
		_brokerShadow = brokerShadow;
		brokerShadow.setLoadWeightRepository(this);
		_messageProfilerLogger = _brokerShadow.getMessageProfilerLogger();
		
		if (_brokerShadow==null) {
			_ioBWEnforcer = null;
			_localAddress = null;
		}else {
			_ioBWEnforcer = _brokerShadow.getBWEnforcer();
			_localAddress = brokerShadow.getLocalAddress();
		}
	}
	
	public void addFreshOutgoingLoadWeight(TLoadWeight tLoadWeight) {
		addFreshLoadWeight(_outgoingLoadWeights, tLoadWeight);
	}
	
	public void addFreshIncomingLoadWeight(TLoadWeight tLoadWeight) {
		addFreshLoadWeight(_incomingLoadWeights, tLoadWeight);
		InetSocketAddress remote = tLoadWeight.getRemote();
		ElementTotal<LimittedSizeList<ProfilerBucket>> outgoingTotalToRemote = _messageProfilerLogger.getTotalOutputPublication(remote);
		
		if (!Broker.RELEASE)
			LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_DUMMY_SOURCE,
					"addFreshIncomingLoadWeight: " + remote +
					" > " + outgoingTotalToRemote +
					" > " + tLoadWeight._currentLoad +
					" > " + tLoadWeight._normalizedLoadWeight);
	}

	protected IHistoryRepository getMessageProfilerLogger() {
		return _brokerShadow.getMessageProfilerLogger();
	}
	
	public TLoadWeight getTLoadWeight(InetSocketAddress remote) {
		if (_ioBWEnforcer == null || _localAddress == null || _messageProfilerLogger == null)
			return null;
		
		final int totalAvailableInBW = _ioBWEnforcer.getTotalAvailableInBW();
		final int totalUsedBW = _ioBWEnforcer.getTotalUsedInBW();
		
		ElementTotal<LimittedSizeList<ProfilerBucket>> incomingTotalFromRemote = _messageProfilerLogger.getTotalInputPublication(remote);
		double currentAvgIncomingLoad = 
			(incomingTotalFromRemote!=null)?incomingTotalFromRemote.getAverage():-1;
		double normalizedRatio = 1;
		if ( totalUsedBW > totalAvailableInBW ) {
			if (currentAvgIncomingLoad > 0)
				normalizedRatio = (currentAvgIncomingLoad * (totalUsedBW/totalAvailableInBW));
		}

		if (!Broker.RELEASE)
			LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_DUMMY_SOURCE,
					"getTLoadWeight: " + remote +
					" > " + totalUsedBW +
					" > " + totalAvailableInBW +
					" > " + incomingTotalFromRemote +
					" > " + normalizedRatio);
		
		return new TLoadWeight(_localAddress, (int)currentAvgIncomingLoad, normalizedRatio);
	}
	
	protected void addFreshLoadWeight(Map<InetSocketAddress, LoadWeight> loadWeights, TLoadWeight tLoadWeight) {
		IHistoryRepository publicationTrafficHistoryRepository = getMessageProfilerLogger();
		if(publicationTrafficHistoryRepository==null)
			return;

		InetSocketAddress remote = tLoadWeight.getInetSocketAddress();
		ElementTotal<LimittedSizeList<ProfilerBucket>> outPublicationHistory = publicationTrafficHistoryRepository.getHistoryOfType(HistoryType.HIST_T_PUB_OUT_BUTLAST, remote);
		double currentOutgoingLoad = (outPublicationHistory==null)?0:outPublicationHistory.getAverage();
		loadWeights.put(remote, new LoadWeight(tLoadWeight, (int)(0.5+currentOutgoingLoad)));
	}
	
	protected LoadWeight getIncomingLoadWeigth(InetSocketAddress remote) {
		synchronized(_lock) {
			return _incomingLoadWeights.get(remote);
		}
	}

	protected LoadWeight getOutgoingLoadWeigth(InetSocketAddress remote) {
		synchronized(_lock) {
			return _outgoingLoadWeights.get(remote);
		}
	}

	protected void addFreshLoadWeight(Map<InetSocketAddress, LoadWeight> loadWeights, TLoadWeight tLoadWeight, int currentSentLoad) {
		synchronized (_lock) {
			LoadWeight loadWeight = new LoadWeight(tLoadWeight, currentSentLoad);
			loadWeights.put(loadWeight._remote, loadWeight);
		}
	}
	
	public void removeOutgoingLoadWeights(Set<InetSocketAddress> remotes) {
		synchronized (_lock) {
			for (InetSocketAddress remote : remotes)
				_incomingLoadWeights.remove(remote);
		}
	}
	
	public void removeOutgoingLoadWeight(InetSocketAddress remote) {
		synchronized (_lock) {
			_incomingLoadWeights.remove(remote);
		}
	}
	
	public void removeIncomingLoadWeights(Set<InetSocketAddress> remotes) {
		synchronized (_lock) {
			for (InetSocketAddress remote : remotes)
				_incomingLoadWeights.remove(remote);
		}
	}
	
	public void removeIncomingLoadWeight(InetSocketAddress remote) {
		synchronized (_lock) {
			_incomingLoadWeights.remove(remote);
		}
	}
	
	public Set<InetSocketAddress> retreiveOutgoingInvalidLoadWeights() {
		return retreiveLoadWeights(_outgoingLoadWeights, false);
	}
	
	public Set<InetSocketAddress> retreiveIncomingValidLoadWeights() {
		return retreiveLoadWeights(_incomingLoadWeights, true);
	}

	protected Set<InetSocketAddress> retreiveLoadWeights(Map<InetSocketAddress, LoadWeight> loadWeights, boolean validity) {
		Set<InetSocketAddress> remotes = new HashSet<InetSocketAddress>();
		synchronized (_lock) {
			for (Map.Entry<InetSocketAddress, LoadWeight> loadWeightEntry : loadWeights.entrySet())
				if (loadWeightEntry.getValue().isValid() == validity)
					remotes.add(loadWeightEntry.getKey());
		}
		
		return remotes;
	}
	
	public int getCurrentOutgoingLoad(InetSocketAddress remote) {
		LoadWeight lw = getOutgoingLoadWeigth(remote);
		if (lw == null)
			return -1;
		else
			return lw.getCurrentLoad();
	}
	
	public int getCappedOutgoingLoad(InetSocketAddress remote) {
		LoadWeight lw = getOutgoingLoadWeigth(remote);
		if (lw == null)
			return -1;
		else
			return lw.getCappedLoad();
	}
	
	public String toString() {
		StringWriter writer = new StringWriter();
		writer.write("LoadWeighRepo: INCOMGIN" + _incomingLoadWeights + " OUTGOING" + _outgoingLoadWeights);
		return writer.toString();
	}
	}
