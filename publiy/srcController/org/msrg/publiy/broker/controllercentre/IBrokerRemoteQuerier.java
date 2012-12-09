package org.msrg.publiy.broker.controllercentre;

import org.msrg.publiy.broker.controller.message.ErrorMessage;
import org.msrg.publiy.broker.controller.message.InfoMessage;
import org.msrg.publiy.broker.core.sequence.Sequence;

// GUI objects implement this interface.

public interface IBrokerRemoteQuerier{

	// weather or not to remove the requestSequence
	public boolean newInfoAvailable(Sequence requestId, InfoMessage infoMessage);
	public void errorOccured(Sequence requestId, ErrorMessage errorMessage);
	
}
