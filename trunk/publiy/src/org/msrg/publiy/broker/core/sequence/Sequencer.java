package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Sequencer {
	
	public static final int SEQUENCER_UNINITIALIZED = -1;
	protected static Map<InetSocketAddress, Sequencer> _sequencers = new HashMap<InetSocketAddress, Sequencer>();
	protected InetSocketAddress _address;
	protected Sequence _lastSequence;
	
	protected Sequencer(InetSocketAddress addr){
		_address = addr;
		_lastSequence = new Sequence(_address, SEQUENCER_UNINITIALIZED, SEQUENCER_UNINITIALIZED);
	}
	
	@Override
	public int hashCode(){
		return _address.hashCode();
	}
	
	@Override
	public boolean equals(Object obj){
		if ( obj==null )
			return false;
		
		if ( Sequencer.class.isInstance(obj) ){
			Sequencer sequencerObj = (Sequencer) obj;
			return this._address.equals(sequencerObj._address);
		}
		
		if ( InetSocketAddress.class.isInstance(obj) ){
			InetSocketAddress inetObj = (InetSocketAddress) obj;
			return this._address.equals(inetObj);
		}
		
		return false;
	}
	
	public static Sequencer getSequencer(InetSocketAddress address){
		Sequencer sequencer = _sequencers.get(address);
		if (sequencer == null)
			_sequencers.put(address, new Sequencer(address));
		return sequencer;
	}
	
	public boolean succeeds(Sequence seq){
		if ( !seq.getAddress().equals(_address) )
			return false;
		if ( _lastSequence.getEpoch() > seq.getEpoch() || _lastSequence.getOrder() > seq.getOrder() )
			return false;
		return true;
	}
	
	public boolean update(Sequence seq){
		if ( !succeeds(seq) )
			return false;
		_lastSequence = seq;
		return true;
	}
	
	public static boolean autoUpdate(Sequence seq){
		InetSocketAddress address = seq.getAddress();
		return getSequencer(address).update(seq);
	}

	public InetSocketAddress getLocalAddress(){
		return _address;
	}
}
