package org.msrg.publiy.client.publisher.guipublisher;

public interface ISimpleGUIPublisher {

	public boolean play();
	public boolean pause();
	public boolean isPaused();
	public int getDelay();
//	public void setDelay(int newDelay);
	public boolean setSpeedLevel(int interval);
	
}
