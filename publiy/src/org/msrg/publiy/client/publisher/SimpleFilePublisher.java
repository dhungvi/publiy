package org.msrg.publiy.client.publisher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerOpStateListener;
import org.msrg.publiy.broker.PubForwardingStrategy;





import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.BFTPublication;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastAnnotatorFactory;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.PropertyGrabber;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Utility;
import org.msrg.publiy.utils.annotations.AnnotationEvent;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;
import org.msrg.publiy.broker.info.PublicationInfo;
import org.msrg.publiy.client.networkcoding.publisher.FilePublisher_NC;
import org.msrg.publiy.client.subscriber.casuallogger.PublicationGenerationLogger;
import org.msrg.publiy.component.ComponentStatus;

public class SimpleFilePublisher extends Broker implements IPublisher, IBrokerOpStateListener {

	protected final static long INITIAL_DELAY = 2000;
	protected final static int DEFAULT_DELAY = 1000;
	public final static String DEFAULT_PUBLICATIONS_FILE_EXTENTION = ".pub";
	public final static String[] DEFAULT_PUBLICATION_FILENAMES = PropertyGrabber.getFileNamesRelative("." + FileUtils.separatorChar + "data" + FileUtils.separatorChar , DEFAULT_PUBLICATIONS_FILE_EXTENTION);
	
	final static String DEFAULT_PUBLICATION_FILENAME1 = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "pFile1.txt";
	final static String DEFAULT_PUBLICATION_FILENAME2 = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "pFile2.txt";
	
	public static final String PROPERTY_FILENAME = "PubFilenames";
	public static final String PROPERTY_DELAY = "Delay";

	public static final Properties SIMPLE_PROPERTIES = createDefaultProperties(DEFAULT_PUBLICATION_FILENAME1, DEFAULT_DELAY);

	public static final InetSocketAddress pAddress1 = new InetSocketAddress("127.0.0.1", 1301);
	
	private Set<TMulticast_Publish> _unconfirmedPublications = null; //new HashSet<TMulticast_Publish>();
	protected final PublicationGenerationLogger _pubGenerationLogger;
	
	protected Timer _timer;
	private String _filename;
	private BufferedReader _bufferedFileReader;
	private int _delay;
	protected long _expectedStartMillis = 0;
	protected int _count;
	protected boolean _paused;
	protected int _totalPublished = 0;
	protected int _publicationBatchSize = 1;
	
	public SimpleFilePublisher(InetSocketAddress localAddress, BrokerOpState bOpState, InetSocketAddress joinPointAddress, PubForwardingStrategy brokerForwardingStrategy, Properties arguments) throws IOException {
		this(localAddress, bOpState, joinPointAddress, brokerForwardingStrategy, arguments.getProperty(PROPERTY_FILENAME), new Double(arguments.getProperty(PROPERTY_DELAY)).doubleValue(), arguments);
	}
	
	public SimpleFilePublisher(InetSocketAddress localAddress, BrokerOpState bOpState, InetSocketAddress joinPointAddress, PubForwardingStrategy brokerForwardingStrategy, String filename, double delay, Properties arguments) throws IOException {
		super(localAddress, bOpState, joinPointAddress, brokerForwardingStrategy, arguments);
		registerBrokerOpStateListener(this);
		
		_filename = filename;
		prepareFile();
		setDelay(delay);
		_pubGenerationLogger = PublicationGenerationLogger.getPublicationGenerationLoggerInstance(this);
		brokerOpStateChanged(this);
		setCanDeliverMessages(false);
	}
	
	protected void setCanDeliverMessages(Boolean canDeliverMessages) {
		_paused = !canDeliverMessages;
		((BrokerShadow)_brokerShadow).setCanDeliverMessages(canDeliverMessages);
	}
	
	protected void cancelTimer() {
		LoggerFactory.getLogger().info(this, "Cancelling timer: " + _timer);
		_timer.cancel();
		_timer = null;
	}
	
