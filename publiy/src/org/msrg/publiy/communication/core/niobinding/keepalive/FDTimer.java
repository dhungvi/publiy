package org.msrg.publiy.communication.core.niobinding.keepalive;

import java.util.Timer;

import org.msrg.publiy.communication.core.niobinding.INIOBinding;

public class FDTimer extends Timer {
	
	private INIOBinding _nioBinding;
	
	public FDTimer(INIOBinding nioBinding) {
		super("THREAD-" + "FDTimer-" + nioBinding);
		_nioBinding = nioBinding;
	}
	
	@Override
	public String toString() {
		return "FDTimer-"+_nioBinding;
	}
}
