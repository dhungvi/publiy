package org.msrg.publiy.communication.core.niobinding;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.packets.CodedPieceIdReqTypes;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPiece;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReq;
import org.msrg.publiy.utils.log.casuallogger.AbstractCasualLogger;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;

public class CommunicationTransportLogger extends AbstractCasualLogger {

	public static final int SAMPLING_INTERVAL = 1000;
	
	protected static final Object _LOCK = new Object();
	protected static final Random _RAND = new Random();
	
	protected static boolean _IS_BROKER = false;
	protected final IBrokerShadow _brokerShadow;
	protected final String _codedBlocksLogFilename;
	protected static TrafficEntry _totalTrafficEntity = new TrafficEntry();
	protected final static Map<InetSocketAddress, NodeTrafficEntry> _nodeTrafficEntries =
		new HashMap<InetSocketAddress, NodeTrafficEntry>();
	protected final static Map<Sequence, SequenceTrafficEntry> _sequenceTrafficEntries =
		new HashMap<Sequence, SequenceTrafficEntry>();
	
	public CommunicationTransportLogger(BrokerShadow brokerShadow) {
		brokerShadow.setCommunicationTransportLogger(this);
		_brokerShadow = brokerShadow;
		_codedBlocksLogFilename = _brokerShadow.getCodedBlocksLogFilename();
		if(_codedBlocksLogFilename == null)
			throw new NullPointerException();
		
		_IS_BROKER = _brokerShadow.isBroker();
	}
	
	protected static InetSocketAddress getSender(IRawPacket raw) {
		InetSocketAddress sender = null;
		switch(raw.getType()) {
		case TCODEDPIECE:
			TNetworkCoding_CodedPiece tCodedPiece = (TNetworkCoding_CodedPiece) raw.getObject();
			sender = tCodedPiece._sender;
			break;
			
		case TCODEDPIECE_ID:
			TNetworkCoding_CodedPieceId tCodedPieceId = (TNetworkCoding_CodedPieceId) raw.getObject();
			sender = tCodedPieceId._sender;
			break;
			
		case TCODEDPIECE_ID_REQ:
			TNetworkCoding_CodedPieceIdReq tCodedPieceIdReq = (TNetworkCoding_CodedPieceIdReq) raw.getObject();
			sender = tCodedPieceIdReq._sender;
			break;
			
		default:
			sender = raw.getSender();
		}

		return sender;
	}
	
	public static void logIncoming(IBrokerShadow brokerShadow, IRawPacket raw) {
		IPacketable packet = PacketFactory.unwrapObject(brokerShadow, raw);
		InetSocketAddress sender = getSender(raw);
		
		report("IN", sender, packet);
		
		if(!Broker.RELEASE)// || _IS_BROKER)
		{
			InetSocketAddress receiver = raw.getReceiver();
//			int reciverPort = receiver.getPort();
			BrokerInternalTimer.inform(
					"Delivering FROM: " + sender.getPort() + 
					" TO: " + receiver.getPort() + 
					"\t" + raw.getString(brokerShadow));
		}
		
		synchronized(_LOCK) {
//			Integer i = _deliveryMap.get(reciverPort);
//			_deliveryMap.put(reciverPort, (i==null?0:(++i)));

			switch(packet.getObjectType()) {
			case TCODEDPIECE:
				{
					_totalTrafficEntity._totalReceived++;
					_totalTrafficEntity._totalBytesReceived+=packet.getContentSize();
				}
				{
					NodeTrafficEntry tEntry = getNodeTrafficEntryPrivately(sender);
					tEntry._totalReceived++;
					tEntry._totalBytesReceived+=packet.getContentSize();
				}
				
				{
					TNetworkCoding_CodedPiece tCodedPiece = (TNetworkCoding_CodedPiece) packet;
					Sequence contentSequence = tCodedPiece._sourceSeq;
					SequenceTrafficEntry sEntry = getSequenceTrafficEntryPrivately(contentSequence);
					sEntry._totalReceived++;
					sEntry._totalBytesReceived+=packet.getContentSize();
				}
				
			default:
				break;
			}
		}
	}

	protected static SequenceTrafficEntry getSequenceTrafficEntryPrivately(Sequence sequence) {
		SequenceTrafficEntry sEntry = _sequenceTrafficEntries.get(sequence);
		if(sEntry == null) {
			sEntry = new SequenceTrafficEntry(sequence);
			_sequenceTrafficEntries.put(sequence, sEntry);
		}
		
		return sEntry;
	}

	protected static NodeTrafficEntry getNodeTrafficEntryPrivately(InetSocketAddress remote) {
		NodeTrafficEntry tEntry = _nodeTrafficEntries.get(remote);
		if(tEntry == null) {
			tEntry = new NodeTrafficEntry(remote);
			_nodeTrafficEntries.put(remote, tEntry);
		}
		
		return tEntry;
	}

	
	public static IRawPacket logOutgoing(IRawPacket raw) {
		InetSocketAddress receiver = raw.getReceiver();
		IPacketable packet = raw.getObject();
		
		report("Out", receiver, packet);
		
		if(!Broker.RELEASE)// || _IS_BROKER)
		{
			InetSocketAddress sender = raw.getSender();
//			int senderPort = sender.getPort();
			BrokerInternalTimer.inform(
					"Sending FROM: " + sender.getPort() + 
					" TO: " + receiver.getPort() + 
					"\t" + packet);
		}
		
		synchronized(_LOCK) {
//			Integer i = _sendMap.get(senderPort);
//			_sendMap.put(senderPort, (i==null?0:(++i)));
			
			switch(packet.getObjectType()) {
			case TCODEDPIECE:
				{
					_totalTrafficEntity._totalSent++;
					_totalTrafficEntity._totalBytesSent+=packet.getContentSize();
				}
				{
					TrafficEntry tEntry = getNodeTrafficEntryPrivately(receiver);
					tEntry._totalSent++;
					tEntry._totalBytesSent+=packet.getContentSize();
				}
				
				{
					TNetworkCoding_CodedPiece tCodedPiece = (TNetworkCoding_CodedPiece) packet;
					Sequence contentSequence = tCodedPiece._sourceSeq;
					SequenceTrafficEntry sEntry = getSequenceTrafficEntryPrivately(contentSequence);
					sEntry._totalSent++;
					sEntry._totalBytesSent+=packet.getContentSize();
				}
				{
					if(_RAND.nextInt(100) < Broker.PACKET_LOSS_PERCENTAGE)
						return null;
				}
				break;
				
			default:
				break;
			}
			
			return raw;
		}
	}
	