	protected synchronized void createTimer() {
		LoggerFactory.getLogger().info(this, "Creating timer for publishing: " + _timer);
		if(_timer==null)
			_timer = new Timer("SimpleFilePublisher-" + this.getBrokerID());
	}
	
	private void prepareFile() {
		try{
			LoggerFactory.getLogger().info(this, "Preparing file: " + _filename);
			FileReader fileReader = new FileReader(_filename);
			_bufferedFileReader = new BufferedReader(fileReader);
		}catch(IOException iox) {iox.printStackTrace(); System.exit(-1);}
	}
	
	protected void prepareReadPublication(Publication pub) {
		pub.addPredicate(getGeneralCounterAttributeName(), ++_count);

		int port = _localAddress.getPort();
		pub.addPredicate(getGeneralNodeIDAttributeName(), port);

		String sourceAddrStr = _localAddress.toString();
		pub.addStringPredicate(getGeneralSourceAddrAttributeName(), sourceAddrStr);
	}
	
	public static String getGeneralSourceAddrAttributeName() {
		return "SOURCEADDR";
	}
	
	public static String getGeneralNodeIDAttributeName() {
		return "NODEID";
	}
	
	public static String getGeneralCounterAttributeName() {
		return "count";
	}
	
	protected void setDelay(final double newDelay) {
		int bestBatchSize = 1;
		double bestRemainder = Integer.MAX_VALUE;
		double bestDelay = 0;
		
		for(int batchSize=1 ; batchSize<=10 ; batchSize++) {
			if(batchSize==3 || batchSize==10)
				System.out.print("");
			double remainder = ((newDelay*batchSize)) % 1;
			if(1-remainder < remainder)
				remainder = 1-remainder;
			
			if(remainder <= bestRemainder) {
				bestRemainder = remainder;
				bestBatchSize = batchSize;
				bestDelay = newDelay * bestBatchSize;
			}
			
			if(remainder == 0)
				break;
		}

		_publicationBatchSize = bestBatchSize;
		_delay = (int) (bestDelay + (bestDelay%1==0 ? 0 : 1));

		boolean error = (_delay==0 || _publicationBatchSize==0);
		if(error) {
			_publicationBatchSize = 1;
			_delay = (int) newDelay;
		}
		
		if(_delay == 1) {
			_delay *= 2;
			_publicationBatchSize *= 2;
		}
		
		LoggerFactory.getLogger().info(this, "Publish delay updated: " + (error?"ERROR " : "") + "intended(" + newDelay + "ms)=timer-delay(" + _delay + "ms) x batch-size(" + _publicationBatchSize + "msgs)");
		
//		if(_publicationBatchSize%_delay == 0) {
//			_publicationBatchSize /= _delay;
//			_delay = 1;
//		}
	}
	
	protected int getDelay() {
		return (int)(_delay * getPublicationBatchSize());
	}
	
	@Override
	public int getPublicationBatchSize() {
		if(_brokerShadow.isNC())
			return 1;
		else
			return _publicationBatchSize;
	}

	protected long getExactDelay() {
		long currTimeMilli = SystemTime.currentTimeMillis();
		long lateMillis = (currTimeMilli - _expectedStartMillis);
		
		if(lateMillis > 0)
			return Utility.max(0, (_delay - lateMillis));
		else
			return (_delay - lateMillis);
	}
	
