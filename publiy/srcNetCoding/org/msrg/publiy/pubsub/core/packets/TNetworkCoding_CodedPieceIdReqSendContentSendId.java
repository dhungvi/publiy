package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class TNetworkCoding_CodedPieceIdReqSendContentSendId extends TNetworkCoding_CodedPieceIdReq {

	public TNetworkCoding_CodedPieceIdReqSendContentSendId(InetSocketAddress sender, Sequence sequence) {
		super(CodedPieceIdReqTypes.SEND_CONTENT_ID, sender, sequence);
	}

	public TNetworkCoding_CodedPieceIdReqSendContentSendId(ByteBuffer bb, int bbOffset) {
		super(bb, bbOffset);
	}
}
