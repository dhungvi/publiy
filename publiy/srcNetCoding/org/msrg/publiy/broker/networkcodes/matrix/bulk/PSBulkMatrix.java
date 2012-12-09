package org.msrg.publiy.broker.networkcodes.matrix.bulk;

import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.raccoon.matrix.bulk.BulkMatrix;
import org.msrg.raccoon.matrix.bulk.SliceMatrix;

public class PSBulkMatrix extends BulkMatrix {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = 5509840761603972510L;
	public final Sequence _sourceSeq;
	
	public PSBulkMatrix(Sequence sourceSeq, PSSliceMatrix[] slices) {
		super(slices);
		
		_sourceSeq = sourceSeq;
	}
	
	public PSBulkMatrix(Sequence sourceSeq, int rows, int cols) {
		super(rows, cols);
		
		_sourceSeq = sourceSeq;
	}

	public SliceMatrix createOneEmtpySlice(int cols) {
		return new PSSliceMatrix(_sourceSeq, cols);
	}
	
	public Sequence getSourceSeq() {
		return _sourceSeq;
	}
	
	@Override
	public int hashCode() {
		return _sourceSeq.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !this.getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		PSBulkMatrix psBulkMatrixObj = (PSBulkMatrix)obj;
		return _sourceSeq.equals(psBulkMatrixObj._sourceSeq);
	}
	
	@Override
	public BulkMatrix createEmptyMatrix(int rows, int cols) {
		return new PSBulkMatrix(_sourceSeq, rows, cols);
	}
	
	public static PSBulkMatrix createBulkMatixIncrementalData(
			Sequence sequence, int rows, int cols) {
		PSBulkMatrix bm = new PSBulkMatrix(sequence, rows, cols);
		int val=0;
		for(int j=0 ; j<bm.getSliceCount() ; j++) {
			SliceMatrix slice = bm.slice(j);
			byte[] b = new byte[slice._cols];
			for(int k=0 ; k<slice._cols ; k++)
				b[k] = (byte) val++;
			slice.loadNoCopy(b);
		}
		
		return bm;
	}
}
