package org.msrg.publiy.broker.core.contentManager;

import org.msrg.raccoon.CodedBatchType;
import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSBulkMatrix;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSReceivedCodedBatch;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSliceMatrix;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;
import org.msrg.publiy.publishSubscribe.Publication;

public class ReceivedPSContent extends Content {

	protected ReceivedPSContent(IBrokerShadow brokerShadow, Sequence sourceSequence, Publication publication, int contentSize, int rows) {
		super(new PSReceivedCodedBatch(brokerShadow, sourceSequence, publication, contentSize, rows));
		
		_mainSeedReceivedCount = 0;
	}
	
	public static ReceivedPSContent createReceivedPSContent(IBrokerShadow brokerShadow,
			Sequence sourceSequence, Publication publication, int rows, int cols) {
		return new ReceivedPSContent(brokerShadow, sourceSequence, publication, rows * cols, rows);
	}
	
	@Override
	public String toString() {
		return "ReceivedPSContent:" + getSourceSequence().toStringShort();
	}
	
	@Override
	public boolean canPotentiallyBeSolved() {
		return _codeBatch.canPotentiallyBeSolved();
	}
	
	@Override
	public final void loadDefaultSourcePSContent() {
		if(_codeBatch.getCodedBatchType() == CodedBatchType.SRC_CODED_BATCH) {
//			throw new IllegalStateException();
			return;
		}
		
		Sequence sourceSequence = getSourceSequence();
		Publication publication = getPublication();
		int rows = getRows();
		int cols = getCols();
		PSSliceMatrix[]slices = new PSSliceMatrix[rows];
		for(int i=0 ; i<slices.length ; i++) {
			slices[i] = new PSSliceMatrix(sourceSequence, cols);
			slices[i].loadNoCopy(DEFAULT_CONTENT[i]);
		}
		PSBulkMatrix content = new PSBulkMatrix(sourceSequence, slices);
		_codeBatch = new PSSourceCodedBatch(sourceSequence, publication, content);
	}
}
