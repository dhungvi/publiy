package org.msrg.publiy.communication.core.packet.types;

public enum SessionInitiationTypes {

	SINIT_PUBSUB ((byte)0),
	SINIT_REOCVERY ((byte)1),
	SINIT_DROP ((byte)2);
	
	
	private byte _typeValue;
	
	private SessionInitiationTypes(byte t){
		_typeValue = t;
	}
	
	public byte value(){
		return _typeValue;
	}
}
