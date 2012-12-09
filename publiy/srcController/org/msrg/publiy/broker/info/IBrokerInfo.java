package org.msrg.publiy.broker.info;

import java.io.Serializable;

public abstract class IBrokerInfo implements Serializable {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 1892522734889758654L;
	final BrokerInfoTypes _bInfoType;
	IBrokerInfo(BrokerInfoTypes bInfoType){
		_bInfoType = bInfoType;
	}
	
	public final BrokerInfoTypes getBrokerInfoType(){
		return _bInfoType;
	}
	
	public final String toString(){
		return _bInfoType.toString() + "::" + toStringPrivately();
	}
	
	protected abstract String toStringPrivately();
}
