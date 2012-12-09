package org.msrg.publiy.communication.core.sessions;

import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.msrg.publiy.utils.Writers;

public class SessionConsiderationTransition {
	public static DateFormat DATE_FORMATE = new SimpleDateFormat("mm:ss");
	
	public final Date _time;
	public final String _comment;
	public final InetSocketAddress _address;
	public final SessionConsiderationTransitionType _transitionType;
	public final SessionConsideration _oldSessionConsideration;
	public final SessionConsideration _newSessionConsideration;
	
	public SessionConsiderationTransition(String comment, InetSocketAddress address, SessionConsideration oldSessionConsideration,
			SessionConsideration newSessionConsideration, SessionConsiderationTransitionType transitionType)
	{
		if ( address == null )
			throw new IllegalArgumentException("Address is null: " + transitionType);
		else if( oldSessionConsideration != null && !address.equals(oldSessionConsideration._address) )
			throw new IllegalArgumentException("oldSessionConsideration's address is mismatch: " + oldSessionConsideration + " vs. " + address);
		else if ( newSessionConsideration != null && !address.equals(newSessionConsideration._address) )
			throw new IllegalArgumentException("newSessionConsideration's address is mismatch: " + newSessionConsideration + " vs. " + address);
		
		if ( oldSessionConsideration == newSessionConsideration )
			throw new IllegalArgumentException("No transition from the smae to the smae: " + address + " _ " + transitionType);
		
		if ( transitionType == null )
			throw new IllegalArgumentException("Type is null: " + address);
		
		_comment = comment;
		_address = address;
		_transitionType = transitionType;
		_oldSessionConsideration = oldSessionConsideration;
		_newSessionConsideration = newSessionConsideration;
		
		_time = new Date();
	}

	@Override
	public int hashCode(){
		return _address.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		if ( SessionConsiderationTransition.class.isInstance(o) ){
			SessionConsiderationTransition oTransition = (SessionConsiderationTransition) o;
			return this._address.equals(oTransition._address);
		}
		
		// Don't count on this..
		if ( SessionConsideration.class.isInstance(o) ){
			SessionConsideration oConsideration = (SessionConsideration) o;
			return this._address.equals(oConsideration._address);
		}
		
		return false;
	}
	
	public String toString(){
		return DATE_FORMATE.format(_time) + " " + Writers.write(_address) + "(" + _transitionType + ") \t old: " + _oldSessionConsideration + "\t new: " + _newSessionConsideration + ")\t: " + _comment;
	}
}
