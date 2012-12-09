package org.msrg.publiy.broker.core.flowManager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class DummyFlow {
	
	public final InetSocketAddress _remote;
	public final Sequence _contentSequence;
	
	DummyFlow(InetSocketAddress remote, Sequence contentSequence) {
		_remote = remote;
		_contentSequence = contentSequence;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(getClass().isAssignableFrom(obj.getClass()))
			throw new IllegalStateException("Cannot compare dummy flows");
		
		if(!Flow.class.isAssignableFrom(obj.getClass()))
			return false;
		
		Flow flowObj = (Flow) obj;
		return (flowObj.getRemoteAddress().equals(_remote) &&
				flowObj.getContentSequence().equalsExact(_contentSequence));
	}
	
	@Override
	public int hashCode() {
		return _remote.hashCode() + _contentSequence.hashCodeExact();
	}
}
