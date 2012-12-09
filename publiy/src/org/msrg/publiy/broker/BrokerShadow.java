package org.msrg.publiy.broker;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.msrg.publiy.communication.core.niobinding.CommunicationTransportLogger;

import org.msrg.publiy.networkcodes.engine.CodingEngineLogger;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.FastConfMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.MessageQueueMP;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.covering.SubscriptionManagerWithCovering;
import org.msrg.publiy.pubsub.multipath.loadweights.LoadWeightRepository;

import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.CasualLoggerEngine;
import org.msrg.publiy.utils.log.casuallogger.ConfirmationDataLogger;
import org.msrg.publiy.utils.log.casuallogger.ICpuCasualReader;
import org.msrg.publiy.utils.log.casuallogger.MessageProfilerLogger;
import org.msrg.publiy.utils.log.casuallogger.RTTLogger;
import org.msrg.publiy.utils.log.casuallogger.SessionsRanksLogger;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;
import org.msrg.publiy.utils.log.casuallogger.TrafficLogger;
import org.msrg.publiy.utils.log.casuallogger.TransitionsLogger;
import org.msrg.publiy.utils.log.casuallogger.ViolationsLogger;
import org.msrg.publiy.utils.log.casuallogger.coding.CasualContentLogger;
import org.msrg.publiy.utils.log.casuallogger.coding.CasualPListLogger;
import org.msrg.publiy.utils.log.casuallogger.exectime.ExecutionTimeLogger;
import org.msrg.publiy.utils.log.casuallogger.exectime.ExecutionTimeType;
import org.msrg.publiy.utils.log.casuallogger.exectime.GenericExecutionTimeEntity;
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeEntity;
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeExecutorInternal;


