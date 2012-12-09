package org.msrg.publiy.utils.log.casuallogger.coding;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimerReading;

abstract class ContentLogEvent {
	
	public final boolean _canBeSummarized;
	public final Sequence _contentSequence;
	public final ContentLogEventType _type;
	public final BrokerInternalTimerReading _creationTime;

	protected ContentLogEvent(Sequence contentSequence, ContentLogEventType type, boolean canBeSummarized) {
		_contentSequence = contentSequence;
		_type = type;
		_canBeSummarized = canBeSummarized;
		_creationTime = BrokerInternalTimer.read();
	}
	
	public String toString(String prefix) {
		return _type + prefix + (_contentSequence==null?"":":" + _contentSequence.toStringVeryVeryShort());
	}
	
	@Override
	public String toString() {
		return toString("");
	}
	
	@Override
	public int hashCode() {
		return (_contentSequence==null?_type.hashCode():_contentSequence.hashCode());
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		ContentLogEvent logEventObj = (ContentLogEvent) obj;
		return _type == logEventObj._type &&
			(_contentSequence==null?logEventObj._contentSequence==null:_contentSequence.equals(logEventObj._contentSequence));
	}
}


class ContentLogEvent_ContentPublished extends ContentLogEvent {
	protected ContentLogEvent_ContentPublished(
			Sequence contentSequence) {
		super(contentSequence, ContentLogEventType.CONTENT_PUBLISHED, false);
	}
}

class ContentLogEvent_ContentPieceDependant extends ContentLogEvent {
	protected ContentLogEvent_ContentPieceDependant(
			Sequence contentSequence) {
		super(contentSequence, ContentLogEventType.CONTENT_PIECE_DEPENDANT, false);
	}
}

class ContentLogEvent_ContentServed extends ContentLogEvent {
	protected ContentLogEvent_ContentServed(
			Sequence contentSequence) {
		super(contentSequence, ContentLogEventType.CONTENT_SERVED, false);
	}
}

class ContentLogEvent_ContentUnServed extends ContentLogEvent {
	protected ContentLogEvent_ContentUnServed(
			Sequence contentSequence) {
		super(contentSequence, ContentLogEventType.CONTENT_UNSERVED, false);
	}
}

class ContentLogEvent_ContentUnWatched extends ContentLogEvent {
	protected ContentLogEvent_ContentUnWatched(
			Sequence contentSequence) {
		super(contentSequence, ContentLogEventType.CONTENT_UNWATCHED, false);
	}
}

class ContentLogEvent_ContentWatched extends ContentLogEvent {
	protected ContentLogEvent_ContentWatched(
			Sequence contentSequence) {
		super(contentSequence, ContentLogEventType.CONTENT_WATCHED, false);
	}
}

class ContentLogEvent_ContentStarted extends ContentLogEvent {
	protected ContentLogEvent_ContentStarted(
			Sequence contentSequence) {
		super(contentSequence, ContentLogEventType.CONTENT_STARTED, false);
	}
}

class ContentLogEvent_ContentFailed extends ContentLogEvent {
	protected ContentLogEvent_ContentFailed(
			Sequence contentSequence) {
		super(contentSequence, ContentLogEventType.CONTENT_FAILED, false);
	}
}

class ContentLogEvent_ContentDecoded extends ContentLogEvent {
	protected ContentLogEvent_ContentDecoded(
			Sequence contentSequence) {
		super(contentSequence, ContentLogEventType.CONTENT_DECODED, false);
	}
}

class ContentLogEvent_ContentBreakReceived extends ContentLogEvent {

	public final InetSocketAddress _remote;
	public final int _breakSize;
	
	protected ContentLogEvent_ContentBreakReceived(
			Sequence contentSequence, InetSocketAddress remote, int breakSize) {
		super(contentSequence, ContentLogEventType.CONTENT_BREAK_RECEIVED, false);
		
		_remote = remote;
		_breakSize = breakSize;
	}
	
