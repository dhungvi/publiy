package org.msrg.publiy.utils.occurance;

import org.msrg.publiy.utils.exceptions.IState;

class counter{
	static int _I = 0;
}

enum EventStatus implements IState {

	PENDING		((byte)(1<<counter._I++)), 
	FAILED		((byte)(1<<counter._I++)),
	SUCCESS		((byte)(1<<counter._I++));
	
	private final byte _bitVector;
	
	EventStatus(byte bitVetor){
		_bitVector = bitVetor;
	}	
	
	public byte getByte(){
		return _bitVector;
	}
	
	public static void main(String[] argv){
		EventStatus f = EventStatus.FAILED;
		EventStatus s = EventStatus.SUCCESS;
		EventStatus p = EventStatus.PENDING;
		
		
		System.out.println(f.getByte());
		System.out.println(s.getByte());
		System.out.println(p.getByte());
	}
}
