package org.msrg.publiy.broker;

import java.net.InetSocketAddress;

import org.msrg.raccoon.utils.BytesUtil;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimerReading;

public class BFTSuspecionReason implements Comparable<BFTSuspecionReason> {

	public final BrokerInternalTimerReading _suspecionTime;
	public final IBFTSuspectable _suspectable;
	public final InetSocketAddress _affectedRemote;
	public final String _reason;
	public final boolean _newSuspicion;
	public final boolean _reactToThis;
	
	public BFTSuspecionReason(boolean reactToThis, boolean registerWithSuspectable, IBFTSuspectable suspectable, InetSocketAddress affectedRemote, String reason) {
		_suspecionTime = BrokerInternalTimer.read();
		_suspectable = suspectable;
		_newSuspicion = _suspectable == null ? true : _suspectable.remoteIsAffected(affectedRemote);
		if(registerWithSuspectable && _suspectable != null)
			_suspectable.addAffectedRemote(affectedRemote);
		_reason = reason;
		_affectedRemote = affectedRemote;
		_reactToThis = reactToThis;
	}
	
	public boolean reactToThis() {
		return _reactToThis;
	}
	
	@Override
	public String toString() {
		return "@" + _suspecionTime + (reactToThis() ? '+' : '-') + " suspecting " + _suspectable.getAddress() + " for " + _affectedRemote + " ***" + _reason + "****";
	}

	public String toString(BrokerIdentityManager idMan) {
		if(idMan == null)
			return toString();
		
		return "@" + _suspecionTime + (reactToThis() ? '+' : '-') + " suspecting " + idMan.getBrokerId(_suspectable.getAddress()) + " for " + idMan.getBrokerId(_affectedRemote) + " ***" + _reason + "****";
	}

	@Override
	public int compareTo(BFTSuspecionReason o) {
		int timeComparison = this._suspecionTime.compareTo(o._suspecionTime);
		if(timeComparison != 0)
			return timeComparison;
		
		int suspectableComparison = BytesUtil.compareAddresses(_suspectable.getAddress(), o._suspectable.getAddress());
		if(suspectableComparison != 0)
			return suspectableComparison;
		
		return BytesUtil.compareAddresses(_affectedRemote, o._affectedRemote);
	}
	
	public boolean isThisANewSuspicion() {
		return _newSuspicion;
	}
	
	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	public boolean affects(InetSocketAddress suspectedNeighbor) {
		return _affectedRemote.equals(suspectedNeighbor);
	}
}
