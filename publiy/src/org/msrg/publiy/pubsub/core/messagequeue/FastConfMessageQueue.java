package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.pubsub.core.ITimestamp;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TDack;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;

public class FastConfMessageQueue extends MessageQueue {

	protected ITimestamp _discardTimestamp;
	
	public FastConfMessageQueue(IConnectionManager connectionManager, IBrokerShadow brokerShadow, boolean allowedToConfirm) {
		super(brokerShadow, connectionManager, connectionManager.getOverlayManager(), connectionManager.getSubscriptionManager(), allowedToConfirm);
		
		_discardTimestamp = new Timestamp(brokerShadow);
		
		if (!brokerShadow.isFastConf())
			throw new IllegalStateException("Broker must allow FASTCONF.");
	}
	
	public FastConfMessageQueue(IConnectionManager connectionManager, IMessageQueue oldMQ){
		super(connectionManager, oldMQ);
	}
	
	@Override
	protected void initializeFromOldMessageQueue(MessageQueue oldMQ){
		if ( FastConfMessageQueue.class.isInstance(oldMQ) )
			_discardTimestamp = ((FastConfMessageQueue)oldMQ)._discardTimestamp;
		else
			_discardTimestamp = new Timestamp(oldMQ._brokerShadow);
		
		super.initializeFromOldMessageQueue(oldMQ);
	}
	
	public boolean canDoFastConfirmation(TMulticastTypes tm){
		return tm._canFastConfirm;
	}
	
	@Override
	protected void addConfMessage(TMulticast tm, InetSocketAddress to){
		if ( canDoFastConfirmation(tm.getType()) )
			return;
		else
			super.addConfMessage(tm, to);
	}
	
	@Override
	protected MessageQueueNode createNewMessageQueueNode(TMulticast tm, ITMConfirmationListener confirmationListener){
		if ( canDoFastConfirmation(tm.getType()) )
			return new FastConfMessageQueueNode(this, tm, confirmationListener, _localSequencer.getNext());
		else
			return super.createNewMessageQueueNode(tm, confirmationListener);
	}
	
	@Override
	protected void confirm(IMessageQueueNode node){
		if ( node.canFastConfirm() )
		{
			TMulticast tm = node.getMessage();
			
			Sequence[] sequencesVector = tm.getSequenceVector();
			updateDiscardTimeStamp(sequencesVector);
	
			removeFromMQSet(node);
		}
		else
		{
			super.confirm(node);
		}
	}
	
	protected void updateDiscardTimeStamp(Sequence[] sequences){
		for ( int i=0 ; i<sequences.length ; i++ )
			if ( sequences[i] != null ){
				_discardTimestamp.isDuplicate(sequences[i]);
			}
	}
	
	@Override
	public ITimestamp getDiscardedTimestamp(){
		return _discardTimestamp;
	}

	@Override
	public void processTDack(TDack dack) {
		getOverlayManager().updateFromTDack(dack);
	}

//	private int purgeCounter=0;
	@Override
	public MQCleanupInfo purge() {
//		if ( purgeCounter++ % 10 == 0 ){
//			System.out.println("============================================");
//			System.out.println("ENTIRE NODECAHCE: " + _overlayManager.getAllArrivedDiscardedSummary());
//			System.out.println("ENTIRE ARRIVED TS: " + _arrivalTimestamp );
//			System.out.println("ENTIRE DISCRDED TS: " + _discardTimestamp );
//			System.out.println("ENTIRE MQ: " + this.toString() + "\n\n");
//		}
		
		getOverlayManager().updateAllNodesCache(_arrivalTimestamp, _discardTimestamp);
		MQCleanupInfo mqInfo = proceedHeadAndJumpConfirmed();
		
		return mqInfo;
	}

}
