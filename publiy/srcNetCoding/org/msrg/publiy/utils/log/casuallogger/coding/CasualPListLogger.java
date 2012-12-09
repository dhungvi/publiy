package org.msrg.publiy.utils.log.casuallogger.coding;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.utils.log.casuallogger.AbstractCasualLogger;

public class CasualPListLogger extends AbstractCasualLogger {

	public static final int SAMPLING_INTERVAL = 1000;
	
	protected final IBrokerShadow _brokerShadow;
	protected final String _contentLogFilename;
	protected final List<PListLogEvent> _plistLogEvents =
		new LinkedList<PListLogEvent>();
	
	public CasualPListLogger(BrokerShadow brokerShadow){
		brokerShadow.setCasualPListLogger(this);
		_brokerShadow = brokerShadow;
		if(_brokerShadow == null) {
			_contentLogFilename = null;
			_problem = true;
		} else { 
			_contentLogFilename = _brokerShadow.getContentFilename();
			if(_contentLogFilename == null)
				throw new NullPointerException();
		}
	}
	
	@Override
	protected String getFileName() {
		return _brokerShadow.getPListLogFilename();
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
			
			for(PListLogEvent plistLogEvent: _plistLogEvents)
				stringWriter.append(" " + plistLogEvent.toString());
			
			_plistLogEvents.clear();
			
			_logFileWriter.write(stringWriter.toString());
			_logFileWriter.write('\n');
		}
	}

	@Override
	public String toString() {
		return "ContentLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	public void plistLaunchClientRequest(InetSocketAddress remote, Sequence sourceSequence) {
		synchronized(_lock) {
			_plistLogEvents.add(
					new PListLogEvent_PListLaunchClientRequest(
							remote, sourceSequence));
		}
	}
	
	public void plistNonLaunchClientRequest(InetSocketAddress remote, Sequence sourceSequence) {
		synchronized(_lock) {
			_plistLogEvents.add(
					new PListLogEvent_PListNonLaunchClientRequest(
							remote, sourceSequence));
		}
	}
	
	public void plistBrokerRequest(InetSocketAddress remote, Sequence sourceSequence) {
		synchronized(_lock) {
			_plistLogEvents.add(
					new PListLogEvent_PListBrokerRequest(
							remote, sourceSequence));
		}
	}
	
	public void plistNonLaunchClientReply(
			InetSocketAddress remote, Sequence sourceSequence,
			Collection<InetSocketAddress> localReplies, Collection<InetSocketAddress> launchReplies) {
		synchronized(_lock) {
			_plistLogEvents.add(
					new PListLogEvent_PListNonLaunchClientReply(
							remote, sourceSequence, localReplies, launchReplies));
		}
	}
	
	public void plistLaunchClientReply(
			InetSocketAddress remote, Sequence sourceSequence,
			Collection<InetSocketAddress> localReplies, Collection<InetSocketAddress> launchReplies) {
		synchronized(_lock) {
			_plistLogEvents.add(
					new PListLogEvent_PListLaunchClientReply(
							remote, sourceSequence, localReplies, launchReplies));
		}
	}

	public void plistBrokerReply(
			InetSocketAddress remote, Sequence sourceSequence,
			Collection<InetSocketAddress> localReplies, Collection<InetSocketAddress> crossReplies) {
		synchronized(_lock) {
			_plistLogEvents.add(
					new PListLogEvent_PListBrokerReply(
							remote, sourceSequence, localReplies, crossReplies));
		}
	}

	public void plistBrokerReplyReceived(
			InetSocketAddress remote, Sequence sourceSequence,
			Collection<InetSocketAddress> localReplies, Collection<InetSocketAddress> crossReplies) {
		synchronized(_lock) {
			_plistLogEvents.add(
					new PListLogEvent_PListBrokerReplyReceived(
							remote, sourceSequence, localReplies, crossReplies));
		}
	}

	@Override
	protected int getDefaultLoggingInterval(){
		return SAMPLING_INTERVAL;
	}
}