	@Override
	public String toString() {
		return super.toString() + ":" + _remote.getPort() + ">" + _breakSize;
	}
}

class ContentLogEvent_ContentBreakDeclineReceived extends ContentLogEvent {

	public final InetSocketAddress _remote;
	public final int _outstanding;
	
	protected ContentLogEvent_ContentBreakDeclineReceived(
			Sequence contentSequence, InetSocketAddress remote, int outstanding) {
		super(contentSequence, ContentLogEventType.CONTENT_BREAK_DECLINE_RECEIVED, false);
		
		_remote = remote;
		_outstanding = outstanding;
	}
	
	@Override
	public String toString() {
		return super.toString() + ":" + _remote.getPort() + ">" + _outstanding;
	}
}

class ContentLogEvent_ContentBreakDeclineSent extends ContentLogEvent {

	public final InetSocketAddress _remote;
	public final int _outstanding;
	
	protected ContentLogEvent_ContentBreakDeclineSent(
			Sequence contentSequence, InetSocketAddress remote, int outstanding) {
		super(contentSequence, ContentLogEventType.CONTENT_BREAK_DECLINE_SENT, false);
		
		_remote = remote;
		_outstanding = outstanding;
	}
	
	@Override
	public String toString() {
		return super.toString() + ":" + _remote.getPort() + ">" + _outstanding;
	}
}

class ContentLogEvent_ContentBreakRequested extends ContentLogEvent {

	public final InetSocketAddress _remote;
	public final int _breakSize;
	
	protected ContentLogEvent_ContentBreakRequested(
			Sequence contentSequence, InetSocketAddress remote, int breakSize) {
		super(contentSequence, ContentLogEventType.CONTENT_BREAK_REQUESTED, false);
		
		_remote = remote;
		_breakSize = breakSize;
	}
	
	@Override
	public String toString() {
		return super.toString() + ":" + _remote.getPort() + ">" + _breakSize;
	}
}

class ContentLogEvent_ContentPlistRequested extends ContentLogEvent {

	public final InetSocketAddress _remote;
	
	protected ContentLogEvent_ContentPlistRequested(
			Sequence contentSequence, InetSocketAddress remoteBroker) {
		super(contentSequence, ContentLogEventType.CONTENT_PLIST_REQUSTED, false);
		
		_remote = remoteBroker;
	}
	
	@Override
	public String toString() {
		return super.toString() + ":" + _remote.getPort();
	}
}

class ContentLogEvent_ContentPieceDiscarded extends ContentLogEvent {

	public final InetSocketAddress _remote;
	public final int _err;
	
	protected ContentLogEvent_ContentPieceDiscarded(
			Sequence contentSequence, InetSocketAddress remoteBroker, int err) {
		super(contentSequence, ContentLogEventType.CONTENT_PIECED_DISCARDED, false);
		
		_remote = remoteBroker;
		_err = err;
	}
	
	@Override
	public String toString() {
		return super.toString() + ":" + _remote.getPort() + ">" + _err;
	}
}

class ContentLogEvent_ContentPlistReceived extends ContentLogEvent {

	public final InetSocketAddress _remote;
	public final boolean _launch;
	
	protected ContentLogEvent_ContentPlistReceived(
			Sequence contentSequence, InetSocketAddress remoteBroker,
			boolean launch) {
		super(contentSequence, ContentLogEventType.CONTENT_PLIST_RECEIVED, false);
		
		_remote = remoteBroker;
		_launch = launch;
	}
	
	@Override
	public String toString() {
		return super.toString() + (_launch ? "*:" : ":") + _remote.getPort();
	}
}

class ContentLogEvent_ContentFlowRemoved extends ContentLogEvent {

	public final InetSocketAddress _remote;
	
	protected ContentLogEvent_ContentFlowRemoved(
			Sequence contentSequence, InetSocketAddress remote) {
		super(contentSequence,
				ContentLogEventType.CONTENT_FLOW_REMOVED, false);
		
		_remote = remote;
	}
	
