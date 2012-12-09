package org.msrg.publiy.communication.core.packet.types;

class PrivateCounter {
	static byte _counter = 0;
}

public enum TCommandTypes {
	
	CMND_BFT_PUBLICATION_MANIPULATE_DROP(PrivateCounter._counter++, "BFT_MANIPULATE_DROP"),
	CMND_BFT_PUBLICATION_MANIPULATE_TAMPER(PrivateCounter._counter++, "BFT_MANIPULATE_TAMBER"),
	
	CMND_MARK (PrivateCounter._counter++, "MARK"),
	CMND_DISSEMINATE (PrivateCounter._counter++, "DISSEMINATE"),
	CMND_NO_DISSEMINATE (PrivateCounter._counter++, "NO_DISSEMINATE");
	
	final byte _typeValue;
	final String _str;
	
	private TCommandTypes(byte type, String str) {
		_typeValue = type;
		_str = str;
	}
	
	public byte value(){
		return _typeValue;
	}
	
	public String toString() {
		return _str;
	}
	
	public static TCommandTypes valueOf(byte type) {
		return TCommandTypes.values()[type];
	}
}
