package org.msrg.publiy.broker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.SimpleBFTSuspectedRepo;



import org.msrg.publiy.networkcodes.engine.CodingEngineLogger;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.multipath.loadweights.LoadWeightRepository;


import org.msrg.publiy.sutils.SystemPackageVersion;
import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.log.ExceptionLogger;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggerTypes;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.BFTDSALogger;
import org.msrg.publiy.utils.log.casuallogger.BFTDackLogger;
import org.msrg.publiy.utils.log.casuallogger.BFTInvalidMessageLogger;
import org.msrg.publiy.utils.log.casuallogger.BFTStatisticsLogger;
import org.msrg.publiy.utils.log.casuallogger.BFTSuspicionLogger;
import org.msrg.publiy.utils.log.casuallogger.CasualLoggerEngine;
import org.msrg.publiy.utils.log.casuallogger.ConfirmationDataLogger;
import org.msrg.publiy.utils.log.casuallogger.FakeCpuCasualReader;
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
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeEntity;

import org.msrg.publiy.client.subscriber.DefaultSubscriptionListener;
import org.msrg.publiy.client.subscriber.ISubscriptionListener;
import org.msrg.publiy.client.subscriber.casuallogger.PublicationDeliveryLogger;
import org.msrg.publiy.communication.core.niobinding.CommunicationTransportLogger;
import org.msrg.publiy.communication.core.packet.types.TBFTMessageManipulator;
import org.msrg.publiy.communication.core.packet.types.TCommand;
import org.msrg.publiy.communication.core.packet.types.TCommandTypes;
import org.msrg.publiy.component.ComponentStatus;
import org.msrg.publiy.component.IComponent;
import org.msrg.publiy.component.IComponentListener;

import org.msrg.publiy.broker.core.BFTConnectionManagerJoin;
import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.IConnectionManagerJoin;
import org.msrg.publiy.broker.core.IConnectionManagerRecovery;
import org.msrg.publiy.broker.core.IMaintenanceManager;
import org.msrg.publiy.broker.core.bwenforcer.BWMetric;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerFactory;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerTypes;
import org.msrg.publiy.broker.core.contentManager.ContentBreakPolicy;
import org.msrg.publiy.broker.core.contentManager.servingPolicy.ContentServingPolicy;
import org.msrg.publiy.broker.core.maintenance.MaintenanceManager;
import org.msrg.publiy.broker.core.plistManager.PListManager;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.core.sequence.SimpleBFTIssuerProxyRepo;
import org.msrg.publiy.broker.info.ExceptionInfo;
import org.msrg.publiy.broker.info.ISessionInfo;
import org.msrg.publiy.broker.info.JoinInfo;
import org.msrg.publiy.broker.info.PSSessionInfo;
import org.msrg.publiy.broker.info.PublicationInfo;
import org.msrg.publiy.broker.info.StatisticalInfo;
import org.msrg.publiy.broker.info.SubscriptionInfo;
import org.msrg.publiy.broker.networkcoding.connectionManager.client.ConnectionManagerNC_Client;

public class Broker implements IBroker, ISubscriptionListener, IComponentListener, ILoggerSource {
	protected final Properties _arguments;
	private boolean _transitionIntallationInProgress = false;
	
	protected static CasualLoggerEngine _casualLoggerEngine;
	protected static ICpuCasualReader _cpuCasualReader;
	protected static TrafficLogger _trafficLogger;
	protected static StatisticsLogger _statisticsLogger;
	protected static ExecutionTimeLogger _execTimeLogger;
	protected static RTTLogger _rttLogger;
	protected static TransitionsLogger _transitionsLogger;
	protected static SessionsRanksLogger _sessionsRanksLogger;
	protected static MessageProfilerLogger _messageProfilerLogger;
	protected static ViolationsLogger _violationsLogger;
	protected static ConfirmationDataLogger _confimationLogger;
	protected static CommunicationTransportLogger _communicationTransportLogger;
	protected static PublicationDeliveryLogger _pubDeliveryLogger;
	protected static CasualContentLogger _contentLogger;
	protected static CodingEngineLogger _codingEngineLogger;
	protected static CasualPListLogger _plistEngineLogger;
	protected final IMaintenanceManager _maintenanceManager;
	protected final LoadWeightRepository _loadWeightRepo;
	protected BFTDackLogger _bftDackLogger;
	protected BFTSuspicionLogger _bftSuspicionLogger;
	protected BFTInvalidMessageLogger _bftInvalidMessageLogger;
	protected BFTDSALogger _bftDSALogger;
	protected final LocalSequencer _localSequencer;
	protected final IBrokerShadow _brokerShadow;
	protected final DefaultSubscriptionListener _defaultSubscriptionListener;
	
	public static final int FAST_CONNECTION_RTT = 350;
	public static final int TPING_SEND_INTERVAL = 5000;
	public static final int TPING_TIMEOUT_INTERVAL = 5000;
	public static final int CANDIDATES_EVENT_TIMER_INTERVAL = 30000;
	public static final int SEND_PLIST_REQ_SHORT_TIMER_INTERVAL = 10000;
	public static final int SEND_PLIST_REQ_LONG_TIMER_INTERVAL = 30000;
	public static final int SEND_METADATA_REQ_TIMER_INTERVAL = 10000;
	
	public static PubForwardingStrategy PUB_FORWARDING_STRATEGY = PubForwardingStrategy.PUB_FORWARDING_STRATEGY_UNKNOWN;
	public static double USED_BW_THRESHOLD = 0.8;
	
	public static final String BASE_DIR = System.getProperty("Broker.BASEDIR", "." + FileUtils.separatorChar);
	public static final String OUTPUT_DIR = System.getProperty("Broker.OUTPUTDIR", BASE_DIR);
	
	public static final String IDENTITY_FILE =
		System.getProperty("Broker.ID_FILE", BASE_DIR + FileUtils.separatorChar + "identityfile");
	
	public static final String S_JAR_VERSION = SystemPackageVersion.getVersion();
	
	public static final boolean LOAD_PREPARED_SUBS;
	static{
		String loadPreparedSubsStr = System.getProperty("Broker.LOAD_PREPARED_SUBS", "true");
		LOAD_PREPARED_SUBS = new Boolean(loadPreparedSubsStr).booleanValue();
	}

	public static final boolean COVERING;
	static{
		String coveringStr = System.getProperty("Broker.COVERING", "false");
		COVERING = new Boolean(coveringStr).booleanValue();
	}
	
	public static final boolean RELEASE;
	static{
		String releaseStr = System.getProperty("Broker.RELEASE", "true");
		RELEASE = new Boolean(releaseStr).booleanValue();
	}
	
	public static final boolean DEBUG;
	static{
		String debugStr = System.getProperty("Broker.DEBUG", "false");
		DEBUG = new Boolean(debugStr).booleanValue();
	}
	
	public static final boolean CORRELATE;
	static{
		String correlateStr = System.getProperty("Broker.CORRELATE", "" + (!RELEASE || DEBUG));
		CORRELATE = new Boolean(correlateStr).booleanValue();
	}
	
	public static final boolean BFT;
	static{
		String bftStr = System.getProperty("Broker.BFT", "false");
		BFT = new Boolean(bftStr).booleanValue();
	}

