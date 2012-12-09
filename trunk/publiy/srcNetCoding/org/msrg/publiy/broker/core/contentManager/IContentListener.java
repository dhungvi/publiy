package org.msrg.publiy.broker.core.contentManager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.IPSCodedBatch;
import org.msrg.publiy.networkcodes.PSCodedPiece;


public interface IContentListener {

	public void codedContentReady(PSCodedPiece psCodedPiece, InetSocketAddress remote);
	public void decodedContentInversed(Sequence sourceSequence);
	public void decodedContentReady(boolean success, IPSCodedBatch psDecodedBatch);
	
}
