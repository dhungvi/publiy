package org.msrg.publiy.broker.core.contentManager.servingPolicy;

import org.msrg.publiy.broker.core.contentManager.Content;

public class ContentServingPolicy_ServeFullReceived extends ContentServingPolicy {

	public ContentServingPolicy_ServeFullReceived() {
		super(ContentServingPolicyType.CONTENT_SERVING_POLICY_SEND_WHEN_FULLY_RECEIVED);
	}

	@Override
	public boolean canServe(Content content) {
		if(!super.canServe(content))
			return false;
		
		if(content.isSolved())
			return true;
		else
			return false;
	}
	
	@Override
	public String toString() {
		return super.toStringPrefix() + "[" + 1.0 + ",0]";
	}

}
