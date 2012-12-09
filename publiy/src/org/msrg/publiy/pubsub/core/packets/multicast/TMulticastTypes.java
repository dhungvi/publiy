package org.msrg.publiy.pubsub.core.packets.multicast;

class PrivateCounter {
	static byte _counter;
}

public enum TMulticastTypes {

	T_MULTICAST_UNKNOWN 		("TM_UNKN", 'K',
									PrivateCounter._counter++,
									false, 	false),
	T_MULTICAST_JOIN 			("TM_J", 'J',
									PrivateCounter._counter++,
									false, 	false),
	T_MULTICAST_ADVERTISEMENT	("TM_A", 'A',
									PrivateCounter._counter++,
									false, 	false),
	T_MULTICAST_SUBSCRIPTION	("TM_S", 'S',
									PrivateCounter._counter++,
									false, 	false),
	T_MULTICAST_PUBLICATION 	("TM_P", 'P',
									PrivateCounter._counter++,
									true, 	true),
	T_MULTICAST_PUBLICATION_MP 	("TM_P_MP", 'M',
									PrivateCounter._counter++,
									true, 	true),
	T_MULTICAST_PUBLICATION_NC 	("TM_P_NC", 'N',
									PrivateCounter._counter++,
									true, 	true),
	T_MULTICAST_PUBLICATION_BFT	("TM_P_BFT", 'B',
									PrivateCounter._counter++,
									true, 	false),
	T_MULTICAST_PUBLICATION_BFT_DACK ("TM_P_BFT_HB", 'H',
									PrivateCounter._counter++,
									true, 	false),
	T_MULTICAST_UNSUBSCRIPTION 	("TM_UN_S", 'U',
									PrivateCounter._counter++,
									false, 	false),
	T_MULTICAST_DEPART 			("TM_UN_J", 'D',
									PrivateCounter._counter++,
									false,	false),
	T_MULTICAST_CONF 			("TM_CONF", 'C',
									PrivateCounter._counter++,
									false, 	false),
	;
	
	private final byte _subtype;
	public final boolean _canFastConfirm;
	public final boolean _canDoEarlyConfirm;
	public final char _charName;
	public final String _shortName;
	
	TMulticastTypes(String shortName, char charName, byte b, boolean canFastConfirm, boolean canMP){
		_subtype = b;
		_canFastConfirm = canFastConfirm;
		_canDoEarlyConfirm = canMP;
		_shortName = shortName;
		_charName = charName;
	}
	
	public byte getValue(){
		return _subtype;
	}
	
	public static TMulticastTypes getType(byte i){
		return values()[i];
	}
	
}
