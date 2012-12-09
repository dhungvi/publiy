package org.msrg.publiy.communication.core.niobinding;

import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

import org.msrg.publiy.broker.core.IConnectionManager;

public class SessionRenewTask extends BrokerTimerTask implements ILoggerSource {

	private ISession _session;
	private IConnectionManager _connectionManager;
	
	public SessionRenewTask(IConnectionManager conManager, ISession session){
		super(BrokerTimerTaskType.BTimerTask_RenewSesssion);
		_session = session;
		_connectionManager = conManager;
		
		super.logCreation();
	}
	
	@Override
	public void run() {
		super.run();
		try{
		LoggerFactory.getLogger().info(this, "Starting to renew session: "  + _session + " with conMan: " + _connectionManager);		
//		IConnectionManager nextConnectionManager = _connectionManager.getNextConnctionManager();
//		if ( nextConnectionManager != null ){
//			_connectionManager = nextConnectionManager;
//			Logger.getLogger().debug(this, "Changing to 'next' connectionManager: " + _connectionManager);
//		}
//		
//		ISessionManager sessionManager = _connectionManager.getSessionManager();
//		synchronized (_session) {
//			synchronized(_connectionManager){
				_connectionManager.renewSessionsConnection(_session);
//				ISessionManager.renewSessionsConnection(_session);
//			}
//		}
		
		LoggerFactory.getLogger().info(this, "Finished renewing session: "  + _session + " with conMan: " + _connectionManager);
		}catch(Exception ex){
			LoggerFactory.getLogger().infoX(this, ex, "Exception in '" + this + "'");
		}
	}
	

	@Override
	public String toStringDetails() {
		return "" + _session;
	}
}