	public static final boolean MP;
	static{
		String mpStr = System.getProperty("Broker.MP", "false");
		MP = new Boolean(mpStr).booleanValue();
	}

	public static final boolean CODING;
	static{
		String codingStr = System.getProperty("Broker.CODING", "false");
		CODING = new Boolean(codingStr).booleanValue();
	}
	
	public static final int PACKET_LOSS_PERCENTAGE;
	static{
		String packetLossStr = System.getProperty("BROKER.PACKETLOSS", "0");
		PACKET_LOSS_PERCENTAGE = new Integer(packetLossStr).intValue();
	}
	
	public static final int ROWS;
	static{
		String rowsStr = System.getProperty("NC.ROWS", "100");
		ROWS = new Integer(rowsStr).intValue();
	}
	
	public static final int COLS;
	static{
		String colsStr = System.getProperty("NC.COLS", "10000");
		COLS = new Integer(colsStr).intValue();
	}
	
	public static final int CONTENT_SERVING_POLICY;
	static {
		String servePolicyStr = System.getProperty("NC.SERVE", "" + ROWS);
		CONTENT_SERVING_POLICY = new Integer(servePolicyStr).intValue();
	}
	
	public static final int PLIST_REQUEST_MIN_INTERVAL = 10000;
	
	public static final boolean LOG_VIOLATIONS;
	static{
		String logViolationsStr = System.getProperty("Broker.LOG_VIOLATIONS", "false");
		LOG_VIOLATIONS = new Boolean(logViolationsStr).booleanValue();
	}

	public static final int FANOUT;
	static{
		String maxFanoutStr = System.getProperty("Broker.FANOUT", (MP?"12":"-1"));
		FANOUT = new Integer(maxFanoutStr).intValue();
	}
	
	public static final int CANDIDATES;
	static{
		String maxCandidatesStr = System.getProperty("Broker.CANDIDATES", (MP?"4":"-1"));
		CANDIDATES = new Integer(maxCandidatesStr).intValue();
	}
	
	public static final long CPU_AFFINITY_MASK;
	static{
		String cpuAffinityMaskStr = System.getProperty("Broker.AFFINITYMASK", "-1");
		CPU_AFFINITY_MASK = new Long(cpuAffinityMaskStr).longValue();
	}
	
	public static final int DACK_SEND_INTERVAL;
	static{
		String dackIntervalStr = System.getProperty("Broker.DACKINTERVAL", "10000");
		DACK_SEND_INTERVAL = new Integer(dackIntervalStr).intValue();
	}

	public static final int LOAD_WEIGHTS_SEND_INTERVAL;
	static{
		String laodWeightIntervalStr = System.getProperty("Broker.LOADWEIGHTSSENDINTERVAL", "10000");
		LOAD_WEIGHTS_SEND_INTERVAL = new Integer(laodWeightIntervalStr).intValue();
	}

	public static final int PURGE_MQ_INTERVAL;
	static{
		String dackIntervalStr = System.getProperty("Broker.PURGEMQINTERVAL", "5000");
		PURGE_MQ_INTERVAL = new Integer(dackIntervalStr).intValue();
	}

	public static final boolean FASTCONF;
	static{
		String fastConfStr = System.getProperty("Broker.FASTCONF", "true");
		FASTCONF = new Boolean(fastConfStr).booleanValue();
	}
	
	public static final boolean ANNOTATE;
	static{
		String annotateStr = System.getProperty("Broker.ANNOTATE", "false");
		ANNOTATE = new Boolean(annotateStr).booleanValue();
	}
	
	public static final boolean LOG_CONFIRMATION_DATA;
	static{
		String confLogStr = System.getProperty("Broker.LOGCONFDATA", "false");
		LOG_CONFIRMATION_DATA = new Boolean(confLogStr).booleanValue();
	}
	
	public static final boolean LOG_EXECTIME;
	static{
		String execTimeLogStr = System.getProperty("Broker.LOGEXECTIME", "false");
		LOG_EXECTIME = new Boolean(execTimeLogStr).booleanValue();
	}
	
	public static final boolean LOG_STATISTICS;
	static{
		String subscriptionLogStr = System.getProperty("Broker.LOGSUBSCRIPTIONS", "true");
		LOG_STATISTICS = new Boolean(subscriptionLogStr).booleanValue();
	}
	
	public static final boolean LOG_TRAFFIC;
	static{
		String trafficLogStr = System.getProperty("Broker.LOGTRAFFIC", "true");
		LOG_TRAFFIC = new Boolean(trafficLogStr).booleanValue();
	}
	
	public static int DELTA;
	static {
		String deltaStr = System.getProperty("Broker.DELTA", "3");
		DELTA = new Integer(deltaStr).intValue();
	}
	
	public static final int FORCED_CONF_ACK_DELAY;
	static{
		String forcedConfAckStr = System.getProperty("Broker.ForcedConfAckDelay", "30000");
		FORCED_CONF_ACK_DELAY = new Integer(forcedConfAckStr).intValue();
	}
	
	public static final int MAX_SEREVE_SEQUENCE;
	static {
		String maxServeSequenceStr = System.getProperty("Broker.MAX_SEREVE_SEQUENCE", "" + COLS * 3);
		MAX_SEREVE_SEQUENCE = new Integer(maxServeSequenceStr);
	}
	
	public static final int MAX_ACTIVE_FLOWS;
	static {
		String maxActiveFlowsStr = System.getProperty("Broker.MAX_ACTIVE_FLOWS", "20");
		MAX_ACTIVE_FLOWS = new Integer(maxActiveFlowsStr);
	}
	
	public static final BWMetric BW_METRIC;
	static{
		String bwMetricStr = System.getProperty("Broker.BW_METRIC", BWMetric.BW_METRIC_INOUT_BYTES.toString());
		BW_METRIC = BWMetric.getBWMetricFromString(bwMetricStr);
	}

	public static final int BW_IN;
	static{
		String bwInStr = System.getProperty("Broker.BW_IN", "100000");
		BW_IN = new Integer(bwInStr);
	}

	public static final int BW_OUT;
	static{
		String bwOutStr = System.getProperty("Broker.BW_OUT", "100000");
		BW_OUT = new Integer(bwOutStr);
	}

	public static final int VSEQ_LENGTH = Broker.DELTA + 1;
	
