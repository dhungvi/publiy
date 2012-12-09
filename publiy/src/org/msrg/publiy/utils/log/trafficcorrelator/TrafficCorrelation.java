package org.msrg.publiy.utils.log.trafficcorrelator;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class TrafficCorrelation {
	
	private List<TrafficCorrelationEvent> _correlationEvents = new LinkedList<TrafficCorrelationEvent>();
	private final Sequence _sourceSequence;
	private final TMulticastTypes _type;
	private boolean _confirmed = false;
	private boolean _acknowledged = false;
	
	TrafficCorrelation(Sequence tmSourceSequence) {
		this(tmSourceSequence, TMulticastTypes.T_MULTICAST_UNKNOWN);
	}
			
	TrafficCorrelation(Sequence tmSourceSequence, TMulticastTypes type) {
		_sourceSequence = tmSourceSequence;
		_type = type;
	}
	
	TrafficCorrelation(TMulticast tm) {
		this(tm.getSourceSequence(), tm.getType());
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj == null || obj.getClass().isInstance(this) )
			return false;
		
		TrafficCorrelation correlation = (TrafficCorrelation) obj;
		return _sourceSequence.equals(correlation._sourceSequence);
	}
	
	@Override
	public int hashCode() {
		return _sourceSequence.hashCode();
	}
	
	void addCorrelationEvent(TrafficCorrelationEvent event) {
		synchronized (_correlationEvents) {
			_correlationEvents.add(event);
		}
	}
	
	void acknowledged(){
		synchronized (_correlationEvents) {
			if (!_acknowledged) {
				if (TrafficCorrelator.CLEAR_ACKED)
					_correlationEvents.clear();
				_acknowledged = true;
			}

			TrafficCorrelationEvent ackEvent = new TrafficCorrelationEvent_Acknowledged();
			_correlationEvents.add(ackEvent);
			
			TrafficCorrelationEvent endEvent = new CorrelationEvent_End();
			_correlationEvents.add(endEvent);
		}
	}
	
	void confirmed() {
		synchronized (_correlationEvents) {
			if (!_confirmed) {
				if (TrafficCorrelator.CLEAR_CONFIRMED)
					_correlationEvents.clear();
				_confirmed = true;
			}
			TrafficCorrelationEvent confirmedEvent = new TrafficCorrelationEvent_Confirmed();
			_correlationEvents.add(confirmedEvent);
		}
	}
	
	boolean isAcknowledged() {
		return _acknowledged;
	}
	
	boolean isConfirmed() {
		return _confirmed;
	}
	
	@Override
	public String toString() {
		StringWriter output = new StringWriter(10000);
		synchronized (_correlationEvents) {
			output.append("CORRELATION{" + _sourceSequence + "} ");
			for(TrafficCorrelationEvent correlationEvent : _correlationEvents) {
				boolean endSeen = false;
				switch (correlationEvent._type) {
				case S_OUT_PENDING_DEQ:
				case S_OUT_PENDING_ENQ:
					break;
					
				case END:
					endSeen = true;
					
				default:
					output.append((endSeen?"=":"") + correlationEvent + ", ");
				}
				
				if(endSeen)
					break;
			}
		}
		return output.toString();
	}
	
	public String merge(List<AbstractSessionsCorrelationEvent> otherEventsList, boolean beginWithMyEvent) {
		synchronized (_correlationEvents) {
			StringWriter output = new StringWriter(10000);
			output.append("Correlation_" + _type._charName + "_" + _sourceSequence.toString() + " \t");
			Iterator<TrafficCorrelationEvent> myEventsIt = _correlationEvents.iterator();
			TrafficCorrelationEvent myPastEvent = (myEventsIt.hasNext()?myEventsIt.next():null);
				
			Iterator<AbstractSessionsCorrelationEvent> otherEventsIt = otherEventsList.iterator();
			TrafficCorrelationEvent otherPastEvent = (otherEventsIt.hasNext()?otherEventsIt.next():null);
			
			if (beginWithMyEvent) {
				while(otherPastEvent!=null) {
					if (myPastEvent==null)
						return output.toString();
					if(myPastEvent.lessThan(otherPastEvent))
						break;
					else
						otherPastEvent = (otherEventsIt.hasNext()?otherEventsIt.next():null);
				}
			}
			
			boolean endSeen = false;
			while (myPastEvent!=null || otherPastEvent!=null) {
				TrafficCorrelationEvent currEvent = null;
				if (myPastEvent == null) {
					currEvent = otherPastEvent;
					otherPastEvent = (otherEventsIt.hasNext()?otherEventsIt.next():null);
				} else if (otherPastEvent == null) {
					currEvent = myPastEvent;
					myPastEvent = (myEventsIt.hasNext()?myEventsIt.next():null);
				} else if (myPastEvent.lessThan(otherPastEvent)) {
					currEvent = myPastEvent;
					myPastEvent = (myEventsIt.hasNext()?myEventsIt.next():null);
				} else {
					currEvent = otherPastEvent;
					otherPastEvent = (otherEventsIt.hasNext()?otherEventsIt.next():null);
				}
				
				switch (currEvent._type) {
				case S_OUT_PENDING_DEQ:
				case S_OUT_PENDING_ENQ:
					break;
					
				case END:
					endSeen = true;
					
				default:
					output.append((endSeen?"=":"") + currEvent + ", ");
				}
				
				if(endSeen)
					break;
			}
			
			return output.toString();
		}
	}
}
