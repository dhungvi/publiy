package org.msrg.publiy.broker.controllercentre.cache;

import java.util.Date;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.BrokerInfoComponentStatus;
import org.msrg.publiy.broker.info.BrokerInfoOpState;
import org.msrg.publiy.broker.info.ExceptionInfo;
import org.msrg.publiy.broker.info.IBrokerInfo;

public class CachedBrokerInfo <T extends IBrokerInfo> {

	private Date _cacheLocalCreationTime;
	private Sequence _remoteSequence;
	private T[] _brokerInfos;
	
	CachedBrokerInfo(T[] brokerInfos, Sequence remoteSequence) {
		_cacheLocalCreationTime = new Date();
		_remoteSequence = remoteSequence;
		_brokerInfos = brokerInfos;
	}
	
//	CachedBrokerInfo(T brokerInfo, Sequence remoteSequence) {
//		this((T[])null, remoteSequence);
//		_brokerInfos = (IBrokerInfo[]) new Object[1];
//		_brokerInfos[0] = brokerInfo;
//	}
	
	CachedBrokerInfo(ExceptionInfo exceptionInfo, Sequence remoteSequence) {
		this((T[])null, remoteSequence);
		_brokerInfos = (T[]) new ExceptionInfo[1];
		_brokerInfos[0] = (T)exceptionInfo;
	}

	CachedBrokerInfo(BrokerInfoComponentStatus componentStatus, Sequence remoteSequence) {
		this((T[])null, remoteSequence);
		_brokerInfos = (T[]) new IBrokerInfo[1];
		_brokerInfos[0] = (T)componentStatus;
	}
	
	CachedBrokerInfo(BrokerInfoOpState opState, Sequence remoteSequence) {
		this((T[])null, remoteSequence);
		_brokerInfos = (T[]) new IBrokerInfo[1];
		_brokerInfos[0] = (T)opState;
	}

	public Date getDate(){
		return _cacheLocalCreationTime;
	}
	
	public Sequence getRemoteSequence(){
		return _remoteSequence;
	}
	
	public T[] getBrokerInfo(){
		return _brokerInfos;
	}
	
}
