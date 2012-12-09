package org.msrg.publiy.broker.info;

import java.io.Serializable;

public enum BrokerInfoTypes implements Serializable {

	BROKER_INFO_OP_STATUS				("INFO_OP_STATUS"),
	BROKER_INFO_COMPONENT_STATUS		("INFO_COMP_STATUS"),
	
	BROKER_INFO_PUBLICATIONS			("INFO_PUB"),
	BROKER_INFO_SUBSCRIPTIONS			("INFO_SUB"),
	BROKER_INFO_JOINS					("INFO_TOP_LINK"),
	BROKER_INFO_SESSIONS				("INFO_SESSION"),
	BROKER_INFO_PS_SESSIONS				("INFO_PSSESSION"),
	
	BROKER_INFO_STATISTICS				("INFO_STAT"),
	BROKER_INFO_EXCEPTIONS				("INFO_EXCEPTION"),
	BROKER_INFO_ERROR					("INFO_ERROR");
	
	private final String _name;
	
	private BrokerInfoTypes(String name){
		_name = name;
	}
	
	public String toString(){
		return _name;
	}
}
