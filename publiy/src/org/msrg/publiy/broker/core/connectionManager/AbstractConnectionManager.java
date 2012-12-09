package org.msrg.publiy.broker.core.connectionManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.Dack_Bundle;
import org.msrg.publiy.pubsub.core.packets.multicast.TDack;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;

import org.msrg.publiy.communication.core.niobinding.ConnectionInfoNonListeningStatus;
import org.msrg.publiy.communication.core.niobinding.IConInfo;
import org.msrg.publiy.communication.core.niobinding.IConInfoListening;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.niobinding.INIOBinding;
import org.msrg.publiy.communication.core.niobinding.NIOBindingFactory;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.TCommand;
import org.msrg.publiy.communication.core.packet.types.TConf_Ack;
import org.msrg.publiy.communication.core.packet.types.TPing;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionManager;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;
import org.msrg.publiy.component.ComponentStatus;
import org.msrg.publiy.component.IComponent;
import org.msrg.publiy.component.IComponentListener;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;
import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationDumpSpecifier;
import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationMessageDumpSpecifier;
import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.IMaintenanceManager;
import org.msrg.publiy.broker.core.bwenforcer.InOutBWEnforcer;
import org.msrg.publiy.broker.core.maintenance.MaintenanceManagerWithNoBroker;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

abstract class AbstractConnectionManager extends Thread implements IConnectionManager, ILoggerSource {
	private static final int MAX_MESSAGE_PROCESS_AT_A_TIME = 5;
	
	protected IMaintenanceManager _maintanenceManager;
	protected ConnectionManager __nextConnectionManager = null;
	protected final String _connectionManagerName;
	protected final ConnectionManagerTypes _type;
	
	protected ComponentStatus _status = ComponentStatus.COMPONENT_STATUS_UNINITIALIZED;
	protected Object _componentStatusChangeWaitObject = new Object();
	protected Set<IComponentListener> _componentListeners = new HashSet<IComponentListener>();

	protected LocalSequencer _localSequencer;
	protected InetSocketAddress _localAddress;
	protected INIOBinding _nioBinding;
	protected IBrokerShadow _brokerShadow;
	protected IBroker _broker;
	protected IConInfoListening<?> _conInfoL;
	protected final ISessionManager _sessionManager;
	protected List<ConnectionEvent> _connectionEvents = new LinkedList<ConnectionEvent>();
	protected int _connectionEventsIn = 0;
	protected int _connectionEventsOut = 0;
	
	protected AbstractConnectionManager(String name, ConnectionManagerTypes type, LocalSequencer localSequencer) {
		super("THREAD-" + name);
		_connectionManagerName = name;
		_type = type;
		_sessionManager = createSessionManager(localSequencer);
	}
	
	////////////////////////////////////////////////////////////////////////
	//							ICOMPONENT								  //
	////////////////////////////////////////////////////////////////////////
	@Override
	public void componentStateChanged(IComponent component) {
		LoggerFactory.getLogger().info(this, "Component '" + component.getComponentName() + "' changed its state to '" + component.getComponentState() + "'.");
		if(component == _nioBinding) {
			synchronized (_componentStatusChangeWaitObject) {
				_componentStatusChangeWaitObject.notify();
				return;
			}
		}
	}
	
	@Override
	public void startComponent() {
		synchronized (_componentStatusChangeWaitObject) {
			if(_status != ComponentStatus.COMPONENT_STATUS_INITIALIZED &&
					_status != ComponentStatus.COMPONENT_STATUS_STARTING)
				throw new IllegalStateException("Connection manager must be initialized before starting: " + _status);
			
			LoggerFactory.getLogger().info(this, "Starting super.start() of '" + this + "'...");
			super.start();
		}
	}

	// IComponent
	@Override
	public final void addNewComponentListener(IComponentListener comListener) {
		if(comListener == null)
			return;
		
		synchronized(_componentListeners) {
			_componentListeners.add(comListener);
		}
	}

	// IComponent
	@Override
	public final void removeComponentListener(IComponentListener comListener) {
		if(comListener == null)
			return;
		
		synchronized(_componentListeners) {
			_componentListeners.remove(comListener);
		}
	}

	// IComponent
	@Override
	public final ComponentStatus getComponentState() {
		return _status;
	}

	@Override
	public void prepareToStart() {
		synchronized (_componentStatusChangeWaitObject) {
			if(_status != ComponentStatus.COMPONENT_STATUS_UNINITIALIZED)
				throw new IllegalStateException("ConnectionManager must be UNINITIALIZED before initializing ('" + _status + "'.");
			setComponentState(ComponentStatus.COMPONENT_STATUS_INITIALIZING);
			try{
				_nioBinding = createNIOBinding(_brokerShadow);
				_sessionManager.setNIOBinding(_nioBinding);
			}catch (IOException iox) {
				iox.printStackTrace();
				setComponentState(ComponentStatus.COMPONENT_STATUS_PROBLEM);
				return;
			}
			
			_nioBinding.addNewComponentListener(this);
			_nioBinding.prepareToStart();
			_nioBinding.startComponent();
			setComponentState(ComponentStatus.COMPONENT_STATUS_INITIALIZED);
		}
	}
	
	protected INIOBinding createNIOBinding(IBrokerShadow brokerShadow) throws IOException {
		return NIOBindingFactory.getNIOBinding(brokerShadow);
	}
	
