package org.msrg.publiy.pubsub.core.packets.multicast;

import java.net.InetSocketAddress;

import org.msrg.publiy.utils.Writers;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class Conf_Ack {

	private InetSocketAddress _ackFrom;
	private InetSocketAddress _ackTo;
	private Sequence _conf_source_sequence;
	
	public Conf_Ack(InetSocketAddress ackFrom, InetSocketAddress ackTo, Sequence confSourceSequence){
		_ackFrom = ackFrom; 
		_ackTo = ackTo;
		_conf_source_sequence = confSourceSequence;
	}
	
	public InetSocketAddress getAckFrom(){
		return _ackFrom;
	}
	
	public InetSocketAddress getAckTo(){
		return _ackTo;
	}
	
	public String toString(){
		return "ConfAck[" + Writers.write(_ackFrom) + "_" + Writers.write(_ackTo) + "_" + Writers.write(_conf_source_sequence);
	}
	
	@Override
	public int hashCode(){
		return _conf_source_sequence.hashCodeExact();
	}
	
	@Override
	public boolean equals(Object obj){
		if ( TMulticast_Conf.class.isInstance(obj) ){
			TMulticast_Conf tmc = (TMulticast_Conf) obj;
			if ( // tmc._confirmedTo.equals(this._ackFrom) && 
//				 tmc._sourceSequence.equalsExact(this._conf_source_sequence) )
				 tmc.getConfirmationFromSequence().equalsExact(this._conf_source_sequence) )
				return true;
			
			else
				return false;
		}
		
		else
			return false;
	}
}
