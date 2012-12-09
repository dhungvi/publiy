package org.msrg.publiy.broker;

import java.net.InetSocketAddress;

import org.msrg.publiy.communication.core.niobinding.CommunicationTransportLogger;

import org.msrg.publiy.networkcodes.engine.CodingEngineLogger;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.multipath.loadweights.LoadWeightRepository;

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
import org.msrg.publiy.utils.log.casuallogger.exectime.IExecutionTimeEntity;


import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.IMaintenanceManager;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerTypes;
import org.msrg.publiy.broker.core.contentManager.ContentBreakPolicy;
import org.msrg.publiy.broker.core.contentManager.servingPolicy.ContentServingPolicy;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public interface IBrokerShadow {

	public LocalSequencer getLocalSequencer();
	public InetSocketAddress getLocalAddress();
	public InetSocketAddress getLocalUDPAddress();
	public ContentServingPolicy getContentServingPolicy();
	public ContentBreakPolicy getContentBreakPolicy();
	public PubForwardingStrategy getPublicationForwardingStrategy();
	public double getUsedBWThreshold();
	public int getDefaultFlowPacketSize();
	
	public String getGenericConfigFilename();
	public String getPreparedSubscriptionFilename(BrokerIdentityManager idManager);
	public String getBrokerSpecificConfigFilename();
	
	public String getBaseDir();
	public String getOutputDir();
	
	public String getRecoveryFileName();
	public String getPSOverlayDumpFileName();
	public String getBrokerID();
	public String getSessionsDumpFileName();
	public String getBrokerSubDumpFileName();

	public String getPListLogFilename();
	public String getContentFilename();
	public String getAnnotationFilename();
	public String getStatisticsLogFilename();
	public String getTrafficLogFilename();
	public String getPublicationDeliveryLogFilename();
	public String getPublicationGenerationLogFilename();
	public String getExecTimeLogFilename();
	public String getRTTLogFilename();
	public String getViolationsLogFilename();
	public String getMessageProfilerLogFilename();
	public String getConfirmationDataLogFilename();
	public String getCodedBlocksLogFilename();
	public String getCodingEngineLogFilename();
	public String getTransitionsFileName();
	public String getRanksLogFileName();

	public long getAffinityMask();

	public boolean canDeliverMessages();
	public boolean canGenerateMessages();
	
	public BrokerIdentityManager getBrokerIdentityManager();
	public int getDelta();
	public int getNeighborhoodRadius();

	public boolean isBroker();
	public boolean isClient();
	public NodeTypes getNodeType();
	
	public boolean isMP();
	public boolean isNC();
	public int getNCRows();
	public int getNCCols();
	public int getNCServePolicy();
	public boolean isBFT();
	public boolean isFastConf();
	public boolean isNormal();
	public boolean coveringEnabled();
	
	public ExecutionTimeLogger getExecutionTimeLogger();
	public IMaintenanceManager getMaintenanceManager();
	public LoadWeightRepository getLoadWeightRepository();
	public InOutBWEnforcer getBWEnforcer();
	public CommunicationTransportLogger getCommunicationTransportLogger();
	public CasualContentLogger getCasualContentLogger();
	public TrafficLogger getTrafficLogger();
	public CasualPListLogger getCasualPListLogger();
	public StatisticsLogger getStatisticsLogger();
	public ViolationsLogger getViolationsLogger();
	public CodingEngineLogger getCodingEngineLogger();
	public ICpuCasualReader getCpuCasualReader();
	public RTTLogger getRTTLogger();
	public TransitionsLogger getTransitionsLogger();
	public CasualLoggerEngine getCasualLoggerEngine();
	public ConfirmationDataLogger getConfirmationDataLogger();
	public MessageProfilerLogger getMessageProfilerLogger();
	public SessionsRanksLogger getSessionsRanksLogger();
	public CasualPListLogger getPlistLogger();
	public CasualContentLogger getContentLogger();
	public IExecutionTimeEntity getExecutionTypeEntity(ExecutionTimeType execTimeCheckoutPub);

	public IOverlayManager createOverlayManager();
	public ISubscriptionManager createSubscriptionManager(IOverlayManager overlayManager, String dumpFileName, boolean shouldLog);
	public IMessageQueue createMessageQueue(ConnectionManagerTypes conManType, IConnectionManager connectionManager, IOverlayManager overlayManager, ISubscriptionManager subscriptionManager);
	
}
