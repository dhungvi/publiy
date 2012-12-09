package org.msrg.publiy.communication.core.listener;

import org.msrg.publiy.communication.core.niobinding.IConInfoListening;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;

public interface INIO_A_Listener extends INIOListener {
	
	public void becomeMyAcceptingListener(IConInfoListening<?> conInfo);
	public void newIncomingConnection(IConInfoListening<?> conInfoL, IConInfoNonListening<?> newConInfoNL);

}