	@Override
	public final void awakeFromPause() {
		_nioBinding.awakeFromPause();
		synchronized (_componentStatusChangeWaitObject) {
			if(_status == ComponentStatus.COMPONENT_STATUS_PAUSED) {
				setComponentState(ComponentStatus.COMPONENT_STATUS_STARTING);
				_componentStatusChangeWaitObject.notify();
			}
			else 
				throw new IllegalStateException("State is not paused! It is: " + _status);
		}
	}

	private boolean _componentStateControlInProgress = false;
	@Override
	public final void pauseComponent() {
		synchronized (_componentStatusChangeWaitObject) 
		{
			if(_componentStateControlInProgress == true) {
				LoggerFactory.getLogger().warn(this, "ConnectionManager is in '" + _status + "' state. Cannot pause component.");
				return;
			}
			else
				_componentStateControlInProgress = true;
		}
		
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent_stateChange stateChangeEvent = new ConnectionEvent_stateChange(localSequencer, ComponentStatus.COMPONENT_STATUS_PAUSING);
		addConnectionEventHead(stateChangeEvent);
	}

	@Override
	public final void stopComponent() {
		synchronized (_componentStatusChangeWaitObject) 
		{
			if(_componentStateControlInProgress == true) {
				LoggerFactory.getLogger().warn(this, "ConnectionManager is in '" + _status + "' state. Cannot stop component.");
				return;
			}
			else
				_componentStateControlInProgress = true;
		}
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent_stateChange stateChangeEvent = new ConnectionEvent_stateChange(localSequencer, ComponentStatus.COMPONENT_STATUS_STOPPING);
		addConnectionEventHead(stateChangeEvent);
	}
	
	
	////////////////////////////////////////////////////////////////////////
	//							ICONNECTIONMANAGER						  //
	////////////////////////////////////////////////////////////////////////
	// Maintenance ...
	private Boolean _tpingSendPending = false;
	private Object _tpingSendLock = new Object();
	private Boolean _dackSendPending = false;
	protected Boolean _purgeMQPending = false;
	protected Object _purgeMQPendingLock = new Object();
	private Object _forcedConfAckPendingLock = new Object();
	private Boolean _forcedConfAckPending = false;

	@Override
	public final InetSocketAddress getLocalAddress() {
		return _localAddress;
	}
	
	@Override
	public BrokerOpState getBrokerOpState() {
		return _broker.getBrokerOpState();
	}
	
	@Override
	public IConnectionManager getNextConnctionManager() {
		return __nextConnectionManager;
	}
	
	@Override
	public String getBrokerSubDumpFileName() {
		if(_brokerShadow != null)
			return _brokerShadow.getBrokerSubDumpFileName();
		else
			return getDefaultSubDumpFileName(_localSequencer);
	}

	@Override
	public final String getComponentName() {
		return toString();
	}
	
	@Override
	public synchronized final void scheduleTaskWithTimer(TimerTask task, long delay) {
		if(_brokerShadow != null)
			_maintanenceManager = _brokerShadow.getMaintenanceManager();

		if(_maintanenceManager==null)
			_maintanenceManager = new MaintenanceManagerWithNoBroker(getComponentName());

		_maintanenceManager.scheduleMaintenanceTask(task, delay);
	}

	@Override
	public synchronized final void scheduleTaskWithTimer(TimerTask task, long delay, long period) {
		if(_brokerShadow != null)
			_maintanenceManager = _brokerShadow.getMaintenanceManager();

		if(_maintanenceManager==null)
			_maintanenceManager = new MaintenanceManagerWithNoBroker(getComponentName());

		_maintanenceManager.schedulePeriodicMaintenanceTask(task, delay, period);
	}

	@Override
	public String getBrokerRecoveryFileName() {
		if(_brokerShadow != null)
			return _brokerShadow.getRecoveryFileName();
		else
			return getDefaultOverlayDupmFileName(_localSequencer);
	}
	
	protected static String getDefaultBrokerSubscriptionDumpFileName(LocalSequencer localSequencer) {
		return getDefaultBrokerName(localSequencer) + ".subm";
	}
	
	protected static String getDefaultOverlayDupmFileName(LocalSequencer localSequencer) {
		if(localSequencer == null)
			return "NULL";
		return getDefaultBrokerName(localSequencer) + ".top";
	}

	@Override
	public final String toString() {
		return _connectionManagerName;
	}

	@Override 
	public String getBrokerSessionsDumpFileName() {
		return _brokerShadow.getSessionsDumpFileName();
	}

	@Override
	public void sendTMulticast(TMulticast tm, ITMConfirmationListener tmConfirmationListener) {
		TM_TMListener tmTmListener = new TM_TMListener(tm, tmConfirmationListener);
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent = new ConnectionEvent_NewLocalOutgoing(localSequencer, tmTmListener);
		addConnectionEvent(connEvent);
	}
	
	@Override
	public void sendTMulticast(TMulticast[] tms, ITMConfirmationListener tmConfirmationListener) {
		for(int i=0 ; i<tms.length ; i++) {
			if(tms[i]==null)
				continue;
			TM_TMListener tmTmListener = new TM_TMListener(tms[i], tmConfirmationListener);
			LocalSequencer localSequencer = _localSequencer;
			if(localSequencer == null)
				return;
			ConnectionEvent connEvent = new ConnectionEvent_NewLocalOutgoing(localSequencer, tmTmListener);
			addConnectionEvent(connEvent);
		}
	}
	
	private Object _candidatesPendingLock = new Object();
	private boolean _candidatesPending = false;
	@Override
	public void insertCandidatesEvent() {
		synchronized (_candidatesPendingLock) {
			_candidatesPending = false;
			
			LocalSequencer localSequencer = _localSequencer;
			if(localSequencer == null)
				return;
			ConnectionEvent_Candidates connEvent = new ConnectionEvent_Candidates(localSequencer);
			addConnectionEvent(connEvent);
		}
		
		scheduleNextCandidatesEvent();
	}
	
