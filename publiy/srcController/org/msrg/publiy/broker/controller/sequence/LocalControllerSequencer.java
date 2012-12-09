package org.msrg.publiy.broker.controller.sequence;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.IBroker;


import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.component.ComponentStatus;

public class LocalControllerSequencer extends LocalSequencer {

	private static LocalControllerSequencer _localControllerSequencer;
	
	public LocalControllerSequencer(InetSocketAddress address) {
		super(address);
	}

	public static LocalControllerSequencer getLocalControllerSequencer() throws IllegalStateException{
		if ( _localControllerSequencer == null )
			throw new IllegalStateException("LocalControllerSequencer is not initialized.");
		return _localControllerSequencer;
	}
	
	public static LocalSequencer init(IBroker broker, InetSocketAddress address) throws IllegalStateException{
		throw new UnsupportedOperationException("Use init(InetSocketAddress)");
	}

	public static void init(InetSocketAddress address) throws IllegalStateException{
		if ( _localControllerSequencer == null )
			_localControllerSequencer = new LocalControllerSequencer(address);
		else
			throw new IllegalStateException("LocalSequencer already initialized.");
		_status = ComponentStatus.COMPONENT_STATUS_INITIALIZED;
	}
	
	public IBroker getBroker(){
		throw new UnsupportedOperationException("This is a LocalControllerSequencer, not a normal LocalSequencer.");
	}

	public SequenceUpdateable<IMessageSequenceUpdateListener> getNextMessageSequence(){
		synchronized(_lock){
			amIinitialized();
			int last = _lastSequence.getOrder();
			SequenceUpdateable<IMessageSequenceUpdateListener> newSeq = new SequenceUpdateable<IMessageSequenceUpdateListener>(_address, _lastSequence.getEpoch(), last+1);
			_lastSequence = newSeq;
			return newSeq;
		}
	}

	public SequenceUpdateable<IConnectionSequenceUpdateListener> getNextConnectionSequence(){
		synchronized(_lock){
			amIinitialized();
			int last = _lastSequence.getOrder();
			SequenceUpdateable<IConnectionSequenceUpdateListener> newSeq = new SequenceUpdateable<IConnectionSequenceUpdateListener>(_address, _lastSequence.getEpoch(), last+1);
			_lastSequence = newSeq;
			return newSeq;
		}
	}
	public static void destroy(){
		_localControllerSequencer = null;
		System.out.println("destroyed");
	}
}
