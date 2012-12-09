package org.msrg.publiy.broker.info;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.publishSubscribe.Subscription;


public class SubscriptionInfo extends IBrokerInfo {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 5542256754510013473L;
	private InetSocketAddress _from;
	private Subscription _subscription;
	private Sequence _sourceSequenceId;
	
	public SubscriptionInfo(Subscription subscription, InetSocketAddress from, Sequence sourceSequence){
		super(BrokerInfoTypes.BROKER_INFO_SUBSCRIPTIONS);
		_from = from;
		_subscription = subscription;
		_sourceSequenceId = sourceSequence;
	}
	
	public Sequence getSourceSequenceId(){
		return _sourceSequenceId;
	}
	
	public Subscription getSubscription(){
		return _subscription;
	}
	
	public InetSocketAddress getFrom(){
		return _from;
	}
	
	@Override
	public String toStringPrivately(){
		return "F: " + _from + "[" + _sourceSequenceId + "]:" + _subscription;
	}
	
}
