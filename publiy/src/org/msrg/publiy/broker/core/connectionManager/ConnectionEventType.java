package org.msrg.publiy.broker.core.connectionManager;

public enum ConnectionEventType {

	CONNECTION_EVENT_SEND_TPING,
	CONNECTION_EVENT_SEND_DACK,
	CONNECTION_EVENT_PURGE_MQ,
	CONNECTION_EVENT_SEND_LOAD_WEIGHT,
	
	CONNECTION_EVENT_STATE_CHANGE,
	
	CONNECTION_EVENT_READY_TO_READ,
	CONNECTION_EVENT_READY_TO_WRITE,
	CONNECTION_EVENT_RECENTLY_UPDATED,
	CONNECTION_EVENT_LOCAL_OUTGOING,
	CONNECTION_EVENT_FAST_TCOMMAND_PROCESSING,
	CONNECTION_EVENT_RENEW_CONNECTION,
	CONNECTION_EVENT_REGISTER_SESSION,
	CONNECTION_EVENT_FAILED_CONNECTION,
	
	CONNECTION_EVENT_MAINTENANCE_FORCE_CONF_ACK,
	
	CONNECTION_EVENT_CANDIDATES,
	CONNECTION_EVENT_LOAD_PREPARED_SUBSCRIPTIONS_FILE,
	
	
	CONNECTION_EVENT_PROCESS_NEXT_NETCODING_FLOW,
	CONNECTION_EVENT_ADD_CONTENT_FLOW,
	CONNECTION_EVENT_PROCESS_INCOMPLETE_BREAKED_WATCH_LIST,
	CONNECTION_EVENT_PROCESS_INCOMPLETE_UNBREAKED_WATCH_LIST,
	CONNECTION_EVENT_PROCESS_SERVE_QUEUE,
	
	CONNECTION_EVENT_CODED_CONTENT_READY,
	CONNECTION_EVENT_DECODED_CONTENT_READY,
	CONNECTION_EVENT_CONTENT_INVERSED,
	CONNECTION_EVENT_PUBLISH_CONTENT,
	
	CONNECTION_EVENT_SEND_PLIST_REQUESTS,
	CONNECTION_EVENT_SEND_CONTENTID_REQ,

	CONNECTION_EVENT_BFT_DACK_SEND,
	CONNECTION_EVENT_BFT_DACK_RECEIVE,
	
	CONNECTION_EVENT_NODE_SUSPECTED;
	
}
