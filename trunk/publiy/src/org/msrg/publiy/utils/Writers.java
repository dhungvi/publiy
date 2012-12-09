package org.msrg.publiy.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.msrg.publiy.broker.BrokerIdentityManager;

import org.msrg.publiy.broker.core.IConnectionManagerDebug;
import org.msrg.publiy.broker.core.nodes.OverlayNodeId;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.core.sequence.SequencePair;
import org.msrg.publiy.communication.core.niobinding.IConInfoNonListening;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.communication.core.sessions.ISessionMPRank;

import org.msrg.publiy.pubsub.core.overlaymanager.NodeCache;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.pubsub.core.subscriptionmanager.SubscriptionEntry;

public class Writers {
	
	public static String write(InetAddress address){
		return address.getHostAddress();
	}
	
	public static String writeAllSessions(IConnectionManagerDebug connMan) {
		ISession[] sessions = connMan.getAllSessions();
		return Writers.write(sessions);		
	}
	
	public static String write(String[] strs, String delim){
		String retStr = "{";
		for(String str : strs) {
			retStr += ((retStr.length()==1?"":delim) + str);
		}
		return retStr + "}";
	}
	
	public static String write(IConInfoNonListening[] nonListeningConnections){
		String str = "{";
		for ( int i=0 ; i<nonListeningConnections.length ; i++ ){
			str += nonListeningConnections[i].toStringLong() + ((i==nonListeningConnections.length-1)?"":", ");
		}
		
		return str + "}";
	}
	
	public static String write(int[] ints){
		if ( ints == null )
			return null;
		else if ( ints.length == 0 )
			return "{}";
		
		String str = "{"; 
		for (int i=0 ; i<ints.length ; i++)
			str += (ints[i] + ((i==ints.length-1)?"}":","));
		
		return str;
	}
	
	public static String write(NodeCache[] trs){
		if ( trs == null )
			return null;
		else if ( trs.length == 0 )
			return "{}";
		
		String str = "{"; 
		for (int i=0 ; i<trs.length ; i++)
			str += (trs[i].toString() + ((i==trs.length-1)?"}":","));
		
		return str;
	}
	
	public static String write(ISession[] trs){
		if ( trs == null )
			return null;
		else if ( trs.length == 0 )
			return "{}";
		
		String str = "{"; 
		for (int i=0 ; i<trs.length ; i++)
			str += (trs[i].toStringShort() + ((i==trs.length-1)?"}":","));
		
		return str;
	}
	
	public static String write(TRecovery[] trs){
		if ( trs == null )
			return null;
		else if ( trs.length == 0 )
			return "{}";
		
		String str = "{"; 
		for (int i=0 ; i<trs.length ; i++)
			str += trs[i].toStringShort();
		
		str += "}";
		return str;
	}
	
	public static String write(TMulticast[] tms){
		if ( tms == null )
			return null;
		else if ( tms.length == 0 )
			return "{}";
		
		String str = "{"; 
		for (int i=0 ; i<tms.length ; i++)
			str += tms[i];
		
		str += "}";
		return str;
	}

	
	public static String write(Map<InetSocketAddress, InetSocketAddress> toMasterNodeMap) {
		if ( toMasterNodeMap == null )
			return null;
		else if ( toMasterNodeMap.size() == 0 )
			return "{}";
		
		String str = "{"; 
		Iterator<Entry<InetSocketAddress, InetSocketAddress>> entrySetIt = toMasterNodeMap.entrySet().iterator();
		while ( entrySetIt.hasNext() ){
			Entry<InetSocketAddress, InetSocketAddress> entry = entrySetIt.next();
			str += (entry.getKey().getPort() + "->" + entry.getValue().getPort() + ((!entrySetIt.hasNext())?"}":","));
		}
		
		return str;
	}
	
	public static String write(SubscriptionEntry[] entries) {
		if ( entries == null )
			return null;
		else if ( entries.length == 0 )
			return "{}";
		
		String str = "{"; 
		for ( int i=0 ; i<entries.length ; i++ )
			str += (entries[i].toString() + ((i==entries.length)?"}":","));
		
		return str;
	}

	
	public static String write(BrokerIdentityManager idMan, Set<InetSocketAddress> outSet) {
		if(outSet == null)
			return "{null}";
		
		String str = "{";
		Iterator<InetSocketAddress> it = outSet.iterator();
		while(it.hasNext()) {
			InetSocketAddress remote = it.next();
			OverlayNodeId nodeId = idMan.getBrokerId(remote.toString());
			str += nodeId == null ? 
					remote.getPort() :
						nodeId.getNodeIdentifier();
			str += (it.hasNext()?", ":"");
		}
		
		return str + "}";
	}

