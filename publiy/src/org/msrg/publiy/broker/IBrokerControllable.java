package org.msrg.publiy.broker;

import org.msrg.publiy.communication.core.packet.types.TBFTMessageManipulator;
import org.msrg.publiy.communication.core.packet.types.TCommand;
import org.msrg.publiy.component.IComponent;

public interface IBrokerControllable extends IBrokerQueriable, IComponent, IBrokerShadow {

	public boolean pause();
	public boolean play();
	public boolean setSpeedLevel(int interval);
	public boolean handleTCommandMessage(TCommand tCommand);
	public boolean loadPreparedSubscriptions();
	
	public boolean installBFTMessageManipulator(TBFTMessageManipulator bftMessageManipulator);
	
}