	protected void scheduleNextCandidatesEvent() {
		synchronized (_candidatesPendingLock) {
			LoggerFactory.getLogger().debug(this, "Scheduing next candidates event [" +_candidatesPending + "]");
			
			if(_status != ComponentStatus.COMPONENT_STATUS_RUNNING) {
				_candidatesPending = false;
				return;
			}
			
			if(_candidatesPending == true)
				return;
			else
				_candidatesPending = true;

			CandidatesEventTimerTask candidatesEventTimerTask = new CandidatesEventTimerTask(this);
			scheduleTaskWithTimer(candidatesEventTimerTask, Broker.CANDIDATES_EVENT_TIMER_INTERVAL);
		}
	}
	
	@Override
	public final void insertForceConfAckEvent() {
		synchronized(_forcedConfAckPendingLock) {
			_forcedConfAckPending = false;
			
			LocalSequencer localSequencer = _localSequencer;
			if(localSequencer == null)
				return;
			ConnectionEvent_ForceConfAck connEvent = new ConnectionEvent_ForceConfAck(localSequencer);
			addConnectionEventHead(connEvent);
		}
		
		scheduleNextForcedConfAck();
	}
	
	protected final void scheduleNextForcedConfAck() {
		synchronized (_forcedConfAckPendingLock) {
			LoggerFactory.getLogger().debug(this, "Scheduing next forced ack conf [" +_forcedConfAckPending + "]");
			
			if(_status != ComponentStatus.COMPONENT_STATUS_RUNNING) {
				_forcedConfAckPending = false;
				return;
			}
			
			if(_forcedConfAckPending == true)
				return;
			else
				_forcedConfAckPending = true;

			ForcedConfAckTimerTask forcedConfAckTTask = new ForcedConfAckTimerTask(this);
			scheduleTaskWithTimer(forcedConfAckTTask, Broker.FORCED_CONF_ACK_DELAY);
		}
	}

	@Override
	public void sendDackOnAllSessions() {
		synchronized (_componentStatusChangeWaitObject) {
			if(_status != ComponentStatus.COMPONENT_STATUS_RUNNING) {
				synchronized (_purgeMQPendingLock) {
					_dackSendPending = false;
				}
				return;
			}
		}

		synchronized (_purgeMQPendingLock)
		{
			_dackSendPending = false;
			
			LocalSequencer localSequencer = _localSequencer;
			if(localSequencer == null)
				return;
			ConnectionEvent_sendDack connEventSendDack = new ConnectionEvent_sendDack(localSequencer);
			addConnectionEventHead(connEventSendDack);
		}
		
		scheduleNextDack();
	}
	
	@Override
	public void sendTPingOnAllSessions() {
		synchronized (_componentStatusChangeWaitObject) {
			if(_status != ComponentStatus.COMPONENT_STATUS_RUNNING) {
				synchronized (_tpingSendLock) {
					_tpingSendPending = false;
				}
				return;
			}
		}

		synchronized (_tpingSendLock)
		{
			_tpingSendPending = false;
			
			LocalSequencer localSequencer = _localSequencer;
			if(localSequencer == null)
				return;
			ConnectionEvent_sendTPing connEventSendTping = new ConnectionEvent_sendTPing(localSequencer);
			addConnectionEventHead(connEventSendTping);
		}
		
		scheduleNextTPing();
	}
	
	@Override
	public void renewSessionsConnection(ISession session) {
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent = new ConnectionEvent_renewConnection(localSequencer, session);
		addConnectionEvent(connEvent);
	}
	
	protected void sendTPingOnSession(ISession session) {
		if(!session.isConnected())
			return;
	
		TPing tping = new TPing(_localAddress);
		
		IRawPacket raw = PacketFactory.wrapObject(_localSequencer, tping);
		_sessionManager.send(session, raw);
	}
	
	protected void scheduleNextTPing() {
		synchronized (_purgeMQPendingLock)
		{
			if(_status != ComponentStatus.COMPONENT_STATUS_RUNNING) {
				_tpingSendPending = false;
				return;
			}
		
			if(_tpingSendPending == true)
				return;

			_tpingSendPending = true;
			SendTPingTask sendTPingTimerTask = new SendTPingTask(this);
			scheduleTaskWithTimer(sendTPingTimerTask, Broker.TPING_SEND_INTERVAL);
		}
	}
	
	protected final void scheduleNextDack() {
		if(!_brokerShadow.isFastConf() || _brokerShadow.isMP())
			return;

		synchronized (_purgeMQPendingLock)
		{
			if(_status != ComponentStatus.COMPONENT_STATUS_RUNNING) {
				_dackSendPending = false;
				return;
			}
		
			if(_dackSendPending == true)
				return;

			_dackSendPending = true;
			SendDackTimerTask sendDackTimerTask = new SendDackTimerTask(this);
			scheduleTaskWithTimer(sendDackTimerTask, Broker.DACK_SEND_INTERVAL);
		}
	}
	
	protected void sendDackOnSession(ISession session) {
		if(!_brokerShadow.isFastConf() || _brokerShadow.isMP())
			return;

		synchronized (_purgeMQPendingLock)
		{
			if(!session.isConnected())
				return;
		
			IOverlayManager overlayManager = getOverlayManager();
			InetSocketAddress remoteAddress = session.getRemoteAddress();
			
			Dack_Bundle[][] bothDackBundles = overlayManager.prepareOutgoingArrivedSequences(remoteAddress);
			if(bothDackBundles == null)
				return;

			TDack tDack = new TDack(bothDackBundles[0], bothDackBundles[1]);
			
			IRawPacket raw = PacketFactory.wrapObject(_localSequencer, tDack);
			_sessionManager.send(session, raw);
		}
	}
	
