package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.communication.core.packet.PacketFactory;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.ITimestamp;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Join;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;

public class TimestampWithoutSequencePlaceHolder implements ITimestamp {
	
	private Map<InetSocketAddress, Sequence> _lastSequence = new HashMap<InetSocketAddress, Sequence>();
	protected final StatisticsLogger _statLogger;
	
	public TimestampWithoutSequencePlaceHolder(IBrokerShadow brokerShadow) {
		_statLogger = brokerShadow.getStatisticsLogger();
	}

	@Override
	public boolean isDuplicate(Sequence seq) {
		InetSocketAddress address = seq.getAddress();
		
		synchronized (_lastSequence) {
			Sequence last = _lastSequence.get(address);
			
			if (last == null){
				_lastSequence.put(address, seq);
				
				if(_statLogger != null)
					_statLogger.timestampMapSizeUpdated(_lastSequence.size());
				
				return false;
			}
			
			boolean succeeds = seq.succeeds(last); 
			if ( succeeds ){
				_lastSequence.put(address, seq);
				return false;	
			}				
			else {
				return true;
			}
		}
	}

	@Override
	public boolean isDuplicate(TMulticast tm) {
		Sequence seq = tm.getSourceSequence();
		return isDuplicate(seq);
	}
	
	public static void main(String[] argv){
		InetSocketAddress addr1 = new InetSocketAddress("127.0.0.1",1000);
		InetSocketAddress addr2 = new InetSocketAddress("127.0.0.1",2000);
		IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, addr1);
		LocalSequencer localSequencer = brokerShadow.getLocalSequencer();
		TMulticast_Join tm = new TMulticast_Join(addr1, NodeTypes.NODE_BROKER, addr2, NodeTypes.NODE_BROKER, localSequencer);
//		tm.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress3, 300));
		TMulticast_Join tmm = new TMulticast_Join(addr1, NodeTypes.NODE_BROKER, addr2, NodeTypes.NODE_BROKER, localSequencer);
		
		TMulticast_Join tm1 = (TMulticast_Join) PacketFactory.unwrapObject(null, PacketFactory.wrapObject(localSequencer, tm));
		TMulticast_Join tm2 = (TMulticast_Join) PacketFactory.unwrapObject(null, PacketFactory.wrapObject(localSequencer, tm));
//		tm2.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress4, 440));
		TMulticast_Join tm3 = (TMulticast_Join) PacketFactory.unwrapObject(null, PacketFactory.wrapObject(localSequencer, tm2));
		
		TimestampWithoutSequencePlaceHolder ts = new TimestampWithoutSequencePlaceHolder(brokerShadow);
		
		System.out.println(tm1 + " \t " + ts.isDuplicate(tm1)); //OK
		System.out.println(tmm + " \t " + ts.isDuplicate(tmm)); //OK
		
		System.out.println(tm1 + " \t " + ts.isDuplicate(tm1)); //OK
		System.out.println(tm1 + " \t " + ts.isDuplicate(tm1)); //OK
		System.out.println(tm2 + " \t " + ts.isDuplicate(tm2)); //OK
		System.out.println(tm3 + " \t " + ts.isDuplicate(tm3)); //OK
	}

	@Override
	public Sequence getLastReceivedSequence(InetSocketAddress remote) {
		Sequence lastSequence = _lastSequence.get(remote);
		return lastSequence;
	}

}
