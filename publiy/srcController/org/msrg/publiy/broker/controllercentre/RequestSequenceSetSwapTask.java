package org.msrg.publiy.broker.controllercentre;

import java.util.TimerTask;

public class RequestSequenceSetSwapTask extends TimerTask {

	BrokerControllerCentreImp _brokerControllerCentre;
	
	public RequestSequenceSetSwapTask(BrokerControllerCentreImp brokerControllerCentre) {
		_brokerControllerCentre = brokerControllerCentre;
	}
	
	@Override
	public void run() {
		_brokerControllerCentre.discardOldRequestSequenceSet();
	}

}