	@Override
	public String toString() {
		return super.toString() + ":" + _remote.getPort();
	}
}


class ContentLogEvent_ContentFlowAdded extends ContentLogEvent {

	public final InetSocketAddress _remote;
	
	protected ContentLogEvent_ContentFlowAdded(
			Sequence contentSequence, InetSocketAddress remote) {
		super(contentSequence, ContentLogEventType.CONTENT_FLOW_ADDED, false);
		
		_remote = remote;
	}
	
	@Override
	public String toString() {
		return super.toString() + ":" + _remote.getPort();
	}
}

class ContentLogEvent_ContentPieceReceived extends ContentLogEvent {
	
	public final InetSocketAddress _remote;
	public final boolean _fromMainSeed;
	
	protected ContentLogEvent_ContentPieceReceived(ContentLogEventType type,
			Sequence contentSequence, InetSocketAddress remote,
			boolean fromMainSeed) {
		super(contentSequence, type, true);
		
		_remote = remote;
		_fromMainSeed = fromMainSeed;
	}
	
	protected ContentLogEvent_ContentPieceReceived(Sequence contentSequence,
			InetSocketAddress remote, boolean fromMainSeed) {
		this(ContentLogEventType.CONTENT_PIECE_RECEIVED,
				contentSequence, remote, fromMainSeed);
	}
	
	@Override
	public String toString() {
		return super.toString() + (_fromMainSeed?"*:":":") + _remote.getPort();
	}
	
	@Override
	public int hashCode() {
		return _remote.hashCode() + super.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!super.equals(obj))
			return false;
		
		return _remote.equals(
				((ContentLogEvent_ContentPieceReceived)obj)._remote);
	}
}

class ContentLogEvent_ContentPieceReceivedAfterInverse extends
		ContentLogEvent_ContentPieceReceived {

	protected ContentLogEvent_ContentPieceReceivedAfterInverse(
			Sequence contentSequence, InetSocketAddress remote,
			boolean fromMainSeed) {
		super(ContentLogEventType.CONTENT_PIECE_RECEIVED_AFTER_INVERSE,
				contentSequence, remote, fromMainSeed);
	}
	
}


class ContentLogEvent_WatchSize extends ContentLogEvent {
	public final int _watchSize;
	
	protected ContentLogEvent_WatchSize(int watchSize) {
		super(null, ContentLogEventType.CONTENT_WATCHSIZE, false);
		
		_watchSize = watchSize;
	}
	
	@Override
	public String toString() {
		return super.toString() + "[" + _watchSize + "]";
	}
}

class ContentLogEvent_ContentPieceSent extends ContentLogEvent {
	
	public final InetSocketAddress _remote;
	public final boolean _fromMainSeed;
	
	protected ContentLogEvent_ContentPieceSent(Sequence contentSequence,
			InetSocketAddress remote, boolean fromMainSeed) {
		super(contentSequence, ContentLogEventType.CONTENT_PIECE_SENT, true);
		
		_remote = remote;
		_fromMainSeed = fromMainSeed;
	}
	
	@Override
	public String toString() {
		return super.toString() + (_fromMainSeed?"*:":":") + _remote.getPort();
	}
	
	@Override
	public int hashCode() {
		return _remote.hashCode() + super.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!super.equals(obj))
			return false;
		
		return _remote.equals(
				((ContentLogEvent_ContentPieceSent)obj)._remote);
	}
}

class ContentLogEvent_ContentTookTooLong extends ContentLogEvent {
	final long _timeFromStart;
	
	protected ContentLogEvent_ContentTookTooLong(
			Sequence contentSequence, long timeFromStart) {
		super(contentSequence, ContentLogEventType.CONTENT_TOO_LONG, false);
		
		_timeFromStart = timeFromStart;
	}
	
	@Override
	public String toString() {
		return super.toString() + '>' + _timeFromStart;
	}
}