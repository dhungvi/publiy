package org.msrg.publiy.utils;

import java.util.Collection;
import java.util.List;

import org.msrg.publiy.broker.BFTSuspecionReason;
import org.msrg.publiy.broker.core.sequence.SequencePair;

public class BFTWriters extends Writers {

	public static String write(List<SequencePair> _splist) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		boolean first = true;
		for(SequencePair sp : _splist) {
			sb.append((first ? "" : ", ") + sp);
			first = false;
		}
		sb.append(']');
		return sb.toString();
	}

	public static String write(Collection<BFTSuspecionReason> reasons) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		boolean first = true;
		for(BFTSuspecionReason reason : reasons) {
			sb.append((first ? "" : ", ") + reason);
			first = false;
		}
		sb.append(']');
		return sb.toString();
	}

	private final static char HEXCHARS[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	public static String write(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		boolean first = true;
		for(byte b : bytes) {
			if(!first)
				sb.append(',');
			
			sb.append("0x" + HEXCHARS[(b/16) & 0x0F] + HEXCHARS[b & 0x0F]);
			first = false;
		}
			
		sb.append(']');
		return sb.toString();
	}
}
