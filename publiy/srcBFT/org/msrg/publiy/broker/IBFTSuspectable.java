package org.msrg.publiy.broker;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

public interface IBFTSuspectable {

	public InetSocketAddress getAddress();
	
	public boolean isSuspected();
	public List<BFTSuspecionReason> getReasons();
	public Set<InetSocketAddress> getAffectedRemotes();
	public boolean addAffectedRemote(InetSocketAddress remote);
	public boolean remoteIsAffected(InetSocketAddress remote);
	
}
