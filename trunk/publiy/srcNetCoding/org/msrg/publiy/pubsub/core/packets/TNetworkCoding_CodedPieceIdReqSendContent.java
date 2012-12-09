package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class TNetworkCoding_CodedPieceIdReqSendContent extends TNetworkCoding_CodedPieceIdReq {

	public TNetworkCoding_CodedPieceIdReqSendContent(InetSocketAddress sender, Sequence sequence) {
		super(CodedPieceIdReqTypes.SEND_CONTENT, sender, sequence);
	}

	public TNetworkCoding_CodedPieceIdReqSendContent(ByteBuffer bb, int bbOffset) {
		super(bb, bbOffset);
	}
}
