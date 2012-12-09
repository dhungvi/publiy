package org.msrg.publiy.communication.core.sessions;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.connectionManager.ConsiderationStatus;

import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;

public class SessionConsideration {
	public final InetSocketAddress _address;
	public final long _considerationTime;
	public final ConsiderationStatus _considerationStatus;
	
	public SessionConsideration(InetSocketAddress address, ConsiderationStatus considerationStatus){
		if ( address == null )
			throw new NullPointerException();
		
		if ( considerationStatus == null )
			throw new NullPointerException();
		
		_address = address;
		_considerationTime = SystemTime.currentTimeMillis();
		_considerationStatus = considerationStatus;
	}
	
	@Override
	public boolean equals(Object o){
		if ( o == null )
			return false;
		else if ( SessionConsideration.class.isInstance(o) )
		{
			SessionConsideration oConsideration = (SessionConsideration) o;
			return oConsideration._address.equals(_address);
		}
		else if ( SessionConsiderationTransition.class.isInstance(o) ){
			SessionConsiderationTransition oConsiderationTransition = (SessionConsiderationTransition) o;
			return oConsiderationTransition.equals(_address);
		}
		else
			return false;
	}

	@Override
	public int hashCode(){
		return _address.hashCode();
	}
	
	public boolean isValid(long now){
		return now < _considerationTime + _considerationStatus._VALIDITY_PERIOD;		
	}
	
	public boolean isValid(){
		return isValid(SystemTime.currentTimeMillis());
	}
	
	@Override
	public String toString(){
		return "Consider[" + Writers.write(_address) + ":" + _considerationStatus + "]";
	}
	
	public ConsiderationStatus getConsiderationStatus(){
		return _considerationStatus;
	}
}
