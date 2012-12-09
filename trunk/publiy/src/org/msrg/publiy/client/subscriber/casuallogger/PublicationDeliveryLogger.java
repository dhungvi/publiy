package org.msrg.publiy.client.subscriber.casuallogger;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;

import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.AbstractCasualLogger;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class PublicationDeliveryLogger extends AbstractCasualLogger {
	@SuppressWarnings("unused")
	private long _lastWrite = SystemTime.currentTimeMillis();
	public boolean _compact = false; 
	public static final int SAMPLING_INTERVAL = 1000;
	public static final int FORCE_FILE_FLUSH = 5;
	private static PublicationDeliveryLogger _publicationDeliveryLoggerInstance = null;

	private IBrokerShadow _broker;
	private String _publicationDeliveryLogFilename;
	
	protected final Object _listLock = new Object();
	private List<PublicationDeliveryEntry> _mainPubDeliveryList = new LinkedList<PublicationDeliveryEntry>();
	private Map<Sequence, TMulticast_Publish> _receivedPublications = (Broker.RELEASE?null:new HashMap<Sequence, TMulticast_Publish>());
	
	private PublicationDeliveryLogger(IBrokerShadow broker){
		_broker = broker;
		_publicationDeliveryLogFilename = _broker.getPublicationDeliveryLogFilename();
	}
	
	public static PublicationDeliveryLogger getExistingPublicationDeliveryLoggerInstance(){
		return _publicationDeliveryLoggerInstance;
	}
	
	public static PublicationDeliveryLogger getPublicationDeliveryLoggerInstance(IBrokerShadow broker){
		if ( _publicationDeliveryLoggerInstance == null )
			_publicationDeliveryLoggerInstance = new PublicationDeliveryLogger(broker);
		
		if ( !_publicationDeliveryLoggerInstance._publicationDeliveryLogFilename.equalsIgnoreCase(broker.getPublicationDeliveryLogFilename()) )
			throw new IllegalStateException("Cannot change Logfile name :(");
		
		return _publicationDeliveryLoggerInstance;
	}
	
	@Override
	protected void runMe() throws IOException {
		runMe(_logFileWriter);
	}
	
	protected void runMe(Writer ioWriter) throws IOException {
		List<PublicationDeliveryEntry> busyList;
		synchronized (_listLock) {
			busyList = _mainPubDeliveryList;
			_mainPubDeliveryList = new LinkedList<PublicationDeliveryEntry>();
		}
		
		synchronized (_lock) {
			if ( _firstTime ){
				writeHeader(ioWriter);
				_firstTime = false;
			}
		
			for (PublicationDeliveryEntry pubDelivEntry : busyList) {
				pubDelivEntry.write(ioWriter);
				ioWriter.append('\n');
			}
		
			busyList.clear();
			_lastWrite = SystemTime.currentTimeMillis();
		}
	}
	
	public void publicationDelivered(TMulticast_Publish tmp) {
		if(!_broker.canDeliverMessages())
			return;
		
		synchronized (_listLock) {
			_mainPubDeliveryList.add(new PublicationDeliveryEntry(tmp));
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
		ioWriter.append(PublicationDeliveryEntry.getFormat() + "\n");
	}
		
	@Override
	public boolean isEnabled() {
		return super.isEnabled();
	}

	@Override
	protected String getFileName() {
		return _publicationDeliveryLogFilename;
	}

	@Override
	public String toString() {
		return "PublicationDeliveryLogger-" + _broker.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected int getDefaultLoggingInterval(){
		return SAMPLING_INTERVAL;
	}

	public LoggingSource getLogSource(){
		return LoggingSource.LOG_SRC_PUBDELIVERY_LOGGER;
	}
}
