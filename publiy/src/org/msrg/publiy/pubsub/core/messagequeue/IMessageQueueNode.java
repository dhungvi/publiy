package org.msrg.publiy.pubsub.core.messagequeue;

import java.net.InetSocketAddress;

import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.broker.core.sequence.Sequence;

interface IMessageQueueNode {
	public int getMQNSequence();
	
	boolean isConfirmed();
	Sequence getNodeSequence();
	Sequence getMessageSourceSequence();
	IMessageQueueNode getNext();
	void setNext(IMessageQueueNode mqn);
	void cancelCheckoutUnVisited(InetSocketAddress remote);
	void cancelCheckoutVisited(InetSocketAddress remote);
	void passThrough(InetSocketAddress remote);
	IRawPacket checkOutAndmorph(ISession session, boolean asGuided);
	void checkForConfirmation();
	TMulticast_Conf getConfirmation(InetSocketAddress remote, InetSocketAddress from, Sequence confirmationFromSequence);
	TMulticast getTMulticast();
	InetSocketAddress getSender();
	InetSocketAddress [] getSenders();
	void addToSenders(InetSocketAddress sender);
	ITMConfirmationListener getConfirmationListener();
	String toString();
	void confirmationReceieved(TMulticast_Conf tmc);
	boolean confirmed(boolean isConfirmed);
	boolean checkedOutSetContains(InetSocketAddress remoteInBetween);
	TMulticast getMessage();
	boolean canFastConfirm();
	String getConfirmationStatus();
	
	String toStringLong(boolean testForConfirmation);
	TMulticastTypes getType();
	boolean visitedBy(InetSocketAddress _remote);
}
