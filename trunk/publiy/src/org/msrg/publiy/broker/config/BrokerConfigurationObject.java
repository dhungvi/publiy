package org.msrg.publiy.broker.config;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.PubForwardingStrategy;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;

import org.msrg.publiy.sutils.SystemPackageVersion;
import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.broker.core.bwenforcer.BWMetric;

public class BrokerConfigurationObject implements ILoggerSource {
	
	public final Broker _broker;
	
	public final int FAST_CONNECTION_RTT = 350;
	public final int TPING_SEND_INTERVAL = 5000;
	public final int TPING_TIMEOUT_INTERVAL = 5000;
	public final int CANDIDATES_EVENT_TIMER_INTERVAL = 60000;
	public PubForwardingStrategy PUB_FORWARDING_STRATEGY = PubForwardingStrategy.PUB_FORWARDING_STRATEGY_UNKNOWN;
	public final String BASE_DIR;
	public final String IDENTITY_FILE;
	public final String S_JAR_VERSION = SystemPackageVersion.getVersion();

	public final boolean LOAD_PREPARED_SUBS;
	public final boolean COVERING;
	public final boolean RELEASE;
	public final boolean DEBUG;
	public final boolean CORRELATE;
	public final boolean MP;
	public final boolean CODING;
	public final boolean PARTITION_TOLERANT;
	public final boolean LOG_VIOLATIONS;
	public final int FANOUT;
	public final int CANDIDATES;
	public final long CPU_AFFINITY_MASK;
	public final int DACK_SEND_INTERVAL;
	public final int LOAD_WEIGHTS_SEND_INTERVAL;
	public final int PURGE_MQ_INTERVAL;
	public final boolean FASTCONF;
	public final boolean ANNOTATE;
	public final boolean LOG_CONFIRMATION_DATA;
	public final boolean LOG_EXECTIME;
	public final boolean LOG_STATISTICS;
	public final boolean LOG_TRAFFIC;
	public final int DELTA;
	public final int FORCED_CONF_ACK_DELAY;
	public final BWMetric BW_METRIC;
	public final int BW_IN;
	public final int BW_OUT;
	public final int VSEQ_LENGTH;
	
	
	BrokerConfigurationObject(Broker broker, String genericConfig, String brokerConfig) throws IOException {
		this(broker, getProperties(genericConfig, brokerConfig));
	}
	
	protected static Properties getProperties(String genericConfig, String brokerConfig) throws IOException {
		FileReader fReader = new FileReader(genericConfig);
		Properties props = new Properties();
		props.load(fReader);
		fReader.close();
		
		try {
			fReader = new FileReader(brokerConfig);
			props.load(fReader);
			fReader.close();
		} catch(IOException iox) {
			LoggerFactory.getLogger().warn(LoggingSource.LOG_SRC_BROKER_CFG, "Failed to load broker specific config: " + brokerConfig);
		}
		
		return props;
	}
	
