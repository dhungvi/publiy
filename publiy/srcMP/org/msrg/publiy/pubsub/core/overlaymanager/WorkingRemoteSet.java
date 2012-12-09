package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;
import java.util.Set;

import org.msrg.publiy.pubsub.core.multipath.IWorkingManager;

public class WorkingRemoteSet {

	private final int _workingVersion;
	private Set<InetSocketAddress> _realRemotes;
	private Set<InetSocketAddress> _matchingRemotes;
	
	public boolean hasRemoteSet() {
		return _realRemotes != null;
	}
	
	public boolean hasMatchingSet() {
		return _matchingRemotes != null;
	}
	
	public WorkingRemoteSet(int workingVersion){
		_workingVersion = workingVersion;
	}
	
	public synchronized void setRealRemotes(Set<InetSocketAddress> realRemotes) {
		if (_realRemotes!=null)
			throw new IllegalStateException();
		
		_realRemotes = realRemotes;
	}

	public synchronized void setMatchingRemotes(Set<InetSocketAddress> matchingRemotes) {
		if (_matchingRemotes!=null)
			throw new IllegalStateException();
		
		_matchingRemotes = matchingRemotes;
	}
	
	public Set<InetSocketAddress> getRealRemotes() {
		return _realRemotes;
	}
	
	public Set<InetSocketAddress> getMatchingRemotes() {
		return _matchingRemotes;
	}
	
	public boolean matchingContains(InetSocketAddress remote) {
		return _matchingRemotes.contains(remote);
	}
	
	public boolean realContains(InetSocketAddress remote) {
		return _realRemotes.contains(remote);
	}
	
	public boolean isValid(IWorkingManager workingManager){
		int workingVersion = workingManager.getWorkingVersion();
		
		if ( workingVersion < _workingVersion )
			throw new IllegalStateException(workingVersion + " vs. " + _workingVersion);
		else if ( workingVersion > _workingVersion )
			return false;
		else
			return true;
	}
	
	@Override
	public String toString() {
		return "<" + _workingVersion + ":" + _matchingRemotes + ":" + _realRemotes + ">";
	}
}
