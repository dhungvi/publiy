package org.msrg.publiy.broker.core.contentManager.servingPolicy;

import org.msrg.publiy.broker.core.contentManager.Content;

public abstract class ContentServingPolicy {

	final ContentServingPolicyType _policyType;
	protected ContentServingPolicy(ContentServingPolicyType policyType) {
		_policyType = policyType;
	}
	
	public boolean canServe(Content content) {
		return (content.getNeedNodes().isEmpty());
	}
	
	@Override
	public abstract String toString();
	
	public String toStringPrefix() {
		return _policyType.toString();
	}
}

enum ContentServingPolicyType {
	
	CONTENT_SERVING_POLICY_SEND_WHEN_FULLY_RECEIVED,
	CONTENT_SERVING_POLICY_SEND_WHEN_HALF_RECEIVED
	
}