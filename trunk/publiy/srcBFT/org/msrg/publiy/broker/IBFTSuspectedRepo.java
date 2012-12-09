package org.msrg.publiy.broker;

import java.net.InetSocketAddress;
import java.util.List;

public interface IBFTSuspectedRepo {

	public void suspect(BFTSuspecionReason reason);

	public boolean isSuspected(String id);
	public boolean isSuspected(InetSocketAddress address);
	
	public List<BFTSuspecionReason> getReasons(IBFTSuspectable suspectable);
	public List<BFTSuspecionReason> getReasons(InetSocketAddress remote);

	public void registerSuspicionListener(IBFTSuspicionListener suspicionListener);
	
}
