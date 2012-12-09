package org.msrg.publiy.broker.core.connectionManager;

import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;

class TM_TMListener{
	TMulticast _tm;
	ITMConfirmationListener _listener;
	TM_TMListener(TMulticast tm, ITMConfirmationListener listener){
		_tm = tm;
		_listener = listener;
	}
}
