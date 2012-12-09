package org.msrg.publiy.utils.log;

import java.io.Serializable;

class PrivateCounter {
	static int COUNTER = 0;
}

public enum LoggingSource implements Serializable {
	LOG_SRC_TEMP_RECOVERY_DATA 		("TMPRECOVERYDATA", 					++PrivateCounter.COUNTER  ),
	LOG_SRC_PSSESSION 				("PSSession", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_ISESSIONMANAGER 		("ISessionManager", 					++PrivateCounter.COUNTER  ),
//	LOG_SRC_SESSION_RENEW_TASK 		("SESSRENEWTASK", 						++PrivateCounter.COUNTER  ),
	LOG_SRC_ISESSION 				("ISession", 							++PrivateCounter.COUNTER  ),
//	LOG_SRC_DESTROY_CONNECTION_TASK	("TaskDestroyCon", 						++PrivateCounter.COUNTER  ),
	LOG_SRC_CON_NL 					("CONNL", 								++PrivateCounter.COUNTER  ),
	LOG_SRC_CHN_NL 					("CHNNL", 								++PrivateCounter.COUNTER  ),
	LOG_SRC_CHN_L 					("CHNL", 								++PrivateCounter.COUNTER  ),
	LOG_SRC_CON_L 					("CONL", 								++PrivateCounter.COUNTER  ),
	LOG_SRC_NIO 					("NIO", 								++PrivateCounter.COUNTER  ),
	LOG_SRC_MQ_BFT				("MQ_BFT", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_MQ 						("MQ", 									++PrivateCounter.COUNTER  ),
	LOG_SRC_MQN  					("MQN",									++PrivateCounter.COUNTER  ),
	LOG_SRC_MATCHER 				("MATCHER", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_OVERLAY_MANAGER 		("OVERLAY_M", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_SUB_MANAGER 			("SUB_M", 								++PrivateCounter.COUNTER  ),
	LOG_SRC_BROKER 					("BROKER", 								++PrivateCounter.COUNTER  ),
	LOG_SRC_BROKER_SHADOW			("BROKER_SDW", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_BROKER_CFG				("BROKER_CFG",							++PrivateCounter.COUNTER  ),
	LOG_SRC_CON_MAN_JOIN 			("CONMANJOIN", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_CON_MAN_PS		 		("CONMANPS", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_CON_MAN_BFT		 		("CONMANBFT", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_CON_MAN_RECOVERY 		("CONMANRECOVERY", 						++PrivateCounter.COUNTER  ),
	LOG_SRC_SESSION_MAN_JOIN 		("SESMANJOIN", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_SESSION_MAN_PS 			("SESMANPS", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_SESSION_MAN_BFT		("SESMANBFT", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_SESSION_MAN_RECOVER 	("SESMANRECOVERY", 						++PrivateCounter.COUNTER  ),
	LOG_SRC_SOCKET_MSG_SENDER 		("MSGSENDER", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_SOCKET_MSG_RECEIVEDER 	("MSGRECVER", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_BROKER_CONTROLLER_CENTRE("BROKERCTRLCNTR", 						++PrivateCounter.COUNTER  ),
	LOG_SRC_BROKER_CONTROLLER 		("BROKERCTRLER", 						++PrivateCounter.COUNTER  ),
	LOG_SRC_NODE_FACTORY 			("NODEFACTORY", 						++PrivateCounter.COUNTER  ),
//	LOG_SRC_PUBLISHER_SUBSCRIBER_TASK("TaskPublisherSubscriber", 			++PrivateCounter.COUNTER  ),
	LOG_SRC_TRANSITION_MAN 			("TransitionMan", 						++PrivateCounter.COUNTER  ),
//	LOG_SRC_FD_TASK 				("FDTask", 								++PrivateCounter.COUNTER  ),
	LOG_SRC_SESSION_REGISTER_MANAGER("S_REG_MAN", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_SUB_MANAGER_COVERING	("SUB_M_C",								++PrivateCounter.COUNTER  ),
	
	LOG_SRC_DUMMY_SOURCE 			("DummyLoggingSource", 					++PrivateCounter.COUNTER  ),
	
	// Add additionanl LoggingSources below (don't exceed LoggingSourceRegistry.
	LOG_SRC_MAINTENANCE_MAN 		("MaintMan",							++PrivateCounter.COUNTER  ),
//	LOG_SRC_GC_TASK 				("GC_TASK",								++PrivateCounter.COUNTER  ),
	LOG_SRC_BTimerTask				("B_TIMER_TASK",						++PrivateCounter.COUNTER  ),
	LOG_SRC_CON_MAN		 			("CONMAN", 								++PrivateCounter.COUNTER  ),
	LOG_SRC_CON_MAN_MP 				("CONMANMP", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_CON_MAN_NC 				("CONMANNC", 							++PrivateCounter.COUNTER  ),
	LOG_SRC_CON_MAN_NC_CLNT			("CONMANNCCLNT", 						++PrivateCounter.COUNTER  ),
	LOG_SRC_SESSION_MAN_MP 			("SESMANPS", 							++PrivateCounter.COUNTER  ),
	
	LOG_SRC_CASUAL_LOGGER			("CASUAL_LOGGER",						++PrivateCounter.COUNTER  ),
	LOG_SRC_CPU_PROFILER 			("CPU_PROFILER",						++PrivateCounter.COUNTER  ),
	LOG_SRC_STATISTICS_LOGGER 		("STATISTICS_LOG",						++PrivateCounter.COUNTER  ),
	LOG_SRC_TRAFFIC_LOGGER 			("TRAFFIC_LOG",							++PrivateCounter.COUNTER  ),
	LOG_SRC_EXECTIME_LOGGER 		("EXECTIME_LOG",						++PrivateCounter.COUNTER  ),
	LOG_SRC_PUBDELIVERY_LOGGER 		("PUB_DELIV_LOG",						++PrivateCounter.COUNTER  ),
	LOG_SRC_PUBGEN_LOGGER	 		("PUB_GEN_LOG",							++PrivateCounter.COUNTER  ),
	LOG_SRC_DEFAULT_SUBSCRIBER 		("DEF_SUBSRIBER",						++PrivateCounter.COUNTER  ),
	
	LOG_SRC_PUB_MP_DELIVERY_INFO 	("PUB_MP_DEL_INFO",						++PrivateCounter.COUNTER  ),
	
	LOG_TRAFFIC_CORRELATOR		 	("TRAFFIC_CORRELATOR",					++PrivateCounter.COUNTER  ),
	
	LOG_SRC_UNKNOWN ("UNKNOWN",												++PrivateCounter.COUNTER  ),
	;
	
	private final String _name;
	private final int _bitPosition;
	
	private LoggingSource(String name, int bitPosition){
		boolean isValid = LoggingSourceRegistry.isValidLoggingSourceBitPosition(bitPosition);
		if ( !isValid )
			throw new IllegalArgumentException("BitPosition '" + bitPosition + "' is an invalid loggingSourceBitPosition.");
		
		_name = name;
		_bitPosition = bitPosition;
	}		
	
	public String toString(){
		return _name; // + "(" + _bitPosition + ")";
	}
	
	public int getBitPoisition(){
		return _bitPosition;
	}
}