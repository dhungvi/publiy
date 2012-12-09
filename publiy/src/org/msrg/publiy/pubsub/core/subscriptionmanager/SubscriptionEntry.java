package org.msrg.publiy.pubsub.core.subscriptionmanager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.IHasBrokerInfo;

import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.subscriptionmanager.covering.StandaloneCoveringSubscription;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.info.SubscriptionInfo;

public class SubscriptionEntry implements IHasBrokerInfo<SubscriptionInfo> {

	public final Sequence _seqID;
	public final StandaloneCoveringSubscription _standaloneCoveringSub;
	public InetSocketAddress _orgFrom;
	public InetSocketAddress _activeFrom;
	
	public SubscriptionEntry(Sequence seqID, Subscription subscription, InetSocketAddress from, boolean realFrom) {
		_seqID = seqID;
		_standaloneCoveringSub = new StandaloneCoveringSubscription(this);
		_standaloneCoveringSub.materialize(null, subscription);
		_orgFrom = from;

		setActiveFrom(_orgFrom);
	}
	
	public void setActiveFrom(InetSocketAddress activeFrom) {
		_activeFrom = activeFrom;
	}

	@Override
	public int hashCode(){
		if ( _seqID == null )
			return super.hashCode();
		else
			return _seqID.hashCodeExact();
	}
	
	@Override
	public boolean equals(Object obj){
		if ( _seqID == null )
			return super.equals(obj);
		
		if ( obj == null )
			return false;
		if ( !this.getClass().isInstance(obj) )
			return false;
		
		SubscriptionEntry subEntryObj = (SubscriptionEntry) obj;
		return _seqID.equals(subEntryObj._seqID);
	}

	public Subscription getSubscription(){
		return _standaloneCoveringSub._subscription;
	}

	@Override
	public SubscriptionInfo getInfo() {
		SubscriptionInfo subscriptionInfo = new SubscriptionInfo(getSubscription(), _orgFrom, _seqID);
		return subscriptionInfo;
	}
	
	public String toString(){
		return "SubEntry: [" + 
							"From:" + ((_orgFrom==null)?null:_orgFrom.getPort()) + ", " +
							"AFrom:" + ((_activeFrom==null)?null:_activeFrom.getPort()) + ", " +
									((_seqID==null)?"":_seqID.toStringShort()) + ", " + 
									getSubscription() +
									"]";  
	}
	
	public static SubscriptionEntry decode(String line, BrokerIdentityManager idManager) {
		return SubscriptionFromLink.decode(line, idManager);
	}

	public boolean isLocal() {
		return false;
	}
}
