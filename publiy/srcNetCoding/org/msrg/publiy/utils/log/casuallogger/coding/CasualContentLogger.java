package org.msrg.publiy.utils.log.casuallogger.coding;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.utils.log.casuallogger.AbstractCasualLogger;

public class CasualContentLogger extends AbstractCasualLogger {

	public static final int SAMPLING_INTERVAL = 1000;
	
	protected final IBrokerShadow _brokerShadow;
	protected final String _contentLogFilename;
	protected final List<ContentLogEvent> _contentLogEvents =
		new LinkedList<ContentLogEvent>();
	
	public CasualContentLogger(BrokerShadow brokerShadow) {
		brokerShadow.setCasualContentLogger(this);
		_brokerShadow = brokerShadow;
		_contentLogFilename = _brokerShadow.getContentFilename();
		if(_contentLogFilename == null)
			throw new NullPointerException();
	}
	
	@Override
	protected String getFileName() {
		return _brokerShadow.getContentFilename();
	}

	@Override
	protected void runMe() throws IOException {
		if(_problem)
			return;
		
		synchronized (_lock) {
			if(_firstTime){
				String headerLine = "#TIME LOGENTRY \n";
				_logFileWriter.write(headerLine);
				_firstTime = false;
			}
			
			StringWriter stringWriter = new StringWriter();
			stringWriter.append(printSkippedTimes());
			stringWriter.append(BrokerInternalTimer.read().toString());
			
			Map<ContentLogEvent, Integer> oneTimeLogEvents =
				new HashMap<ContentLogEvent, Integer>();
			for(ContentLogEvent contentLogEvent: _contentLogEvents) {
				if(!contentLogEvent._canBeSummarized) {
					stringWriter.append(" " + contentLogEvent.toString());
				} else {
					Integer i = oneTimeLogEvents.get(contentLogEvent);
					if(i== null)
						i=0;
					
					oneTimeLogEvents.put(contentLogEvent, i + 1);
				}
			}
			
			for(Entry<ContentLogEvent, Integer> oneTimeLogEvent
					: oneTimeLogEvents.entrySet())
				stringWriter.append(
						" " + oneTimeLogEvent.getKey()
						+ ">" + oneTimeLogEvent.getValue());
		
			_contentLogEvents.clear();
			
			_logFileWriter.write(stringWriter.toString());
			_logFileWriter.write('\n');
		}
	}

	@Override
	public String toString() {
		return "ContentLogger-" + _brokerShadow.getBrokerID()
				+ "[" + _problem + "_" + _initialized + "]";
	}

	public void contentPieceDependant(Sequence contentSequence) {
		synchronized(_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentPieceDependant(
							contentSequence));
		}
	}
	
	public void contentPublished(Sequence contentSequence) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentPublished(contentSequence));
		}
	}
	
	public void contentFailed(Sequence contentSequence) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentFailed(contentSequence));
		}
	}
	
	public void contentDecoded(Sequence contentSequence) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentDecoded(contentSequence));
		}
	}
	
	public void contentPlistReceived(Sequence contentSequence,
			InetSocketAddress remoteBroker, boolean launch) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentPlistReceived(
							contentSequence, remoteBroker, launch));
		}
	}
	
	public void contentPlistRequested(Sequence contentSequence,
			InetSocketAddress remoteBroker) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentPlistRequested(
							contentSequence, remoteBroker));
		}
	}
	
	public void contentBreakReceived(Sequence contentSequence,
			InetSocketAddress remote, int breakSize) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentBreakReceived(
							contentSequence, remote, breakSize));
		}
	}

	public void contentBreakDeclineReceived(Sequence contentSequence,
			InetSocketAddress remote, int outstanding) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentBreakDeclineReceived(
							contentSequence, remote, outstanding));
		}
	}
	
	public void contentBreakDeclineSent(Sequence contentSequence,
			InetSocketAddress remote, int outstanding) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentBreakDeclineSent(
							contentSequence, remote, outstanding));
		}
	}
	
	public void contentBreakRequested(Sequence contentSequence,
			InetSocketAddress remote, int breakSize) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentBreakRequested(
							contentSequence, remote, breakSize));
		}
	}

	public void contentServed(Sequence contentSequence) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentServed(contentSequence));
		}
	}
	
	public void contentWatched(Sequence contentSequence) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentWatched(contentSequence));
		}
	}
	
	public void contentUnWatched(Sequence contentSequence) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentUnWatched(contentSequence));
		}
	}
	
	public void contentUnServed(Sequence contentSequence) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentUnServed(contentSequence));
		}
	}
	
	public void contentCodedPieceReceivedAfterInverse(Sequence contentSequence,
			InetSocketAddress sender, boolean fromMainSeed) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentPieceReceivedAfterInverse(
							contentSequence, sender, fromMainSeed));
		}
	}

	public void contentCodedPieceDiscarded(Sequence contentSequence,
			InetSocketAddress sender, int err) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentPieceDiscarded(
							contentSequence, sender, err));
		}
	}

	public void contentCodedPieceReceived(Sequence contentSequence,
			InetSocketAddress sender, boolean fromMainSeed) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentPieceReceived(
							contentSequence, sender, fromMainSeed));
		}
	}
	
	public void processingIncompleteWatchList(int watchSize) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_WatchSize(watchSize));
		}

	}
	
	public void contentCodedPieceSent(Sequence contentSequence,
			InetSocketAddress receiver, boolean fromMainSeed) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentPieceSent(
							contentSequence, receiver, fromMainSeed));
		}
	}
	
	public void contentFlowAdded(Sequence contentSequence,
			InetSocketAddress remote) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentFlowAdded(
							contentSequence, remote));
		}
	}
	
	public void contentFlowRemoved(Sequence contentSequence,
			InetSocketAddress remote) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentFlowRemoved(
							contentSequence, remote));
		}
	}

	
	public void contentStarted(Sequence contentSequence) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentStarted(
							contentSequence));
		}
	}
	
	@Override
	protected int getDefaultLoggingInterval(){
		return SAMPLING_INTERVAL;
	}

	public void contentTookTooLong(Sequence contentSequence,
			Long timeFromStart) {
		synchronized (_lock) {
			_contentLogEvents.add(
					new ContentLogEvent_ContentTookTooLong(
							contentSequence, timeFromStart));
		}

	}
}
