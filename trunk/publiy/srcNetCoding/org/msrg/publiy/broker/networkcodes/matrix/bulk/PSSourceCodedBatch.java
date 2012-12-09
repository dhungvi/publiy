package org.msrg.publiy.broker.networkcodes.matrix.bulk;

import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.networkcodes.PSCodedCoefficients;
import org.msrg.publiy.networkcodes.PSCodedPiece;
import org.msrg.publiy.publishSubscribe.Publication;

import org.msrg.raccoon.CodedCoefficients;
import org.msrg.raccoon.CodedPiece;
import org.msrg.raccoon.ICodedBatch;
import org.msrg.raccoon.SourceCodedBatch;

public class PSSourceCodedBatch extends SourceCodedBatch implements IPSCodedBatch {

	public final Sequence _sequence;
	public final Publication _publication;
	
	public PSSourceCodedBatch(Sequence sequence, Publication publication, PSBulkMatrix content) {
		super(content);

		_sequence = sequence;
		if(_sequence == null)
			throw new NullPointerException();
		
		_publication = publication;
		if(_publication == null)
			throw new NullPointerException();
	}

	@Override
	public boolean equalsExact(ICodedBatch cBatch) {
		if(cBatch == null)
			return false;
		
		if(!IPSCodedBatch.class.isAssignableFrom(cBatch.getClass()))
			return false;
		
		return super.equalsExact(cBatch);
	}

	@Override
	public Publication getPublication() {
		return _publication;
	}

	@Override
	public Sequence getSourceSequence() {
		return _sequence;
	}

	@Override
	public boolean loadPublicationForFirstTime(Publication publication) {
		if(publication == null)
			return false;
		
		if(!publication.equals(_publication))
			throw new IllegalStateException();
		
		return false;
	}
	
	@Override @Deprecated
	public CodedPiece code() {
		return PSCodedPiece.makeCodedPiece(_sequence, _bm, true);
	}
	
	@Override
	public CodedCoefficients getNewCodedCoefficients(int length) {
		return new PSCodedCoefficients(length);
	}

	@Override
	public CodedCoefficients getNewCodedCoefficients(byte[] b) {
		return new PSCodedCoefficients(b);
	}
	
	@Override
	public String toString() {
		return "PSSourceCodedBatch: [" + _publication + "] " + _sequence;
	}
}
