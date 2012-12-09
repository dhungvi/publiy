package org.msrg.publiy.utils.log.casuallogger.coding;

import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Collection;

import org.msrg.publiy.broker.core.sequence.Sequence;

public abstract class PListLogEvent { //implements Comparable<PListLogEvent> {
	
	public final PListLogEventTypes _type;
	public final InetSocketAddress _remote;
	public final Sequence _sourceSequence;
	
	private final int _index;
	private static Object LOCK = new Object();
	private static int COUNTER = 0;
	
	protected PListLogEvent(PListLogEventTypes type, InetSocketAddress remote, Sequence sourceSequence) {
		_type = type;
		_remote = remote;
		_sourceSequence = sourceSequence;
		
		synchronized(LOCK) {
			_index = COUNTER++;
		}
	}
	
	@Override
	public String toString() {
		return _type.toString() + ":" + _sourceSequence + ":" + _remote.getPort();
	}

//	@Override
//	public int compareTo(PListLogEvent o) {
//		if(_remote.equals(o._remote))
//			return (_index<o._index ? -1 : +1);
//		else if(_remote.getPort() < o._remote.getPort())
//			return -1;
//		else
//			return 1;
//	}
}

enum PListLogEventTypes {
	PLIST_LAUNCHCLIENT_REQUEST,
	PLIST_NONLAUNCHCLIENT_REQUEST,
	
	PLIST_LAUNCHCLIENT_REPLY,
	PLIST_NONLAUNCHCLIENT_REPLY,
	
	PLIST_ENDED,
	
	PLIST_BROKER_REQUEST,
	PLIST_BROKER_REPLY,
	PLIST_BROKER_REPLY_RECEIVED,
}

class PListLogEvent_PListLaunchClientRequest extends PListLogEvent {
	PListLogEvent_PListLaunchClientRequest(InetSocketAddress remote, Sequence sourceSequence) {
		super(PListLogEventTypes.PLIST_LAUNCHCLIENT_REQUEST, remote, sourceSequence);
	}
}

class PListLogEvent_PListNonLaunchClientRequest extends PListLogEvent {
	PListLogEvent_PListNonLaunchClientRequest(InetSocketAddress remote, Sequence sourceSequence) {
		super(PListLogEventTypes.PLIST_NONLAUNCHCLIENT_REQUEST, remote, sourceSequence);
	}
}

class PListLogEvent_PListBrokerRequest extends PListLogEvent {
	PListLogEvent_PListBrokerRequest(InetSocketAddress remote, Sequence sourceSequence) {
		super(PListLogEventTypes.PLIST_BROKER_REQUEST, remote, sourceSequence);
	}
}

abstract class PListLogEvent_PListReply extends PListLogEvent {
	private String _str;
	
	public final Collection<InetSocketAddress> _localReplies;
	public final Collection<InetSocketAddress> _crossReplies;
	
	PListLogEvent_PListReply(
			PListLogEventTypes type, InetSocketAddress remote,
			Sequence sourceSequence,
			Collection<InetSocketAddress> localReplies, Collection<InetSocketAddress> crossReplies) {
		super(type, remote, sourceSequence);
		
		_localReplies = localReplies;
		_crossReplies = crossReplies;
	}
	
	@Override
	public String toString() {
		if(_str != null)
			return _str;
		
		StringWriter writer = new StringWriter();
		writer.append(super.toString() + ":[");
		boolean first = true;
		if(_localReplies != null) {
			for(InetSocketAddress remote : _localReplies) {
				writer.append((first?"":",") + (remote!=null?remote.getPort():"null"));
				first = false;
			}
		} else {
			writer.append("null");	
		}
		
		writer.append(';');
		
		first = true;
		if(_crossReplies != null) {
			for(InetSocketAddress remote : _crossReplies) {
				writer.append((first?"":",") + (remote!=null?remote.getPort():"null"));
				first = false;
			}
		} else {
			writer.append("null");	
		}
		
		writer.append(']');
		return _str = writer.toString();
	}
}

class PListLogEvent_PListBrokerReplyReceived extends PListLogEvent_PListReply {
	PListLogEvent_PListBrokerReplyReceived(
			InetSocketAddress remote, Sequence sourceSequence,
			Collection<InetSocketAddress> localReplies, Collection<InetSocketAddress> crossReplies) {
		super(PListLogEventTypes.PLIST_BROKER_REPLY_RECEIVED, remote, sourceSequence, localReplies, crossReplies);
	}
}

class PListLogEvent_PListBrokerReply extends PListLogEvent_PListReply {
	PListLogEvent_PListBrokerReply(
			InetSocketAddress remote, Sequence sourceSequence,
			Collection<InetSocketAddress> localReplies, Collection<InetSocketAddress> crossReplies) {
		super(PListLogEventTypes.PLIST_BROKER_REPLY, remote, sourceSequence, localReplies, crossReplies);
	}
}

class PListLogEvent_PListLaunchClientReply extends PListLogEvent_PListReply {
	PListLogEvent_PListLaunchClientReply(
			InetSocketAddress remote, Sequence sourceSequence,
			Collection<InetSocketAddress> localReplies, Collection<InetSocketAddress> launchReplies) {
		super(PListLogEventTypes.PLIST_LAUNCHCLIENT_REPLY, remote, sourceSequence, localReplies, launchReplies);
	}
}

class PListLogEvent_PListNonLaunchClientReply extends PListLogEvent_PListReply {
	PListLogEvent_PListNonLaunchClientReply(
			InetSocketAddress remote, Sequence sourceSequence,
			Collection<InetSocketAddress> localReplies, Collection<InetSocketAddress> launchReplies) {
		super(PListLogEventTypes.PLIST_NONLAUNCHCLIENT_REPLY, remote, sourceSequence, localReplies, launchReplies);
	}
}
