package org.msrg.publiy.broker.core.maintenance;

import java.util.TimerTask;

import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.component.ComponentStatus;
import org.msrg.publiy.component.IComponent;



public class MaintenanceManager extends MaintenanceManagerWithNoBroker {
	
	public static final long MAINTENANCE_GC_INTERVAL = 300000;

	protected final IBroker _broker;
	protected final IBrokerShadow _brokerShadow;
	
	private boolean _destroyed = false;
	private Object _lock = new Object();
	
	public MaintenanceManager(IBroker broker, IBrokerShadow brokerShadow){
		super(broker.getBrokerID());
		_broker = broker;
		_brokerShadow = brokerShadow;
		((BrokerShadow)_brokerShadow).setMaintenanceManager(this);
		
		_broker.addNewComponentListener(this);
	}
	
	@Override
	public boolean scheduleMaintenanceTask(TimerTask task, long delay){
		synchronized (_lock)
		{
			if ( _destroyed )
				return false;
				
			if ( _broker.getBrokerOpState() == BrokerOpState.BRKR_END )
				return false;
			
			_timer.schedule(task, delay);
			return true;
		}
	}
	
	private void destroy() {
		synchronized (_lock)
		{
			if ( _destroyed )
				throw new IllegalStateException("Mainenance Manager is already destroyed.");
			
			_destroyed = true;
			_timer.cancel();
		}
	}
	
	private void initializeAll(){
		scheduleNextGC_maintenanceTask();
	}

	@Override
	public synchronized void componentStateChanged(IComponent comonent) {
		if ( comonent != _broker )
			throw new IllegalArgumentException("This '" + comonent + "' is not my broker: '" + _broker + "'.");
		
		ComponentStatus brokerStatus = _broker.getBrokerComponentState();
		switch(brokerStatus){
		case COMPONENT_STATUS_STOPPED:
			destroy();
			break;
			
		case COMPONENT_STATUS_RUNNING:
			initializeAll();
			break;
			
		default:
			break;
			
		}
	}

}
