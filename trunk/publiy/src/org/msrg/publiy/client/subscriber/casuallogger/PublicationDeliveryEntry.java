package org.msrg.publiy.client.subscriber.casuallogger;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;
import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;

public class PublicationDeliveryEntry extends PublicationGenerationEntry{

	private static final char ENTRY_STR_SEPARATOR = ' ';
	final int _pathLength;
	final long _dTimestamp;
	final boolean _deltaViolating;
	final boolean _legitimacyViolating;
	final boolean _legitimacyViolated;
	final int _validSequencePairsCount;
	final int _invalidSequencePairsCount;

	PublicationDeliveryEntry(TMulticast_Publish tmp) {
		super(tmp);

		switch(_tmType) {
		case T_MULTICAST_PUBLICATION_MP:
			TMulticast_Publish_MP tmp_mp = (TMulticast_Publish_MP) tmp;
			_pathLength = tmp_mp._pathLength;
			_dTimestamp = tmp_mp._dTime;
			_validSequencePairsCount = 0;
			_invalidSequencePairsCount = 0;
			break;
			
		case T_MULTICAST_PUBLICATION_BFT:
			_validSequencePairsCount = ((TMulticast_Publish_BFT)tmp).getValidSequencePairsCount();
			_invalidSequencePairsCount = ((TMulticast_Publish_BFT)tmp).getInvalidSequencePairsCount();
			_pathLength = -1;
			_dTimestamp = -1;
			break;
			
		case T_MULTICAST_PUBLICATION:
			_validSequencePairsCount = 0;
			_invalidSequencePairsCount = 0;
			_pathLength = -1;
			_dTimestamp = -1;
			break;
			
		default:
			throw new UnsupportedOperationException("" + tmp);
		}
		
		_deltaViolating = tmp.getDeltaViolating();
		_legitimacyViolating = tmp.getLegitimacyViolating();
		_legitimacyViolated = tmp.getLegitimacyViolated();
	}
	
	public static String getFormat() {
		return "#DELIV_TIME TMTYPE STR BUNDIDX PATHLEN GEN_TIME DELIV_TIME SRC_SEQ [VALID:INVALID-SEQ-PAIR-COUNT]";
	}

	public String getViolationStatus() {
		String ret = null;
		if(_deltaViolating)
			ret = "DVG";
		
		if(_legitimacyViolating)
			ret = ret == null ? "LVG" : "-LVG";
		
		if(_legitimacyViolated)
			ret = ret == null ? "LVD" : "-LVD";
		
		return ret;
	}
	
	@Override
	public String toString() {
		String violationStr = getViolationStatus();
		
		StringBuilder sbuilder = new StringBuilder();
		sbuilder.append(_entryCreatitonInternalTime.toString() + ENTRY_STR_SEPARATOR);
		sbuilder.append(_tmType._shortName + "[" + ENTRY_STR_SEPARATOR);
		sbuilder.append(_forwardingStrategy == null ? ("N/A" + ENTRY_STR_SEPARATOR) : (_forwardingStrategy.toString() + ENTRY_STR_SEPARATOR));
		sbuilder.append(_mpBundleIndex + ENTRY_STR_SEPARATOR);
		sbuilder.append(_pathLength + ENTRY_STR_SEPARATOR);
		sbuilder.append(_gTimestamp + ENTRY_STR_SEPARATOR);
		sbuilder.append(_dTimestamp + "]" + ENTRY_STR_SEPARATOR);
		sbuilder.append(_seqID + (violationStr == null ? "" : "\t" + violationStr) + ENTRY_STR_SEPARATOR);
		sbuilder.append(_validSequencePairsCount + ":" + _invalidSequencePairsCount);
		
		return sbuilder.toString();
	}
}