	public void publishNext() {
		if(_paused || _componentStatus != ComponentStatus.COMPONENT_STATUS_RUNNING) {
			LoggerFactory.getLogger().warn(this, "Returning from publishNext because: ", _paused + "_" + _componentStatus);
			return;
		}
		
		scheduleNextTimer();
		
		Publication[] publications = createPublicationBatch(getPublicationBatchSize(), true);
		if(publications == null || publications.length == 0)
			return;
		
		TMulticast_Publish[] tmps = publish(publications, this);
		if(tmps == null || tmps.length == 0)
			return;
		
		if(_unconfirmedPublications!=null) {
			synchronized (_unconfirmedPublications) {
				for(int i=0 ; i<tmps.length ; i++)
					if(tmps[i]!=null)
						_unconfirmedPublications.add(tmps[i]);
			}
		}
		
		int nonNullPublicationCount = 0;
		for(int i=0 ; i<tmps.length ; i++)
			if(tmps[i]!=null)
				nonNullPublicationCount++;
		
		StatisticsLogger statLogger = getStatisticsLogger();
		if(statLogger != null)
			statLogger.newPublish(nonNullPublicationCount);
	}

	protected void scheduleNextTimer() {
		long exactDelay = getExactDelay();
		_expectedStartMillis = exactDelay + SystemTime.currentTimeMillis();
		if(_timer != null)
			_timer.schedule(new PublisherTask(this), exactDelay);
	}

	protected Publication[] createPublicationBatch(int publicationBatchSize, boolean loop) {
		String line = null;
		Publication[] publications = new Publication[publicationBatchSize];
		for(int i=0 ; i<publications.length ; i++) {
			try{
				line = _bufferedFileReader.readLine();
			}catch(IOException iox) {
				return null;
			}
	
			if(line == null && loop) {
				try{
					_bufferedFileReader.close();
					FileReader fileReader = new FileReader(_filename);
					_bufferedFileReader = new BufferedReader(fileReader);
					line = _bufferedFileReader.readLine();
				}catch(IOException iox) {iox.printStackTrace(); System.exit(-1);}
			}
			
			if(line == null)
				return null;
			
			Publication publication = Publication.decode(line);
			if(publication == null)
				return null;
			prepareReadPublication(publication);
			publications[i] = publication;
		}
		
		return publications;
	}
	
	protected TMulticast_Publish createTMulticastPublication(Publication publication) {
		if(_brokerShadow.isNormal())
			return new TMulticast_Publish(publication, _localAddress, _brokerShadow.getLocalSequencer());
		
		if(_brokerShadow.isBFT())
			return new TMulticast_Publish_BFT(new BFTPublication(publication, _localAddress), _localAddress, _brokerShadow.getLocalSequencer().getNext());

		if(_brokerShadow.isMP())
			return new TMulticast_Publish_MP(publication, _localAddress, 0, (byte)0, _brokerShadow.getPublicationForwardingStrategy(), _localSequencer.getNext());

		if(_brokerShadow.isNC())
			throw new UnsupportedOperationException("Please use " + FilePublisher_NC.class.getCanonicalName() + " instead.");
			
		throw new UnsupportedOperationException();
	}
	
	@Override
	public TMulticast_Publish publish(Publication publication, ITMConfirmationListener tmConfirmationListener) {
		TMulticast_Publish tmp = createTMulticastPublication(publication);
		TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(getLocalSequencer(), tmp, AnnotationEvent.ANNOTATION_EVENT_PUBLICATION_GENERATION, "SimpleFilePublisher #" + (_count));
		
		if(Broker.DEBUG || !Broker.RELEASE)
			LoggerFactory.getLogger().debug(this, "########## Publishing: " + tmp + " ##########");
		
		_connectionManager.sendTMulticast(tmp, tmConfirmationListener);
		
		return tmp;
	}

	@Override
	public TMulticast_Publish[] publish(Publication[] publications, ITMConfirmationListener tmConfirmationListener) {
		TMulticast_Publish[] tmps = new TMulticast_Publish[publications.length];
		for(int i=0 ; i<tmps.length ; i++) {
			if(publications[i] == null)
				continue;
			TMulticast_Publish tmp = createTMulticastPublication(publications[i]);
			TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(getLocalSequencer(), tmp, AnnotationEvent.ANNOTATION_EVENT_PUBLICATION_GENERATION, "SimpleFilePublisher #" + (_count));
			
			tmps[i] = tmp;
			if(Broker.DEBUG || !Broker.RELEASE)
				LoggerFactory.getLogger().debug(this, "########## Publishing: " + tmp + " ##########");
		}
		
		_connectionManager.sendTMulticast(tmps, tmConfirmationListener);
		_pubGenerationLogger.publicationGenerated(tmps);
		return tmps;
	}

