package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class TNetworkCoding_CodedPieceIdReqBreakDeclined extends TNetworkCoding_CodedPieceIdReqBreak {
	
	public TNetworkCoding_CodedPieceIdReqBreakDeclined(
			InetSocketAddress sender, Sequence sequence,
			int outstandingPieces) {
		super(CodedPieceIdReqTypes.BREAK_DECLINE, sender, sequence, outstandingPieces);
	}
	
	public int getOustandingPieces() {
		return super._breakingSize;
	}
	
	public TNetworkCoding_CodedPieceIdReqBreakDeclined(ByteBuffer bb, int bbOffset) {
		super(bb, bbOffset);
	}
}