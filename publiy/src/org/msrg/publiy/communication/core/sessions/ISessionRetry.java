package org.msrg.publiy.communication.core.sessions;

public interface ISessionRetry {

	public boolean testRetry();
	public void resetRetry();
	
}