	@Override
	public void purgeMQ() {
		if(!_brokerShadow.isFastConf() || _brokerShadow.isMP())
			return;

		synchronized (_purgeMQPendingLock) {
			_purgeMQPending = false;

			if(_status != ComponentStatus.COMPONENT_STATUS_RUNNING)
				return;

			LocalSequencer localSequencer = _localSequencer;
			if(localSequencer == null)
				return;
			ConnectionEvent_purgeMQ connEventPurgeMQ = new ConnectionEvent_purgeMQ(localSequencer);
			addConnectionEventHead(connEventPurgeMQ);
		}
		
		scheduleNextPurgeMQ();
	}
	
	protected void scheduleNextPurgeMQ() {
		if(!_brokerShadow.isFastConf() || _brokerShadow.isMP())
			return;

		synchronized (_purgeMQPendingLock) {
			LoggerFactory.getLogger().debug(this, "Scheduing next purge MQ [" + _purgeMQPending + "]");
			if(_status != ComponentStatus.COMPONENT_STATUS_RUNNING) {
				_purgeMQPending = false;
				return;
			}
		
			if(_purgeMQPending == true)
				return;
			else
				_purgeMQPending = true;
			
			PurgeMQTimerTask purgeMQTimerTask = new PurgeMQTimerTask(this);
			scheduleTaskWithTimer(purgeMQTimerTask, Broker.PURGE_MQ_INTERVAL);
		}
	}

	@Override
	public void failed(ISession fSession) {
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent = new ConnectionEvent_failedConnection(localSequencer, fSession);
		addConnectionEvent(connEvent);
	}
	
	protected void prepareListeningConnection() {
		if(_conInfoL == null)
			_conInfoL = _nioBinding.makeIncomingConnection(this, this, this, _localAddress);
	}
	
	////////////////////////////////////////////////////////////////////////
	//								THREAD								  //
	////////////////////////////////////////////////////////////////////////
	// Thread
	@Override
	public final void run() {
		prepareListeningConnection();

		while(true)
		{
			try{
				boolean shouldContinue = doComponentStateTransitions();
				if(shouldContinue == false)
					return;

				runMe();
			}catch(Exception ex) {
				if(Broker.CORRELATE) {
					TrafficCorrelationDumpSpecifier dumpSpecifier =
						TrafficCorrelationMessageDumpSpecifier.getAllCorrelationDump("FATAL:" + ex, false);
					TrafficCorrelator.getInstance().dump(dumpSpecifier, true);
				}
				LoggerFactory.getLogger().infoX(this, ex, "Exception in '" + this + "'");
			}
		}
	}
	
	protected boolean doComponentStateTransitions() {
		synchronized (_componentStatusChangeWaitObject) {
			switch(_status) {
			case COMPONENT_STATUS_STOPPING:
				{
					_nioBinding.stopComponent();
					while ( _nioBinding.getComponentState() != ComponentStatus.COMPONENT_STATUS_STOPPED)
						try{ _componentStatusChangeWaitObject.wait(); } catch (InterruptedException itx) {}
//					synchronized(_nioStopWaitObject) {
//						if(_nioBinding.getComponentState() != ComponentStatus.COMPONENT_STATUS_STOPPED)
//							try{ _nioStopWaitObject.wait(); } catch(InterruptedException itx) {itx.printStackTrace();}
//					}
					setComponentState(ComponentStatus.COMPONENT_STATUS_STOPPED);
					return false;
				}
			
			case COMPONENT_STATUS_PAUSED:
				{
					try{_componentStatusChangeWaitObject.wait();}catch(InterruptedException itx) {}
					return true;
				}
	
			case COMPONENT_STATUS_PAUSING:
				{
					_nioBinding.pauseComponent();
					while ( _nioBinding.getComponentState() != ComponentStatus.COMPONENT_STATUS_PAUSED)
						try{ _componentStatusChangeWaitObject.wait(); } catch (InterruptedException itx) {}
//					synchronized(_nioPauseWaitObject) {
//						if(_nioBinding.getComponentState() != ComponentStatus.COMPONENT_STATUS_PAUSED)
//							try{ _nioPauseWaitObject.wait(); } catch(InterruptedException itx) {itx.printStackTrace();}
//					}
					setComponentState(ComponentStatus.COMPONENT_STATUS_PAUSED);
					return true;
				}
				
			case COMPONENT_STATUS_INITIALIZED:
			case COMPONENT_STATUS_STARTING:
				{
					setComponentState(ComponentStatus.COMPONENT_STATUS_RUNNING);
					connectionManagerJustStarted();
					return true;
				}
				
			case COMPONENT_STATUS_INITIALIZING:
			case COMPONENT_STATUS_UNINITIALIZED:
				{
					try{_componentStatusChangeWaitObject.wait();}catch(InterruptedException itx) {}
					return true;
				}
				
			case COMPONENT_STATUS_RUNNING:
				{
					return true;
				}
			
			case COMPONENT_STATUS_PROBLEM:
			case COMPONENT_STATUS_STOPPED:
				{
					return false;
				}
		
			default:
				throw new UnsupportedOperationException("Donno how to handle this state: " + _status);
			}
		}
	}

