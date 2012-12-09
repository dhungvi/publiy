package org.msrg.publiy.broker.core.flowManager;

import org.msrg.publiy.broker.core.contentManager.Content;

public class LaunchFlow extends Flow {

	LaunchFlow(Content content, IAllFlows allFlows) {
		super(content, allFlows);
	}

	@Override
	public FlowPriority degradeFlowPriority() {
		if(!isActive())
			return null;
		
		return _flowPriority;
	}

	@Override
	protected void setFlowPriority(FlowPriority flowPriority) {
		super.setFlowPriority(FlowPriority.ULTIMATE);
	}
}
