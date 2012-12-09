package org.msrg.publiy.broker.core;

import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBFTSuspectedRepo;

public interface IBFTConnectionManager extends IConnectionManagerPS {

	@Override
	public IBFTBrokerShadow getBrokerShadow();

	public IBFTSuspectedRepo getSuspicionRepo();

}
