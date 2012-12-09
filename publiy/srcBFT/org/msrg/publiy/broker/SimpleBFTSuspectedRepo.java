package org.msrg.publiy.broker;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class SimpleBFTSuspectedRepo implements IBFTSuspectedRepo {

	protected final IBFTBrokerShadow _bftBrokerShadow;
	protected final BrokerIdentityManager _identityManager;
	protected final Map<InetSocketAddress, List<BFTSuspecionReason>> _suspectedAddresses =
			new HashMap<InetSocketAddress, List<BFTSuspecionReason>>();
	protected final Set<IBFTSuspicionListener> _suspicionListeners =
			new HashSet<IBFTSuspicionListener>();

	public SimpleBFTSuspectedRepo(BFTBrokerShadow bftBrokerShadow) {
		_bftBrokerShadow = bftBrokerShadow;
		bftBrokerShadow.setBFTSuspectedRedpo(this);
		_identityManager = _bftBrokerShadow.getBrokerIdentityManager();
	}

	@Override
	public boolean isSuspected(String id) {
		InetSocketAddress address = _identityManager.getNodeAddress(id);
		return isSuspected(address);
	}

	@Override
	public boolean isSuspected(InetSocketAddress address) {
		List<BFTSuspecionReason> reasons = _suspectedAddresses.get(address);
		return !(reasons == null || reasons.size() == 0);
	}

	@Override
	public void suspect(BFTSuspecionReason reason) {
		BrokerInternalTimer.inform("SUSPECT: " + reason);
		IBFTSuspectable suspectable = reason._suspectable;
		InetSocketAddress address = suspectable.getAddress();
		List<BFTSuspecionReason> l = _suspectedAddresses.get(address);
		boolean informListeners = false;
		if(l == null) {
			_suspectedAddresses.put(address, l=new LinkedList<BFTSuspecionReason>());
			
			// Only inform listeners for the first time a node is suspected.
			informListeners = true;
		}
		
		l.add(reason);
		if(informListeners)
			informListeners(reason);
	}

	@Override
	public List<BFTSuspecionReason> getReasons(IBFTSuspectable suspectable) {
		return _suspectedAddresses.get(suspectable.getAddress());
	}

	@Override
	public List<BFTSuspecionReason> getReasons(InetSocketAddress remote) {
		return _suspectedAddresses.get(remote);
	}
	
	protected final void informListeners(BFTSuspecionReason reason) {
		synchronized(_suspicionListeners) {
			for(IBFTSuspicionListener suspicionListener : _suspicionListeners)
				suspicionListener.nodeSuspected(reason);
		}		
	}
	
	@Override
	public void registerSuspicionListener(IBFTSuspicionListener suspicionListener) {
		synchronized(_suspicionListeners) {
			_suspicionListeners.add(suspicionListener);
		}
	}
}
