package org.msrg.publiy.pubsub.core.packets.recovery;

public enum TRecoveryTypes {

	T_RECOVERY_UNKNOWN ((byte)0),
	T_RECOVERY_JOIN ((byte)1),
	T_RECOVERY_SUBSCRIPTION ((byte)2),
	T_RECOVERY_PUBLICATION ((byte)3),
	T_RECOVERY_LAST_JOIN ((byte)4),
	T_RECOVERY_LAST_SUBSCRIPTION((byte)5);
	
	private byte _subtype;
	
	TRecoveryTypes(byte b){
		_subtype = b;
	}
	
	public byte getValue(){
		return _subtype;
	}
	
	public static TRecoveryTypes getType(byte i){
		switch(i){
		case 0:
			return T_RECOVERY_UNKNOWN;
		case 1:
			return T_RECOVERY_JOIN;
		case 2:
			return T_RECOVERY_SUBSCRIPTION;
		case 3:
			return T_RECOVERY_PUBLICATION;
		case 4:
			return T_RECOVERY_LAST_JOIN;
		case 5:
			return T_RECOVERY_LAST_SUBSCRIPTION;
		default:
			throw new IllegalArgumentException("No such TRecovery type '" + i + "'");
		}
	}
}