	// Called from the main loop in Thread.run()
	private final void runMe() {
		Thread.currentThread().setPriority(MAX_PRIORITY);
		while (_status == ComponentStatus.COMPONENT_STATUS_RUNNING)
		{
//			synchronized(_connectionEvents) {
//				spilloutJunk("~" + (_connectionEventsIn!=_connectionEventsOut + _connectionEvents.size()?"!":"") + _connectionEventsIn + "/" + _connectionEventsOut + "(" + _connectionEvents.size() + ")~");
//			}
			while(_status == ComponentStatus.COMPONENT_STATUS_RUNNING && !_connectionEvents.isEmpty())
			{
				// A blocking call to wait for availability of BW.
//				checkBWAvailability();

				ConnectionEvent connEvent = null;
				synchronized(_connectionEvents) {
					_connectionEventsOut++;
					connEvent = _connectionEvents.remove(0);
				}
				
				try{
					handleConnectionEvents(connEvent);
					
				}catch(IllegalStateException x) {
					if(Broker.CORRELATE) {
						TrafficCorrelationDumpSpecifier dumpSpecifier =
							TrafficCorrelationMessageDumpSpecifier.getAllCorrelationDump("FATAL:" + x, false);
						TrafficCorrelator.getInstance().dump(dumpSpecifier, true);
					}
					throw x;
				}
			}
			
			synchronized (_connectionEvents) 
			{
				if(_status == ComponentStatus.COMPONENT_STATUS_RUNNING && _connectionEvents.isEmpty())
					try{_connectionEvents.wait();}catch(InterruptedException itx) {itx.printStackTrace();}
			}
		}
	}
	
	private final int JUNK_FILTER_COUNT = 25;
	private int _junkFilterCounter = 0;
	protected void spilloutJunk(String str) {
		if(!Broker.DEBUG)
			return;
		
		if(++_junkFilterCounter % JUNK_FILTER_COUNT == 0)
			System.out.println(str + " " + BrokerInternalTimer.read().toString() + " \t" + _status);
	}

	protected final void setComponentState(ComponentStatus state) {
		LoggerFactory.getLogger().info(
				this, "Component '" + getComponentName() + "' state change to " + state + " from " + _status);
		
		if(_status == ComponentStatus.COMPONENT_STATUS_STOPPED)
			return;
		
		_status = state ;

		synchronized (_connectionEvents) {
			_connectionEvents.notify();
		}
		
		synchronized(_componentListeners) {
			Iterator<IComponentListener> it = _componentListeners.iterator();
			while ( it.hasNext()) {
				it.next().componentStateChanged(this);
			}
		}
	}
	
