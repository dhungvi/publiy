package org.msrg.publiy.broker.networkcoding.connectionManager.client;

import java.util.TimerTask;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class SendPListRequest_TimerTask extends TimerTask {

	private final ConnectionManagerNC_Client _connManNCClient;
	private final Sequence _contentSequence;
	private final int _rows;
	private final int _cols;
	private final boolean _fromSource;
	
	SendPListRequest_TimerTask(
			ConnectionManagerNC_Client connManNCClient, Sequence contentSequence,
			int rows, int cols, boolean fromSource) {
		_connManNCClient = connManNCClient;
		_contentSequence = contentSequence;
		_rows = rows;
		_cols = cols;
		_fromSource = fromSource;
	}
	
	@Override
	public void run() {
		_connManNCClient.sendPListRequest(
				_contentSequence,
				_rows, _cols);
	}

}