	protected void logDeploySummaries() {
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_BROKER,
				"Params: S_JAR_VERSION=" + S_JAR_VERSION,
				"\nParams: COVERING=" + COVERING,
				"\nParams: CODING=" + _brokerShadow.isNC(),
				"\nParams: PACKET_LOSS_PERCENTAGE=" + PACKET_LOSS_PERCENTAGE,
				"\nParams: ROWS=" + _brokerShadow.getNCRows(),
				"\nParams: COLS=" + _brokerShadow.getNCCols(),
				"\nParams: SERVE=" + _brokerShadow.getNCServePolicy(),
				"\nParams: LOAD_PREPARED_SUBS=" + LOAD_PREPARED_SUBS,
				"\nParams: RELEASE=" + RELEASE,
				"\nParams: STRATEGY=" + _brokerShadow.getPublicationForwardingStrategy(),
				"\nParams: USED_BW_THRESHOLD=" + USED_BW_THRESHOLD,
				"\nParams: DEBUG=" + DEBUG,
				"\nParams: LOG_TRAFFIC=" + (_brokerShadow.getTrafficLogger() != null),
				"\nParams: LOG_EXECTIME=" + (_brokerShadow.getExecutionTimeLogger() != null),
				"\nParams: LOG_CONFIRMATION_DATA=" + (_brokerShadow.getConfirmationDataLogger() != null),
				"\nParams: ANNOTATE=" + ANNOTATE,
				"\nParams: FASTCONF=" + _brokerShadow.isFastConf(),
				"\nParams: PURGE_MQ_INTERVAL=" + PURGE_MQ_INTERVAL,
				"\nParams: DACK_SEND_INTERVAL=" + DACK_SEND_INTERVAL,
				"\nParams: CPU_AFFINITY_MASK=" + CPU_AFFINITY_MASK,
				"\nParams: FORCED_CONF_ACK_DELAY=" + FORCED_CONF_ACK_DELAY,
				"\nParams: PUB_WORKLOAD_SIZE=" + TMulticast_Publish.PUBLICATION_DEFAULT_WORKLOAD_SIZE,
				"\nParams: MP=" + _brokerShadow.isMP(),
				"\nParams: BFT=" + _brokerShadow.isBFT(),
				"\nParams: DELTA=" + _brokerShadow.getDelta(),
				"\nParams: VSEQ_LENGTH=" + _brokerShadow.getNeighborhoodRadius(),
				"\nParams: CORRELATE=" + CORRELATE,
				"\nParams: FANOUT=" + FANOUT,
				"\nParams: CANDIDATES=" + CANDIDATES,
				"\nParams: BW_METRIC=" + BW_METRIC,
				"\nParams: BW_IN=" + BW_IN,
				"\nParams: BW_OUT=" + BW_OUT,
				"\nParams: BASE_DIR=" + _brokerShadow.getBaseDir(),
				"\nParams: OUTPUT_DIR=" + _brokerShadow.getOutputDir());
		
