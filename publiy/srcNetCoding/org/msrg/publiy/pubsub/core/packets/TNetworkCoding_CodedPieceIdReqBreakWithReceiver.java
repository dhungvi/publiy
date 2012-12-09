package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class TNetworkCoding_CodedPieceIdReqBreakWithReceiver extends
		TNetworkCoding_CodedPieceIdReqBreak {

	public final InetSocketAddress _receiver;
	
	public TNetworkCoding_CodedPieceIdReqBreakWithReceiver(
			InetSocketAddress sender, InetSocketAddress receiver, Sequence sequence, int breakingSize) {
		super(sender, sequence, breakingSize);
		
		_receiver = receiver;
	}
}
