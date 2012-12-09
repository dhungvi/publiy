package org.msrg.publiy.client.subscriber;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastAnnotatorFactory;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.annotations.AnnotationEvent;
import org.msrg.publiy.utils.annotations.IAnnotator;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.PublicationInfo;
import org.msrg.publiy.client.publisher.SimpleFilePublisher;
import org.msrg.publiy.client.subscriber.casuallogger.PublicationDeliveryLogger;

public class DefaultSubscriptionListener implements ISubscriptionListener, ILoggerSource {

	private int _count = 0;
	private final PublicationDeliveryLogger _pubDeliveryLogger;
	private final IBrokerShadow _brokerShadow;
	PublicationInfo _lastReceivedPublication;
	private long _lastPublicationReceiptTime = -1;
	private final Map<String, Integer> _deliveredPublicationCounterPerPublisher = new HashMap<String, Integer>();
	private final Map<String, Long> _lastDeliveryTimesPerPublisher = new HashMap<String, Long>();
	private final Map<String, Publication> _lastPublicationDeliveredPerPublisher = new HashMap<String, Publication>();
	
	private static DefaultSubscriptionListener _instance = null;
	
	public static DefaultSubscriptionListener createInstance(IBrokerShadow brokerShadow) {
		if(_instance == null)
			_instance = new DefaultSubscriptionListener(brokerShadow);
		else
			throw new IllegalStateException("A DefaultSubscriptionListener already exists; use getInstance() instead.");
		
		return _instance;
	}
	
	public static void discardInstance() {
		_instance = null;
	}
	
	public static DefaultSubscriptionListener getInstance() {
		return _instance;
	}
	
	private DefaultSubscriptionListener(IBrokerShadow brokerShadow) {
		_brokerShadow = brokerShadow;
		_pubDeliveryLogger = PublicationDeliveryLogger.getExistingPublicationDeliveryLoggerInstance();
		if(_pubDeliveryLogger == null)
			throw new IllegalStateException("No PublicationDeliveryLogger instance exists.");
	}

	@Override
	public PublicationInfo getLastReceivedPublications() {
		return _lastReceivedPublication;
	}
	
	@Override
	public long getLastPublicationReceiptTime() {
		return _lastPublicationReceiptTime;
	}
	
	@Override
	public PublicationInfo[] getReceivedPublications() {
		return null;
		
		
//		private Map<Sequence, TMulticast_Publish> _receivedPublications = new HashMap<Sequence, TMulticast_Publish>();
//		Publication[] publications;
//		synchronized (_receivedPublications) {
//			publications = _receivedPublications.values().toArray(new Publication[0]);
//			_receivedPublications.clear();
//		}
//
//		PublicationInfo[] pubInfos = new PublicationInfo[publications.length];
//		for ( int i=0 ; i<publications.length ; i++ )
//			pubInfos[i] = new PublicationInfo(publications[i], null);
//		
//		return pubInfos;
	}

	@Override
	public void matchingPublicationDelivered(TMulticast_Publish tmp) {
		_pubDeliveryLogger.publicationDelivered(tmp);
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, "DELIVERED: " + tmp + " ******");
		
		IAnnotator annotator = TMulticastAnnotatorFactory.getTMulticast_Annotator();
		annotator.annotate(_brokerShadow.getLocalSequencer(), tmp, AnnotationEvent.ANNOTATION_EVENT_PUBLICATION_RECEVED, "DefaultSubscriber #" + (_count+1));
		
		annotator.dumpAnnotations(tmp, _brokerShadow.getAnnotationFilename());
		
		Sequence sourceSequence = tmp.getSourceSequence();
		Publication publication = tmp.getPublication();
		matchingPublicationDelivered(sourceSequence, publication);
	}

	@Override
	public void matchingPublicationDelivered(Sequence sourceSequence, Publication publication) {
		StatisticsLogger statLogger = _brokerShadow.getStatisticsLogger();
		if ( statLogger != null )
			statLogger.newDelivery(sourceSequence);
		
		_count++;
//		List<Integer> idList = publication == null ? null : publication.getValue("NODEID");
//		int id = (idList==null || idList.isEmpty() ? -1 : idList.get(0));
//		String nodeId = "n" + id;
		List<String> sourceAddrList  = publication == null ? null : publication.getStringValue(SimpleFilePublisher.getGeneralSourceAddrAttributeName());
		String sourceAddr = (sourceAddrList==null || sourceAddrList.isEmpty() ? "UNKNOWN" : sourceAddrList.get(0));
		
		synchronized(_deliveredPublicationCounterPerPublisher) {
			Integer count = _deliveredPublicationCounterPerPublisher.get(sourceAddr);
			_deliveredPublicationCounterPerPublisher.put(sourceAddr, count==null ? 0 : ++count);
			_lastDeliveryTimesPerPublisher.put(sourceAddr, SystemTime.currentTimeMillis());
			_lastPublicationDeliveredPerPublisher.put(sourceAddr, publication);
		}
		
		_lastReceivedPublication = new PublicationInfo(publication, sourceSequence._address);
		_lastPublicationReceiptTime = SystemTime.currentTimeMillis();
	}

	@Override
	public Map<String, Publication> getLastPublicationDeliveredPerPublisher() {
		return _lastPublicationDeliveredPerPublisher;
	}
	
	@Override
	public Map<String, Long> getLastPublicationDeliveryTimesPerPublisher() {
		return _lastDeliveryTimesPerPublisher;
	}
	
	@Override
	public Map<String, Integer> getDeliveredPublicationCounterPerPublisher() {
		return _deliveredPublicationCounterPerPublisher;
	}
	
	@Override
	public int getCount() {
		return _count;
	}
	
	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_DEFAULT_SUBSCRIBER;
	}

	@Override
	public void tmConfirmed(TMulticast tm) {
		return;
	}

}
