package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class TNetworkCoding_CodedPieceIdReqBreakSendId extends TNetworkCoding_CodedPieceIdReq {

	public TNetworkCoding_CodedPieceIdReqBreakSendId(InetSocketAddress sender, Sequence sequence) {
		super(CodedPieceIdReqTypes.BREAK_SEND_ID, sender, sequence);
	}

	public TNetworkCoding_CodedPieceIdReqBreakSendId(ByteBuffer bb, int bbOffset) {
		super(bb, bbOffset);
	}
}
