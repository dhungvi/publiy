package org.msrg.publiy.broker.networkcodes.matrix.bulk;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.raccoon.ICodedBatch;

public interface IPSCodedBatch extends ICodedBatch {

	public Sequence getSourceSequence();
	public Publication getPublication();
	
	
	// Returns true only the 'first' time publication is loaded
	public boolean loadPublicationForFirstTime(Publication publication);

}
