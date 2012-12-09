package org.msrg.publiy.gui.failuretimeline;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.broker.BrokerIdentityManager;


import org.msrg.publiy.communication.core.packet.types.TBFTMessageManipulator;

public class ExtendedFailureTimelineEvents {

	protected final Set<InetSocketAddress> _affectRemotes =
			new HashSet<InetSocketAddress>();
	protected final Set<InetSocketAddress> _affectSources =
			new HashSet<InetSocketAddress>();
	protected final Set<InetSocketAddress> _affectSubscribers =
			new HashSet<InetSocketAddress>();
	
	protected final TBFTMessageManipulator _bftManipulator;
			
	public ExtendedFailureTimelineEvents(String str, BrokerIdentityManager idMan) {
		_bftManipulator = TBFTMessageManipulator.decode(str, idMan);
	}
	
	public TBFTMessageManipulator getBFTMessageManipulator() {
		return _bftManipulator;
	}

	public ExtendedFailureTimelineEvents addAffectRemote(InetSocketAddress remote) {
		_affectRemotes.add(remote);
		return this;
	}

	public ExtendedFailureTimelineEvents addAffectSource(InetSocketAddress remote) {
		_affectSources.add(remote);
		return this;
	}

	public ExtendedFailureTimelineEvents addAffectSubscribers(InetSocketAddress remote) {
		_affectSubscribers.add(remote);
		return this;
	}
}
