package org.msrg.publiy.broker.core.connectionManager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.networkcodes.PSCodedPiece;

public class ConnectionEvent_CodedContentReady extends ConnectionEvent {

	public final PSCodedPiece _psCodedPiece;
	public final InetSocketAddress _remote;
	
	public ConnectionEvent_CodedContentReady(
			LocalSequencer localSequence, PSCodedPiece psCodedPiece, InetSocketAddress remote) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_CODED_CONTENT_READY);
		
		_psCodedPiece = psCodedPiece;
		_remote = remote;
	}

	@Override
	public String toString() {
		return "CONNECTION_EVENT_CODED_CONTENT_READY";
	}

}
