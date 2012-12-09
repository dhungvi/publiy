package org.msrg.publiy.communication.core.sessions;

import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.communication.core.packet.IRawPacket;

import org.msrg.publiy.pubsub.multipath.loadweights.ProfilerBucket;
import org.msrg.publiy.utils.ElementTotal;
import org.msrg.publiy.utils.HistoryType;
import org.msrg.publiy.utils.LimittedSizeList;
import org.msrg.publiy.utils.log.casuallogger.MessageProfilerLogger;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;

public class ISessionMP extends ISession { // implements Comparable<ISessionMP>{
	private ISession _realSession = this;
	protected Set<ISessionMP> _redirectedSessions = new HashSet<ISessionMP>();
	private int _distance = -1;
	private double _normalizationFactor = 1;
	private double _inPubTotal = -1;
	private double _outPubTotal = -1;
	private boolean _isInPubTrafficAvgValid = false;
	private boolean _isOutPubTrafficAvgValid = false;
	
	ISessionMP(IBrokerShadow brokerShadow, SessionObjectTypes sessionObjectType, SessionTypes type) {
		super(brokerShadow, sessionObjectType, type);
	}
	
	@Override
	public void resetEverything(boolean preserveRetryCount) {
		super.resetEverything(preserveRetryCount);
		
		_inPubTotal = -1;
		_isInPubTrafficAvgValid = false;
		_outPubTotal = -1;
		_isOutPubTrafficAvgValid = false;
	}
	
	public double getTotalNonnormalizedOutPublications() {
		if(_isOutPubTrafficAvgValid)
			return _outPubTotal;
		else
			return -1;
	}
	
	public double getTotalOutPublications() {
		if(_isOutPubTrafficAvgValid)
			return _outPubTotal * _normalizationFactor;
		else
			return -1;
	}
	
	public double getTotalInPublitcations() {
		if(_isInPubTrafficAvgValid)
			return _inPubTotal * _normalizationFactor;
		else
			return -1;
	}
	
	public ElementTotal<LimittedSizeList<ProfilerBucket>> getOutElementTotalPublications() {
		return _brokerShadow.getMessageProfilerLogger().getHistoryOfType(HistoryType.HIST_T_PUB_OUT_BUTLAST, _remote);
	}

	public ElementTotal<LimittedSizeList<ProfilerBucket>> getInElementTotalPublications() {
		return _brokerShadow.getMessageProfilerLogger().getHistoryOfType(HistoryType.HIST_T_PUB_IN_BUTLAST, _remote);
	}

	public int compareISessionMPCounters(ISessionMP o) {
		if(_session_index > o._session_index)
			return -1;
		if(_session_index < o._session_index)
			return 1;
		else
			throw new IllegalStateException(_session_index + " vs. " + o._session_index);
	}
	
	public void setDistance(int distance) {
		_distance = distance;
	}
	
	public boolean isDistanceSet() {
		return _distance != -1;
	}
	
	public int getDistance() {
		return _distance;
	}
	
	private int compareToPrivately(ISessionMP o) {
		if(_distance == -1 || o._distance == -1)
			throw new IllegalStateException("" + _distance + "" + o._distance + "_" + this + " vs. " + o);
		
		if(_isOutPubTrafficAvgValid) {
			if(_isOutPubTrafficAvgValid) {
				if(_outPubTotal * _distance > o._outPubTotal * o._distance)
					return -1;
				else if(_outPubTotal * _distance < o._outPubTotal * o._distance)
					return 1;
				else
					return compareISessionMPCounters(o);
			}
			else
				return o.compareToPrivately(this);
		}
		else
			return compareISessionMPCounters(o);
	}
	
	public void updatePublicationAverages(double sessionNormalizationFactor) {
		_normalizationFactor = sessionNormalizationFactor;
		
		ElementTotal<LimittedSizeList<ProfilerBucket>> inTotal = getInElementTotalPublications();
		if(inTotal == null) {
			_inPubTotal = -1;
			_isInPubTrafficAvgValid = false;
		}
		else{
			_inPubTotal = inTotal._total;
			if(inTotal._totaledValuesCount < MessageProfilerLogger.PROFILER_CACHE_SIZE - 1)
				_isInPubTrafficAvgValid = false;
			else
				_isInPubTrafficAvgValid = true;
		}
			
		ElementTotal<LimittedSizeList<ProfilerBucket>> outAvg = getOutElementTotalPublications();
		if(outAvg == null) {
			_outPubTotal = -1;
			_isOutPubTrafficAvgValid = false;
		}
		else{
			_outPubTotal = outAvg._total;
			if(outAvg._totaledValuesCount < MessageProfilerLogger.PROFILER_CACHE_SIZE - 1)
				_isOutPubTrafficAvgValid = false;
			else
				_isOutPubTrafficAvgValid = true;
		}
	}

	@Override
	protected String toStringPrefix() {
		return "SMP#" + _session_index + "##" + (_conInfoNL==null?null:_conInfoNL.toStringShort()) + ")";
	}

	@Override
	public String toString() {
		return super.toString() + 
				((_realSession==this)?
						"":"\t ~~~> "+((_realSession==null)?"NULL":_realSession.getRemoteAddress()));
	}
	
	@Override
	public void setRealSession(ISession realSession) {
		if(_sessionConnectionType == SessionConnectionType.S_CON_T_UNKNOWN) {
			if(this == realSession)
				setRealSessionPrivately(realSession);
			else
				throw new IllegalStateException(this + " vs " + realSession);
		}
		else if(realSession == null) {
			if(isLocallyActive() && false)
				throw new IllegalStateException(this + " vs. " + realSession);
		}
		else if(realSession.isLocallyActive() || realSession.isPrimaryActive() || realSession.isBeingConnected())
			setRealSessionPrivately(realSession);
		else
			throw new IllegalStateException(this + " vs " + realSession);
	}
	
	protected final void setRealSessionPrivately(ISession realSession) {
		if(realSession != null && realSession != this)
			if(!realSession.isReal())
				throw new IllegalArgumentException("RealSession: " + realSession + " cannot be set as a realSession for: " + this);
		
		ISession oldRealSession = _realSession;
		if(realSession != _realSession) {
			((ISessionMP)_realSession).unbecomeISessionReal(this);
			_realSession = realSession;
			((ISessionMP)_realSession).becomeISessionReal(this);
		}

		if(Broker.CORRELATE)
			TrafficCorrelator.getInstance().setRealSession(this, oldRealSession, realSession);
	}
	
	@Override
	public ISession getRealSession() {
		return _realSession;
	}
	
	@Override
	public ISession getRealSession(IRawPacket raw) {
		if(raw.getType()._canRedirect) {
			if(!isDiscarded())
				return _realSession;
			else
				throw new IllegalStateException(this + " is discarded: " + raw.getString(_brokerShadow));
		}
		else
			return this;
	}

	@Override
	public void discard() {
		super.discard();
		_redirectedSessions.clear();
	}
	
	public Set<ISessionMP> getRedirectedSessions() {
		return _redirectedSessions;
	}
	
	protected void becomeISessionReal(ISessionMP session) {
		_redirectedSessions.add(session);
	}
	
	protected void unbecomeISessionReal(ISessionMP session) {
		_redirectedSessions.remove(session);
	}
	
	@Override
	public boolean mustGoBackInMQ() {
		switch(_sessionConnectionType) {
		case S_CON_T_SOFT:
		case S_CON_T_SOFTING:
		case S_CON_T_CANDIDATE:
			return false;
			
		default:
			return super.mustGoBackInMQ();
		}
	}
}
