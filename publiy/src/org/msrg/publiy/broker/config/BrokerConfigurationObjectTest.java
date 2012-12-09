package org.msrg.publiy.broker.config;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.msrg.publiy.utils.FileUtils;

import org.msrg.publiy.broker.core.bwenforcer.BWMetric;

import junit.framework.TestCase;

public class BrokerConfigurationObjectTest extends TestCase {

	BrokerConfigurationObject _bcObj;
	
	String _brokerId = "p0";
	String _configDir = "d:/temp/config/";
	String _configFile = _configDir + "brokers.config";
	String _brokerConfigFile = _configDir + _brokerId + ".config";
	String _brokerSpecificFanoutValue = "14";

	@Override
	public void setUp() {
		Properties props = new Properties();
		manuallyLoadProperties(props);
		try {
			props.store(
					new FileOutputStream(_configFile),
							"Brokers' default configuration file");
			
			props.setProperty("Broker.FANOUT", _brokerSpecificFanoutValue);
			props.store(
					new FileOutputStream(_brokerConfigFile),
							"Broker " + _brokerId + "'s default configuration file");
			
		} catch (IOException e1) {
			e1.printStackTrace();
			fail("Could not save file.");
		}

		try {
			_bcObj = new BrokerConfigurationObject(null, props);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Error initializing BrokerConfigurationObject; check file: " + _configFile);
		}
	}
	
	protected void manuallyLoadProperties(Properties props) {
		props.setProperty("Broker.BASEDIR", 		"." + FileUtils.separatorChar);
		props.setProperty("Broker.ID_FILE", 		"." + FileUtils.separatorChar + "identityfile");
		props.setProperty("Broker.LOAD_PREPARED_SUBS", 	"true");
		props.setProperty("Broker.COVERING", 		"true");
		props.setProperty("Broker.RELEASE", 		"true");
		props.setProperty("Broker.DEBUG", 			"false");
		props.setProperty("Broker.CORRELATE", 		"false");
		props.setProperty("Broker.MP", 			"true");
		props.setProperty("Broker.CODING", 			"true");
		props.setProperty("Broker.PARTITION_TOLERANT", 	"false");
		props.setProperty("Broker.LOG_VIOLATIONS", 		"false");
		props.setProperty("Broker.FANOUT", 			"12");
		props.setProperty("Broker.CANDIDATES", 		"4");
		props.setProperty("Broker.AFFINITYMASK", 		"-1");
		props.setProperty("Broker.DACKINTERVAL", 		"10000");
		props.setProperty("Broker.LOADWEIGHTSSENDINTERVAL", "10000");
		props.setProperty("Broker.PURGEMQINTERVAL", 	"15000");
		props.setProperty("Broker.FASTCONF", 		"true");
		props.setProperty("Broker.ANNOTATE", 		"false");
		props.setProperty("Broker.LOGCONFDATA", 		"false");
		props.setProperty("Broker.LOGEXECTIME", 		"false");
		props.setProperty("Broker.LOGSUBSCRIPTIONS", 	"true");
		props.setProperty("Broker.LOGTRAFFIC", 		"false");
		props.setProperty("Broker.DELTA", 		"3");
		props.setProperty("Broker.ForcedConfAckDelay", 	"30000");
		props.setProperty("Broker.BW_METRIC", 		BWMetric.BW_METRIC_INOUT_BYTES.toString());
		props.setProperty("Broker.BW_IN", 		"10000");
		props.setProperty("Broker.BW_OUT", 		"10000");
	}
	
	public void testLoadProperties() {
		assertTrue(_bcObj.BASE_DIR.equals("." + FileUtils.separatorChar));
		assertTrue(_bcObj.FANOUT == new Integer(_brokerSpecificFanoutValue).intValue());
	}
}
