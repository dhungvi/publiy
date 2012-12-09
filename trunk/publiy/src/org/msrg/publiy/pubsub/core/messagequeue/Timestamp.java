package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.core.sequence.SequenceHolder;
import org.msrg.publiy.pubsub.core.ITimestamp;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;

public class Timestamp implements ITimestamp {
	
	protected final StatisticsLogger _statisticsLogger;
	private Map<InetSocketAddress, SequenceHolder> _lastSequence = new HashMap<InetSocketAddress, SequenceHolder>();
	
	public Timestamp(IBrokerShadow brokerShadow) {
		_statisticsLogger = (brokerShadow==null) ? null : brokerShadow.getStatisticsLogger();
	}
	
	@Override
	public boolean isDuplicate(Sequence seq) {
		InetSocketAddress address = seq.getAddress();
		
		synchronized (_lastSequence) {
			SequenceHolder last = _lastSequence.get(address);
			
			if ( last == null) {
				_lastSequence.put(address, new SequenceHolder(seq));
				
				if(_statisticsLogger != null)
					_statisticsLogger.timestampMapSizeUpdated(_lastSequence.size());

				return false;
			}
			
			boolean succeeds = seq.succeeds(last.getSequence()); 
			if ( succeeds) {
//				_lastSequence.put(address, seq);
				last.updateSequence(seq);
				return false;	
			}				
			else {
				return true;
			}
		}
	}
	
	protected int max(int a, int b) {
		return a > b ? a : b;
	}

	@Override
	public boolean isDuplicate(TMulticast tm) {
		Sequence sourceSequence = tm.getSourceSequence();
		return isDuplicate(sourceSequence);
//		Sequence[] seq = tm.getSequenceVector();
//		
//		for(int i=0 ; i<seq.length ; i++)
//			if ( seq[i] != null && isDuplicate(seq[i])) 
//				return true;
//		
//		return false;
	}
	
	@Override
	public String toString() {
		String str = "[";
		Iterator<Map.Entry<InetSocketAddress, SequenceHolder>> it = _lastSequence.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<InetSocketAddress, SequenceHolder> entry = it.next();
			InetSocketAddress address = entry.getKey();
			Sequence seq = entry.getValue().getSequence();
			
			str += address.getPort() + "=" + seq.toStringVeryShort() +(it.hasNext()?", ":"}");
		}
		
		return "TS=" + str;
	}
	
	@Override
	public Sequence getLastReceivedSequence(InetSocketAddress remote) {
		SequenceHolder holder = _lastSequence.get(remote);
		if ( holder == null)
			return null;
		else
			return holder.getSequence();
	}

}
