package org.msrg.publiy.broker.core.connectionManager;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.msrg.publiy.utils.log.casuallogger.TransitionsLogger;

import org.msrg.publiy.communication.core.sessions.SessionConsiderationTransition;
import org.msrg.publiy.communication.core.sessions.SessionConsideration;
import org.msrg.publiy.communication.core.sessions.SessionConsiderationTransitionType;

public class SessionsConsiderationList {
	
	private final Map<InetSocketAddress, SessionConsideration> _considerationList;
	protected final TransitionsLogger _transitionLogger;
	
	SessionsConsiderationList(TransitionsLogger transitionLogger){
		_considerationList = new LinkedHashMap<InetSocketAddress, SessionConsideration>();
		_transitionLogger = transitionLogger;
	}
	
	public SessionConsideration getSessionConsideration(InetSocketAddress remote){
		if ( remote == null )
			return null;
		else
			return _considerationList.get(remote);
	}
	
	private SessionConsideration removeSessionConsideration(InetSocketAddress remote, SessionConsideration consideration){
		if ( consideration == null )
			return null;
		SessionConsideration existingConsidertion = _considerationList.remove(remote);
		if ( existingConsidertion != consideration )
			throw new IllegalStateException(existingConsidertion + " _ " + this);
		
		return existingConsidertion;
	}
	
	public SessionConsideration updateSessionsConsideration(SessionConsiderationTransition transition){
		_transitionLogger.logTransition(transition);
		
		InetSocketAddress address = transition._address;
		SessionConsideration newConsideration = transition._newSessionConsideration;
		SessionConsideration oldConsideration = transition._oldSessionConsideration;
		SessionConsideration retConsideration = null; 
		SessionConsiderationTransitionType transitionType = transition._transitionType;
		switch(transitionType){
		case S_CONSIDER_TRANSITION_INITIATE_CANDIDATE:
			if ( newConsideration == null || newConsideration._considerationStatus != ConsiderationStatus.S_CONSIDERATION_INITIATED)
				throw new IllegalStateException(transition.toString());
			else
				retConsideration = insertSessionsConsideration(address, newConsideration, oldConsideration);
			break;
			
		case S_CONSIDER_TRANSITION_PROMOTE_SOFT:
			if ( oldConsideration._considerationStatus != ConsiderationStatus.S_CONSIDERATION_INITIATED )
				throw new IllegalStateException(transition.toString());
			else if ( newConsideration == null || newConsideration._considerationStatus != ConsiderationStatus.S_CONSIDERATION_ONGOING)
				throw new IllegalStateException(transition.toString());
			else
				retConsideration = insertSessionsConsideration(address, newConsideration, oldConsideration);
			break;
			
		case S_CONSIDER_TRANSITION_REMOVE:
			if ( newConsideration != null )
				throw new IllegalStateException("NewConsideration in the transition_remove is not null: " + transition);
			else
				retConsideration = removeSessionConsideration(address, oldConsideration);
			break;
			
		case S_CONSIDER_TRANSITION_DEMOTE:
			if ( newConsideration == null || oldConsideration == null )
				throw new IllegalStateException("considerations must not be null: " + transition);
			else
				retConsideration = insertSessionsConsideration(address, newConsideration, oldConsideration);
			break;
			
		default:
			throw new UnsupportedOperationException("Donno how to handle this transition: " + transition + " _ " + this);
		}

		return retConsideration;
	}
			
	private SessionConsideration insertSessionsConsideration(InetSocketAddress address, SessionConsideration newConsideration, SessionConsideration oldConsideration){
		SessionConsideration existingConsideration = _considerationList.put(address, newConsideration);
		if ( existingConsideration != oldConsideration )
			throw new IllegalStateException(existingConsideration + " vs. " + oldConsideration);
		
		if ( _considerationList.get(address) != newConsideration) 
			throw new IllegalStateException(newConsideration.toString());
		
		return existingConsideration;
	}

	public InetSocketAddress[] getAllConsiderationAddresses(){
		Set<InetSocketAddress> keySet = _considerationList.keySet();
		return keySet.toArray(new InetSocketAddress[0]);
	}
	
	@Override
	public String toString(){
		return "ConsiderationList: " + _considerationList.toString();
	}
}