	@Override
	public void tmConfirmed(TMulticast tm) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, "Conf_Rcvd::" + tm);
		
		if(_unconfirmedPublications!=null) {
			synchronized (_unconfirmedPublications) {
				_unconfirmedPublications.remove(tm);
			}
		}
	}

	@Override
	public synchronized void brokerOpStateChanged(IBroker broker) {
		if(broker != this)
			throw new IllegalStateException();
		
		if(broker.getBrokerOpState() != BrokerOpState.BRKR_PUBSUB_PS)
			return;
	}
	
	public static void main(String[] argv) throws IOException {
		Properties arguments = null;
		SimpleFilePublisher sfp = new SimpleFilePublisher(pAddress1, BrokerOpState.BRKR_PUBSUB_JOIN, bAddress1, PubForwardingStrategy.PUB_FORWARDING_STRATEGY_0, DEFAULT_PUBLICATION_FILENAME1, DEFAULT_DELAY, arguments);
		sfp.prepareToStart();
	}

	@Override
	public PublicationInfo[] getUnconfirmedPublications() {
		if(_unconfirmedPublications==null)
			return null;
		
		TMulticast_Publish[] tmps;
		synchronized (_unconfirmedPublications) {
			tmps = _unconfirmedPublications.toArray(new TMulticast_Publish[0]);
		}
		
		PublicationInfo[] publicationInfos = new PublicationInfo[tmps.length];
		for(int i=0 ; i<publicationInfos.length ; i++)
			publicationInfos[i] = new PublicationInfo(tmps[i].getPublication(), _localAddress);
		
		return publicationInfos;
	}
	
	private static Properties createDefaultProperties(String sFilename, double delay) {
		Properties props = new Properties();
		props.put(PROPERTY_DELAY, delay);
		props.put(PROPERTY_FILENAME, sFilename);
		
		return props;
	}
	
	@Override
	public NodeTypes getNodeType() {
		return NodeTypes.NODE_PUBLISHER;
	}
	
	@Override
	public boolean pause() {
		if(_paused)
			return false;
		
		LoggerFactory.getLogger().info(this, "PAUSING: " + this);
		setCanDeliverMessages(false);
		cancelTimer();
		
		return true;
	}
	
	@Override
	public boolean play() {
		BrokerOpState brokerOpState = getBrokerOpState();
		if(brokerOpState != BrokerOpState.BRKR_PUBSUB_PS) {
			System.out.println("Return from play() because: " + brokerOpState + "_" + _paused);
			return false;
		}
		
		if(!_paused)
			return true;
		
		setCanDeliverMessages(true);
		createTimer();
		_timer.schedule(new PublisherTask(this), INITIAL_DELAY + getExactDelay());
		
		return true;
	}
	
	@Override
	public boolean setSpeedLevel(int interval) {
		LoggerFactory.getLogger().info(this, "Setting speed from: " + _delay + ", to: " + interval);
		setDelay(interval);
		return true;
	}

	@Override
	public void prepareToStart() {
		_casualLoggerEngine.registerCasualLogger(_pubGenerationLogger);
		super.prepareToStart();
	}
	
	@Override
	protected void logDeploySummaries() {
		super.logDeploySummaries();
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_BROKER, "Param: PDELAY=" + _delay);
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_BROKER, "Param: PFILENAMES=" + _filename);
	}
	
	@Override
	public boolean canDeliverMessages() {
		return !_paused;
	}

	@Override
	public boolean canGenerateMessages() {
		return !_paused;
	}
}
