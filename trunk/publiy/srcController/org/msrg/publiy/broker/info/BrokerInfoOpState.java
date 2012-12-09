package org.msrg.publiy.broker.info;

import org.msrg.publiy.broker.BrokerOpState;

public class BrokerInfoOpState extends IBrokerInfo  {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = -46446499085190767L;
	
	private final BrokerOpState _bOpState;
	
	public BrokerInfoOpState(BrokerOpState bOpState){
		super(BrokerInfoTypes.BROKER_INFO_OP_STATUS);
		_bOpState = bOpState;
	}

	public BrokerOpState getBrokerOpState(){
		return _bOpState;
	}
	
	@Override
	protected String toStringPrivately() {
		return _bOpState.toString();
	}
	
}
