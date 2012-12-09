package org.msrg.publiy.networkcodes;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSBulkMatrix;
import junit.framework.TestCase;

public class PSCodedPieceTest extends TestCase {

	int _rows = 500;
	int _cols = 500;
	
	PSBulkMatrix _bm;
	protected final InetSocketAddress _sourceAddress = Sequence.getRandomAddress();
	protected final LocalSequencer _localSequencer = LocalSequencer.init(null, _sourceAddress);
	protected final Sequence _sourceSeq = Sequence.getPsuedoSequence(_sourceAddress, 10);
			
	@Override
	public void setUp() {
		_bm = PSBulkMatrix.createBulkMatixIncrementalData(_sourceSeq, _rows, _cols);
	}
	
	public void testCodedPieceInOutByteBuffer() {
		PSCodedPiece pscp = PSCodedPiece.makeCodedPiece(_sourceSeq, _bm, true);
		
		ByteBuffer bb = ByteBuffer.allocate(pscp.getContentSize());
		pscp.putObjectInBuffer(_localSequencer, bb, 0);
		PSCodedPiece pscp1 = new PSCodedPiece(bb, 0);
		
		assertTrue(pscp1.equals(pscp));
	}
}
