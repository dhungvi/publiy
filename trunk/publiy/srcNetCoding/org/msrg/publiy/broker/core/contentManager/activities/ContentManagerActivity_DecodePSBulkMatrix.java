package org.msrg.publiy.broker.core.contentManager.activities;

import org.msrg.publiy.broker.core.contentManager.IContentListener;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.IPSCodedBatch;

public class ContentManagerActivity_DecodePSBulkMatrix extends ContentManagerActivity {
	
	public final IPSCodedBatch _psCodedBatch;
	
	public ContentManagerActivity_DecodePSBulkMatrix(IContentListener contentListener, IPSCodedBatch psCodedBatch) {
		super(ContentManagerActivityTypes.CONTENTMANAGER_ACTIVITY_DECODE, contentListener);
		
		_psCodedBatch = psCodedBatch;
	}
	
	@Override
	public String toString() {
		return super.toString() + "_SEQ[" + _psCodedBatch.getSourceSequence() + "]";
	}
}