	BrokerConfigurationObject(Broker broker, Properties props) throws IOException {
		_broker = broker;
		
		{
			BASE_DIR = props.getProperty("Broker.BASEDIR", "." + FileUtils.separatorChar);
		}
		{	
			IDENTITY_FILE = props.getProperty("Broker.ID_FILE", BASE_DIR + "." + FileUtils.separatorChar + "identityfile");
		}
		{	
			String loadPreparedSubsStr = props.getProperty("Broker.LOAD_PREPARED_SUBS", "true");
			LOAD_PREPARED_SUBS = new Boolean(loadPreparedSubsStr).booleanValue();
		}
		{	
			String coveringStr = props.getProperty("Broker.COVERING", "true");
			COVERING = new Boolean(coveringStr).booleanValue();
		}
		{	
			String releaseStr = props.getProperty("Broker.RELEASE", "true");
			RELEASE = new Boolean(releaseStr).booleanValue();
		}
		{	
			String debugStr = props.getProperty("Broker.DEBUG", "false");
			DEBUG = new Boolean(debugStr).booleanValue();
		}
		{	
			String correlateStr = props.getProperty("Broker.CORRELATE", "" + (!RELEASE || DEBUG));
			CORRELATE = new Boolean(correlateStr).booleanValue();
		}
		{	
			String mpStr = props.getProperty("Broker.MP", "true");
			MP = new Boolean(mpStr).booleanValue();
		}
		{	
			String codingStr = props.getProperty("Broker.CODING", "true");
			CODING = new Boolean(codingStr).booleanValue();
		}
		{	
			String partitionTolerantStr = props.getProperty("Broker.PARTITION_TOLERANT", "" + !MP);
			PARTITION_TOLERANT = new Boolean(partitionTolerantStr).booleanValue();
			if(MP && PARTITION_TOLERANT)
				throw new IllegalStateException("Cannot have Broker.MP=true && Broker.PARTITION_TOLERANT=true");
		}
		{	
			String logViolationsStr = props.getProperty("Broker.LOG_VIOLATIONS", "false");
			LOG_VIOLATIONS = new Boolean(logViolationsStr).booleanValue();
			if(MP && PARTITION_TOLERANT)
				throw new IllegalStateException("Cannot have Broker.MP=true && Broker.PARTITION_TOLERANT=true");
		}
		{	
			String maxFanoutStr = props.getProperty("Broker.FANOUT", (MP?"12":"-1"));
			FANOUT = new Integer(maxFanoutStr).intValue();
		}
		{	
			String maxCandidatesStr = props.getProperty("Broker.CANDIDATES", (MP?"4":"-1"));
			CANDIDATES = new Integer(maxCandidatesStr).intValue();
		}
		{	
			String cpuAffinityMaskStr = props.getProperty("Broker.AFFINITYMASK", "-1");
			CPU_AFFINITY_MASK = new Long(cpuAffinityMaskStr).longValue();
		}
		{	
			String dackIntervalStr = props.getProperty("Broker.DACKINTERVAL", "10000");
			DACK_SEND_INTERVAL = new Integer(dackIntervalStr).intValue();
		}
		{	
			String laodWeightIntervalStr = props.getProperty("Broker.LOADWEIGHTSSENDINTERVAL", "10000");
			LOAD_WEIGHTS_SEND_INTERVAL = new Integer(laodWeightIntervalStr).intValue();
		}
		{	
			String purgeMQIntervalStr = props.getProperty("Broker.PURGEMQINTERVAL", "15000");
			PURGE_MQ_INTERVAL = new Integer(purgeMQIntervalStr).intValue();
		}
		{	
			String fastConfStr = props.getProperty("Broker.FASTCONF", "true");
			FASTCONF = new Boolean(fastConfStr).booleanValue();
		}
		{	
			String annotateStr = props.getProperty("Broker.ANNOTATE", "false");
			ANNOTATE = new Boolean(annotateStr).booleanValue();
		}
		{	
			String confLogStr = props.getProperty("Broker.LOGCONFDATA", "false");
			LOG_CONFIRMATION_DATA = new Boolean(confLogStr).booleanValue();
		}
		{	
			String execTimeLogStr = props.getProperty("Broker.LOGEXECTIME", "false");
			LOG_EXECTIME = new Boolean(execTimeLogStr).booleanValue();
		}
		{	
			String subscriptionLogStr = props.getProperty("Broker.LOGSUBSCRIPTIONS", "true");
			LOG_STATISTICS = new Boolean(subscriptionLogStr).booleanValue();
		}
		{	
			String trafficLogStr = props.getProperty("Broker.LOGTRAFFIC", "false");
			LOG_TRAFFIC = new Boolean(trafficLogStr).booleanValue();
		}
		{	
			String deltaStr = props.getProperty("Broker.DELTA", "3");
			DELTA = new Integer(deltaStr).intValue();
		}
		{	
			String forcedConfAckStr = props.getProperty("Broker.ForcedConfAckDelay", "30000");
			FORCED_CONF_ACK_DELAY = new Integer(forcedConfAckStr).intValue();
		}
		{	
			String bwMetricStr = props.getProperty("Broker.BW_METRIC", BWMetric.BW_METRIC_INOUT_BYTES.toString());
			BW_METRIC = BWMetric.getBWMetricFromString(bwMetricStr);
		}
		{	
			String bwInStr = props.getProperty("Broker.BW_IN", "10000");
			BW_IN = new Integer(bwInStr);
		}
		{	
			String bwOutStr = props.getProperty("Broker.BW_OUT", "10000");
			BW_OUT = new Integer(bwOutStr);
		}
		{	
			VSEQ_LENGTH = (PARTITION_TOLERANT ? 3 * DELTA+1 : Broker.DELTA + 1);
		}
	}
	
