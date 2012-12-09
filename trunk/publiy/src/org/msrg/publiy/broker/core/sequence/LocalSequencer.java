package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.component.ComponentStatus;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.utils.SystemTime;




public class LocalSequencer extends Sequencer {
	
	protected Object _lock = new Object();
	private static LocalSequencer _localSequencer;
	private static IBroker _broker;
	protected static ComponentStatus _status = ComponentStatus.COMPONENT_STATUS_UNINITIALIZED;
	
	public static boolean isInitialized() {
		return _localSequencer != null;
	}
	
	public static LocalSequencer init(IBroker broker, InetSocketAddress address) throws IllegalStateException{
		_localSequencer = new LocalSequencer(address);
		_broker = broker;
		_status = ComponentStatus.COMPONENT_STATUS_INITIALIZED;
		return _localSequencer;
	}
	
	protected void amIinitialized() {
		if(_status != ComponentStatus.COMPONENT_STATUS_INITIALIZED)
			throw new IllegalStateException("LocalSequencer::amIinitialized() - ERROR, component is not initialized.");
	}
	
	protected LocalSequencer(InetSocketAddress addr) {
		super(addr);
		_lastSequence = new Sequence(_address, SystemTime.currentTimeMillis(), 1);
	}
	
	public Sequence getNext() {
		synchronized(_lock) {
			amIinitialized();
			int last = _lastSequence.getOrder();
			_lastSequence = new Sequence(_address, _lastSequence.getEpoch(), last+1);
			return _lastSequence;
		}
	}
	
	public IBroker getBroker() {
		amIinitialized();
		return _broker;
	}

	public static NodeTypes getNodeTypeGeneric() {
		if(_broker == null) 
			return NodeTypes.NODE_BROKER;
		
		NodeTypes type = _broker.getNodeType();
		switch (type) {
		case NODE_BROKER:
			return NodeTypes.NODE_BROKER;
			
		case NODE_GUI_PUBLISHER:
		case NODE_PUBLISHER:
		case NODE_MP_PUBLISHER:
		case NODE_NC_PUBLISHER:
			return NodeTypes.NODE_PUBLISHER;
			
		case NODE_GUI_SUBSCRIBER:
		case NODE_SUBSCRIBER:
		case NODE_MP_SUBSCRIBER:
		case NODE_NC_SUBSCRIBER:
			return NodeTypes.NODE_SUBSCRIBER;
			
		default:
			throw new IllegalStateException("Donno the type of: " + type);
		}
	}
}
