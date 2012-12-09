package org.msrg.publiy.pubsub.core.packets;

public enum CodedPieceIdReqTypes {

	BREAK			(COUNTER._counter++),
	BREAK_DECLINE	(COUNTER._counter++),
	SEND_ID			(COUNTER._counter++),
	BREAK_SEND_ID	(COUNTER._counter++),
	SEND_CONTENT	(COUNTER._counter++),
	SEND_CONTENT_ID	(COUNTER._counter++),
	PLIST_REQ		(COUNTER._counter++),
	PLIST_REPLY		(COUNTER._counter++);
	
	public final byte _byteValue;
	
	CodedPieceIdReqTypes(byte byteValue) {
		_byteValue = byteValue;
	}
	
	static CodedPieceIdReqTypes getCodedPieceIdReqTypes(byte b) {
		CodedPieceIdReqTypes ret = values()[b];
		if(ret._byteValue != b)
			throw new IllegalStateException();
		
		return ret;
	}
}


class COUNTER {
	static byte _counter = 0;
}