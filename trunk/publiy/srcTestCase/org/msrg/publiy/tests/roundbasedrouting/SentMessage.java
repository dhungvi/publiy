package org.msrg.publiy.tests.roundbasedrouting;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;


class SentMessage {
	final IRawPacket _raw;
	final InetSocketAddress _receiver;
	final InetSocketAddress _sender;
	
	static Set<String> _senderRecieverPairs = new HashSet<String>();
	static Set<String> _recieverPairs = new HashSet<String>();
	
	SentMessage(IRawPacket raw, InetSocketAddress receiver, InetSocketAddress sender) {
		_raw = raw;
		_receiver = receiver;
		_sender = sender;
		
		IPacketable packet = raw.getObject();
		String sendReceiveIdentifier = "'" + _sender + "' -> '" + _receiver + "': " + ((TMulticast_Publish_MP)packet).getSourceSequence();
		if (!_senderRecieverPairs.add(sendReceiveIdentifier) )
			System.out.println("DUPLICATE: " + sendReceiveIdentifier);
		String receiveIdentifier = "'" + _receiver + "': " + ((TMulticast_Publish_MP)packet).getSourceSequence();
		if (!_recieverPairs.add(receiveIdentifier) )
			System.out.println("DUPLICATE: " + receiveIdentifier);
	}
	
	public static void clear() {
		_senderRecieverPairs.clear();
		_recieverPairs.clear();
	}

	public String toString() {
		return "Sent (" + ((_sender==null)?null:_sender.getPort()) + "_" + ((_receiver==null)?null:_receiver.getPort()) + "): " + _raw.getQuickString();
	}
}