	public static String write(Set<InetSocketAddress> outSet) {
		String str = "{";
		Iterator<InetSocketAddress> it = outSet.iterator();
		while ( it.hasNext() ){
			str += it.next().getPort() + (it.hasNext()?", ":"");
		}
		
		return str + "}";
	}

	
	public static String write(HashMap<InetSocketAddress, Sequence> arrivedSequence) {
		String str = "{";
		
		Iterator<Map.Entry<InetSocketAddress, Sequence>> it = arrivedSequence.entrySet().iterator();
		while ( it.hasNext() ){
			Map.Entry<InetSocketAddress, Sequence> entry = it.next();
			Sequence seq = entry.getValue();
			
			str += seq.toStringVeryShort() + (it.hasNext()?", ":"");
		}
		
		return str + "}";
	}

	public static String write(BrokerIdentityManager idMan, Sequence[] vector) {
		if ( vector == null )
			return null;
		
		String str = "{";
		
		for (int i=0 ;i<vector.length ; i++)
			if ( vector[i] == null )
				str += "null" + ((i==vector.length-1)?"":", ");
			else
				str += vector[i].toString(idMan) + ((i==vector.length-1)?"":", ");
		
		return str + "}";
	}
	
	public static String write(Sequence[] vector) {
		if ( vector == null )
			return null;
		
		String str = "{";
		
		for (int i=0 ;i<vector.length ; i++)
			if ( vector[i] == null )
				str += "null" + ((i==vector.length-1)?"":", ");
			else
				str += vector[i].toStringVeryShort() + ((i==vector.length-1)?"":", ");
		
		return str + "}";
	}

	
	public static String write(InetSocketAddress remote) {
		if ( remote == null )
			return null;
		else
			return "" + remote.getPort();
	}

	
	public static String write(ISessionMPRank[] sortedRanks) {
		String str = "RANKS: ";
		for ( int i=0 ; i<sortedRanks.length ; i++ )
			if ( sortedRanks[i] == null )
				throw new NullPointerException();
			else
				str += sortedRanks[i].toString() + ((i==sortedRanks.length-1)?"":", ");
		
		return str;
	}

	
	public static String write(Sequence sequence) {
		if ( sequence == null )
			return null;
		else
			return sequence.toString();
	}

	public static String write(byte[] bytes, int length) {
		if ( length < 0 )
			return "null";
		else
			return new String(bytes, 0, length);
	}

//	public static String write(BrokerIdentityManager brokerIdentityManager, Set<InetSocketAddress> outSet) {
//		if(outSet == null)
//			return "{NULL}";
//		
//		StringBuilder sb = new StringBuilder();
//		sb.append('{');
//		boolean first = true;
//		for(InetSocketAddress remote : outSet) {
//			if(brokerIdentityManager == null)
//				sb.append(String.valueOf(remote.getPort()));
//			else
//				 sb.append(brokerIdentityManager.getBrokerId(remote.toString()));
//			if(!first)
//				sb.append(':');
//			first = false;
//		}
//		
//		sb.append('}');
//		return sb.toString();
//	}

	public static String write(BrokerIdentityManager brokerIdentityManager, Collection<SequencePair> sequencePairs) {
		if(sequencePairs == null)
			return "{null}";
		
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		boolean first = true;
		for(SequencePair sp : sequencePairs) {
			if(!first)
				sb.append(',');
			sb.append(sp.toString(brokerIdentityManager));
			first = false;
		}
		sb.append('}');
		return sb.toString();
	}

	public static String write(InetSocketAddress remote, BrokerIdentityManager brokerIdentityManager) {
		if(brokerIdentityManager == null)
			return write(remote);
		
		OverlayNodeId nodeId = brokerIdentityManager.getBrokerId(remote);
		if(nodeId == null)
			return String.valueOf(remote.getPort());
		
		return nodeId.getNodeIdentifier();
	}
}
