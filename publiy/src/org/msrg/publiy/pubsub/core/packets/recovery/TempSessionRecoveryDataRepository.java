package org.msrg.publiy.pubsub.core.packets.recovery;

import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.communication.core.packet.types.TSessionInitiation;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

public class TempSessionRecoveryDataRepository implements ILoggerSource {

	private static final TSessionInitiation[] sInitArraySample = new TSessionInitiation[0];
	private static final TMulticast[] tmArraySample = new TMulticast[0];
	private static final TRecovery_Join [] trArraySampleJoin = new TRecovery_Join[0];
	private static final TRecovery_Subscription [] trArraySampleSub = new TRecovery_Subscription[0];
	
	private List<TSessionInitiation> _sInitMessages = new LinkedList<TSessionInitiation>();
	private List<TMulticast> _confirmedMessages = new LinkedList<TMulticast>();
	private List<TMulticast> _unconfirmedMessages = new LinkedList<TMulticast>();
	private List<TRecovery_Join> _recoveryMessagesJoin = new LinkedList<TRecovery_Join>();
	private List<TRecovery_Subscription> _recoveryMessagesSubscription = new LinkedList<TRecovery_Subscription>();
	
	private boolean _lastRecoveryJoinRecieved = false;
	private boolean _lastRecoverySubscriptionReceived = false;
	private final ISession _session;
	
	public TempSessionRecoveryDataRepository(ISession session){
		_session = session;
	}
	
	public void flushTempSessionRecoveryData(){
		_sInitMessages = new LinkedList<TSessionInitiation>();
		_confirmedMessages = new LinkedList<TMulticast>();
		_unconfirmedMessages = new LinkedList<TMulticast>();
		_recoveryMessagesJoin = new LinkedList<TRecovery_Join>();
		_recoveryMessagesSubscription = new LinkedList<TRecovery_Subscription>();
		
		_lastRecoveryJoinRecieved = false;
		_lastRecoverySubscriptionReceived = false;
	}
	
	public void addNewSInitMessage(TSessionInitiation sInit){
		LoggerFactory.getLogger().debug(this, "Adding new SInit msg: " + sInit);
		_sInitMessages.add(sInit);
	}
	
	public void addNewConfirmedMessage(TMulticast tm){
		LoggerFactory.getLogger().debug(this, "Adding new TM msg: " + tm);
		_confirmedMessages.add(tm);
	}
	
	public void addNewUnconfirmedMessage(TMulticast tm){
		LoggerFactory.getLogger().debug(this, "Adding new Unconfirmed TM msg: " + tm);
		_unconfirmedMessages.add(tm);
	}
	
	public void addNewRecoveryMessage(TRecovery tr){
		LoggerFactory.getLogger().debug(this, "Adding new TR msg: " + tr);
		
		switch(tr.getType()){
		case T_RECOVERY_LAST_JOIN:
			if ( _lastRecoveryJoinRecieved == false )
				_lastRecoveryJoinRecieved = true;
			break;

		case T_RECOVERY_LAST_SUBSCRIPTION:
			if ( _lastRecoverySubscriptionReceived == false )
				_lastRecoverySubscriptionReceived = true;
			break;

		case T_RECOVERY_SUBSCRIPTION:
			if ( _lastRecoverySubscriptionReceived ){
				Exception ex = new IllegalStateException("Recieving a new subscribe recovery message after TR_subscribe_last: " + tr + " on session: " + _session);
				LoggerFactory.getLogger().infoX(this, ex, "DAMN IT!");
			}
			else
				_recoveryMessagesSubscription.add((TRecovery_Subscription) tr);
			break;
			
		case T_RECOVERY_JOIN:
			if ( _lastRecoveryJoinRecieved ){
				Exception ex = new IllegalStateException("Recieving a new join recovery message after TR_join_last: " + tr + " on session: " + _session);
				LoggerFactory.getLogger().infoX(this, ex, "DAMN IT!");
			}
			else
				_recoveryMessagesJoin.add((TRecovery_Join)tr);
			break;
			
		default:
			break;
		}
	}
	
	public TSessionInitiation[] getAllSInitMessages(){
		return _sInitMessages.toArray(sInitArraySample);
	}
	
	public TRecovery_Subscription[] getAllRecoverySubscriptionMessages(){
		return _recoveryMessagesSubscription.toArray(trArraySampleSub);
	}
	
	public TRecovery_Join[] getAllRecoveryJoinMessages(){
		return _recoveryMessagesJoin.toArray(trArraySampleJoin);
	}
	
	public TMulticast[] getAllConfirmedMulticastMessages(){
		return _confirmedMessages.toArray(tmArraySample);
	}
	
	public TMulticast[] getAllUnconfirmedMulticastMessages(){
		return _unconfirmedMessages.toArray(tmArraySample);
	}

	public boolean isAllRecoveryDataReceived(){
		return (_lastRecoveryJoinRecieved == true) && 
				(_lastRecoverySubscriptionReceived == true);
	}
	
	public boolean isAllRecoveryJoinDataReceived(){
		return _lastRecoveryJoinRecieved;
	}
	
	public boolean isAllrecoverySubscirptionReceived(){
		return _lastRecoverySubscriptionReceived;
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_TEMP_RECOVERY_DATA;
	}
}
