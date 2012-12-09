package org.msrg.publiy.broker;

import org.msrg.publiy.broker.info.IBrokerInfo;

public interface IHasBrokerInfo<T extends IBrokerInfo> {

	T getInfo();
	
}
