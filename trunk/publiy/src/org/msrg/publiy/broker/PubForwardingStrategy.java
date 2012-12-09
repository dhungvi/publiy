package org.msrg.publiy.broker;

class DUMMY {
	static byte DUMMYCOUNTER = 0;
}

public enum PubForwardingStrategy {

	PUB_FORWARDING_STRATEGY_UNKNOWN	("UNKN_STR",(byte)DUMMY.DUMMYCOUNTER++, false),
	PUB_FORWARDING_STRATEGY_0		("STR0",	(byte)DUMMY.DUMMYCOUNTER++, false),
	PUB_FORWARDING_STRATEGY_1		("STR1",	(byte)DUMMY.DUMMYCOUNTER++, false),
	PUB_FORWARDING_STRATEGY_2		("STR2",	(byte)DUMMY.DUMMYCOUNTER++, false),
	PUB_FORWARDING_STRATEGY_3		("STR3",	(byte)DUMMY.DUMMYCOUNTER++, false),
	PUB_FORWARDING_STRATEGY_4		("STR4",	(byte)DUMMY.DUMMYCOUNTER++, true),
	PUB_FORWARDING_STRATEGY_NC		("STRNC",	(byte)DUMMY.DUMMYCOUNTER++, false);
	
	public final byte _byteCode;
	public final String _name;
	public final boolean _isLoadAware;
	
	private PubForwardingStrategy(String name, byte byteCode, boolean isLoadAware){
		_name = name;
		_byteCode = byteCode;
		_isLoadAware = isLoadAware;
	}
	
	@Override
	public String toString(){
		return _name + "{" + _isLoadAware + "}";
	}
	
	public static PubForwardingStrategy getPubForwardingStrategy(byte byteCode){
		return values()[byteCode];
	}
	
	public byte getByteCode(){
		return _byteCode;
	}
	
	public boolean equalsString(String str) {
		if(str == null)
			return false;
		
		return str.equalsIgnoreCase(_name);
	}
	
	public static PubForwardingStrategy getBrokerForwardingStrategy(String str){
		if ( str == null )
			return null;
		
		PubForwardingStrategy[] allStrategies = values();
		for ( int i=0 ; i<allStrategies.length ; i++ )
			if ( str.equalsIgnoreCase(allStrategies[i].toString()))
				return allStrategies[i];
		
		return null;
	}

	public static void main(String[] argv){
		PubForwardingStrategy[] strategies = PubForwardingStrategy.values();
		for ( int i=0 ; i<strategies.length ; i++ )
			System.out.println(strategies[i]);
	}
}