	protected List<String> getDeploySummary() {
		List<String> summaries = new LinkedList<String>();
		summaries.add("S_JAR_VERSION=" + S_JAR_VERSION);
		summaries.add("COVERING=" + COVERING);
		summaries.add("LOAD_PREPARED_SUBS=" + LOAD_PREPARED_SUBS);
		summaries.add("RELEASE=" + RELEASE);
		summaries.add("STRATEGY=" + PUB_FORWARDING_STRATEGY);
		summaries.add("DEBUG=" + DEBUG);
		summaries.add("LOG_TRAFFIC=" + LOG_TRAFFIC);
		summaries.add("LOG_EXECTIME=" + LOG_EXECTIME);
		summaries.add("LOG_CONFIRMATION_DATA=" + LOG_CONFIRMATION_DATA);
		summaries.add("ANNOTATE=" + ANNOTATE);
		summaries.add("FASTCONF=" + FASTCONF);
		summaries.add("PURGE_MQ_INTERVAL=" + PURGE_MQ_INTERVAL);
		summaries.add("DACK_SEND_INTERVAL=" + DACK_SEND_INTERVAL);
		summaries.add("CPU_AFFINITY_MASK=" + CPU_AFFINITY_MASK);
		summaries.add("FORCED_CONF_ACK_DELAY=" + FORCED_CONF_ACK_DELAY);
		summaries.add("PUB_WORKLOAD_SIZE=" + TMulticast_Publish.PUBLICATION_DEFAULT_WORKLOAD_SIZE);
		summaries.add("MP=" + MP);
		summaries.add("PARTITION_TOLERANT=" + PARTITION_TOLERANT);
		summaries.add("DELTA=" + DELTA);
		summaries.add("VSEQ_LENGTH=" + VSEQ_LENGTH);
		summaries.add("CORRELATE=" + CORRELATE);
		summaries.add("FANOUT=" + FANOUT);
		summaries.add("CANDIDATES=" + CANDIDATES);
		summaries.add("BW_METRIC=" + BW_METRIC);
		summaries.add("BW_IN=" + BW_IN);
		summaries.add("BW_OUT=" + BW_OUT);
		summaries.add("BASE_DIR=" + BASE_DIR);
		
		String condense_config_description =
			"SJAR{" + S_JAR_VERSION + "}-" +
			"d" + DELTA + "-" +
			"s" + PUB_FORWARDING_STRATEGY + "-" +
			(FASTCONF?"FAST-":"") +
			("W" + TMulticast_Publish.PUBLICATION_DEFAULT_WORKLOAD_SIZE + "-") +
			(MP?"MP-FOUT"+FANOUT+"-CAND"+CANDIDATES+"-":"NMP-") + 
			(BW_METRIC + "{" + BW_IN + "-" + BW_OUT + "}");
		summaries.add("CONDENSE_DESCRIPTION: " + condense_config_description);
		
		return summaries;
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_BROKER_CFG;
	}

	@Override
	public String toString() {
		StringWriter writer = new StringWriter();
		toString("Param: ", writer);
		
		return writer.toString();
	}

	public void toString(String prefix, StringWriter writer) {
		List<String> summaries = getDeploySummary();
		for(String summary : summaries)
			writer.append(prefix + summary + "\n");
	}
}
