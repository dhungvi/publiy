package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;


import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionLocal;
import org.msrg.publiy.communication.core.sessions.SessionConnectionType;

public class PSSessionLocal extends PSSession {
	
	private ISessionLocal _session;
	
	public String toString(){
		return "PSSessionLocal: \t" + _session.getRemoteAddress() + "::" + _session.getSessionConnectionType();// + " _ " + _session;
	}
	
	public InetSocketAddress getRemoteAddress(){
		return _session.getRemoteAddress();
	}
	
	public ISession getISession(){
		return _session;
	}
	
	public SessionConnectionType getSessionConnectionType(){
		return _session.getSessionConnectionType();
	}
	
	protected PSSessionLocal(ISessionLocal sessionLocal, MessageQueue mQ){
		super(sessionLocal, mQ);
		_session = sessionLocal;
	}
	
	void swapMessageQueue(IMessageQueue newMQ){
		throw new UnsupportedOperationException();
	}
	
	void resetSession(ISession session, InetSocketAddress[] nodesInBetween){
		return;
	}
	
	boolean advance(){
		return false;
	}
	
	@Override
	public int hashCode(){
		return _session.hashCode();
	}
	
	@Override
	public boolean equals(Object obj){
		if ( obj == null )
			return false;
		
		if ( !this.getClass().isInstance(obj) )
			return false;
		
		PSSessionLocal pssObj = (PSSessionLocal) obj;
		
		if ( pssObj._session.getRemoteAddress().equals(this._session.getRemoteAddress()) )
			return true;
		
		return false;
	}
	
	public boolean send(IRawPacket raw){
		throw new UnsupportedOperationException("PSSessionLocal does not support the send operation.");
	}
	
	public void cancel(){
		return;			
	}
}
