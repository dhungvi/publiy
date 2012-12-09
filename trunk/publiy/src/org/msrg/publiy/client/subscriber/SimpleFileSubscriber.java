package org.msrg.publiy.client.subscriber;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;
import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.broker.info.SubscriptionInfo;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class SimpleFileSubscriber extends Broker implements ISubscriber, ITMConfirmationListener, IBrokerOpStateListener {

	protected final static int DEFAULT_DELAY = 1000;
	final static String DEFAULT_SUBSCRIPTION_FILENAME1 = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "sFile1_2.txt";
	final static String DEFAULT_SUBSCRIPTION_FILENAME2 = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "sFile1_3.txt";
	final static String DEFAULT_SUBSCRIPTION_FILENAME3 = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "sFile1_5.txt";
	final static String DEFAULT_SUBSCRIPTION_FILENAME4 = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "sFile2_2.txt";
	final static String DEFAULT_SUBSCRIPTION_FILENAME5 = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "sFile2_3.txt";
	final static String DEFAULT_SUBSCRIPTION_FILENAME6 = "." + FileUtils.separatorChar + "data" + FileUtils.separatorChar + "sFile2_5.txt";
	
	public static final String PROPERTY_FILENAME = "SubFilenames";
	public static final String PROPERTY_DELAY = "Delay";

	public static final Properties SIMPLE_PROPERTIES = createDefaultProperties(DEFAULT_SUBSCRIPTION_FILENAME1, DEFAULT_DELAY);
	
	public static final InetSocketAddress sAddress1 = new InetSocketAddress("127.0.0.1", 1201);
	
	private Map<TMulticast_Subscribe, TMulticast_Subscribe> _unconfirmedSubscriptions = new HashMap<TMulticast_Subscribe, TMulticast_Subscribe>();
	private Set<TMulticast_Subscribe> _confirmedTMSubscriptions = new HashSet<TMulticast_Subscribe>();
	private Set<Subscription> _confirmedSubscriptions = new HashSet<Subscription>();
	
	private boolean _isPlaying;
	Timer _timer;
	File _subscriptionFile;
	String _name;
	String _filename;
	int _delay;
	BufferedReader _bufferedFileReader;
	
	public SimpleFileSubscriber(InetSocketAddress localAddress, BrokerOpState bOpState, InetSocketAddress joinPointAddress, PubForwardingStrategy brokerForwardingStrategy, Properties arguments) throws IOException{
		this ( localAddress, bOpState, joinPointAddress, brokerForwardingStrategy, arguments.getProperty(PROPERTY_FILENAME), new Integer(arguments.getProperty(PROPERTY_DELAY)).intValue(), arguments);
	}

	public SimpleFileSubscriber(InetSocketAddress localAddress, BrokerOpState bOpState, InetSocketAddress joinPointAddress, PubForwardingStrategy brokerForwardingStrategy, String filename, int delay, Properties arguments) throws IOException{
		super(localAddress, bOpState, joinPointAddress, brokerForwardingStrategy, arguments);
		_name = "SimpleFileSubscriber-" + this.getBrokerID();
		_delay = delay;
		_filename = filename;
		_isPlaying = false;
		prepareFile();
		registerBrokerOpStateListener(this);
		setCanDeliverMessages(false);
		
		LoggerFactory.getLogger().info(this, "Creating a " + getNodeType());
	}
	
	protected void setCanDeliverMessages(boolean canDeliverMessages) {
		_isPlaying = canDeliverMessages;
		((BrokerShadow)_brokerShadow).setCanDeliverMessages(_isPlaying);
	}

	private void prepareFile(){
		try{
			FileReader fileReader = new FileReader(_filename);
			_bufferedFileReader = new BufferedReader(fileReader);
		}catch(IOException iox){iox.printStackTrace(); System.exit(-1);}
	}

	@Override
	public void tmConfirmed(TMulticast tm) {
		LoggerFactory.getLogger().debug(this, "Conf_Rcvd::" + tm);
		TMulticast_Subscribe tms;
		synchronized (_unconfirmedSubscriptions) {
			tms = _unconfirmedSubscriptions.remove(tm);
			if ( tms == null )
				return;
		}

		synchronized (_confirmedTMSubscriptions) {
			_confirmedTMSubscriptions.add(tms);
			Subscription sub = tms.getSubscription();
			_confirmedSubscriptions.add(sub);
		}
	}
	
	int subs_count=0;
	public void subscribeNext(){
		String line = null;
		
		do{
			try{
				line = _bufferedFileReader.readLine();
			}catch(IOException iox){return;}
	
			if ( line == null ){
				return;
			}
		
		}while ( line.length() == 0 || line.charAt(0) == '#' );
			
		
		Subscription subscription = Subscription.decode(line);
		
		if ( subscription == null )
			return;
		
		TMulticast_Subscribe tms = subscribe(subscription, this);
		if ( DEBUG )
			LoggerFactory.getLogger().debug(this, "Next Subscription issed:: (" + (++subs_count) + ")" + tms);
		
		SubscriberTask subTask = new SubscriberTask(this);
		_timer.schedule(subTask, _delay);

	}

	@Override
	public synchronized void brokerOpStateChanged(IBroker broker) {
		if ( broker != this )
			throw new IllegalStateException();
		
		if ( broker.getBrokerOpState() == BrokerOpState.BRKR_END )
			if (_timer != null)
				_timer.cancel();
		
//		if ( broker.getBrokerOpState() != BrokerOpState.BRKR_PUBSUB_PS )
//			return;
//		SubscriberTask subTask = new SubscriberTask(this);
//		_timer.schedule(subTask, _delay);
	}

	public static void main(String[] argv) throws IOException {
		Properties arguments = null;
		SimpleFileSubscriber sfs = new SimpleFileSubscriber(sAddress1, BrokerOpState.BRKR_PUBSUB_JOIN, null, PubForwardingStrategy.PUB_FORWARDING_STRATEGY_0, DEFAULT_SUBSCRIPTION_FILENAME1, DEFAULT_DELAY, arguments);
		sfs.prepareToStart();
	}

	@Override
	public SubscriptionInfo[] getConfirmedSubscriptionInfos() {
		TMulticast_Subscribe[] tmss;
		synchronized (_confirmedTMSubscriptions) {
			tmss = _confirmedTMSubscriptions.toArray(new TMulticast_Subscribe[0]);
		}
		SubscriptionInfo[] subscriptionInfos = new SubscriptionInfo[tmss.length];
		for ( int i=0 ; i<subscriptionInfos.length ; i++ )
			subscriptionInfos[i] = new SubscriptionInfo(tmss[i].getSubscription(), _localAddress, tmss[i].getSourceSequence());
		
		return subscriptionInfos;
	}
	
	protected Subscription[] getConfirmedSubscriptions() {
		Subscription[] subs = _confirmedSubscriptions.toArray(new Subscription[0]);
		
		return subs;
	}
	
	@Override
	public SubscriptionInfo[] getUnconfirmedSubscriptions() {
		TMulticast_Subscribe[] tmss;
		synchronized (_unconfirmedSubscriptions) {
			tmss = _unconfirmedSubscriptions.entrySet().toArray(new TMulticast_Subscribe[0]);
		}
		SubscriptionInfo[] subscriptionInfos = new SubscriptionInfo[tmss.length];
		for ( int i=0 ; i<subscriptionInfos.length ; i++ )
			subscriptionInfos[i] = new SubscriptionInfo(tmss[i].getSubscription(), _localAddress, tmss[i].getSourceSequence());
		
		return subscriptionInfos;
	}

	@Override
	public TMulticast_Subscribe subscribe(Subscription subscription, ISubscriptionListener subscriptionListener) {
		TMulticast_Subscribe tms = _connectionManager.issueSubscription(subscription, this);
		synchronized (_unconfirmedSubscriptions) {
			_unconfirmedSubscriptions.put(tms, tms);
		}
		return tms;			
	}
	
	private static Properties createDefaultProperties(String sFilename, int delay){
		Properties props = new Properties();
		props.put(PROPERTY_DELAY, delay);
		props.put(PROPERTY_FILENAME, sFilename);
		
		return props;
	}
	
	@Override
	public NodeTypes getNodeType() {
		return NodeTypes.NODE_SUBSCRIBER;
	}

	@Override
	public boolean pause() {
		if (!_isPlaying)
			return false;
		
		LoggerFactory.getLogger().info(this, "PAUSING: " + this);
		setCanDeliverMessages(false);
		_timer.cancel();
		
		return true;
	}

	@Override
	public boolean play() {
		BrokerInternalTimer.inform("Going to play()");
		if ( getBrokerOpState() != BrokerOpState.BRKR_PUBSUB_PS )
			return false;
		
		if (_isPlaying)
			return false;
		
		setCanDeliverMessages(true);
		_timer = new Timer(_name);
		SubscriberTask subTask = new SubscriberTask(this);
		_timer.schedule(subTask, _delay);
		
		BrokerInternalTimer.inform("Finished playing");
		return true;
	}

	@Override
	public boolean setSpeedLevel(int interval) {
		_delay = interval;
		return true;
	}

	@Override
	protected void logDeploySummaries() {
		super.logDeploySummaries();
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_BROKER, "Param: SDELAY=" + _delay);
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_BROKER, "Param: SFILENAMES=" + _filename);
	}
	
	@Override
	public boolean canDeliverMessages() {
		return _isPlaying;
	}

	@Override
	public boolean canGenerateMessages() {
		return false;
	}
}
