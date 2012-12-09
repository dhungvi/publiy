package org.msrg.publiy.communication.core.listener;

import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;

public interface INIO_W_Listener extends INIOListener {
	
	void becomeMyWriteListener(IConInfoNonListening<?> conInfo);
	public void conInfoGotEmptySpace(IConInfoNonListening<?> conInfo);
	
}
