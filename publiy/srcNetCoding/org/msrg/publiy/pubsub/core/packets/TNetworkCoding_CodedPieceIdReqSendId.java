package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class TNetworkCoding_CodedPieceIdReqSendId extends TNetworkCoding_CodedPieceIdReq {

	public TNetworkCoding_CodedPieceIdReqSendId(InetSocketAddress sender, Sequence sequence) {
		super(CodedPieceIdReqTypes.SEND_ID, sender, sequence);
	}

	public TNetworkCoding_CodedPieceIdReqSendId(ByteBuffer bb, int bbOffset) {
		super(bb, bbOffset);
	}
}