	static boolean _seenCodedPiece = false;
	private static void report(String string, InetSocketAddress remote, IPacketable packet) {
//		if(Broker.RELEASE && !Broker.DEBUG)
//			return;
		
		if(packet == null)
			return;
		PacketableTypes type = packet.getObjectType();
		
		if(type == PacketableTypes.TCODEDPIECE_ID_REQ) {
			TNetworkCoding_CodedPieceIdReq tCodedIdReq =
				(TNetworkCoding_CodedPieceIdReq) packet;
			CodedPieceIdReqTypes subType = tCodedIdReq._reqType;
			switch(subType) {
			case PLIST_REPLY:
			case PLIST_REQ:
				BrokerInternalTimer.inform("HERE " + string + ": " + remote.getPort() + ": " + packet);
				break;
				
			default:
				break;
			}
		}
		
		if(type == PacketableTypes.TCODEDPIECE) {
			TNetworkCoding_CodedPiece tCodedPiece = (TNetworkCoding_CodedPiece) packet;
			if(!_seenCodedPiece)
				BrokerInternalTimer.inform("HERE " + string + ": " + remote.getPort() + ": " + tCodedPiece);
			_seenCodedPiece = true;
		}
	}

	public static String toStringStatic() {
		return "Unavailable";
//		StringWriter writer = new StringWriter();
//
//		synchronized(_LOCK) {
//			writer.append('[');
//			for(Entry<Integer, Integer> entry : _deliveryMap.entrySet())
//				writer.append(entry.getKey() + "<-" + entry.getValue() + ",");
//			writer.append(']');
//			
//			writer.append('[');
//			for(Entry<Integer, Integer> entry : _sendMap.entrySet())
//				writer.append(entry.getKey() + "->" + entry.getValue() + ",");
//			writer.append(']');
//		}
//		
//		return writer.toString();
	}

	@Override
	protected String getFileName() {
		return _codedBlocksLogFilename;
	}
	
	@Override
	protected void runMe() throws IOException {
		synchronized (_lock) {
			if ( _firstTime ) {
				String headerLine = "#TIME SENDER:SENT:BSENT:RCVD:BRCVD\n";
				_logFileWriter.write(headerLine);
				_firstTime = false;
			}
			
			StringWriter stringWriter = new StringWriter();
			synchronized(_LOCK) {
				stringWriter.append(printSkippedTimes());
				stringWriter.append(BrokerInternalTimer.read().toString());
				stringWriter.append(" " + _totalTrafficEntity);
				
				for(Entry<InetSocketAddress, NodeTrafficEntry> entry : _nodeTrafficEntries.entrySet())
					stringWriter.append(" " + entry.getValue());
			
				for(Entry<Sequence, SequenceTrafficEntry> entry : _sequenceTrafficEntries.entrySet())
					stringWriter.append(" " + entry.getValue());

				_totalTrafficEntity = new TrafficEntry();
				_nodeTrafficEntries.clear();
				_sequenceTrafficEntries.clear();
			}
			
			_logFileWriter.write(stringWriter.toString());
			_logFileWriter.write('\n');
		}
	}
	
	@Override
	public boolean isEnabled() {
		return _brokerShadow.isNC() && super.isEnabled();
	}

	@Override
	public String toString() {
		return "CodedBlocksLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}
	
	@Override
	protected int getDefaultLoggingInterval() {
		return SAMPLING_INTERVAL;
	}
}

class SequenceTrafficEntry extends TrafficEntry {
	final Sequence _sequence;
	
	SequenceTrafficEntry(Sequence sequence) {
		_sequence = sequence;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		SequenceTrafficEntry stEntityObj = (SequenceTrafficEntry) obj;
		return _sequence.equals(stEntityObj._sequence);
	}
	
	@Override
	public int hashCode() {
		return _sequence.hashCode();
	}
	
	@Override
	public String toString() {
		return _sequence._address.getPort() + "_" + _sequence._order + ":::" + super.toString();
	}
}

class NodeTrafficEntry extends TrafficEntry {

	final InetSocketAddress _remote;
	
	NodeTrafficEntry(InetSocketAddress remote) {
		_remote = remote;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		NodeTrafficEntry ntEntityObj = (NodeTrafficEntry) obj;
		return _remote.equals(ntEntityObj._remote);
	}

	@Override
	public int hashCode() {
		return _remote.hashCode();
	}
	
	@Override
	public String toString() {
		return _remote.getPort() + "::" + super.toString();
	}
}

class TrafficEntry {
	int _totalBytesSent;
	int _totalSent;
	
	int _totalBytesReceived;
	int _totalReceived;
	
	protected TrafficEntry() {
		_totalBytesSent = 0;
		_totalSent = 0;
		_totalBytesReceived = 0;
		_totalReceived = 0;	
	}

	public String toString() {
		return _totalSent + ":" + _totalBytesSent + ":" + _totalReceived + ":" + _totalBytesReceived;
	}
}