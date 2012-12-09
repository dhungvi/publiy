package org.msrg.publiy.broker.core.connectionManager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.packet.types.TCommand;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.component.ComponentStatus;

public abstract class ConnectionEvent {

	public final Sequence _sequence;
	public final ConnectionEventType _eventType;

	protected ConnectionEvent(LocalSequencer localSequence, ConnectionEventType eventType) {
		_sequence  = localSequence.getNext();
		_eventType = eventType;
	}
	
	public abstract String toString();

}

class ConnectionEvent_stateChange extends ConnectionEvent{
	
	final ComponentStatus _requestedState;
	
	ConnectionEvent_stateChange(LocalSequencer localSequence, ComponentStatus requestedState) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_STATE_CHANGE);
		_requestedState = requestedState;
	}
	
	public String toString() {
		return "CONNECTION_EVENT_STATE_CHANGE" + _requestedState;
	}
}

class ConnectionEvent_ForceConfAck extends ConnectionEvent {

	ConnectionEvent_ForceConfAck(LocalSequencer localSequence) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_MAINTENANCE_FORCE_CONF_ACK);
		
	}
	
	@Override
	public String toString() {
		return _eventType.toString();
	}

}

class ConnectionEvent_renewConnection extends ConnectionEvent{
	
	final ISession _session;
	
	ConnectionEvent_renewConnection(LocalSequencer localSequence, ISession session) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_RENEW_CONNECTION);
		_session = session;
	}
	
	public String toString() {
		return "CONNECTION_EVENT_RENEW_CONNECTION:::" + _sequence;
	}
}

class ConnectionEvent_updatedConnection extends ConnectionEvent{
	
	final IConInfoNonListening<?> _conInfoNL;
	
	ConnectionEvent_updatedConnection(LocalSequencer localSequence, IConInfoNonListening<?> conInfoNL) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_RECENTLY_UPDATED);
		_conInfoNL = conInfoNL;
	}
	
	public String toString() {
		return "CONNECTION_EVENT_RECENTLY_UPDATED:::" + _conInfoNL;
	}
}

class ConnectionEvent_writeConnection extends ConnectionEvent{
	
	final IConInfoNonListening<?> _conInfoNL;
	
	ConnectionEvent_writeConnection(LocalSequencer localSequence, IConInfoNonListening<?> conInfoNL) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_READY_TO_WRITE);
		_conInfoNL = conInfoNL;
	}
	
	public String toString() {
		return "CONNECTION_EVENT_READY_TO_WRITE:::" + _conInfoNL;
	}
}

class ConnectionEvent_sendDack extends ConnectionEvent{
	
	ConnectionEvent_sendDack(LocalSequencer localSequence) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_SEND_DACK);
	}
	
	public String toString() {
		return "CONNECTION_EVENT_SEND_DACK";
	}
}

class ConnectionEvent_sendTPing extends ConnectionEvent{
	
	ConnectionEvent_sendTPing(LocalSequencer localSequence) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_SEND_TPING);
	}
	
	public String toString() {
		return "CONNECTION_EVENT_SEND_TPING";
	}
}

class ConnectionEvent_readConnection extends ConnectionEvent{
	
	final IConInfoNonListening<?> _conInfoNL;
	
	ConnectionEvent_readConnection(LocalSequencer localSequence, IConInfoNonListening<?> conInfoNL) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_READY_TO_READ);
		_conInfoNL = conInfoNL;
	}
	
	public String toString() {
		return "CONNECTION_EVENT_READY_TO_READ:::" + _conInfoNL;
	}
}

class ConnectionEvent_Candidates extends ConnectionEvent{
	
	ConnectionEvent_Candidates(LocalSequencer localSequence) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_CANDIDATES);
	}
	
	public String toString() {
		return "CONNECTION_EVENT_CANDIDATES:::";
	}
}


class ConnectionEvent_registerSession extends ConnectionEvent{
	
	final ISession _session;
	
	ConnectionEvent_registerSession(LocalSequencer localSequence, ISession session) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_REGISTER_SESSION);
		_session = session;
	}
	
	public String toString() {
		return "CONNECTION_EVENT_REGISTER_SESSION:::" + _session;
	}
}

class ConnectionEvent_failedConnection extends ConnectionEvent{
	
	final ISession _session;
	
	ConnectionEvent_failedConnection(LocalSequencer localSequence, ISession session) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_FAILED_CONNECTION);
		_session = session;
	}
	
	public String toString() {
		return "CONNECTION_EVENT_FAILED_CONNECTION:::" + _session;
	}
}

class ConnectionEvent_NewLocalOutgoing extends ConnectionEvent{
	
	final TM_TMListener _tmTmListener;
	
	ConnectionEvent_NewLocalOutgoing(LocalSequencer localSequence, TM_TMListener tmTmListener) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_LOCAL_OUTGOING);
		_tmTmListener = tmTmListener;
	}
	
	public String toString() {
		return "CONNECTION_EVENT_LOCAL_OUTGOING:::" + _tmTmListener;
	}
}

class ConnectionEvent_FastTCommandProcessing extends ConnectionEvent{
	
	final TCommand _tCommand;
	final InetSocketAddress _from;
	
	ConnectionEvent_FastTCommandProcessing(LocalSequencer localSequence, TCommand tCommand, InetSocketAddress from) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_FAST_TCOMMAND_PROCESSING);
		_tCommand = tCommand;
		_from = from;
	}
	
	public String toString() {
		return "CONNECTION_EVENT_FAST_TCOMMAND_PROCESSING:::" + _tCommand;
	}
}

class ConnectionEvent_sendLoadWeight extends ConnectionEvent{
	
	ConnectionEvent_sendLoadWeight(LocalSequencer localSequence) {
		super(localSequence, ConnectionEventType.CONNECTION_EVENT_SEND_LOAD_WEIGHT);
	}
	
	public String toString() {
		return "CONNECTION_EVENT_SEND_LOAD_WEIGHT";
	}
}