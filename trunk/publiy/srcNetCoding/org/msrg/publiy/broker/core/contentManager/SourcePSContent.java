package org.msrg.publiy.broker.core.contentManager;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.IPSCodedBatch;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSBulkMatrix;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSliceMatrix;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;
import org.msrg.publiy.publishSubscribe.Publication;

public class SourcePSContent extends Content {

	protected SourcePSContent(PSSourceCodedBatch psSourceCodedBatch) {
		super(psSourceCodedBatch);
		
		_mainSeedReceivedCount = -1;
	}
	
	protected SourcePSContent(Sequence sourceSequence, Publication publication, PSBulkMatrix psBM) {
		this(new PSSourceCodedBatch(sourceSequence, publication, psBM));
	}
	
	public static SourcePSContent createDefaultSourcePSContent(
			Sequence sourceSequence, Publication publication, int rows, int cols) {
//		PSBulkMatrix psBM = PSBulkMatrix.createBulkMatixIncrementalData(sourceSequence, rows, cols);
		if(rows != DEFAULT_CONTENT.length)
			throw new IllegalArgumentException("" + rows + " vs. " + DEFAULT_CONTENT.length);
		
		if(cols != DEFAULT_CONTENT[0].length)
			throw new IllegalArgumentException("" + cols + " vs. " + DEFAULT_CONTENT[0].length);
		
		PSSliceMatrix[]slices = new PSSliceMatrix[rows];
		for(int i=0 ; i<slices.length ; i++) {
			slices[i] = new PSSliceMatrix(sourceSequence, cols);
			slices[i].loadNoCopy(DEFAULT_CONTENT[i]);
		}
		PSBulkMatrix psBM = new PSBulkMatrix(sourceSequence, slices);
		SourcePSContent ret = new SourcePSContent(sourceSequence, publication, psBM);
		return ret;
	}
	
	public static boolean verifyIntegrityDefaultSourcePSContent(Content content) {
		if(Broker.RELEASE)
			return true;
		
		IPSCodedBatch psCodedBatch = content._codeBatch;
		PSBulkMatrix psBulkMatrix = (PSBulkMatrix)psCodedBatch.getBulkMatrix();
		Sequence sourceSequence = content.getSourceSequence();
		
		if(sourceSequence == null)
			return false;
		
		if(psBulkMatrix._sourceSeq != sourceSequence)
			return false;
		
		if(psBulkMatrix._cols * psBulkMatrix._rows != psBulkMatrix._size)
			return false;
		
		if(psBulkMatrix._rows != DEFAULT_CONTENT.length)
			return false;
		
		if(psBulkMatrix._cols != DEFAULT_CONTENT[0].length)
			return false;
		
		byte[][] byteContent = psBulkMatrix.getContent();
		if(byteContent.length != psBulkMatrix._rows)
			return false;
		
		for(int i=0 ; i<psBulkMatrix._rows ; i++) {
			for(int j=0 ; j<psBulkMatrix._cols ; j++)
				if(byteContent[i][j] != DEFAULT_CONTENT[i][j])
					return false;
		}
			
		return true;
	}
	
	@Override
	public String toString() {
		return "SourcePSContent:" + getSourceSequence().toStringShort();
	}

	@Override
	public boolean canPotentiallyBeSolved() {
		return false;
	}
	
	@Override
	public void loadDefaultSourcePSContent() {
		throw new UnsupportedOperationException();
	}
}
