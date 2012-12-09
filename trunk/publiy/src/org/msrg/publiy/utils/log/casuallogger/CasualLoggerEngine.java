package org.msrg.publiy.utils.log.casuallogger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.component.ComponentStatus;
import org.msrg.publiy.component.IComponent;
import org.msrg.publiy.component.IComponentListener;

import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;



public class CasualLoggerEngine implements IComponent {
	
	class CasualLoggerTimerTask extends BrokerTimerTask {
		
		private final ICasualLogger _casualLogger;
		private final boolean _logOrForceFlush;
		
		CasualLoggerTimerTask(ICasualLogger casualLogger, boolean logOrForceFlush) {
			super(BrokerTimerTaskType.BTimerTask_CasualLogger);
			_casualLogger = casualLogger;
			_logOrForceFlush = logOrForceFlush;
			
			super.logCreation();
		}

		@Override
		public void run() {
			super.run();
			synchronized (_componentStateLock) 
			{
				while ( _state != ComponentStatus.COMPONENT_STATUS_RUNNING)
					if(_state == ComponentStatus.COMPONENT_STATUS_PAUSED || _state == ComponentStatus.COMPONENT_STATUS_STOPPED)
						return;
					else
						try{_componentStateLock.wait();}catch(InterruptedException itx) {}
			
				if(!_casualLogger.isEnabled())
					return;
				
				if(_logOrForceFlush)
				{
					_casualLogger.performLogging();
					scheduleLogging(_casualLogger);
				}
				else
				{
					_casualLogger.forceFlush();
					scheduleFlush(_casualLogger);
				}
			}
		}

		@Override
		public String toStringDetails() {
			return "";
		}
		
	}
	
	private final Set<ICasualLogger> _casualLoggers;
	private final IBrokerShadow _brokerShadow;
	private final String _id;
	private ComponentStatus _state = ComponentStatus.COMPONENT_STATUS_UNINITIALIZED;
	private final Object _componentStateLock = new Object();
	
	public CasualLoggerEngine(BrokerShadow brokerShadow) {
		brokerShadow.setCasualLoggerEngine(this);
		_state = ComponentStatus.COMPONENT_STATUS_UNINITIALIZED;
		_brokerShadow = brokerShadow;
		_id = brokerShadow.getBrokerID();
		_casualLoggers = new HashSet<ICasualLogger>();
	}
	
	private String getNamePrefix() {
		return "CasualLoggerEngine-";
	}
	
	public void registerCasualLogger(ICasualLogger casualLogger) {
		synchronized (_componentStateLock)
		{
			_casualLoggers.add(casualLogger);
		}
	}
	
	public void deregisterCasualLogger(ICasualLogger casualLogger) {
		synchronized (_componentStateLock)
		{
			_casualLoggers.remove(casualLogger);
		}
	}

	@Override
	public void addNewComponentListener(IComponentListener comListener) {
		throw new UnsupportedOperationException("CasualLoggerEngine does not accept component listeners!");
	}

	@Override
	public void removeComponentListener(IComponentListener comListener) {
		throw new UnsupportedOperationException("CasualLoggerEngine does not accept component listeners!");		
	}
	
	@Override
	public void awakeFromPause() {
		synchronized (_componentStateLock)
		{
			if(_state == ComponentStatus.COMPONENT_STATUS_PAUSED)
			{
				_state = ComponentStatus.COMPONENT_STATUS_STARTING;
				
				scheduleAllLogging();
				scheduleAllFlushing();
				
				_state = ComponentStatus.COMPONENT_STATUS_RUNNING;
				_componentStateLock.notify();
			}
			else
				throw new IllegalStateException("ComponentState must be COMPONENT_STATUS_PAUSED, it is: " + _state);
		}
	}

	@Override
	public String getComponentName() {
		return getNamePrefix() + _id;
	}

	@Override
	public ComponentStatus getComponentState() {
		return _state;
	}

	@Override
	public void pauseComponent() {
		synchronized (_componentStateLock) 
		{
			if(_state != ComponentStatus.COMPONENT_STATUS_RUNNING)
				throw new IllegalStateException("ComponentState must be COMPONENT_STATUS_RUNNING, it is: " + _state);
			
			flushAll();
			
			_state = ComponentStatus.COMPONENT_STATUS_PAUSED;
			_componentStateLock.notify();
		}
	}

	@Override
	public void prepareToStart() {
		synchronized (_componentStateLock) 
		{
			if(_state != ComponentStatus.COMPONENT_STATUS_UNINITIALIZED)
				throw new IllegalStateException("ComponentState must be COMPONENT_STATUS_UNINITIALIZED, it is: " + _state);
			
			_state = ComponentStatus.COMPONENT_STATUS_INITIALIZED;
			_componentStateLock.notify();
		}
	}

	@Override
	public void startComponent() {
		synchronized (_componentStateLock) 
		{
			if(_state != ComponentStatus.COMPONENT_STATUS_INITIALIZED)
				throw new IllegalStateException("ComponentState must be COMPONENT_STATUS_INITIALIZED, it is: " + _state);
			
			scheduleAllLogging();
			scheduleAllFlushing();
			
			_state = ComponentStatus.COMPONENT_STATUS_RUNNING;
			_componentStateLock.notify();
		}
	}

	private void scheduleAllLogging() {
		Iterator<ICasualLogger> casualLoggersIt = _casualLoggers.iterator();
		while ( casualLoggersIt.hasNext()) {
			ICasualLogger casualLogger = casualLoggersIt.next();

			scheduleLogging(casualLogger);
		}
	}
	
	private void scheduleLogging(ICasualLogger casualLogger) {
		if(!casualLogger.isEnabled())
			return;
		
		long nextLogging = casualLogger.getNextDesiredLoggingTime();
		CasualLoggerTimerTask nextTimerLogTask = new CasualLoggerTimerTask(casualLogger, true);
		_brokerShadow.getMaintenanceManager().scheduleMaintenanceTask(nextTimerLogTask, nextLogging);
	}
	
	private void scheduleFlush(ICasualLogger casualLogger) {
		if(!casualLogger.isEnabled())
			return;
		
		long nextFlush = casualLogger.getNextDesiredForcedFlushTime();
		CasualLoggerTimerTask nextTimerLogTask = new CasualLoggerTimerTask(casualLogger, false);
		_brokerShadow.getMaintenanceManager().scheduleMaintenanceTask(nextTimerLogTask, nextFlush);
	}
	
	private void scheduleAllFlushing() {
		Iterator<ICasualLogger> casualLoggersIt = _casualLoggers.iterator();
		while ( casualLoggersIt.hasNext()) {
			ICasualLogger casualLogger = casualLoggersIt.next();

			scheduleFlush(casualLogger);
		}
	}

	private void flushAll() {
		Iterator<ICasualLogger> casualLoggersIt = _casualLoggers.iterator();
		while ( casualLoggersIt.hasNext()) {
			ICasualLogger casualLogger = casualLoggersIt.next();
			casualLogger.forceFlush();
		}
	}
	
	@Override
	public void stopComponent() {
		synchronized (_componentStateLock) 
		{
			_state = ComponentStatus.COMPONENT_STATUS_STOPPING;
			
			flushAll();
			
			_state = ComponentStatus.COMPONENT_STATUS_STOPPED;
			_componentStateLock.notify();
		}
	}
	
}
