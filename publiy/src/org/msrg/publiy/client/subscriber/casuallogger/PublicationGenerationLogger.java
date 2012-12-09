package org.msrg.publiy.client.subscriber.casuallogger;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;

import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.AbstractCasualLogger;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class PublicationGenerationLogger extends AbstractCasualLogger {
	@SuppressWarnings("unused")
	private long _lastWrite = SystemTime.currentTimeMillis();
	public boolean _compact = false; 
	public static final int SAMPLING_INTERVAL = 1000;
	public static final int FORCE_FILE_FLUSH = 5;
	private static PublicationGenerationLogger _publicationGenerationLoggerInstance = null;

	private IBrokerShadow _broker;
	private String _publicationGenerationLogFilename;
	
	protected final Object _listLock = new Object();
	private List<PublicationGenerationEntry> _mainPubGenerationList = new LinkedList<PublicationGenerationEntry>();
	private Map<Sequence, TMulticast_Publish> _receivedPublications = (Broker.RELEASE?null:new HashMap<Sequence, TMulticast_Publish>());
	
	private PublicationGenerationLogger(IBrokerShadow broker){
		_broker = broker;
		_publicationGenerationLogFilename = _broker.getPublicationGenerationLogFilename();
	}
	
	public static PublicationGenerationLogger getExistingPublicationGenerationLoggerInstance(){
		return _publicationGenerationLoggerInstance;
	}
	
	public static PublicationGenerationLogger getPublicationGenerationLoggerInstance(IBrokerShadow broker){
		if ( _publicationGenerationLoggerInstance == null )
			_publicationGenerationLoggerInstance = new PublicationGenerationLogger(broker);
		
		if ( !_publicationGenerationLoggerInstance._publicationGenerationLogFilename.equalsIgnoreCase(broker.getPublicationGenerationLogFilename()) )
			throw new IllegalStateException("Cannot change Logfile name :(");
		
		return _publicationGenerationLoggerInstance;
	}
	
	@Override
	protected void runMe() throws IOException {
		runMe(_logFileWriter);
	}
	
	protected void runMe(Writer ioWriter) throws IOException {
		List<PublicationGenerationEntry> busyList;
		synchronized (_listLock) {
			busyList = _mainPubGenerationList;
			_mainPubGenerationList = new LinkedList<PublicationGenerationEntry>();
		}
		
		synchronized (_lock) {
			if ( _firstTime ){
				writeHeader(ioWriter);
				_firstTime = false;
			}
		
			for (PublicationGenerationEntry pubGenerationEntry : busyList) {
				pubGenerationEntry.write(ioWriter);
				ioWriter.append('\n');
			}
		
			busyList.clear();
			_lastWrite = SystemTime.currentTimeMillis();
		}
	}
	
	public void publicationGenerated(TMulticast_Publish[] tmps) {
		for(TMulticast_Publish tmp : tmps) 
			publicationGenerated(tmp);
	}
	
	public void publicationGenerated(Sequence sourceSequence, TMulticastTypes tmType) {
		if(!_broker.canGenerateMessages())
			return;
		
		synchronized (_listLock) {
			_mainPubGenerationList.add(new PublicationGenerationEntry(sourceSequence, tmType));
		}
	}
	
	public void publicationGenerated(TMulticast_Publish tmp) {
		if(!_broker.canGenerateMessages())
			return;
		
		if (tmp==null)
			return;
		
		synchronized (_listLock) {
			_mainPubGenerationList.add(new PublicationGenerationEntry(tmp));
		}

		if(_receivedPublications!=null) {
			Sequence sourceSequence = tmp.getSourceSequence();
			synchronized (_receivedPublications) {
				if (!Broker.RELEASE) {
					TMulticast_Publish existingPublication = _receivedPublications.put(sourceSequence, tmp);
					if (existingPublication != null)
						LoggerFactory.getLogger().warn(this, "DUPLICATES: " + existingPublication + " vs. " + tmp);
				}
			}
		}
	}
	
	protected void writeHeader(Writer ioWriter) throws IOException {
		ioWriter.append(PublicationGenerationEntry.getFormat() + "\n");
	}
		
	@Override
	public boolean isEnabled() {
		return super.isEnabled();
	}

	@Override
	protected String getFileName() {
		return _publicationGenerationLogFilename;
	}

	@Override
	public String toString() {
		return "PublicationGenerationLogger-" + _broker.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected int getDefaultLoggingInterval(){
		return SAMPLING_INTERVAL;
	}

	public LoggingSource getLogSource(){
		return LoggingSource.LOG_SRC_PUBGEN_LOGGER;
	}
}