	protected void addConnectionEvent(ConnectionEvent connEvent) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, "Adding conn event: " + connEvent);
		
		synchronized (_connectionEvents) {
			_connectionEventsIn++;
			
			switch(connEvent._eventType) {
			case CONNECTION_EVENT_CONTENT_INVERSED:
				_connectionEvents.add(0, connEvent);
				break;
				
			default:
				_connectionEvents.add(connEvent);
				break;
			}
			
			_connectionEvents.notify();
		}
	}

	protected void addConnectionEventHead(ConnectionEvent connEvent) {
		if(_status != ComponentStatus.COMPONENT_STATUS_RUNNING)
			return;
		
		synchronized (_connectionEvents) {
			_connectionEventsIn++;
			_connectionEvents.add(0, connEvent);
			_connectionEvents.notify();
		}
	}
	
	private void handleConnectionEvents(ConnectionEvent connEvent) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, "Handling connectionEvents:" + connEvent);
		
		switch(connEvent._eventType) {
			case CONNECTION_EVENT_FAILED_CONNECTION:
				handleConnectionEvent_failedConnection((ConnectionEvent_failedConnection) connEvent);
				break;
				
			case CONNECTION_EVENT_RENEW_CONNECTION:
				handleConnectionEvent_renewConnection((ConnectionEvent_renewConnection) connEvent);
				break;
				
			case CONNECTION_EVENT_LOCAL_OUTGOING:
				handleConnectionEvent_NewLocalOutgoing((ConnectionEvent_NewLocalOutgoing) connEvent);
				break;
				
			case CONNECTION_EVENT_FAST_TCOMMAND_PROCESSING:
				handleConnectionEvent_FastTCommandProcessing((ConnectionEvent_FastTCommandProcessing) connEvent);
				break;
				
			case CONNECTION_EVENT_READY_TO_READ:
				handleConnectionEvent_readConnection((ConnectionEvent_readConnection) connEvent);
				break;
				
			case CONNECTION_EVENT_RECENTLY_UPDATED:
				handleConnectionEvent_updatedConnection((ConnectionEvent_updatedConnection) connEvent);
				break;
				
			case CONNECTION_EVENT_READY_TO_WRITE:
				handleConnectionEvent_writeConnection((ConnectionEvent_writeConnection) connEvent);
				break;
				
			case CONNECTION_EVENT_SEND_DACK:
				handleConnectionEvent_sendDack((ConnectionEvent_sendDack) connEvent);
				break;

			case CONNECTION_EVENT_PURGE_MQ:
				handleConnectionEvent_purgeMQ((ConnectionEvent_purgeMQ) connEvent);
				break;
				
			case CONNECTION_EVENT_STATE_CHANGE:
				handleConnectionEvent_stateChange((ConnectionEvent_stateChange) connEvent);
				break;
				
			case CONNECTION_EVENT_MAINTENANCE_FORCE_CONF_ACK:
				handleConnectionEvent_ForceConfAck((ConnectionEvent_ForceConfAck)connEvent);
				break;
				
			case CONNECTION_EVENT_SEND_TPING:
				handleConnectionEvent_sendTPing((ConnectionEvent_sendTPing)connEvent);
				break;
				
			case CONNECTION_EVENT_LOAD_PREPARED_SUBSCRIPTIONS_FILE:
				handleConnectionEvent_loadPrepareSubscriptionsFile((ConnectionEvent_loadPrepareSubscriptionsFile)connEvent);
				break;

			default:
				if(!handleConnectionEvent_Special(connEvent))
					throw new IllegalStateException("Should not reach here: " + connEvent);
		}
		
		LoggerFactory.getLogger().debug(this, "Done handling connectionEvents:"+connEvent);
	}
	
	protected boolean handleConnectionEvent_Special(ConnectionEvent connEvent) {
		return false;
	}
	
	protected void handleConnectionUpdateEvent(IConInfoNonListening<?> conInfoNL)
	{
		ISession session = conInfoNL.getSession();
		if(session==null)
			return;

		synchronized(session)
		{
			if(session.connectionHasBeenProcessed())
				return;
			else
				session.connectionProcessed();
			
			ConnectionInfoNonListeningStatus cStatus = conInfoNL.getStatus();
			// This is when the session is being reconnected after a disconnection
			switch(cStatus) {
			case NL_CANCELLED:
				_sessionManager.connectionBecameDisconnected(session);
				return;

			case NL_CONNECTED:
				_sessionManager.connectionBecameConnected(_brokerShadow, session);
				sendDackOnSession(session);
				return;

			case NL_CONNECTING:
				_sessionManager.connectionBecameConnecting(session);
				return;
				
			default:
				break;
			}
		}
	}

	protected void handleConnectionEvent_renewConnection(ConnectionEvent_renewConnection connEvent) {
		ISession session = connEvent._session;
		_sessionManager.renewSessionsConnection(session);
	}
	
	private void handleConnectionEvent_ForceConfAck(ConnectionEvent_ForceConfAck connEvent) {
		if(Broker.DEBUG)
			LoggerFactory.getLogger().debug(this, "Forcing acknowledgement of confirmations...");
		
		ISession[] allSessions = getAllSessions(); //.values().toArray(new ISession[0]);
		for(int i=0 ; i<allSessions.length ; i++) {
			ISession session = allSessions[i];
			if(session.isConnected())
				acknowledgeTMulticast_Conf(session, null, true);
		}
	}
	
	protected abstract ISession[] getAllSessions();
	
	protected synchronized void acknowledgeTMulticast_Conf(ISession session, TMulticast_Conf conf, boolean force) {
		boolean sendConfAck = false;
		
		if(conf != null)
			sendConfAck = session.receiveTMulticast_Conf(conf);
		
		if(sendConfAck || force) {
			TConf_Ack tConfAck = session.flushTConf_Acks();
			if(tConfAck == null || tConfAck.getTConfSize() == 0)
				return;
			
			IRawPacket raw = PacketFactory.wrapObject(_localSequencer, tConfAck);
			_sessionManager.send(session, raw);
			
			LoggerFactory.getLogger().debug(this, "Sending acknowledgement for " + session);
		}
	}
	
	
	private void handleConnectionEvent_sendTPing(ConnectionEvent_sendTPing connEvent) {
		ISession[] sessions = getAllSessions();
		for(int i=0 ; i<sessions.length ; i++)
			sendTPingOnSession(sessions[i]);
	}
	
	private void handleConnectionEvent_readConnection(ConnectionEvent_readConnection connEvent) {
		IConInfoNonListening<?> readConInfoNL = connEvent._conInfoNL;
		ISession readSession = null;
		switch(readConInfoNL.getChannelType()) {
		case TCP:
			{
				readSession = readConInfoNL.getSession();
				// Continue to UDP
			}

		case UDP:
			{
				IRawPacket raw = null;
				int processed = 0;
				while ( (_status == ComponentStatus.COMPONENT_STATUS_RUNNING) &&
						(raw = readConInfoNL.getNextIncomingData()) !=null)
				{
					InOutBWEnforcer ioBWEnforcer = getBWEnforcer();
					if(ioBWEnforcer != null)
						ioBWEnforcer.addIncomingMessage(raw.getLength());
					processMessage(readSession, raw);
					if(++processed>MAX_MESSAGE_PROCESS_AT_A_TIME) {
						addConnectionEvent(connEvent);
						break;
					}
				}
				break;
			}
			
		default:
			throw new UnsupportedOperationException("Unknown type: " + readConInfoNL.getChannelType());
		}
	}
	
	protected abstract void connectionManagerJustStarted();
	protected abstract ISessionManager createSessionManager(LocalSequencer localSequencer);
	protected abstract void failedSession(ISession fSession);
	protected abstract void processMessage(ISession session, IRawPacket raw);
	protected abstract void handleConnectionEvent_writeConnection(ConnectionEvent_writeConnection connEvent);
	protected abstract void handleConnectionEvent_NewLocalOutgoing(ConnectionEvent_NewLocalOutgoing connEvent);
	protected abstract void handleConnectionEvent_sendDack(ConnectionEvent_sendDack connEvent);
	protected abstract void handleConnectionEvent_purgeMQ(ConnectionEvent_purgeMQ connEvent);
	
	protected void handleConnectionEvent_FastTCommandProcessing(ConnectionEvent_FastTCommandProcessing connEvent) {
		// NIO Channels Summarize 
		_nioBinding.dumpChannelSummary();
		
		InetSocketAddress from = connEvent._from;
		TCommand inTCommand = connEvent._tCommand;
		String inTCommandComment = inTCommand.getComment();
		
		ISession[] sessions = getAllSessions();
		for(int i=0 ; i<sessions.length ; i++) {
			ISession session = sessions[i];
			InetSocketAddress remote = session.getRemoteAddress();
			if(remote == null || remote == from)
				continue;
			
			if(session.getSessionConnectionType() != SessionConnectionType.S_CON_T_ACTIVE)
				continue;
			
			int remotePort = remote.getPort();
			
			TCommand outTCommand = PacketFactory.createTCommandDisseminateMessage("" + remotePort + "->" + inTCommandComment);
			IRawPacket raw = PacketFactory.wrapObject(_localSequencer, outTCommand);
			_sessionManager.send(session, raw);
			LoggerFactory.getLogger().info(this, "Sending '" + outTCommand + "' from '" + (from!=null?from.getPort():null) + "' to '" + remotePort + "'.");			
		}
		LoggerFactory.getLogger().info(this, "Processed '" + inTCommand + "' from '" + from + "'.");
	}
	
	private void handleConnectionEvent_failedConnection(ConnectionEvent_failedConnection connEvent) {
		ISession fSession = connEvent._session;
		failedSession(fSession);
	}

	private void handleConnectionEvent_updatedConnection(ConnectionEvent_updatedConnection connEvent) {
		IConInfoNonListening<?> updatedConInfoNL = connEvent._conInfoNL;
		handleConnectionUpdateEvent(updatedConInfoNL);
	}
	
	private void handleConnectionEvent_stateChange(ConnectionEvent_stateChange connEvent) {
		LoggerFactory.getLogger().info(this, "Handling connectionEvents_stateChange:" + connEvent);
		
		ComponentStatus requestedState = connEvent._requestedState;
		synchronized (_componentStatusChangeWaitObject) 
		{
			if(_componentStateControlInProgress != true)
				throw new IllegalStateException("_componentStateControlInProgress = false! :(");
			
			_componentStateControlInProgress = false;
			switch(requestedState) {
			case COMPONENT_STATUS_PAUSING:
				setComponentState(ComponentStatus.COMPONENT_STATUS_PAUSING);
				break;
				
			case COMPONENT_STATUS_STOPPING:
				setComponentState(ComponentStatus.COMPONENT_STATUS_STOPPING);
				break;
				
			default: 
				throw new UnsupportedOperationException("Donno how to handle this state: " + requestedState);
			}
			
			_componentStatusChangeWaitObject.notify();
		}
	}
	
	////////////////////////////////////////////////////////////////////////
	//							INIO_A_LISTENER							  //
	////////////////////////////////////////////////////////////////////////

	//INIO_A_Listener
	@Override
	public void becomeMyAcceptingListener(IConInfoListening<?> conInfo) {
		if(conInfo == null)
			throw new NullPointerException();
		
		if(!conInfo.getListeningAddress().equals(_localAddress))
			throw new IllegalStateException ("ConnectionManager::becomeMyAcceptingListener(.) - ERROR, this conInfoListening is not known");
		_conInfoL = conInfo;
	}

	//INIO_Listener
	@Override
	public final void becomeMyListener(IConInfo<?> conInfo) {
		return;
	}
	
	//INIO_Listener
	@Override
	public void conInfoUpdated(IConInfo<?> conInfo) {
		if(_conInfoL == conInfo) {
			synchronized (_componentStatusChangeWaitObject) {
				if(_status == ComponentStatus.COMPONENT_STATUS_STARTING && _conInfoL.isListening()) {
					setComponentState(ComponentStatus.COMPONENT_STATUS_RUNNING);
					scheduleNextPurgeMQ();
					scheduleNextDack();
					scheduleNextTPing();
				}
			}
			return;
		}
		
		IConInfoNonListening<?> conInfoNL = (IConInfoNonListening<?>) conInfo;
		
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent = new ConnectionEvent_updatedConnection(localSequencer, conInfoNL);
		addConnectionEvent(connEvent);
	}
	
	
	////////////////////////////////////////////////////////////////////////
	//							INIO_R_LISTENER							  //
	////////////////////////////////////////////////////////////////////////

	//INIO_R_Listener
	@Override
	public final void becomeMyReaderListener(IConInfoNonListening<?> conInfo) {
		return;
	}

	//INIO_R_Listener
	@Override
	public final void conInfoGotFirstDataItem(IConInfoNonListening<?> conInfo) {
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent = new ConnectionEvent_readConnection(localSequencer, conInfo);
		addConnectionEvent(connEvent);
	}

	
	////////////////////////////////////////////////////////////////////////
	//							INIO_W_LISTENER							  //
	////////////////////////////////////////////////////////////////////////

	//INIO_W_Listener
	@Override
	public final void becomeMyWriteListener(IConInfoNonListening<?> conInfo) {
		return;
	}

	//INIO_W_Listener
	@Override
	public final void conInfoGotEmptySpace(IConInfoNonListening<?> conInfo) {
		ISession session = conInfo.getSession();
		if(session == null)
			throw new IllegalStateException("Session cannot be null!" + conInfo);
		
		LoggerFactory.getLogger().debug(this, "Connection got empty outgoing space: " + session);
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return;
		ConnectionEvent connEvent = new ConnectionEvent_writeConnection(localSequencer, conInfo);
		LoggerFactory.getLogger().debug(this, "Adding to connectionEvents: " + connEvent);
		addConnectionEvent(connEvent);
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	//								PRIVATE METHODS							 //
	///////////////////////////////////////////////////////////////////////////
	protected final static boolean isOutgoingValid(InetSocketAddress A, InetSocketAddress B) {
		if(A.equals(B))
			throw new IllegalStateException("ConnectionManager::isOutgoingValid(.) - ERROR, cannot connect to oneself.");
		byte ipLocal[] = A.getAddress().getAddress();
		byte ipRemote[] = B.getAddress().getAddress();
		
		if((((int)ipLocal[0])&0xFF) < (((int)ipRemote[0])&0xFF))
			return true;
		if((((int)ipLocal[0])&0xFF) > (((int)ipRemote[0])&0xFF))
			return false;

		if((((int)ipLocal[1])&0xFF) < (((int)ipRemote[1])&0xFF))
			return true;
		if((((int)ipLocal[1])&0xFF) > (((int)ipRemote[1])&0xFF))
			return false;

		if((((int)ipLocal[2])&0xFF) < (((int)ipRemote[2])&0xFF))
			return true;
		if((((int)ipLocal[2])&0xFF) > (((int)ipRemote[2])&0xFF))
			return false;

		if((((int)ipLocal[3])&0xFF) < (((int)ipRemote[3])&0xFF))
			return true;
		if((((int)ipLocal[3])&0xFF) > (((int)ipRemote[3])&0xFF))
			return false;

		int localPort = A.getPort();
		int remotePort = B.getPort();
		
		if(localPort < remotePort)
			return true;
		else 
			return false;
	}
	
	///////////////////////////////////////////////////////////////////////////
	//								PRIVATE METHODS							 //
	///////////////////////////////////////////////////////////////////////////
	protected static String getDefaultSessionsDupmFileName(LocalSequencer localSequencer) {
		return getDefaultBrokerName(localSequencer) + ".stop";
	}
	
	protected static String getDefaultSubDumpFileName(LocalSequencer localSequencer) {
		if(localSequencer == null)
			return "NULL";
		return getDefaultBrokerName(localSequencer) + ".sub";
	}
	
	protected static String getDefaultBrokerName(IBrokerShadow brokerShadow) {
		return brokerShadow.getLocalAddress().toString();
	}
	
	protected static String getDefaultBrokerName(LocalSequencer localSequencer) {
		return localSequencer.getLocalAddress().toString();
	}
	
	@Override
	public boolean issueTCommandDisseminateMessage(TCommand tCommand) {
		return handleTCommandDisseminateMessage(tCommand, null);
	}
	
	protected boolean handleTCommandDisseminateMessage(TCommand tCommand, InetSocketAddress from) {
		LocalSequencer localSequencer = _localSequencer;
		if(localSequencer == null)
			return true;
		ConnectionEvent connEvent = new ConnectionEvent_FastTCommandProcessing(localSequencer, tCommand, from);
		addConnectionEventHead(connEvent);
		return true;
	}
	
	@Override
	public synchronized boolean checkOutputBWAvailability() {
		InOutBWEnforcer ioBWEnforcer = getBWEnforcer();
		if(ioBWEnforcer.hasRemaingBW())
			return true;
		StatisticsLogger statLogger = _brokerShadow.getStatisticsLogger();
		if(statLogger!=null)
			statLogger.addedNewBWInavailabilityEvent(1);
		return false;
	}
	
	protected void checkBWAvailability() {
		InOutBWEnforcer ioBWEnforcer = getBWEnforcer();
		while (!ioBWEnforcer.hasRemaingBW()) {
			try {
				StatisticsLogger statLogger = _brokerShadow.getStatisticsLogger();
				if(statLogger!=null)
					statLogger.addedNewBWInavailabilityEvent(1);
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		throw new UnsupportedOperationException("Deprecated!");
	}
	
	@Override
	public abstract boolean loadPrepareSubscription(SubscriptionEntry subEntry);
	
	@Override
	public final void loadPrepareSubscriptionsFile(String subscriptionFile, BrokerIdentityManager idManager) {
		ConnectionEvent_loadPrepareSubscriptionsFile connEvent =
			new ConnectionEvent_loadPrepareSubscriptionsFile(_localSequencer, subscriptionFile, idManager);
		addConnectionEvent(connEvent);
	}
	
	protected void handleConnectionEvent_loadPrepareSubscriptionsFile(ConnectionEvent_loadPrepareSubscriptionsFile connEvent) {
		try {
			String subscriptionFile = connEvent._subscriptionFile;
			BrokerIdentityManager idManager = connEvent._idManager;
			LoggerFactory.getLogger().info(this, "Loading subscription file: " + subscriptionFile);
			
			BufferedReader reader = new BufferedReader(new FileReader(subscriptionFile));
			String line = null;
			while((line = reader.readLine()) != null) {
				SubscriptionEntry subEntry = SubscriptionEntry.decode(line, idManager);
				if(subEntry==null)
					continue;
				
				InetSocketAddress from = subEntry._orgFrom;
				InetSocketAddress brokerFrom = _brokerShadow.getBrokerIdentityManager().getJoinpointAddress(from.toString());
				if(brokerFrom != null && !from.equals(_localAddress) && !brokerFrom.equals(_localAddress))
					subEntry._orgFrom = brokerFrom;
				
				if(!loadPrepareSubscription(subEntry)) {
					LoggerFactory.getLogger().error(
							this, "Loading prepared subscriptions from file (" + subscriptionFile + ") failed: " + subEntry);
					return;
				}
			}
			
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	@Override
	public LocalSequencer getLocalSequencer() {
		return _localSequencer;
	}
	
	public abstract String getThreadName();
	
	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}
}

