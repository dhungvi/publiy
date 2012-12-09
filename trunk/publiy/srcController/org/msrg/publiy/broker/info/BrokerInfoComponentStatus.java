package org.msrg.publiy.broker.info;

import org.msrg.publiy.component.ComponentStatus;

public class BrokerInfoComponentStatus extends IBrokerInfo {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 1L;

	private final ComponentStatus _componentStatus;
	
	public BrokerInfoComponentStatus(ComponentStatus componentStatus) {
		super(BrokerInfoTypes.BROKER_INFO_COMPONENT_STATUS);
		_componentStatus = componentStatus;
	}

	@Override
	protected String toStringPrivately() {
		return _componentStatus.toString();
	}

}