		String condense_config_description =
			"SJAR{" + S_JAR_VERSION + "}-" +
			"d" + _brokerShadow.getDelta() + "-" +
			"s" + PUB_FORWARDING_STRATEGY + "-" +
			"t" + USED_BW_THRESHOLD + "-" +
			(_brokerShadow.isFastConf()?"FAST-":"") +
			("W" + TMulticast_Publish.PUBLICATION_DEFAULT_WORKLOAD_SIZE + "-") +
			(_brokerShadow.isMP()?"MP-FOUT"+FANOUT+"-CAND"+CANDIDATES+"-":"NMP-") + 
			(_brokerShadow.isBFT()? "BFT-" : "") +
			(BW_METRIC + "{" + BW_IN + "-" + BW_OUT + "}") +
			(_brokerShadow.isNC()? "-CODING-F"+MAX_ACTIVE_FLOWS+"-R"+_brokerShadow.getNCRows()+"-C"+_brokerShadow.getNCCols()+"-S"+_brokerShadow.getNCServePolicy()+"-L"+PACKET_LOSS_PERCENTAGE : "");
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_BROKER, "Params: CONDENSE_DESCRIPTION: " + condense_config_description);
		
		if(_brokerShadow.isNC()) {
			LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_BROKER, 
					"\nParams: TIMER_DELAY_PROCESS_INCOMPLETE_BREAKED_WATCH_LIST" + ConnectionManagerNC_Client.TIMER_DELAY_PROCESS_INCOMPLETE_BREAKED_WATCH_LIST,
					"\nParams: TIMER_DELAY_PROCESS_INCOMPLETE_UNBREAKED_WATCH_LIST" + ConnectionManagerNC_Client.TIMER_DELAY_PROCESS_INCOMPLETE_UNBREAKED_WATCH_LIST,
					"\nParams: TIMER_DELAY_PROCESS_NEXT_FLOW" + ConnectionManagerNC_Client.TIMER_DELAY_PROCESS_NEXT_FLOW,
					"\nParams: TIMER_DELAY_PROCESS_SERVE_QUEUE" + ConnectionManagerNC_Client.TIMER_DELAY_PROCESS_SERVE_QUEUE,
					"\nParams: SOURCE_PLISTS_REQUESTS" + ConnectionManagerNC_Client.SOURCE_PLISTS_REQUESTS,
					"\nParams: NONSOURCE_PLISTS_REQUESTS" + ConnectionManagerNC_Client.NONSOURCE_PLISTS_REQUESTS,
					"\nParams: MAX_SERVE_TIME" + ConnectionManagerNC_Client.MAX_SERVE_TIME,
					"\nParams: MIN_PIECE_SERVE_COUNT" + ConnectionManagerNC_Client.MIN_PIECE_SERVE_COUNT,
					//
					"\nParams: MAX_PLIST_CROSS_SIZE_BROKER_BROKER_REPLY" + PListManager.MAX_PLIST_CROSS_SIZE_BROKER_BROKER_REPLY,
					"\nParams: MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY" + PListManager.MAX_PLIST_CROSS_SIZE_BROKER_CLIENT_REPLY,
					"\nParams: MAX_PLIST_LOCAL_SIZE_SRC" + PListManager.MAX_PLIST_LOCAL_SIZE_SRC,
					"\nParams: MAX_PLIST_LOCAL_SIZE_NONLAUNCH" + PListManager.MAX_PLIST_LOCAL_SIZE_NONLAUNCH);
		}
	}
	
	public static final Class<?>[] GLOBAL_BROKER_CONSTRUCTOR_ARGUMENT_TYPES = new Class[]{};
	
	protected Object _componentStatusLock = new Object();
	protected ComponentStatus _componentStatus = ComponentStatus.COMPONENT_STATUS_UNINITIALIZED;
	protected Set<IComponentListener> _componentListeners = new HashSet<IComponentListener>();
		
	public Set<IBrokerOpStateListener> _opStateListeners = new HashSet<IBrokerOpStateListener>();
	
	public static final int AGGREGATED_CONFIRMATION_ACK_COUNT = 200;
	public static final int FORCED_ACKNOWLEDGEMENT_DELAY = 10000;
	public static final String BROKER_RECOVERY_TOPOLOGY_FILE = "TOPOLOGY";
	
	public static final InetSocketAddress[] 
	            b1Addresses = new InetSocketAddress[50], 
	            b2Addresses = new InetSocketAddress[50];
	static{
		for ( int i=0 ; i<b1Addresses.length ; i++)
			b1Addresses[i] = new InetSocketAddress("127.0.0.1", 1000 + i);
		for ( int i=0 ; i<b2Addresses.length ; i++)
			b2Addresses[i] = new InetSocketAddress("127.0.0.1", 2000 + i);
	}
	
	public static final InetSocketAddress bAddress0 = new InetSocketAddress("127.0.0.1", 1000);
	public static final InetSocketAddress bAddress1 = new InetSocketAddress("127.0.0.1", 1001);
	public static final InetSocketAddress bAddress2 = new InetSocketAddress("127.0.0.1", 1002);
	public static final InetSocketAddress bAddress3 = new InetSocketAddress("127.0.0.1", 1003);
	public static final InetSocketAddress bAddress4 = new InetSocketAddress("127.0.0.1", 1004);
	public static final InetSocketAddress bAddress5 = new InetSocketAddress("127.0.0.1", 1005);
	public static final InetSocketAddress bAddress6 = new InetSocketAddress("127.0.0.1", 1006);
	public static final InetSocketAddress bAddress7 = new InetSocketAddress("127.0.0.1", 1007);
	public static final InetSocketAddress bAddress8 = new InetSocketAddress("127.0.0.1", 1008);
	public static final InetSocketAddress bAddress9 = new InetSocketAddress("127.0.0.1", 1009);
	public static final InetSocketAddress bAddress10 = new InetSocketAddress("127.0.0.1", 1010);
	public static final InetSocketAddress bAddress11 = new InetSocketAddress("127.0.0.1", 1011);
	public static final InetSocketAddress bAddress12 = new InetSocketAddress("127.0.0.1", 1012);

	protected final InetSocketAddress _localAddress, _localUDPAddress;
	
	protected IConnectionManager _connectionManager;
	protected BrokerOpState _brokerOpState;

	protected InetSocketAddress _joinPointAddress;
	
	////////////////////////////////////////////////////////////////////////
	//							ICOMPONENTLISTENER						  //
	////////////////////////////////////////////////////////////////////////
	
	@Override
	public void componentStateChanged(IComponent component) {

		LoggerFactory.getLogger().info(this, component.getComponentName() + " changed state to '" + component.getComponentState() + "' by " + Thread.currentThread());

		if ( component == _connectionManager )
		{
			ComponentStatus connectionManagerState = component.getComponentState();
			switch( connectionManagerState )
			{
			case COMPONENT_STATUS_RUNNING:
				{
					if ( getBrokerOpState() == BrokerOpState.BRKR_PUBSUB_JOIN )
					{	// initiate the join by trying to send a join message.
						((IConnectionManagerJoin)_connectionManager).join(_joinPointAddress);
					}
					return;
				}
			
			case COMPONENT_STATUS_PAUSING:
			{
				return;
			}
			
			case COMPONENT_STATUS_PAUSED:
			{
//				connectionManagerIsPaused();
				LoggerFactory.getLogger().info(this, "ConnectionManager is now paused." );
				TransitionManager transitionManager = new TransitionManager(true);
				Thread transitionThread = new Thread(transitionManager, "TransitionThread-" + getBrokerID());
				transitionThread.start();
				return;
			}
			
			case COMPONENT_STATUS_STOPPED:
			{
//				connectionManagerIsStopped();
				LoggerFactory.getLogger().info(this, "ConnectionManager is now stopped." );
				TransitionManager transitionManager = new TransitionManager(false);
				Thread transitionThread = new Thread(transitionManager, "TransitionThread-" + getBrokerID());
				transitionThread.start();
				return;
			}
			
			default:
				return;
			}
		}
	}

	class TransitionManager implements Runnable, ILoggerSource {

		private final boolean _replaceOrStop;
		
		TransitionManager(boolean replaceOrStop) {
			LoggerFactory.getLogger().info(this, "Creating new transition manager (" + (replaceOrStop?"REPLACE":"STOP") + ")");
			_replaceOrStop = replaceOrStop;	
		}
		
		@Override
		public void run() {
			LoggerFactory.getLogger().info(this, "Starting to do transition (" + (_replaceOrStop?"REPLACE":"STOP") + ")");
			
			if ( _replaceOrStop )
				connectionManagerIsPaused();
			else
				connectionManagerIsStopped();
		}
		
		private void connectionManagerIsPaused() {
			synchronized (_componentStatusLock)
			{
				if ( _transitionIntallationInProgress )
//					return;
					throw new IllegalStateException("" + Thread.currentThread());
				else
					_transitionIntallationInProgress = true;

				setComponentStatus(ComponentStatus.COMPONENT_STATUS_PAUSED);
				_brokerOpState = BrokerOpState.BRKR_PUBSUB_PS;
				installConnectionManagerPSImmediately();

				_connectionManager.awakeFromPause();
				_connectionManager.startComponent();
				
				setBrokerOpState(BrokerOpState.BRKR_PUBSUB_PS);
				setComponentStatus(ComponentStatus.COMPONENT_STATUS_RUNNING);

				_transitionIntallationInProgress = false;
			}
		}
		
		private void connectionManagerIsStopped() {
			synchronized (_componentStatusLock)
			{
				if ( _transitionIntallationInProgress )
					return;
				else
					_transitionIntallationInProgress = true;

				if ( _componentStatus == ComponentStatus.COMPONENT_STATUS_STOPPING ) {
					setComponentStatus(ComponentStatus.COMPONENT_STATUS_STOPPED);
				}
				else
					setComponentStatus(ComponentStatus.COMPONENT_STATUS_PROBLEM);
				
			}
		}

		@Override
		public LoggingSource getLogSource() {
			return LoggingSource.LOG_SRC_TRANSITION_MAN;
		}
	}
	
	public Broker(InetSocketAddress address, BrokerOpState brokerOpState, InetSocketAddress joinPointAddress, PubForwardingStrategy forwardingStrategy, Properties arguments) throws IOException{
		_localAddress = address;
		_localUDPAddress = (CODING ? getUDPListeningSocket(_localAddress) : null);
		
		_brokerShadow = createNewBrokerShadow(arguments);
		initWithBrokerShadow((BrokerShadow)_brokerShadow).setPublicationForwardingStrategy(forwardingStrategy);
		_localSequencer = _brokerShadow.getLocalSequencer();
		LoggerFactory.create(_brokerShadow.getLocalSequencer(),
				LoggerTypes.LOGGER_CONSOLE, 
				LoggingSource.values(),
				Broker.DEBUG ? LoggingSource.values() : null,
				LoggingSource.values(),
				LoggingSource.values(), 
				LoggingSource.values());

		LoggerFactory.getLogger().info(
				this,
				"Creating a: " + getNodeType() +
				"(" + address + ", " + brokerOpState + ", " + joinPointAddress + ")");
		setComponentStatus(ComponentStatus.COMPONENT_STATUS_INITIALIZING);
		
		setBrokerOpState(brokerOpState);
		_joinPointAddress = joinPointAddress;
		_loadWeightRepo=((_brokerShadow.isMP() && _brokerShadow.getPublicationForwardingStrategy()._isLoadAware)?new LoadWeightRepository((BrokerShadow)_brokerShadow):null);
		_maintenanceManager = new MaintenanceManager(this, _brokerShadow);
		
		switch(_brokerOpState) {
		case BRKR_RECOVERY:
			_connectionManager = ConnectionManagerFactory.getConnectionManager(ConnectionManagerTypes.CONNECTION_MANAGER_RECOVERY, this, _brokerShadow, this);
			break;
			
		case BRKR_PUBSUB_PS:
			_connectionManager = ConnectionManagerFactory.getConnectionManager(ConnectionManagerTypes.CONNECTION_MANAGER_PUBSUB, this, _brokerShadow, this);
			break;
			
		case BRKR_PUBSUB_JOIN:
			_connectionManager = ConnectionManagerFactory.getConnectionManager(ConnectionManagerTypes.CONNECTION_MANAGER_JOIN, this, _brokerShadow, this);
			break;
			
		default:
			throw new UnsupportedOperationException("Donno how to handle this broker state: " + _brokerOpState);
		}
		setComponentStatus(ComponentStatus.COMPONENT_STATUS_INITIALIZED);
		
		_pubDeliveryLogger = PublicationDeliveryLogger.getPublicationDeliveryLoggerInstance(this);
		_defaultSubscriptionListener = DefaultSubscriptionListener.createInstance(_brokerShadow);
		_arguments = arguments;
		
		if(_brokerShadow.isBFT()) {
			if(((IBFTBrokerShadow)_brokerShadow).getKeyManager() == null)
				throw new IllegalArgumentException("Key manager is not initialized: " + _arguments);
		}
	}
	
	protected IBrokerShadow createNewBrokerShadow(Properties arguments) {
		if(BFT)
			return new BFTBrokerShadow(
					getNodeType(), DELTA, _localAddress, Broker.BASE_DIR, Broker.OUTPUT_DIR, IDENTITY_FILE, arguments);
		else
			return new BrokerShadow(
					getNodeType(), DELTA, _localAddress, _localUDPAddress, Broker.BASE_DIR, Broker.OUTPUT_DIR, IDENTITY_FILE);
	}

	public BrokerShadow initWithBrokerShadow(BrokerShadow brokerShadow) {
		brokerShadow.setBWEnforcer(new InOutBWEnforcer(BW_METRIC, BW_IN, BW_OUT));
		
		brokerShadow.setNC(CODING);
		if(CODING)
			brokerShadow.setNCRows(ROWS).setNCCols(COLS).setNCServePolicy(CONTENT_SERVING_POLICY);

		brokerShadow.setMP(MP);
		brokerShadow.setDelta(DELTA);
		brokerShadow.setFastConf(FASTCONF);
		
		if(COVERING)
			brokerShadow.setCovering(COVERING);
		
		if(BFT) {
			brokerShadow.setBFT(BFT);
			BFTBrokerShadow bftBrokerShadow = (BFTBrokerShadow) brokerShadow;
			new SimpleBFTIssuerProxyRepo(bftBrokerShadow);
			_bftDackLogger = new BFTDackLogger(bftBrokerShadow);
			_bftSuspicionLogger = new BFTSuspicionLogger(bftBrokerShadow);
			new SimpleBFTSuspectedRepo(bftBrokerShadow);
			_bftInvalidMessageLogger = new BFTInvalidMessageLogger(bftBrokerShadow);
			_bftDSALogger = new BFTDSALogger(bftBrokerShadow);
		}
		
//		_cpuCasualReader = new CpuCasualReader(brokerShadow);
		_cpuCasualReader = new FakeCpuCasualReader(brokerShadow);
		_rttLogger = new RTTLogger(brokerShadow);
		_transitionsLogger = new TransitionsLogger(brokerShadow);
		_sessionsRanksLogger = new SessionsRanksLogger(brokerShadow);
		_casualLoggerEngine = new CasualLoggerEngine(brokerShadow);
		_confimationLogger = new ConfirmationDataLogger(brokerShadow);
		_messageProfilerLogger = new MessageProfilerLogger(brokerShadow);
		
		if(LOG_VIOLATIONS)
			_violationsLogger = new ViolationsLogger(brokerShadow);
		
		if (LOG_STATISTICS)
			if(BFT)
				_statisticsLogger = new BFTStatisticsLogger((BFTBrokerShadow) brokerShadow);
			else
				_statisticsLogger = new StatisticsLogger(brokerShadow);
			
		if (LOG_TRAFFIC)
			_trafficLogger = new TrafficLogger(brokerShadow);
		
		if (LOG_EXECTIME)
			_execTimeLogger = new ExecutionTimeLogger(brokerShadow);
		
		if (CODING) {
			_communicationTransportLogger = new CommunicationTransportLogger(brokerShadow);
			_plistEngineLogger = new CasualPListLogger(brokerShadow);
		}
		
		return brokerShadow;
	}
	
	@Override
	public double getCpuAverage() {
		return _brokerShadow.getCpuCasualReader().getAverageCpuPerc();
	}
	
	@Override
	public void prepareToStart() {
		LoggerFactory.getLogger().info(this, " beginning to prepareAndStart.");
		logDeploySummaries();
		
		/* 	Logging infrastructure */
		_casualLoggerEngine.registerCasualLogger(_messageProfilerLogger);
		_casualLoggerEngine.registerCasualLogger(_rttLogger);
		_casualLoggerEngine.registerCasualLogger(_transitionsLogger);
		_casualLoggerEngine.registerCasualLogger(_sessionsRanksLogger);
		_casualLoggerEngine.registerCasualLogger(_cpuCasualReader);
		if(_brokerShadow.isBFT()) {
			_casualLoggerEngine.registerCasualLogger(_bftDackLogger);
			_casualLoggerEngine.registerCasualLogger(_bftInvalidMessageLogger);
			_casualLoggerEngine.registerCasualLogger(_bftDSALogger);
			_casualLoggerEngine.registerCasualLogger(_bftSuspicionLogger);
		}
		if ( LOG_VIOLATIONS )
			_casualLoggerEngine.registerCasualLogger(_violationsLogger);
		if ( LOG_STATISTICS )
			_casualLoggerEngine.registerCasualLogger(_statisticsLogger);
		if ( LOG_TRAFFIC )
			_casualLoggerEngine.registerCasualLogger(_trafficLogger);
		if ( LOG_EXECTIME )
			_casualLoggerEngine.registerCasualLogger(_execTimeLogger);
		if ( LOG_CONFIRMATION_DATA )
			_casualLoggerEngine.registerCasualLogger(_confimationLogger);
		if (_brokerShadow.isNC()) {
			_casualLoggerEngine.registerCasualLogger(_communicationTransportLogger);
			if(!isClient())
				_casualLoggerEngine.registerCasualLogger(_plistEngineLogger);
		}
		
		_casualLoggerEngine.registerCasualLogger(_pubDeliveryLogger);
		
		_casualLoggerEngine.prepareToStart();
		_casualLoggerEngine.startComponent();
		
		/* 	Connection Manager */
		_connectionManager.prepareToStart();
		_connectionManager.startComponent();
		
		LoggerFactory.getLogger().info(this, "Prepare to start is done!.");
	}

	@Override
	public void startComponent() {
		throw new UnsupportedOperationException("Broker cannot be started (just do a prepareToStart())!");
	}
	
	@Override
	public void stopComponent() {
		LoggerFactory.getLogger().info(this, "Starting the STOP operation");
		synchronized (_componentStatusLock) {
			setComponentStatus(ComponentStatus.COMPONENT_STATUS_STOPPING);
			_connectionManager.stopComponent();
		}
	}
	
	@Override
	public BrokerOpState getBrokerOpState() {
		return _brokerOpState;
	}

	@Override
	public LocalSequencer getLocalSequencer() {
		return _brokerShadow.getLocalSequencer();
	}
	
	@Override
	public InetSocketAddress getLocalUDPAddress() {
		return _brokerShadow.getLocalUDPAddress();
	}
	
	@Override
	public InetSocketAddress getLocalAddress() {
		return _brokerShadow.getLocalAddress();
	}
	
	@Override
	public ContentServingPolicy getContentServingPolicy() {
		return _brokerShadow.getContentServingPolicy();
	}
	
	//IBroker
	@Override
	public void joinComplete() {
		LoggerFactory.getLogger().info(this, "Join is now complete.");
		
		if ( _connectionManager.getComponentState() != ComponentStatus.COMPONENT_STATUS_RUNNING )
			throw new IllegalStateException("ConMan is not running: " + _connectionManager + " _ " + _connectionManager.getComponentState());
		
		if ( this._brokerOpState != BrokerOpState.BRKR_PUBSUB_JOIN )
			throw new IllegalStateException("Broker::joinComplete - ERROR, never was trying to join!!!");
		
		_connectionManager.pauseComponent();	
	}

	@Override
	public void recoveryComplete() {
		LoggerFactory.getLogger().info(this, "Recovery is now complete.");
		
		if ( _brokerOpState != BrokerOpState.BRKR_RECOVERY )
			throw new IllegalStateException("Broker::recoveryComplete - ERROR, never was trying to recover!!!");
		
		_connectionManager.pauseComponent();
	}

	@Override
	public void departComplete() {
		LoggerFactory.getLogger().info(this, "Depart is now complete.");
		if ( _brokerOpState != BrokerOpState.BRKR_PUBSUB_DEPART )
			throw new IllegalStateException("Broker::departComplete - ERROR, never was trying to depart!!!");
		
		setBrokerOpState(BrokerOpState.BRKR_END);
	}

	@Override
	public final String getBrokerID() {
		return _brokerShadow.getBrokerID();
	}
	
	@Override
	public int getDefaultFlowPacketSize() {
		return _brokerShadow.getDefaultFlowPacketSize();
	}
	
	@Override
	public String getGenericConfigFilename() {
		return _brokerShadow.getGenericConfigFilename();
	}
	
	@Override
	public String getBrokerSpecificConfigFilename() {
		return _brokerShadow.getBrokerSpecificConfigFilename();
	}
	
	@Override
	public String getRecoveryFileName() {
		return _brokerShadow.getRecoveryFileName();
	}
	
	@Override
	public String getPublicationDeliveryLogFilename() {
		return _brokerShadow.getPublicationDeliveryLogFilename();
	}

	@Override
	public String getPublicationGenerationLogFilename() {
		return _brokerShadow.getPublicationGenerationLogFilename();
	}
	
	@Override
	public String getTrafficLogFilename() {
		return _brokerShadow.getTrafficLogFilename();
	}
	
	@Override
	public String getExecTimeLogFilename() {
		return _brokerShadow.getExecTimeLogFilename();
	}

	@Override
	public String getViolationsLogFilename() {
		return _brokerShadow.getViolationsLogFilename();
	}
	
	@Override
	public String getRTTLogFilename() {
		return _brokerShadow.getRTTLogFilename();
	}
	
	@Override
	public String getTransitionsFileName() {
		return _brokerShadow.getTransitionsFileName();
	}
	
	@Override
	public String getRanksLogFileName() {
		return _brokerShadow.getRanksLogFileName();
	}
	
	@Override
	public String getMessageProfilerLogFilename() {
		return _brokerShadow.getMessageProfilerLogFilename();
	}

	@Override
	public String getConfirmationDataLogFilename() {
		return _brokerShadow.getConfirmationDataLogFilename();
	}
	
	@Override
	public String getPSOverlayDumpFileName() {
		return _brokerShadow.getPSOverlayDumpFileName();
	}

	@Override
	public String getSessionsDumpFileName() {
		return _brokerShadow.getSessionsDumpFileName();
	}

	@Override
	public String getBrokerSubDumpFileName() {
		return _brokerShadow.getBrokerSubDumpFileName();
	}
	
	@Override
	public String getStatisticsLogFilename() {
		return _brokerShadow.getStatisticsLogFilename();
	}
	
	@Override
	public String getAnnotationFilename() {
		return _brokerShadow.getAnnotationFilename();
	}
	
	@Override
	public BrokerIdentityManager getBrokerIdentityManager() {
		return _brokerShadow.getBrokerIdentityManager();
	}
	
	private void installConnectionManagerPSImmediately() {
		LoggerFactory.getLogger().info(this, "Starting to install ConnectionManagerPS by Threa '" + Thread.currentThread());

		switch( _connectionManager.getType()) {
		case CONNECTION_MANAGER_JOIN:
		{
			IConnectionManagerJoin conManJoin = (IConnectionManagerJoin) _connectionManager;
			conManJoin.enableDisableMQProceed(false);
			if(_brokerShadow.isNC()) {
				if(getNodeType().isClient())
					_connectionManager = ConnectionManagerFactory.getConnectionManagerNC_Client(_brokerShadow, conManJoin);
				else
					_connectionManager = ConnectionManagerFactory.getConnectionManagerNC(_brokerShadow, conManJoin);
			} else if(_brokerShadow.isMP()) {
				_connectionManager = ConnectionManagerFactory.getConnectionManagerMP(_brokerShadow, conManJoin);
			} else if(_brokerShadow.isBFT()) {
				_connectionManager = ConnectionManagerFactory.getConnectionManagerBFT(_brokerShadow, (BFTConnectionManagerJoin) conManJoin);
			} else {
				_connectionManager = ConnectionManagerFactory.getConnectionManagerPS(_brokerShadow, conManJoin);
			}
			_connectionManager.enableDisableMQProceed(true);
			break;
		}

		case CONNECTION_MANAGER_RECOVERY:
		{
			IConnectionManagerRecovery conManRecovery = (IConnectionManagerRecovery) _connectionManager;
			conManRecovery.enableDisableMQProceed(false);
			if(_brokerShadow.isNC()) { 
				if(getNodeType().isClient())
					_connectionManager = ConnectionManagerFactory.getConnectionManagerNC_Client(_brokerShadow, conManRecovery);
				else
					_connectionManager = ConnectionManagerFactory.getConnectionManagerNC(_brokerShadow, conManRecovery);
			} else if(_brokerShadow.isMP()) {
				_connectionManager = ConnectionManagerFactory.getConnectionManagerMP(_brokerShadow, conManRecovery);
			} else {
				_connectionManager = ConnectionManagerFactory.getConnectionManagerPS(_brokerShadow, conManRecovery);
			}
			_connectionManager.enableDisableMQProceed(true);
			break;
		}
		
		default:
			break;
		}
		
		LoggerFactory.getLogger().info(this, "Finished installing ConnectionManagerPS");
	}

	private void setBrokerOpState(BrokerOpState brokerOpState) {
		LoggerFactory.getLogger().info(this, "Setting opState from '" + _brokerOpState + "' to '" + brokerOpState + "'.");
		_brokerOpState = brokerOpState;
		
		synchronized(_opStateListeners) {
			Iterator<IBrokerOpStateListener> listeners = _opStateListeners.iterator();
			while ( listeners.hasNext() )
			{
				IBrokerOpStateListener listener = listeners.next();
				listener.brokerOpStateChanged(this);
			}
		}
	}

	@Override
	public void registerBrokerOpStateListener(IBrokerOpStateListener brokerOpStateListener) {
		LoggerFactory.getLogger().info(this, "Registering new BrokerOpStateListener '" + brokerOpStateListener + "'.");
		synchronized (_opStateListeners) {
			_opStateListeners.add(brokerOpStateListener);
		}
	}

	@Override
	public void deregisterBrokerOpStateListener(IBrokerOpStateListener brokerOpStateListener) {
		LoggerFactory.getLogger().info(this, "Deregistering new BrokerOpStateListener '" + brokerOpStateListener + "'.");
		synchronized (_opStateListeners) {
			_opStateListeners.remove(brokerOpStateListener);
		}
	}

	@Override
	public IConnectionManager getConnectionManager() {
		return _connectionManager;
	}

	@Override
	public void addNewComponentListener(IComponentListener comListener) {
		LoggerFactory.getLogger().info(this, "Adding new ComponentListener '" + comListener + "'.");
		synchronized (_componentListeners) {
			_componentListeners.add(comListener);
		}
	}

	@Override
	public void awakeFromPause() {
		LoggerFactory.getLogger().info(this, "awakeFromPause");
		throw new UnsupportedOperationException("Broker does not support pause.");
	}

	@Override
	public ComponentStatus getComponentState() {
		return _componentStatus;
	}

	@Override
	public void pauseComponent() {
		throw new UnsupportedOperationException("Broker does not support pause.");
	}

	@Override
	public void removeComponentListener(IComponentListener comListener) {
		LoggerFactory.getLogger().info(this, "Removing new ComponentListener '" + comListener + "'.");
		synchronized (_componentListeners) {
			_componentListeners.remove(comListener);
		}
	}

	private void setComponentStatus(ComponentStatus componentStatus) {
		LoggerFactory.getLogger().info(this, "Setting ComponentStatus from '" + _componentStatus + "' to '" + componentStatus + "'.");
		_componentStatus = componentStatus;
		
		synchronized (_componentListeners) {
			Iterator<IComponentListener> comListenerIt = _componentListeners.iterator();
			while ( comListenerIt.hasNext() ) {
				IComponentListener comListener = comListenerIt.next();
				comListener.componentStateChanged(this);
			}
		}
		
		if ( _componentStatus == ComponentStatus.COMPONENT_STATUS_STOPPED )
			endAndReleaseResources();
	}

	private void endAndReleaseResources() {
		LoggerFactory.getLogger().info(this, "Ending broker and releasing resources ...");
		
//		_maintenanceManager.destroy();
		
		setBrokerOpState(BrokerOpState.BRKR_END);
		_componentListeners.clear();
		_componentListeners = null;
		_opStateListeners.clear();
		_opStateListeners = null;
		_connectionManager = null;
	}

	@Override
	public String getComponentName() {
		return getBrokerID();
	}

	@Override
	public ComponentStatus getBrokerComponentState() {
		return getComponentState();
	}

	@Override
	public BrokerOpState getBrokerOpStatus() {
		return _brokerOpState;
	}

	@Override
	public PSSessionInfo[] getPSSessions() {
		if ( _connectionManager == null )
			return null;
		
		return _connectionManager.getPSSessions();
	}

	@Override
	public ISessionInfo[] getSessions() {
		if ( _connectionManager == null )
			return null;
		
		return _connectionManager.getSessionInfos();
	}

	@Override
	public SubscriptionInfo[] getSubscriptionInfos() {
		if ( _connectionManager == null )
			return null;
		
		return _connectionManager.getSubscriptionInfos();
	}

	@Override
	public JoinInfo[] getTopologyLinks() {
		if ( _connectionManager == null )
			return null;
		
		return _connectionManager.getTopologyLinks();
	}

	@Override
	public ExceptionInfo[] getExceptionInfos() {
		List<ExceptionInfo> exceptionInfos = ExceptionLogger.collectExceptoinInfos();
		if ( exceptionInfos == null )
			return null;
		
		ExceptionInfo[] retExceptionInfos = exceptionInfos.toArray(new ExceptionInfo[0]);
		return retExceptionInfos;
	}

	@Override
	public StatisticalInfo[] getBrokerStatisticalInfos() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_BROKER;
	}

	@Override
	public NodeTypes getNodeType() {
		return NodeTypes.NODE_BROKER;
	}

	@Override
	public boolean setSpeedLevel(int interval) {
		throw new UnsupportedOperationException("Broker's speed cannot be changed (no speed basicaly)!!");
	}

	@Override
	public boolean play() {
		return true;
	}
	
	@Override
	public boolean pause() {
		return true;
	}

	@Override
	public StatisticsLogger getStatisticsLogger() {
		return _brokerShadow.getStatisticsLogger();
	}

	@Override
	public ViolationsLogger getViolationsLogger() {
		return _brokerShadow.getViolationsLogger();
	}

	@Override
	public CasualContentLogger getContentLogger() {
		return _brokerShadow.getContentLogger();
	}
	
	@Override
	public CasualPListLogger getPlistLogger() {
		return _brokerShadow.getPlistLogger();
	}
	
	@Override
	public ExecutionTimeLogger getExecutionTimeLogger() {
		return _brokerShadow.getExecutionTimeLogger();
	}
	
	@Override
	public RTTLogger getRTTLogger() {
		return _brokerShadow.getRTTLogger();
	}
	
	@Override
	public TransitionsLogger getTransitionsLogger() {
		return _brokerShadow.getTransitionsLogger();
	}
	
	@Override
	public SessionsRanksLogger getSessionsRanksLogger() {
		return _brokerShadow.getSessionsRanksLogger();
	}
	
	@Override
	public MessageProfilerLogger getMessageProfilerLogger() {
		return _brokerShadow.getMessageProfilerLogger();
	}

	@Override
	public ConfirmationDataLogger getConfirmationDataLogger() {
		return _brokerShadow.getConfirmationDataLogger();
	}
	
	public static int getNIOThreadPriority() {
		return Thread.MAX_PRIORITY;
	}

	@Override
	public IMaintenanceManager getMaintenanceManager() {
		return _brokerShadow.getMaintenanceManager();
	}

	@Override
	public long getAffinityMask() {
		return CPU_AFFINITY_MASK;
	}

	@Override
	public PubForwardingStrategy getPublicationForwardingStrategy() {
		return _brokerShadow.getPublicationForwardingStrategy();
	}

	@Override
	public boolean loadPreparedSubscriptions() {
		if(!LOAD_PREPARED_SUBS)
			return true;
		
		BrokerIdentityManager idManager = _brokerShadow.getBrokerIdentityManager();
		String preparedSubscriptionFilename = _brokerShadow.getPreparedSubscriptionFilename(idManager);
		LoggerFactory.getLogger().info(
				this, "Loading prepared subscriptions: " + preparedSubscriptionFilename);
		// Load subscriptions
		_connectionManager.loadPrepareSubscriptionsFile(preparedSubscriptionFilename, idManager);
		
		return true;
	}
	
	@Override
	public boolean handleTCommandMessage(TCommand tCommand) {
		TCommandTypes type = tCommand.getType();
		switch (type) {
		case CMND_MARK:
			break;
			
		case CMND_DISSEMINATE:
			_connectionManager.issueTCommandDisseminateMessage(tCommand);
			break;
			
		default:
			throw new UnsupportedOperationException("Unknown TCommandTypes: " + type);
		}
		return false;
	}

	@Override
	public InOutBWEnforcer getBWEnforcer() {
		return _brokerShadow.getBWEnforcer();
	}

	@Override
	public String getBaseDir() {
		return _brokerShadow.getBaseDir();
	}
	
	@Override
	public String getOutputDir() {
		return _brokerShadow.getOutputDir();
	}

	@Override
	public LoadWeightRepository getLoadWeightRepository() {
		return _brokerShadow.getLoadWeightRepository();
	}

	@Override
	public boolean canDeliverMessages() {
		return true;
	}

	@Override
	public boolean canGenerateMessages() {
		return false;
	}
	
	@Override
	public void matchingPublicationDelivered(TMulticast_Publish tmp) {
		_defaultSubscriptionListener.matchingPublicationDelivered(tmp);
	}
	
	@Override
	public void matchingPublicationDelivered(Sequence sourceSequence, Publication publication) {
		_defaultSubscriptionListener.matchingPublicationDelivered(sourceSequence, publication);
	}

	@Override
	public PublicationInfo[] getReceivedPublications() {
		return _defaultSubscriptionListener.getReceivedPublications();
	}
	
	public static InetSocketAddress getUDPListeningSocket(InetSocketAddress tcpListeningSocket) {
		return new InetSocketAddress(tcpListeningSocket.getAddress(), tcpListeningSocket.getPort()+1);
	}

	public static InetSocketAddress getTCPListeningSocket(InetSocketAddress tcpListeningSocket) {
		return new InetSocketAddress(tcpListeningSocket.getAddress(), tcpListeningSocket.getPort()-1);
	}

	@Override
	public String getPreparedSubscriptionFilename(BrokerIdentityManager idManager) {
		return _brokerShadow.getPreparedSubscriptionFilename(idManager);
	}

	@Override
	public String getPListLogFilename() {
		return _brokerShadow.getPListLogFilename();
	}
	
	@Override
	public String getContentFilename() {
		return _brokerShadow.getContentFilename();
	}

	@Override
	public ContentBreakPolicy getContentBreakPolicy() {
		return _brokerShadow.getContentBreakPolicy();
	}

	@Override
	public String getCodedBlocksLogFilename() {
		return _brokerShadow.getCodedBlocksLogFilename();
	}

	@Override
	public int getDelta() {
		return _brokerShadow.getDelta();
	}

	@Override
	public boolean isClient() {
		return _brokerShadow.isClient();
	}
	
	@Override
	public InetSocketAddress getClientsJoiningBrokerAddress() {
		return null;
	}

	@Override
	public void tmConfirmed(TMulticast tm) {
		_defaultSubscriptionListener.tmConfirmed(tm);
	}

	@Override
	public boolean isBroker() {
		return _brokerShadow.isBroker();
	}

	@Override
	public String getCodingEngineLogFilename() {
		return _brokerShadow.getCodingEngineLogFilename();
	}

	@Override
	public int getCount() {
		return _defaultSubscriptionListener.getCount();
	}

	@Override
	public PublicationInfo getLastReceivedPublications() {
		return _defaultSubscriptionListener.getLastReceivedPublications();
	}

	@Override
	public double getUsedBWThreshold() {
		return _brokerShadow.getUsedBWThreshold();
	}

	public static void setDelta(int delta) {
		DELTA = delta;
	}

	@Override
	public int getNeighborhoodRadius() {
		return _brokerShadow.getNeighborhoodRadius();
	}

	@Override
	public boolean isMP() {
		return _brokerShadow.isMP();
	}

	@Override
	public boolean isNC() {
		return _brokerShadow.isNC();
	}

	@Override
	public boolean isBFT() {
		return _brokerShadow.isBFT();
	}

	@Override
	public int getNCRows() {
		return _brokerShadow.getNCRows();
	}

	@Override
	public int getNCCols() {
		return _brokerShadow.getNCCols();
	}

	@Override
	public int getNCServePolicy() {
		return _brokerShadow.getNCServePolicy();
	}

	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}

	@Override
	public CommunicationTransportLogger getCommunicationTransportLogger() {
		return _brokerShadow.getCommunicationTransportLogger();
	}

	@Override
	public CasualContentLogger getCasualContentLogger() {
		return _brokerShadow.getCasualContentLogger();
	}

	@Override
	public TrafficLogger getTrafficLogger() {
		return _brokerShadow.getTrafficLogger();
	}

	@Override
	public CasualPListLogger getCasualPListLogger() {
		return _brokerShadow.getCasualPListLogger();
	}

	@Override
	public CodingEngineLogger getCodingEngineLogger() {
		return _brokerShadow.getCodingEngineLogger();
	}

	@Override
	public ICpuCasualReader getCpuCasualReader() {
		return _brokerShadow.getCpuCasualReader();
	}

	@Override
	public CasualLoggerEngine getCasualLoggerEngine() {
		return _brokerShadow.getCasualLoggerEngine();
	}

	@Override
	public IExecutionTimeEntity getExecutionTypeEntity(ExecutionTimeType type) {
		return _brokerShadow.getExecutionTypeEntity(type);
	}

	@Override
	public boolean isFastConf() {
		return _brokerShadow.isFastConf();
	}

	@Override
	public boolean isNormal() {
		return _brokerShadow.isNormal();
	}

	@Override
	public boolean coveringEnabled() {
		return _brokerShadow.coveringEnabled();
	}

	@Override
	public IOverlayManager createOverlayManager() {
		return _brokerShadow.createOverlayManager();
	}

	@Override
	public ISubscriptionManager createSubscriptionManager(IOverlayManager overlayManager, String dumpFileName, boolean shouldLog) {
		return _brokerShadow.createSubscriptionManager(overlayManager, dumpFileName, shouldLog);
	}

	@Override
	public boolean installBFTMessageManipulator(TBFTMessageManipulator bftMesssageManipulator) {
		if(_brokerShadow.isBFT()) {
			((BFTBrokerShadow)_brokerShadow).setBFTMessageManipulator(bftMesssageManipulator);
		}
		
		return false;
	}

	@Override
	public long getLastPublicationReceiptTime() {
		return _defaultSubscriptionListener.getLastPublicationReceiptTime();
	}

	@Override
	public Map<String, Integer> getDeliveredPublicationCounterPerPublisher() {
		return _defaultSubscriptionListener.getDeliveredPublicationCounterPerPublisher();
	}

	@Override
	public Map<String, Long> getLastPublicationDeliveryTimesPerPublisher() {
		return _defaultSubscriptionListener.getLastPublicationDeliveryTimesPerPublisher();
	}

	@Override
	public Map<String, Publication> getLastPublicationDeliveredPerPublisher() {
		return _defaultSubscriptionListener.getLastPublicationDeliveredPerPublisher();
	}

	@Override
	public IMessageQueue createMessageQueue(
			ConnectionManagerTypes conManType,
			IConnectionManager connectionManager,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager) {
		return _brokerShadow.createMessageQueue(
				conManType, connectionManager, overlayManager, subscriptionManager);
	}
}