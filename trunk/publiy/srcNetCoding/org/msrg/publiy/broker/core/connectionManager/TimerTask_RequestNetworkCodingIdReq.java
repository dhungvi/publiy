package org.msrg.publiy.broker.core.connectionManager;

import java.util.TimerTask;

import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.networkcoding.connectionManager.client.ConnectionManagerNC_Client;

public class TimerTask_RequestNetworkCodingIdReq extends TimerTask {

	public final ConnectionManagerNC_Client _connManNC_Client;
	public final Content _content;
	
	public TimerTask_RequestNetworkCodingIdReq(
			ConnectionManagerNC_Client connManNC_Client, Content content) {
		
		_connManNC_Client = connManNC_Client;
		_content = content;
	}
	
	@Override
	public void run() {
		_connManNC_Client.sendMetadataRequest(_content);
	}

}
