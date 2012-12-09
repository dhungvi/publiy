package org.msrg.publiy.client.subscriber.casuallogger;

import java.io.IOException;
import java.io.Writer;

import org.msrg.publiy.broker.PubForwardingStrategy;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimerReading;

class PublicationGenerationEntry {

	private static final char ENTRY_STR_SEPARATOR = ' ';
	final BrokerInternalTimerReading _entryCreatitonInternalTime;
	final Sequence _seqID;
	final long _gTimestamp;
	final int _mpBundleIndex;
	final TMulticastTypes _tmType;
	final PubForwardingStrategy _forwardingStrategy;

	PublicationGenerationEntry(TMulticast_Publish tmp) {
		_entryCreatitonInternalTime = BrokerInternalTimer.read();
		_seqID = tmp.getSourceSequence();
		_tmType = tmp.getType();
		
		switch(_tmType) {
		case T_MULTICAST_PUBLICATION_MP:
			TMulticast_Publish_MP tmp_mp = (TMulticast_Publish_MP) tmp;
			_mpBundleIndex = tmp_mp._mpBundleIndex;
			_gTimestamp = tmp_mp._gTime;
			_forwardingStrategy = tmp_mp._forwardingStrategy;
			break;

		case T_MULTICAST_PUBLICATION_BFT:
		case T_MULTICAST_PUBLICATION_NC:
		case T_MULTICAST_PUBLICATION:
			_forwardingStrategy = null;
			_mpBundleIndex = -1;
			_gTimestamp = -1;
			break;
			
		default:
			throw new UnsupportedOperationException("" + tmp);
		}
	}
	
	PublicationGenerationEntry(Sequence sequence, TMulticastTypes tmType) {
		_entryCreatitonInternalTime = BrokerInternalTimer.read();
		_seqID = sequence;
		_tmType = tmType;
		_forwardingStrategy = null;
		_mpBundleIndex = -1;
		_gTimestamp = -1;
	}
	
	void write(Writer ioWriter) throws IOException {
		ioWriter.append(toString());
	}
	
	public static String getFormat() {
		return "#GEN_TIME TMTYPE STR BUNDIDX SRC_SEQ";
	}

	@Override
	public String toString() {
		return _entryCreatitonInternalTime.toString() + ENTRY_STR_SEPARATOR +
			_tmType._shortName + "[" + ENTRY_STR_SEPARATOR +
			_forwardingStrategy + ENTRY_STR_SEPARATOR + 
			_mpBundleIndex + ENTRY_STR_SEPARATOR +
			_gTimestamp + "]" + ENTRY_STR_SEPARATOR +
			_seqID;
	}

}
