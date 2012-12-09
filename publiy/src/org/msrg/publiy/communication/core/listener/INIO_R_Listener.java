package org.msrg.publiy.communication.core.listener;

import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;

public interface INIO_R_Listener extends INIOListener {
	
	void becomeMyReaderListener(IConInfoNonListening<?> conInfo);
	public void conInfoGotFirstDataItem(IConInfoNonListening<?> conInfo);
	
}