import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.IMaintenanceManager;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerTypes;
import org.msrg.publiy.broker.core.connectionManager.IConnectionManagerMP;
import org.msrg.publiy.broker.core.contentManager.ContentBreakPolicy;
import org.msrg.publiy.broker.core.contentManager.servingPolicy.ContentServingPolicy;
import org.msrg.publiy.broker.core.contentManager.servingPolicy.ContentServingPolicy_ServeHalfReceived;
import org.msrg.publiy.broker.core.maintenance.MaintenanceManager;
import org.msrg.publiy.broker.core.nodes.OverlayNodeId;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class BrokerShadow implements IBrokerShadow, ILoggerSource {

	static final int DEFAULT_ROWS = Broker.ROWS;
	static final int DEFAULT_COLS = Broker.COLS;
	static final int DEFAULT_FLOW_PACKET_SIZE = DEFAULT_ROWS + DEFAULT_COLS + 10;

	protected final LocalSequencer _localSequencer;
	final NodeTypes _nodeType;
	final InetSocketAddress _localAddress;

	protected boolean _canDeliverMessages;
	protected InetSocketAddress _localUDPAddress;
	protected String _basedirname, _outputdirname;
	protected final String _brokerID;
	protected BrokerIdentityManager _idManager;
	protected int _delta = -1;
	protected boolean _isNC, _isBFT, _isMP;
	protected int _rows, _cols, _ncServePolicy;
	protected boolean _isFastConf;
	protected boolean _coveringEnabled;
	protected PubForwardingStrategy _publicationForwardingStrategy;
	
	protected ExecutionTimeLogger _execTimeLogger;
	protected MaintenanceManager _maintenanceManager;
	protected LoadWeightRepository _loadWeightRepository;
	protected InOutBWEnforcer _ioBWEnformer;
	protected CommunicationTransportLogger _communicationTransportLogger;
	protected CasualContentLogger _casualContentLogger;
	protected ExecutionTimeLogger _executionTimeLogger;
	protected TrafficLogger _trafficLogger;
	protected CasualPListLogger _casualPListLogger;
	protected StatisticsLogger _statisticsLogger;
	protected ViolationsLogger _violationsLogger;
	protected SessionsRanksLogger _sessionsRanksLogger;
	protected CasualLoggerEngine _casualLoggerEngine;
	protected ConfirmationDataLogger _confirmationDataLogger;
	protected MessageProfilerLogger _messageProfilerLogger;
	protected TransitionsLogger _transitionsLogger;
	protected RTTLogger _rttLogger;
	protected ICpuCasualReader _cpuCasualReader;
	protected CodingEngineLogger _codingEngineLogger;

	public BrokerShadow(NodeTypes nodeType, InetSocketAddress localAddress) {
//		this(nodeType, delta, localAddress, Broker.getUDPListeningSocket(localAddress), "." + FileUtils.separatorChar , "." + FileUtils.separatorChar, Broker.IDENTITY_FILE);
		_nodeType = nodeType;
		_localAddress = localAddress;
		_localSequencer = LocalSequencer.init(null, _localAddress);
		byte[] ip = _localAddress.getAddress().getAddress();
		int port = _localAddress.getPort();
		_brokerID = (((int)ip[0])&0xFF) + "." + (((int)ip[1])&0xFF) + "." + (((int)ip[2])&0xFF) + "." + (((int)ip[3])&0xFF) + "_" + port;
	}
	
	public BrokerShadow(NodeTypes nodeType, int delta, InetSocketAddress localAddress, InetSocketAddress localUDPAddress, String basedirname, String outputdirname, String identityfilename) {
		this(nodeType, localAddress);
		
		setLocalUDPAddress(localUDPAddress);
		setBasedirname(basedirname);
		setOutputdirname(outputdirname);
		setDelta(delta);
		setIdentityFilename(identityfilename);
	}
	
	public BrokerShadow setBasedirname(String basedirname) {
		if(_basedirname != null)
			throw new IllegalStateException();
		_basedirname = basedirname;
		return this;
	}
	
	public BrokerShadow setOutputdirname(String outputdirname) {
		if(_outputdirname != null)
			throw new IllegalStateException();
		_outputdirname = outputdirname;
		return this;
	}
	
	public BrokerShadow setLocalUDPAddress(InetSocketAddress localUDPAddress) {
		_localUDPAddress = localUDPAddress;
		return this;
	}

	public BrokerShadow setIdentityFilename(String identityfilename) {
		if(_idManager != null)
			throw new IllegalStateException();
		
		if(identityfilename == null) {
			LoggerFactory.getLogger().warn(this, "Identityfilename is null.");
			return this;
		}
			
		_idManager = new BrokerIdentityManager(_localAddress, _delta);
		try {
			if(_idManager.loadIdFile(identityfilename))
				LoggerFactory.getLogger().info(this, "Loading identity file (" + identityfilename + "): " + _idManager);
		} catch (IOException e) {
			LoggerFactory.getLogger().info(this, "Loading identity file (" + identityfilename + ") failed.");
		}
		return this;
	}
	
	@Override
	public String getBrokerID() {
		if(_idManager != null) {
			OverlayNodeId nodeId = _idManager.getBrokerId(_localAddress);
			if(nodeId == null)
				return _brokerID;
			
			return nodeId.getNodeIdentifier();
		}
		
		return _brokerID;
	}
	
	@Override
	public String getAnnotationFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".ann";
	}
	
	@Override
	public String getRecoveryFileName() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".top";
	}
	
	@Override
	public String getPublicationDeliveryLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".deliv";
	}

	@Override
	public String getPublicationGenerationLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".gener";
	}

	@Override
	public String getStatisticsLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".stats";
	}

	@Override
	public String getTrafficLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".traffic";
	}
	
	@Override
	public String getRTTLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".rtt";
	}
	
	@Override
	public String getTransitionsFileName() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".trans";
	}
	
	@Override
	public String getRanksLogFileName() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".ranks";
	}
	
	@Override
	public String getMessageProfilerLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".prof";
	}

	@Override
	public String getConfirmationDataLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".confdt";
	}

	
	@Override
	public String getPSOverlayDumpFileName() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".xtop";
	}

	@Override
	public String getSessionsDumpFileName() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".stop";
	}

	@Override
	public String getBrokerSubDumpFileName() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".sub";
	}

	@Override
	public long getAffinityMask() {
		return 0;
	}

	@Override
	public String getExecTimeLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".exec";
	}

	@Override
	public String getViolationsLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".vio";
	}
	
	@Override
	public String getPListLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".plist";
	}

	@Override
	public String getContentFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".content";
	}
	
	@Override
	public String getBaseDir() {
		return _basedirname;
	}

	@Override
	public String getOutputDir() {
		return _outputdirname;
	}

	@Override
	public boolean canDeliverMessages() {
		return _canDeliverMessages;
	}

	@Override
	public boolean canGenerateMessages() {
		return false;
	}

	@Override
	public String getPreparedSubscriptionFilename(BrokerIdentityManager idManager) {
		OverlayNodeId nodeId = idManager.getBrokerId(_localAddress.toString());
		return getBaseDir() + FileUtils.separatorChar + "subscriptionloading" + FileUtils.separatorChar + nodeId + ".delta" + (getNeighborhoodRadius()-1) + ".prepsubs";
	}

	@Override
	public String getBrokerSpecificConfigFilename() {
		return getBaseDir() + FileUtils.separatorChar + getBrokerID() + ".config";
	}

	@Override
	public String getGenericConfigFilename() {
		return getBaseDir() + FileUtils.separatorChar + "brokers.config"; 
	}

	@Override
	public String getCodedBlocksLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".coded";
	}

	@Override
	public String getCodingEngineLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".coding";
	}

	@Override
	public InetSocketAddress getLocalUDPAddress() {
		return _localUDPAddress;
	}
	
	@Override
	public InetSocketAddress getLocalAddress() {
		return _localAddress;
	}

	@Override
	public ContentServingPolicy getContentServingPolicy() {
//		return new ContentServingPolicy_ServeFullReceived();
		return new ContentServingPolicy_ServeHalfReceived(Broker.CONTENT_SERVING_POLICY);
	}

	@Override
	public int getDefaultFlowPacketSize() {
		return DEFAULT_FLOW_PACKET_SIZE;
	}

	@Override
	public ContentBreakPolicy getContentBreakPolicy() {
		return getDefaultContentBreakPolicy(); //new ContentBreakPolicy((int)(0.9 * DEFAULT_ROWS), 5, true);
	}
	
	public static ContentBreakPolicy getDefaultContentBreakPolicy() {
		return new ContentBreakPolicy((int)(1.0 * DEFAULT_ROWS - 2), 0, true);
//		return new ContentBreakPolicy((int)(1.0 * DEFAULT_ROWS - 10), 5, true);
//		return new ContentBreakPolicy((int)(DEFAULT_ROWS - 3), 1, true);
	}

	@Override
	public BrokerIdentityManager getBrokerIdentityManager() {
		return _idManager;
	}

	@Override
	public int getDelta() {
		if(_delta < 0)
			throw new IllegalStateException("Delta is not initialized: " + _delta);
		return _delta;
	}

	@Override
	public NodeTypes getNodeType() {
		return _nodeType;
	}

	@Override
	public boolean isBroker() {
		return !_nodeType.isClient();
	}

	@Override
	public boolean isClient() {
		return _nodeType.isClient();
	}

	@Override
	public double getUsedBWThreshold() {
		return Broker.USED_BW_THRESHOLD;
	}

	@Override
	public int getNeighborhoodRadius() {
		return getDelta() + 1;
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_BROKER_SHADOW;
	}

	public BrokerShadow setBFT(boolean bft) {
		_isBFT = bft;
		return this;
	}
	
	public BrokerShadow setNC(boolean nc) {
		_isNC = nc;
		return this;
	}

	public BrokerShadow setMP(boolean mp) {
		_isMP = mp;
		return this;
	}

	@Override
	public boolean isMP() {
		return _isMP;
	}

	@Override
	public boolean isNC() {
		return _isNC;
	}

	@Override
	public boolean isBFT() {
		return _isBFT;
	}

	public BrokerShadow setDelta(int delta) {
		_delta = delta;
		return this;
	}

	public BrokerShadow setNCRows(int rows) {
		if(_isNC && rows < 0)
			throw new IllegalArgumentException();
		if(!_isNC && rows > 0)
			throw new IllegalArgumentException();
		
		_rows = rows;
		return this;
	}

	public BrokerShadow setNCCols(int cols) {
		if(_isNC && cols < 0)
			throw new IllegalArgumentException();
		if(!_isNC && cols > 0)
			throw new IllegalArgumentException();
		
		_cols = cols;
		return this;
	}

	public BrokerShadow setNCServePolicy(int ncServePolicy) {
		if(_isNC && ncServePolicy < 0)
			throw new IllegalArgumentException();
		if(!_isNC && ncServePolicy > 0)
			throw new IllegalArgumentException();
		
		_ncServePolicy = ncServePolicy;
		return this;
	}

	@Override
	public int getNCRows() {
		return _rows;
	}

	@Override
	public int getNCCols() {
		return _cols;
	}

	@Override
	public int getNCServePolicy() {
		return _ncServePolicy;
	}

	public BrokerShadow setExecutionTimeLogger(ExecutionTimeLogger execTimeLogger) {
		if(_execTimeLogger != null)
			throw new IllegalStateException();
		_execTimeLogger = execTimeLogger;
		return this;
	}
	
	@Override
	public ExecutionTimeLogger getExecutionTimeLogger() {
		return _execTimeLogger;
	}

	public BrokerShadow setMaintenanceManager(MaintenanceManager maintenanceManager) {
		if(_maintenanceManager != null)
			throw new IllegalStateException();
		_maintenanceManager = maintenanceManager;
		return this;
	}

	@Override
	public IMaintenanceManager getMaintenanceManager() {
		return _maintenanceManager;
	}

	@Override
	public LoadWeightRepository getLoadWeightRepository() {
		return _loadWeightRepository;
	}

	@Override
	public LocalSequencer getLocalSequencer() {
		return _localSequencer;
	}

	public BrokerShadow setCanDeliverMessages(boolean canDeliverMessages) {
		_canDeliverMessages = canDeliverMessages;
		return this;
	}

	public BrokerShadow setLoadWeightRepository(
			LoadWeightRepository loadWeightRepository) {
		if(_loadWeightRepository != null)
			throw new IllegalStateException();
		_loadWeightRepository = loadWeightRepository;
		return this;
	}

	@Override
	public InOutBWEnforcer getBWEnforcer() {
		return _ioBWEnformer;
	}
	
	public BrokerShadow setBWEnforcer(InOutBWEnforcer bwEnforcer) {
		if(_ioBWEnformer != null)
			throw new IllegalStateException();
		_ioBWEnformer = bwEnforcer;
		return this;
	}

	@Override
	public CommunicationTransportLogger getCommunicationTransportLogger() {
		return _communicationTransportLogger;
	}

	public BrokerShadow setCommunicationTransportLogger(CommunicationTransportLogger communicationTransportLogger) {
		if(_communicationTransportLogger != null)
			throw new IllegalStateException();
		_communicationTransportLogger = communicationTransportLogger;
		return this;
	}

	@Override
	public CasualContentLogger getCasualContentLogger() {
		return _casualContentLogger;
	}
	
	public BrokerShadow setCasualContentLogger(CasualContentLogger casualContentLogger) {
		if(_casualContentLogger != null)
			throw new IllegalStateException();
		_casualContentLogger = casualContentLogger;
		return this;
	}

	public BrokerShadow setTrafficLogger(TrafficLogger trafficLogger) {
		if(_trafficLogger != null)
			throw new IllegalStateException();
		_trafficLogger = trafficLogger;
		return this;
	}
	
	@Override
	public TrafficLogger getTrafficLogger() {
		return _trafficLogger;
	}

	public BrokerShadow setCasualPListLogger(CasualPListLogger casualPListLogger) {
		if(_casualPListLogger != null)
			throw new IllegalStateException();
		_casualPListLogger = casualPListLogger;
		return this;
	}
	
	@Override
	public CasualPListLogger getCasualPListLogger() {
		return _casualPListLogger;
	}

	public BrokerShadow setStatisticsLogger(StatisticsLogger statisticsLogger) {
		if(_statisticsLogger != null)
			throw new IllegalStateException();
		_statisticsLogger = statisticsLogger;
		return this;
	}
	
	@Override
	public StatisticsLogger getStatisticsLogger() {
		return _statisticsLogger;
	}

	public BrokerShadow setViolationsLogger(ViolationsLogger violationsLogger) {
		if(_violationsLogger != null)
			throw new IllegalStateException();
		_violationsLogger = violationsLogger;
		return this;
	}
	
	@Override
	public ViolationsLogger getViolationsLogger() {
		return _violationsLogger;
	}

	public BrokerShadow setCodingEngineLogger(CodingEngineLogger codingEngineLogger) {
		if(_codingEngineLogger != null)
			throw new IllegalStateException();
		_codingEngineLogger = codingEngineLogger;
		return this;
	}
	
	@Override
	public CodingEngineLogger getCodingEngineLogger() {
		return _codingEngineLogger;
	}

	public BrokerShadow setCpuCasualReader(ICpuCasualReader cpuCasualReader) {
		if(_cpuCasualReader != null)
			throw new IllegalStateException();
		_cpuCasualReader = cpuCasualReader;
		return this;
	}
	
	@Override
	public ICpuCasualReader getCpuCasualReader() {
		return _cpuCasualReader;
	}

	public BrokerShadow setRTTLogger(RTTLogger rttLogger) {
		if(_rttLogger != null)
			throw new IllegalStateException();
		_rttLogger = rttLogger;
		return this;
	}
	
	@Override
	public RTTLogger getRTTLogger() {
		return _rttLogger;
	}

	public BrokerShadow setTransitionsLogger(TransitionsLogger transitionsLogger) {
		if(_transitionsLogger != null)
			throw new IllegalStateException();
		_transitionsLogger = transitionsLogger;
		return this;
	}
	
	@Override
	public TransitionsLogger getTransitionsLogger() {
		return _transitionsLogger;
	}

	public BrokerShadow setSessionsRanksLogger(SessionsRanksLogger sessionsRanksLogger) {
		if(_sessionsRanksLogger != null)
			throw new IllegalStateException();
		_sessionsRanksLogger = sessionsRanksLogger;
		return this;
	}
	
	@Override
	public SessionsRanksLogger getSessionsRanksLogger() {
		return _sessionsRanksLogger;
	}

	public BrokerShadow setCasualLoggerEngine(CasualLoggerEngine casualLoggerEngine) {
		if(_casualLoggerEngine != null)
			throw new IllegalStateException();
		_casualLoggerEngine = casualLoggerEngine;
		return this;
	}
	
	@Override
	public CasualLoggerEngine getCasualLoggerEngine() {
		return _casualLoggerEngine;
	}

	public BrokerShadow setConfirmationDataLogger(ConfirmationDataLogger confirmationDataLogger) {
		if(_confirmationDataLogger != null)
			throw new IllegalStateException();
		_confirmationDataLogger = confirmationDataLogger;
		return this;
	}
	
	@Override
	public ConfirmationDataLogger getConfirmationDataLogger() {
		return _confirmationDataLogger;
	}

	public BrokerShadow setMessageProfilerLogger(MessageProfilerLogger messageProfilerLogger) {
		if(_messageProfilerLogger != null)
			throw new IllegalStateException();
		_messageProfilerLogger = messageProfilerLogger;
		return this;
	}
	
	@Override
	public MessageProfilerLogger getMessageProfilerLogger() {
		return _messageProfilerLogger;
	}

	@Override
	public CasualPListLogger getPlistLogger() {
		return _casualPListLogger;
	}

	@Override
	public CasualContentLogger getContentLogger() {
		return _casualContentLogger;
	}

	@Override
	public IExecutionTimeEntity getExecutionTypeEntity(ExecutionTimeType type) {
		if (_execTimeLogger==null)
			return null;
		
		IExecutionTimeExecutorInternal execTimeExecutor = _execTimeLogger.getExecutionTimeExecutors(type);
		return new GenericExecutionTimeEntity(execTimeExecutor, type);
	}

	public BrokerShadow setFastConf(boolean fastconf) {
		_isFastConf = fastconf;
		return this;
	}
	
	@Override
	public boolean isFastConf() {
		return _isFastConf;
	}

	@Override
	public boolean isNormal() {
		return !isBFT() && !isMP() && !isNC();
	}

	public BrokerShadow setPublicationForwardingStrategy(PubForwardingStrategy publicationForwardingStrategy) {
		if(_publicationForwardingStrategy != null)
			throw new IllegalStateException();
		_publicationForwardingStrategy = publicationForwardingStrategy;
		return this;
	}
	
	@Override
	public PubForwardingStrategy getPublicationForwardingStrategy() {
		return _publicationForwardingStrategy;
	}

	public BrokerShadow setCovering(boolean covering) {
		if(covering)
			_coveringEnabled = covering;
		
		if(_coveringEnabled)
			throw new IllegalStateException();
		
		return this;
	}

	@Override
	public boolean coveringEnabled() {
		return _coveringEnabled;
	}
	
	@Override
	public IOverlayManager createOverlayManager() {
		return new OverlayManager(this);
	}

	@Override
	public ISubscriptionManager createSubscriptionManager(IOverlayManager overlayManager, String dumpFileName, boolean shouldLog) {
		if(coveringEnabled())
			return new SubscriptionManagerWithCovering(this, getBrokerSubDumpFileName(), true);
		else
			return new SubscriptionManager(this, getBrokerSubDumpFileName(), true);
	}

	@Override
	public IMessageQueue createMessageQueue(
			ConnectionManagerTypes conManType,
			IConnectionManager connectionManager,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager) {
		if(isNormal() || conManType == ConnectionManagerTypes.CONNECTION_MANAGER_JOIN || conManType == ConnectionManagerTypes.CONNECTION_MANAGER_RECOVERY)
			return new MessageQueue(this, connectionManager, overlayManager, subscriptionManager, true);
		
		if(isMP())
			return new MessageQueueMP(this, (IConnectionManagerMP) connectionManager, overlayManager, subscriptionManager, true);
		
		if(isFastConf())
			return new FastConfMessageQueue(connectionManager, this, true);

		throw new UnsupportedOperationException();
	}
}
