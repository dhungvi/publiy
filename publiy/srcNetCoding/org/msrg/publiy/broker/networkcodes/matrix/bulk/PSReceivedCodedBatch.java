package org.msrg.publiy.broker.networkcodes.matrix.bulk;

import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.networkcodes.PSCodedCoefficients;
import org.msrg.publiy.networkcodes.engine.CodingEngineLogger;
import org.msrg.publiy.publishSubscribe.Publication;

import org.msrg.raccoon.matrix.bulk.BulkMatrix;
import org.msrg.raccoon.matrix.bulk.SliceMatrix;
import org.msrg.raccoon.CodedCoefficients;
import org.msrg.raccoon.CodedPiece;
import org.msrg.raccoon.ICodedBatch;
import org.msrg.raccoon.ReceivedCodedBatch;

public class PSReceivedCodedBatch extends ReceivedCodedBatch implements IPSCodedBatch {

	protected Publication _publication;
	protected final Sequence _sequence;
	protected final CodingEngineLogger _logger;
	
	public PSReceivedCodedBatch(IBrokerShadow brokerShadow, Sequence sequence, Publication publication, int batchSize, int rows) {
		super(batchSize, rows);
		
		_publication = publication;
		_sequence = sequence;
		_logger = (brokerShadow != null) ? brokerShadow.getCodingEngineLogger() : null;
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
		
		synchronized(_sequence) {
			if(_publication == null) {
				_publication = publication;
				return true;
			} else if(!_publication.equals(publication))
				throw new IllegalStateException("Publications mismatch: " + publication + " vs. " + _publication);
			
			return false;
		}
	}
	
	@Override
	public BulkMatrix createNewBulkMatrix(CodedPiece[] codedPieces){
		int slicesCount = codedPieces.length;
		PSSliceMatrix[] psSlicesContent = new PSSliceMatrix[slicesCount];
		for(int i=0 ; i<slicesCount ; i++)
			psSlicesContent[i] = (PSSliceMatrix) codedPieces[i]._codedContent;

		return new PSBulkMatrix(_sequence, psSlicesContent);
	}
	
	@Override
	public BulkMatrix createNewBulkMatrix(SliceMatrix[] slicesContent) {
		PSSliceMatrix[] psSlicesContent = new PSSliceMatrix[slicesContent.length];
		for(int i=0 ; i<psSlicesContent.length ; i++)
			psSlicesContent[i] = (PSSliceMatrix) slicesContent[i];
		return new PSBulkMatrix(_sequence, psSlicesContent);
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
	protected void removeBadRow(int badRow){
		if(_logger != null)
			_logger.codingPieceDiscarded();
		super.removeBadRow(badRow);
	}
	
	@Override
	protected final CodedPiece[] canSolveHighPriority() {
		CodedPiece[] retCodedPiece = null;
		if(_logger != null)
			_logger.inverseStarted();
		{
			retCodedPiece = super.canSolveHighPriority();
		}
		if(_logger != null)
			_logger.inverseFinished(retCodedPiece != null);
		return retCodedPiece;
	}
}
