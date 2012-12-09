package org.msrg.publiy.broker.core.plistManager;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.msrg.publiy.utils.SortableNodeAddress;
import org.msrg.publiy.utils.SortableNodeAddressSet;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class PList {

	public static final int _CROSS_CLIENT_REFRENCE_COUNT = 3;

	public final Sequence _contentSequence;
	public final int _rows;
	public final int _cols;
	
	final InetSocketAddress _sourceAddress;
	final InetSocketAddress _homeBroker;
	private final Set<InetSocketAddress> _localClients =
		new HashSet<InetSocketAddress>();
	private final Set<InetSocketAddress> _launchClients =
		new HashSet<InetSocketAddress>();
	private final Set<InetSocketAddress> _completedLaunchClients =
		new HashSet<InetSocketAddress>();
	final List<InetSocketAddress> _requesters =
		new LinkedList<InetSocketAddress>();
	
	protected PList(Sequence contentSequence,
			int rows, int cols, InetSocketAddress sourceAddress,
			InetSocketAddress homeBroker) {
		_contentSequence = contentSequence;
		_rows = rows;
		_cols = cols;
		_sourceAddress = sourceAddress;
		_homeBroker = homeBroker;
	}
	
	protected void downloadCompleted(InetSocketAddress remote) {
		if(_launchClients.contains(remote))
			_completedLaunchClients.add(remote);
		
		_localClients.remove(remote);
	}
	
	public boolean treatAsLaunchClient(InetSocketAddress remote) {
		if(remote.equals(_sourceAddress))
			return true;
		
//		if(_launchClients.contains(remote))
//			if(_launchClients.size() < _completedLaunchClients.size())
//				return true;
		
		return false;
	}
	
	protected Set<InetSocketAddress> getLocalClients(
			SortableNodeAddressSet sortableSet,
			int count, InetSocketAddress requester) {
		return getLocalClients(
				sortableSet, count, requester, _localClients);
	}
	
	protected static Set<InetSocketAddress> getLocalClients(
			SortableNodeAddressSet sortableSet,
			int count, InetSocketAddress requester,
			Set<InetSocketAddress> localClients) {
		Set<InetSocketAddress> ret = new HashSet<InetSocketAddress>();
		SortedSet<SortableNodeAddress> sortedLocalClients =
			sortableSet.convertToSortableSet(localClients);
		
		while(ret.size() < count && !sortedLocalClients.isEmpty()) {
			SortableNodeAddress sortedClient = sortedLocalClients.first();
			sortedLocalClients.remove(sortedClient);
			if(sortedClient.equals(requester))
				continue;
			
			ret.add(sortedClient);
		}
		
		return ret;
	}
	
	public int getLocalClientsSize() {
		return _localClients.size();
	}
	
	protected Set<InetSocketAddress> getLocalClients() {
		return _localClients;
	}
	
	@Deprecated
	protected Set<InetSocketAddress> getCrossClients() {
		return null;
	}
	
	@Deprecated
	public int getCrossClientsSize() {
		return getCrossClients().size();
	}
	
	public void addRequester(InetSocketAddress requester) {
		_requesters.add(requester);
	}
	
	public InetSocketAddress getRequester() {
		if(_requesters.isEmpty())
			return null;
		else
			return _requesters.remove(0);
	}
	
	@Override
	public String toString() {
		return "PList:" + _contentSequence + "," + _localClients + "," + getCrossClients();
	}

	public void loadAllLocalClients(Set<InetSocketAddress> localMatches) {
		_localClients.addAll(localMatches);
		_localClients.remove(_sourceAddress);
	}

	public Set<InetSocketAddress> loadLaunchClients(
			Set<InetSocketAddress> launchClients) {
		_launchClients.addAll(launchClients);
		return _launchClients;
	}
	
	public Set<InetSocketAddress> getLaunchClients() {
		return _launchClients;
	}
}

