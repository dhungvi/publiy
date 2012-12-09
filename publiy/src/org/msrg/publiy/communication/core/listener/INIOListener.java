package org.msrg.publiy.communication.core.listener;

import org.msrg.publiy.communication.core.niobinding.IConInfo;

public interface INIOListener {
	
	void becomeMyListener(IConInfo<?> conInfo);
	public void conInfoUpdated(IConInfo<?> conInfo);
	
}
