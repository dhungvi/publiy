package org.msrg.publiy.broker.core.contentManager.servingPolicy;

import org.msrg.publiy.broker.core.contentManager.Content;

public class ContentServingPolicy_ServeHalfReceived extends
		ContentServingPolicy {

	final int _minReceivedPieces;
	final double _minReceivedPercentage;
	
	public ContentServingPolicy_ServeHalfReceived(int minReceivedPieces) {
		super(ContentServingPolicyType.CONTENT_SERVING_POLICY_SEND_WHEN_HALF_RECEIVED);
		
		_minReceivedPieces = minReceivedPieces;
		if(_minReceivedPieces < 1)
			throw new IllegalArgumentException("Invlaid arg: " + _minReceivedPieces);
		_minReceivedPercentage = -1;
	}
	
	public ContentServingPolicy_ServeHalfReceived(double minReceivedPercentage) {
		super(ContentServingPolicyType.CONTENT_SERVING_POLICY_SEND_WHEN_HALF_RECEIVED);
		
		_minReceivedPieces = -1;
		_minReceivedPercentage = minReceivedPercentage;
		if(_minReceivedPercentage > 1.0 || _minReceivedPercentage < 0.0)
			throw new IllegalArgumentException("Invlaid arg: " + _minReceivedPercentage);
	}

	@Override
	public boolean canServe(Content content) {
		if(!super.canServe(content))
			return false;
		
		double availablePieces = content.getAvailableCodedPieceCount();
		double requiredPieces = content.getRequiredCodedPieceCount();

		if(availablePieces < _minReceivedPieces)
			return false;
		
		double receivedPercentage = availablePieces / requiredPieces;
		if(receivedPercentage < _minReceivedPercentage)
			return false;
			
		return true;
	}

	@Override
	public String toString() {
		return super.toStringPrefix() + "[" + _minReceivedPercentage + "," + _minReceivedPieces + "]";
	}

}
